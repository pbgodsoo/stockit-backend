#!/usr/bin/env python3
"""
Update only circular_buyer.description and circular_buyer.embedding_description
from db_factory_new.csv.

CSV mapping:
  E column / "설명" -> embedding_description
  J column / "자연스러운 설명" -> description

The script never deletes or inserts circular_buyer rows. By default it runs in
dry-run mode and only validates the CSV/DB counts and prints samples.
"""

from __future__ import annotations

import argparse
import csv
import os
import subprocess
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path


DEFAULT_CSV = "/Users/pbgodsoo/Desktop/Desktop - Beomsoo's MacBook Pro/BYOND SW CAMP/final/db_factory_new.csv"
REQUIRED_HEADERS = ["설명", "자연스러운 설명"]
STAGING_TABLE = "staging_circular_buyer_description_update"


@dataclass(frozen=True)
class UpdateRow:
    source_row_no: int
    code: str
    description: str
    embedding_description: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Update only circular_buyer description columns from CSV."
    )
    parser.add_argument("--csv", default=DEFAULT_CSV, help=f"CSV path (default: {DEFAULT_CSV})")
    parser.add_argument("--encoding", default="utf-8-sig", help="CSV encoding (default: utf-8-sig)")
    parser.add_argument("--apply", action="store_true", help="Actually update DB. Omit for dry-run.")
    parser.add_argument("--db-host", default="127.0.0.1", help="MySQL/MariaDB host")
    parser.add_argument("--db-port", type=int, default=3306, help="MySQL/MariaDB port")
    parser.add_argument("--db-name", required=True, help="Database name")
    parser.add_argument("--db-user", required=True, help="Database user")
    parser.add_argument("--db-password", default="", help="Database password")
    parser.add_argument(
        "--driver",
        choices=("auto", "pymysql", "mysql-cli"),
        default="auto",
        help="DB driver. auto uses pymysql if installed, otherwise mysql CLI.",
    )
    parser.add_argument(
        "--expected-count",
        type=int,
        default=0,
        help="Optional guard. If set, CSV row count and DB row count must match this value.",
    )
    parser.add_argument(
        "--sample-size",
        type=int,
        default=3,
        help="Number of CSV rows to print for inspection in dry-run/apply logs.",
    )
    return parser.parse_args()


def read_rows(csv_path: Path, encoding: str) -> list[UpdateRow]:
    rows: list[UpdateRow] = []
    with csv_path.open("r", encoding=encoding, newline="") as fp:
        reader = csv.DictReader(fp)
        validate_headers(reader.fieldnames)
        for index, raw in enumerate(reader, start=1):
            embedding_description = clean(raw.get("설명"))
            description = clean(raw.get("자연스러운 설명"))
            if not embedding_description:
                raise ValueError(f"CSV {index + 1}행: '설명' 값이 비어 있습니다.")
            if not description:
                raise ValueError(f"CSV {index + 1}행: '자연스러운 설명' 값이 비어 있습니다.")
            rows.append(
                UpdateRow(
                    source_row_no=index + 1,
                    code=build_code(index),
                    description=description,
                    embedding_description=embedding_description,
                )
            )
    return rows


def validate_headers(fieldnames: list[str] | None) -> None:
    if not fieldnames:
        raise ValueError("CSV header 가 없습니다.")
    missing = [header for header in REQUIRED_HEADERS if header not in fieldnames]
    if missing:
        raise ValueError(f"CSV header 누락: {', '.join(missing)}")


def clean(value: object) -> str:
    return str(value or "").strip()


def build_code(index_from_1: int) -> str:
    return f"RCV-{index_from_1:05d}"


def connect(args: argparse.Namespace):
    try:
        import pymysql  # type: ignore
    except ImportError as exc:
        raise RuntimeError("pymysql 이 필요합니다. `pip install pymysql` 후 재실행하세요.") from exc

    return pymysql.connect(
        host=args.db_host,
        port=args.db_port,
        user=args.db_user,
        password=args.db_password,
        database=args.db_name,
        charset="utf8mb4",
        autocommit=False,
    )


def mysql_base_cmd(args: argparse.Namespace) -> list[str]:
    cmd = [
        "mysql",
        "--batch",
        "--raw",
        "--skip-column-names",
        "--default-character-set=utf8mb4",
        "--host",
        args.db_host,
        "--port",
        str(args.db_port),
        "--user",
        args.db_user,
        f"--password={args.db_password}",
        args.db_name,
    ]
    return cmd


