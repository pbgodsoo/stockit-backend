#!/usr/bin/env python3
"""
circular_buyer CSV 대량 적재 도구.

기능:
1) CSV 검증/정규화
2) valid/reject 분리 + 리포트 생성
3) (옵션) MySQL staging/reject 테이블 적재 + 본 테이블 전체 교체

예시:
  python3 tools/load_circular_buyer_from_csv.py \
    --csv /path/to/circular_buyer.csv \
    --out-dir /tmp/circular-buyer-load \
    --run-id 20260515_230000

  python3 tools/load_circular_buyer_from_csv.py \
    --csv /path/to/circular_buyer.csv \
    --out-dir /tmp/circular-buyer-load \
    --run-id 20260515_230000 \
    --apply \
    --db-host 127.0.0.1 \
    --db-port 3306 \
    --db-name stockit \
    --db-user root \
    --db-password secret
"""

from __future__ import annotations

import argparse
import csv
import json
import os
import sys
from collections import Counter
from dataclasses import dataclass
from datetime import datetime
from typing import Any

EXPECTED_HEADERS = [
    "회사명",
    "전화번호",
    "생산품",
    "주소",
    "설명",
    "소재분류",
    "담당자",
    "산업군",
    "기업 분류",
]

REQUIRED_HEADERS = set(EXPECTED_HEADERS)
REQUIRED_FIELDS = [
    ("회사명", "company_name"),
    ("전화번호", "phone"),
    ("주소", "address"),
    ("소재분류", "primary_material_fit"),
    ("담당자", "manager_name"),
    ("산업군", "industry_group"),
    ("기업 분류", "partner_type"),
]

MATERIAL_FIT_MAP = {
    "natural-single": "natural-single",
    "synthetic": "synthetic",
    "blended": "blended",
    "천연 단일 섬유": "natural-single",
    "합성 섬유": "synthetic",
    "혼방": "blended",
}

PARTNER_TYPE_MAP = {
    "GENERAL": "general",
    "SMALL_BUSINESS": "local_small",
    "SOCIAL_ENTERPRISE": "social_enterprise",
    "general": "general",
    "local_small": "local_small",
    "social_enterprise": "social_enterprise",
}

CODE_NUMBER_MAX = 99_999_999


@dataclass
class ValidRow:
    source_row_no: int
    code: str
    company_name: str
    phone: str
    address: str
    description: str
    primary_material_fit: str
    manager_name: str
    industry_group: str
    partner_type: str
    factory_product_json: str


@dataclass
class RejectRow:
    source_row_no: int
    company_name: str
    phone: str
    factory_product_raw: str
    address: str
    description: str
    material_fit_raw: str
    manager_name: str
    industry_group: str
    partner_type_raw: str
    reject_reason: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Validate/normalize circular_buyer CSV and optionally load into MySQL."
    )
    parser.add_argument("--csv", required=True, help="입력 CSV 경로")
    parser.add_argument("--out-dir", required=True, help="결과 파일 출력 디렉터리")
    parser.add_argument("--run-id", default="", help="실행 식별자(미입력 시 현재시각)")
    parser.add_argument(
        "--encoding",
        default="utf-8-sig",
        help="CSV 인코딩(기본값: utf-8-sig)",
    )
    parser.add_argument(
        "--apply",
        action="store_true",
        help="검증 후 MySQL 적재/교체까지 수행",
    )
    parser.add_argument("--db-host", default="127.0.0.1", help="MySQL host")
    parser.add_argument("--db-port", type=int, default=3306, help="MySQL port")
    parser.add_argument("--db-name", default="", help="MySQL database name")
    parser.add_argument("--db-user", default="", help="MySQL user")
    parser.add_argument("--db-password", default="", help="MySQL password")
    parser.add_argument(
        "--sql-path",
        default="src/main/resources/sql/circular_buyer_csv_batch.sql",
        help="staging/swap SQL 파일 경로",
    )
    return parser.parse_args()


def non_empty(value: Any) -> str:
    return str(value or "").strip()