def run_mysql_cli_query(args: argparse.Namespace, sql: str) -> str:
    result = subprocess.run(
        mysql_base_cmd(args) + ["--execute", sql],
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or "mysql CLI query failed")
    return result.stdout.strip()


def validate_db_state_mysql_cli(args: argparse.Namespace, rows: list[UpdateRow], expected_count: int) -> None:
    db_count = int(run_mysql_cli_query(args, "SELECT COUNT(*) FROM circular_buyer"))
    csv_count = len(rows)
    if expected_count and (csv_count != expected_count or db_count != expected_count):
        raise RuntimeError(
            f"expected-count 불일치: expected={expected_count}, csv={csv_count}, db={db_count}"
        )
    if csv_count != db_count:
        raise RuntimeError(f"CSV/DB row count 불일치: csv={csv_count}, db={db_count}")

    code_range = run_mysql_cli_query(args, "SELECT MIN(code), MAX(code) FROM circular_buyer")
    min_code, max_code = code_range.split("\t", 1)
    expected_min = rows[0].code if rows else ""
    expected_max = rows[-1].code if rows else ""
    if min_code != expected_min or max_code != expected_max:
        raise RuntimeError(
            "circular_buyer code 범위가 CSV 행번호 기반 code와 다릅니다. "
            f"db=({min_code}, {max_code}), csv=({expected_min}, {expected_max})"
        )


def apply_update_mysql_cli(args: argparse.Namespace, rows: list[UpdateRow]) -> int:
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", suffix=".sql", delete=False) as fp:
        sql_path = fp.name
        fp.write("SET NAMES utf8mb4;\n")
        fp.write("START TRANSACTION;\n")
        fp.write(f"DROP TEMPORARY TABLE IF EXISTS {STAGING_TABLE};\n")
        fp.write(
            f"""
CREATE TEMPORARY TABLE {STAGING_TABLE} (
    code VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY,
    description TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    embedding_description TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
"""
        )

        chunk_size = 300
        for start in range(0, len(rows), chunk_size):
            chunk = rows[start : start + chunk_size]
            fp.write(
                f"INSERT INTO {STAGING_TABLE} "
                "(code, description, embedding_description) VALUES\n"
            )
            values = [
                f"({sql_string(row.code)}, {sql_string(row.description)}, {sql_string(row.embedding_description)})"
                for row in chunk
            ]
            fp.write(",\n".join(values))
            fp.write(";\n")

        fp.write(
            f"""
SET @missing_count := (
    SELECT COUNT(*)
    FROM {STAGING_TABLE} s
    LEFT JOIN circular_buyer cb ON cb.code = s.code
    WHERE cb.code IS NULL
);

UPDATE circular_buyer cb
    JOIN {STAGING_TABLE} s ON s.code = cb.code
SET cb.description = s.description,
    cb.embedding_description = s.embedding_description,
    cb.update_date = NOW()
WHERE @missing_count = 0;

SELECT ROW_COUNT(), @missing_count;
COMMIT;
"""
        )

    try:
        with open(sql_path, "r", encoding="utf-8") as fp:
            result = subprocess.run(
                mysql_base_cmd(args),
                stdin=fp,
                text=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                check=False,
            )
        if result.returncode != 0:
            raise RuntimeError(result.stderr.strip() or "mysql CLI apply failed")
        lines = [line.strip() for line in result.stdout.splitlines() if line.strip()]
        if not lines:
            raise RuntimeError("mysql CLI apply 결과를 확인할 수 없습니다.")
        updated_raw, missing_raw = lines[-1].split("\t", 1)
        missing = int(missing_raw)
        if missing:
            raise RuntimeError(f"DB에 없는 code 가 staging 에 있습니다: missing_count={missing}")
        return int(updated_raw)
    finally:
        try:
            os.remove(sql_path)
        except OSError:
            pass


def sql_string(value: str) -> str:
    escaped = (
        value.replace("\\", "\\\\")
        .replace("\0", "\\0")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
        .replace("'", "''")
    )
    return f"'{escaped}'"