def normalize_factory_product(raw: str) -> list[str]:
    if not raw:
        return []
    candidates = []
    for chunk in str(raw).split(","):
        item = chunk.strip()
        if item:
            candidates.append(item)
    deduped = list(dict.fromkeys(candidates))
    return deduped


def normalize_material_fit(raw: str) -> str:
    value = non_empty(raw)
    return MATERIAL_FIT_MAP.get(value, "")


def normalize_partner_type(raw: str) -> str:
    value = non_empty(raw)
    return PARTNER_TYPE_MAP.get(value, "")


def build_code(index_from_1: int) -> str:
    return f"RCV-{index_from_1:05d}"


def validate_header(fieldnames: list[str] | None) -> None:
    if not fieldnames:
        raise ValueError("CSV header 가 없습니다.")
    missing = sorted(REQUIRED_HEADERS - set(fieldnames))
    if missing:
        raise ValueError(f"CSV header 누락: {', '.join(missing)}")


def validate_row(raw: dict[str, str], source_row_no: int, code_index: int) -> tuple[ValidRow | None, RejectRow | None]:
    reasons: list[str] = []

    company_name = non_empty(raw.get("회사명"))
    phone = non_empty(raw.get("전화번호"))
    factory_product_raw = non_empty(raw.get("생산품"))
    address = non_empty(raw.get("주소"))
    description = non_empty(raw.get("설명"))
    material_fit_raw = non_empty(raw.get("소재분류"))
    manager_name = non_empty(raw.get("담당자"))
    industry_group = non_empty(raw.get("산업군"))
    partner_type_raw = non_empty(raw.get("기업 분류"))

    for key, label in REQUIRED_FIELDS:
        if not non_empty(raw.get(key)):
            reasons.append(f"필수값 누락: {label}")

    primary_material_fit = normalize_material_fit(material_fit_raw)
    if material_fit_raw and not primary_material_fit:
        reasons.append(f"소재분류 매핑 실패: {material_fit_raw}")

    partner_type = normalize_partner_type(partner_type_raw)
    if partner_type_raw and not partner_type:
        reasons.append(f"기업 분류 매핑 실패: {partner_type_raw}")

    factory_product = normalize_factory_product(factory_product_raw)
    try:
        factory_product_json = json.dumps(factory_product, ensure_ascii=False)
        json.loads(factory_product_json)
    except json.JSONDecodeError:
        reasons.append("factory_product JSON 변환 실패")
        factory_product_json = "[]"

    if reasons:
        reject = RejectRow(
            source_row_no=source_row_no,
            company_name=company_name,
            phone=phone,
            factory_product_raw=factory_product_raw,
            address=address,
            description=description,
            material_fit_raw=material_fit_raw,
            manager_name=manager_name,
            industry_group=industry_group,
            partner_type_raw=partner_type_raw,
            reject_reason=" | ".join(reasons),
        )
        return None, reject

    valid = ValidRow(
        source_row_no=source_row_no,
        code=build_code(code_index),
        company_name=company_name,
        phone=phone,
        address=address,
        description=description,
        primary_material_fit=primary_material_fit,
        manager_name=manager_name,
        industry_group=industry_group,
        partner_type=partner_type,
        factory_product_json=factory_product_json,
    )
    return valid, None


def write_valid_csv(path: str, rows: list[ValidRow]) -> None:
    with open(path, "w", encoding="utf-8", newline="") as fp:
        writer = csv.writer(fp)
        writer.writerow(
            [
                "source_row_no",
                "code",
                "company_name",
                "phone",
                "address",
                "description",
                "primary_material_fit",
                "manager_name",
                "industry_group",
                "partner_type",
                "factory_product_json",
            ]
        )
        for row in rows:
            writer.writerow(
                [
                    row.source_row_no,
                    row.code,
                    row.company_name,
                    row.phone,
                    row.address,
                    row.description,
                    row.primary_material_fit,
                    row.manager_name,
                    row.industry_group,
                    row.partner_type,
                    row.factory_product_json,
                ]
            )


def write_reject_csv(path: str, rows: list[RejectRow]) -> None:
    with open(path, "w", encoding="utf-8", newline="") as fp:
        writer = csv.writer(fp)
        writer.writerow(
            [
                "source_row_no",
                "company_name",
                "phone",
                "factory_product_raw",
                "address",
                "description",
                "material_fit_raw",
                "manager_name",
                "industry_group",
                "partner_type_raw",
                "reject_reason",
            ]
        )
        for row in rows:
            writer.writerow(
                [
                    row.source_row_no,
                    row.company_name,
                    row.phone,
                    row.factory_product_raw,
                    row.address,
                    row.description,
                    row.material_fit_raw,
                    row.manager_name,
                    row.industry_group,
                    row.partner_type_raw,
                    row.reject_reason,
                ]
            )


def write_summary(path: str, run_id: str, total_rows: int, valid_rows: list[ValidRow], reject_rows: list[RejectRow]) -> None:
    counter = Counter()
    for row in reject_rows:
        for reason in row.reject_reason.split(" | "):
            counter[reason] += 1

    payload = {
        "run_id": run_id,
        "total_rows": total_rows,
        "success_rows": len(valid_rows),
        "reject_rows": len(reject_rows),
        "reject_reason_counts": dict(counter),
        "created_at": datetime.now().isoformat(timespec="seconds"),
    }
    with open(path, "w", encoding="utf-8") as fp:
        json.dump(payload, fp, ensure_ascii=False, indent=2)


def load_with_pymysql(
    args: argparse.Namespace,
    run_id: str,
    valid_rows: list[ValidRow],
    reject_rows: list[RejectRow],
) -> None:
    try:
        import pymysql  # type: ignore
    except ImportError as exc:
        raise RuntimeError(
            "pymysql 이 설치되어 있지 않습니다. `pip install pymysql` 후 재실행하세요."
        ) from exc

    if not args.db_name or not args.db_user:
        raise RuntimeError("--apply 사용 시 --db-name, --db-user 는 필수입니다.")

    conn = pymysql.connect(
        host=args.db_host,
        port=args.db_port,
        user=args.db_user,
        password=args.db_password,
        database=args.db_name,
        charset="utf8mb4",
        autocommit=False,
    )

    try:
        with conn.cursor() as cur:
            with open(args.sql_path, "r", encoding="utf-8") as fp:
                sql = fp.read()
            for statement in split_sql_statements(sql):
                if statement.strip():
                    cur.execute(statement)

            cur.execute("DELETE FROM staging_circular_buyer_valid WHERE run_id = %s", (run_id,))
            cur.execute("DELETE FROM staging_circular_buyer_reject WHERE run_id = %s", (run_id,))

            if valid_rows:
                cur.executemany(
                    """
                    INSERT INTO staging_circular_buyer_valid (
                        run_id, source_row_no, code, company_name, phone, address, description,
                        primary_material_fit, manager_name, industry_group, partner_type, factory_product
                    ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, CAST(%s AS JSON))
                    """,
                    [
                        (
                            run_id,
                            r.source_row_no,
                            r.code,
                            r.company_name,
                            r.phone,
                            r.address,
                            r.description,
                            r.primary_material_fit,
                            r.manager_name,
                            r.industry_group,
                            r.partner_type,
                            r.factory_product_json,
                        )
                        for r in valid_rows
                    ],
                )

            if reject_rows:
                cur.executemany(
                    """
                    INSERT INTO staging_circular_buyer_reject (
                        run_id, source_row_no, company_name, phone, factory_product_raw, address, description,
                        material_fit_raw, manager_name, industry_group, partner_type_raw, reject_reason
                    ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    """,
                    [
                        (
                            run_id,
                            r.source_row_no,
                            r.company_name,
                            r.phone,
                            r.factory_product_raw,
                            r.address,
                            r.description,
                            r.material_fit_raw,
                            r.manager_name,
                            r.industry_group,
                            r.partner_type_raw,
                            r.reject_reason,
                        )
                        for r in reject_rows
                    ],
                )

            cur.execute("SET @run_id = %s", (run_id,))
            cur.execute("START TRANSACTION")
            cur.execute("DELETE FROM circular_buyer")
            cur.execute(
                """
                INSERT INTO circular_buyer (
                    code, company_name, industry_group, factory_product, description,
                    primary_material_fit, manager_name, phone, address, partner_type,
                    embedding, create_date, update_date
                )
                SELECT
                    v.code, v.company_name, v.industry_group, v.factory_product, v.description,
                    v.primary_material_fit, v.manager_name, v.phone, v.address, v.partner_type,
                    NULL, NOW(), NOW()
                FROM staging_circular_buyer_valid v
                WHERE v.run_id = %s
                ORDER BY v.source_row_no
                """,
                (run_id,),
            )
            cur.execute("COMMIT")
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def split_sql_statements(sql: str) -> list[str]:
    statements = []
    current = []
    in_string = False
    quote_char = ""
    for ch in sql:
        if ch in ("'", '"'):
            if not in_string:
                in_string = True
                quote_char = ch
            elif quote_char == ch:
                in_string = False
                quote_char = ""
        if ch == ";" and not in_string:
            statement = "".join(current).strip()
            if statement:
                statements.append(statement)
            current = []
        else:
            current.append(ch)
    tail = "".join(current).strip()
    if tail:
        statements.append(tail)
    return statements