def validate_db_state(conn, rows: list[UpdateRow], expected_count: int) -> None:
    db_count = fetch_one_int(conn, "SELECT COUNT(*) FROM circular_buyer")
    csv_count = len(rows)
    if expected_count and (csv_count != expected_count or db_count != expected_count):
        raise RuntimeError(
            f"expected-count 불일치: expected={expected_count}, csv={csv_count}, db={db_count}"
        )
    if csv_count != db_count:
        raise RuntimeError(f"CSV/DB row count 불일치: csv={csv_count}, db={db_count}")

    min_code, max_code = fetch_one_tuple(conn, "SELECT MIN(code), MAX(code) FROM circular_buyer")
    expected_min = rows[0].code if rows else None
    expected_max = rows[-1].code if rows else None
    if min_code != expected_min or max_code != expected_max:
        raise RuntimeError(
            "circular_buyer code 범위가 CSV 행번호 기반 code와 다릅니다. "
            f"db=({min_code}, {max_code}), csv=({expected_min}, {expected_max})"
        )


def fetch_one_int(conn, sql: str) -> int:
    with conn.cursor() as cur:
        cur.execute(sql)
        row = cur.fetchone()
    return int(row[0])


def fetch_one_tuple(conn, sql: str) -> tuple[object, ...]:
    with conn.cursor() as cur:
        cur.execute(sql)
        row = cur.fetchone()
    return tuple(row)


def apply_update(conn, rows: list[UpdateRow]) -> int:
    with conn.cursor() as cur:
        cur.execute(f"DROP TEMPORARY TABLE IF EXISTS {STAGING_TABLE}")
        cur.execute(
            f"""
            CREATE TEMPORARY TABLE {STAGING_TABLE} (
                code VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY,
                description TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
                embedding_description TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
        )
        cur.executemany(
            f"""
            INSERT INTO {STAGING_TABLE} (code, description, embedding_description)
            VALUES (%s, %s, %s)
            """,
            [(row.code, row.description, row.embedding_description) for row in rows],
        )

        missing = find_missing_codes(cur)
        if missing:
            raise RuntimeError(f"DB에 없는 code 가 staging 에 있습니다: {', '.join(missing[:10])}")

        cur.execute(
            f"""
            UPDATE circular_buyer cb
            JOIN {STAGING_TABLE} s ON s.code = cb.code
            SET cb.description = s.description,
                cb.embedding_description = s.embedding_description,
                cb.update_date = NOW()
            """
        )
        affected = cur.rowcount
    conn.commit()
    return affected


def find_missing_codes(cur) -> list[str]:
    cur.execute(
        f"""
        SELECT s.code
        FROM {STAGING_TABLE} s
        LEFT JOIN circular_buyer cb ON cb.code = s.code
        WHERE cb.code IS NULL
        ORDER BY s.code
        LIMIT 10
        """
    )
    return [row[0] for row in cur.fetchall()]


def print_summary(rows: list[UpdateRow], sample_size: int) -> None:
    print(f"csv_rows={len(rows)}")
    print(f"first_code={rows[0].code if rows else ''}")
    print(f"last_code={rows[-1].code if rows else ''}")
    for row in rows[: max(sample_size, 0)]:
        print("--- sample ---")
        print(f"source_row_no={row.source_row_no}")
        print(f"code={row.code}")
        print(f"description={row.description[:160]}")
        print(f"embedding_description={row.embedding_description[:160]}")


def resolve_driver(requested: str) -> str:
    if requested != "auto":
        return requested
    try:
        import pymysql  # noqa: F401

        return "pymysql"
    except ImportError:
        return "mysql-cli"


def main() -> int:
    args = parse_args()
    csv_path = Path(args.csv).expanduser()
    if not csv_path.exists():
        print(f"[ERROR] CSV 파일을 찾을 수 없습니다: {csv_path}", file=sys.stderr)
        return 1

    try:
        rows = read_rows(csv_path, args.encoding)
        if not rows:
            raise ValueError("CSV 데이터 행이 없습니다.")
        print_summary(rows, args.sample_size)

        driver = resolve_driver(args.driver)
        print(f"driver={driver}")

        if driver == "mysql-cli":
            validate_db_state_mysql_cli(args, rows, args.expected_count)
            print("validation=OK")
            if not args.apply:
                print("apply_status=SKIPPED (--apply 미지정)")
                return 0

            affected = apply_update_mysql_cli(args, rows)
            print(f"apply_status=SUCCESS")
            print(f"updated_rows={affected}")
            return 0

        conn = connect(args)
        try:
            validate_db_state(conn, rows, args.expected_count)
            print("validation=OK")
            if not args.apply:
                print("apply_status=SKIPPED (--apply 미지정)")
                conn.rollback()
                return 0

            affected = apply_update(conn, rows)
            print("apply_status=SUCCESS")
            print(f"updated_rows={affected}")
            return 0
        finally:
            conn.close()
    except Exception as exc:
        print(f"[ERROR] {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