def main() -> int:
    args = parse_args()

    run_id = args.run_id.strip() or datetime.now().strftime("%Y%m%d_%H%M%S")
    os.makedirs(args.out_dir, exist_ok=True)

    valid_rows: list[ValidRow] = []
    reject_rows: list[RejectRow] = []
    total_rows = 0
    valid_index = 0

    try:
        with open(args.csv, "r", encoding=args.encoding, newline="") as fp:
            reader = csv.DictReader(fp)
            validate_header(reader.fieldnames)
            for i, raw in enumerate(reader, start=2):
                total_rows += 1
                valid, reject = validate_row(raw, i, valid_index + 1)
                if valid:
                    valid_rows.append(valid)
                    valid_index += 1
                else:
                    assert reject is not None
                    reject_rows.append(reject)
    except Exception as exc:
        print(f"[ERROR] CSV 처리 실패: {exc}", file=sys.stderr)
        return 1

    valid_csv = os.path.join(args.out_dir, f"{run_id}.valid.csv")
    reject_csv = os.path.join(args.out_dir, f"{run_id}.reject.csv")
    summary_json = os.path.join(args.out_dir, f"{run_id}.summary.json")

    write_valid_csv(valid_csv, valid_rows)
    write_reject_csv(reject_csv, reject_rows)
    write_summary(summary_json, run_id, total_rows, valid_rows, reject_rows)

    print(f"run_id={run_id}")
    print(f"total_rows={total_rows}")
    print(f"success_rows={len(valid_rows)}")
    print(f"reject_rows={len(reject_rows)}")
    print(f"valid_csv={valid_csv}")
    print(f"reject_csv={reject_csv}")
    print(f"summary_json={summary_json}")

    if args.apply:
        try:
            load_with_pymysql(args, run_id, valid_rows, reject_rows)
            print("apply_status=SUCCESS")
        except Exception as exc:
            print(f"[ERROR] DB 적재 실패: {exc}", file=sys.stderr)
            return 2
    else:
        print("apply_status=SKIPPED (--apply 미지정)")

    # 참고: 서비스 코드 신규 생성 상한과 동기화.
    if len(valid_rows) > CODE_NUMBER_MAX:
        print(
            f"[WARN] valid row 가 {CODE_NUMBER_MAX:,}를 초과합니다. "
            f"API 신규 등록 코드 상한(CODE_NUMBER_MAX={CODE_NUMBER_MAX:,}) "
            "확장 작업을 별도로 진행하세요."
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
