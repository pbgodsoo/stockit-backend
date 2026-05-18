#!/usr/bin/env python3
"""
카카오 주소 지오코딩 배치.

대상:
- circular_buyer(address -> latitude, longitude)
- infrastructure(address -> latitude, longitude)

환경변수:
- KAKAO_REST_API_KEY (필수)
- DB_URL / DB_USER / DB_PASS (Spring dev 설정과 동일하게 사용)
  예) DB_URL=jdbc:mariadb://127.0.0.1:3306/stockit
"""

from __future__ import annotations

import argparse
import os
import re
import sys
import time
from dataclasses import dataclass
from urllib.parse import urlparse

import pymysql
import requests

KAKAO_URL = "https://dapi.kakao.com/v2/local/search/address.json"


@dataclass
class DbConfig:
    host: str
    port: int
    user: str
    password: str
    database: str


def clean_basic(address: str) -> str:
    if not address:
        return ""
    out = address.strip()
    out = out.replace("（", "(").replace("）", ")")
    out = re.sub(r"\([^)]*\)", "", out)
    out = out.split(",")[0]
    out = re.sub(r"[)\]}>]+$", "", out)  # 꼬리 특수문자 제거
    out = re.sub(r"\s*-\s*", "-", out)   # 168 -9 -> 168-9
    out = re.sub(r"\s+", " ", out).strip()
    return out


def normalize_admin_spacing(address: str) -> str:
    """
    붙어있는 행정구역 토큰을 분리한다.
    예) 천안시서북구 -> 천안시 서북구, 안산시단원구초지동 -> 안산시 단원구 초지동
    """
    out = address
    patterns = [
        # 광역/특별 + 기초 (서울특별시중구, 경기도안양시 ...)
        (r"([가-힣]+(?:특별자치시|특별자치도|특별시|광역시|시|도))([가-힣]+(?:시|군|구))", r"\1 \2"),
        # 기초 + 하위 (안양시만안구, 단원구초지동, 군위군효령면 ...)
        (r"([가-힣]+(?:시|군|구))([가-힣0-9]+(?:군|구|읍|면|동|리))", r"\1 \2"),
    ]
    prev = None
    while prev != out:
        prev = out
        for pattern, repl in patterns:
            out = re.sub(pattern, repl, out)
    out = re.sub(r"\s+", " ", out).strip()
    return out


def normalize_je_dong_notation(address: str) -> str:
    """
    구식 법정동 표기 보정.
    예) 방화제동 -> 방화동, 중화제3동 -> 중화3동
    """
    out = re.sub(r"([가-힣]+)제(\d+)동\b", r"\1\2동", address)
    out = re.sub(r"([가-힣]+)제동\b", r"\1동", out)
    out = re.sub(r"\s+", " ", out).strip()
    return out


def strip_trailing_business_name(address: str) -> str:
    """
    도로명+건물번호 뒤에 붙은 상호 꼬리를 제거한 후보를 만든다.
    예) 해안로 259 신명전기 -> 해안로 259
    """
    out = re.sub(r"(\d+(?:-\d+)?)\s+[A-Za-z가-힣][A-Za-z0-9가-힣().-]*\s*$", r"\1", address)
    out = re.sub(r"\s+", " ", out).strip()
    return out


def trim_detail_tokens(address: str) -> str:
    out = address
    # 외 N필지 제거
    out = re.sub(r"\s*외\s*\d+\s*필지.*$", "", out)
    # 건물 동/층/호 상세 제거 (A동, B동, 101동, 3층, 1409호 등)
    # 주의: 지번 행정동(예: 가산동)은 제거하면 안 되므로 "숫자/영문 + 동"만 제거
    out = re.sub(r"\s+[A-Za-z]\s*동\b.*$", "", out)
    out = re.sub(r"\s+\d+(?:-\d+)?동\b.*$", "", out)
    out = re.sub(r"\s+\d+\s*층.*$", "", out)
    out = re.sub(r"\s+\d+\s*호.*$", "", out)
    # 번지 표기는 카카오에서 없어도 잘 찾는 경우가 많아 후보를 넓히기 위해 제거본 생성
    out = re.sub(r"번지", "", out)
    out = re.sub(r"\s+", " ", out).strip()
    return out


def normalize_spacing_road(address: str) -> str:
    out = address
    # 붙어있는 도로명 숫자/길 토큰 보정: 장한로27가길 -> 장한로 27가길
    out = re.sub(r"([로길])(\d)", r"\1 \2", out)
    # 번길 앞 숫자 붙음 보정: 247번길 -> 247번길 (공백 제거 방향으로 통일)
    out = re.sub(r"(\d)\s+번길", r"\1번길", out)
    out = re.sub(r"\s+", " ", out).strip()
    return out


def normalize_collapse_bunggil(address: str) -> str:
    """'42 번길' → '42번길': 카카오 API 는 번길 앞 공백 없는 형식을 선호."""
    out = re.sub(r"(\d)\s+번길", r"\1번길", address)
    out = re.sub(r"\s+", " ", out).strip()
    return out


def strip_trailing_building_number(address: str) -> str:
    """도로명 뒤 건물번호(예: '1-18', '39') 제거 — 도로명만 남겨 후보 확장."""
    out = re.sub(r"\s+\d+(?:-\d+)?\s*$", "", address)
    out = re.sub(r"\s+", " ", out).strip()
    return out


def normalize_dong_number(address: str) -> str:
    """청천2동 → 청천동: 숫자 행정동은 카카오가 모르는 경우가 있어 번호 제거 시도."""
    out = re.sub(r"([가-힣]+)\d+(동)\b", r"\1\2", address)
    out = re.sub(r"\s+", " ", out).strip()
    return out


def truncate_to_eupmon(address: str) -> str:
    """읍/면 레벨까지만 남김 — 리(里) 단위 지번이 카카오에 없을 때 fallback.
    세종특별자치시처럼 시/도 바로 아래 읍/면이 오는 구조도 지원."""
    # 일반: 시/도 > 시/군/구 > 읍/면
    m = re.match(r"^(.+?(?:시|도)\s+.+?(?:시|군|구)\s+\S+?(?:읍|면))\b", address)
    if m:
        return m.group(1).strip()
    # 세종 등 군/구 없이 시/도 > 읍/면 구조
    m2 = re.match(r"^(.+?(?:시|도)\s+\S+?(?:읍|면))\b", address)
    if m2:
        return m2.group(1).strip()
    return ""


def build_query_candidates(address: str) -> list[str]:
    base = clean_basic(address)
    if not base:
        return []
    base = normalize_admin_spacing(base)
    base = normalize_je_dong_notation(base)

    candidates: list[str] = []
    base_col = normalize_collapse_bunggil(base)
    trimmed = trim_detail_tokens(base)
    trimmed_col = normalize_collapse_bunggil(trimmed)

    variants = [
        # 번길 공백 제거 버전을 앞에 배치 — MISS 주원인이므로 우선 시도
        base_col,
        base,
        trimmed_col,
        trimmed,
        normalize_spacing_road(base_col),
        normalize_spacing_road(base),
        normalize_spacing_road(trimmed_col),
        normalize_spacing_road(trimmed),
        # 건물번호 제거 버전 (도로명 레벨)
        strip_trailing_building_number(base_col),
        strip_trailing_building_number(base),
        # 상호 꼬리 제거 버전
        strip_trailing_business_name(base_col),
        strip_trailing_business_name(base),
        strip_trailing_business_name(trimmed_col),
        strip_trailing_business_name(trimmed),
    ]

    # 숫자 행정동 정규화 (청천2동 → 청천동)
    dong_norm = normalize_dong_number(base)
    if dong_norm != base:
        variants.append(dong_norm)
        variants.append(strip_trailing_building_number(dong_norm))

    # 읍/면 레벨 fallback — 리 단위 지번이 카카오 DB에 없는 농촌 필지 대응
    eupmon = truncate_to_eupmon(base)
    if eupmon:
        variants.append(eupmon)

    # 지번형 축약: 시/군/구 + 읍/면/동 + 번지(산 포함)까지만
    m = re.match(
        r"^(.+?(?:시|도)\s+.+?(?:시|군|구)\s+.+?(?:읍|면|동)\s+(?:산\s*)?\d+(?:-\d+)?)",
        base,
    )
    if m:
        variants.append(m.group(1))
        variants.append(normalize_spacing_road(m.group(1)))

    seen: set[str] = set()
    for v in variants:
        v = re.sub(r"\s+", " ", (v or "").strip())
        if not v or v in seen:
            continue
        seen.add(v)
        candidates.append(v)
    return candidates


def extract_region_tokens(address: str) -> list[str]:
    source = clean_basic(address)
    source = normalize_admin_spacing(source)
    source = normalize_je_dong_notation(source)
    tokens = source.split()
    regions: list[str] = []
    for token in tokens:
        if token.endswith(("시", "도", "군", "구")):
            regions.append(token)
        if len(regions) >= 3:
            break
    return regions


# 카카오 API 가 공식 도명 대신 약칭을 반환하는 경우 매핑
# 예: 충청북도 -> 충북, 경상남도 -> 경남
_DO_ABBR: dict[str, str] = {
    "충청북도": "충북",
    "충청남도": "충남",
    "경상북도": "경북",
    "경상남도": "경남",
    "전라북도": "전북",
    "전라남도": "전남",
    "전북특별자치도": "전북",
    "강원특별자치도": "강원",
    "강원도": "강원",
}
_DO_ABBR_REV: dict[str, str] = {v: k for k, v in _DO_ABBR.items()}


def normalize_region_token(token: str) -> str:
    out = token.strip()
    # 광역/특별/자치 표기 차이 흡수
    out = out.replace("특별자치시", "시")
    out = out.replace("특별자치도", "도")
    out = out.replace("특별시", "시")
    out = out.replace("광역시", "시")
    return out


def region_aliases(token: str) -> set[str]:
    norm = normalize_region_token(token)
    aliases = {norm}
    # 끝 접미 제거한 축약도 허용 (경기도 -> 경기, 대구시 -> 대구)
    for suffix in ("시", "도", "군", "구"):
        if norm.endswith(suffix) and len(norm) > 1:
            aliases.add(norm[: -len(suffix)])
    # 도 약칭 추가 (충청북도 -> 충북)
    if norm in _DO_ABBR:
        aliases.add(_DO_ABBR[norm])
    # 역방향: 약칭이 입력된 경우 원형도 alias 에 포함 (충북 -> 충청북도, 충청북)
    if norm in _DO_ABBR_REV:
        full = _DO_ABBR_REV[norm]
        aliases.add(full)
        for suffix in ("시", "도", "군", "구"):
            if full.endswith(suffix) and len(full) > 1:
                aliases.add(full[: -len(suffix)])
    return aliases


def is_region_match(raw_address: str, result_address: str) -> bool:
    expected = extract_region_tokens(raw_address)
    if not expected:
        return True
    normalized_result = normalize_region_token(result_address)
    normalized_result = normalize_admin_spacing(normalized_result)

    # 행정구역 개편 예외(과거 주소 -> 현재 행정구역 표기)
    if "군위군" in raw_address:
        normalized_result = normalized_result.replace("대구", "경북")
    if "연기군" in raw_address:
        normalized_result = normalized_result.replace("세종", "충남")

    # 표기 정규화 + 축약(경기도/경기) 별칭 중 하나라도 포함되면 매칭
    for token in expected:
        aliases = region_aliases(token)
        if not any(alias in normalized_result for alias in aliases):
            return False
    return True


def parse_db_url(db_url: str) -> tuple[str, int, str]:
    # jdbc:mariadb://127.0.0.1:3306/stockit
    if db_url.startswith("jdbc:"):
        db_url = db_url[5:]
    parsed = urlparse(db_url)
    host = parsed.hostname or "127.0.0.1"
    port = parsed.port or 3306
    database = parsed.path.lstrip("/")
    if not database:
        raise ValueError("DB_URL 에 database 이름이 없습니다.")
    return host, port, database


def load_db_config(args: argparse.Namespace) -> DbConfig:
    db_url = args.db_url or os.getenv("DB_URL", "")
    db_user = args.db_user or os.getenv("DB_USER", "")
    db_pass = args.db_pass or os.getenv("DB_PASS", "")
    if not db_url or not db_user:
        raise ValueError("DB_URL/DB_USER 가 필요합니다. (DB_PASS 는 비어도 됨)")
    host, port, database = parse_db_url(db_url)
    return DbConfig(host=host, port=port, user=db_user, password=db_pass, database=database)


def geocode_kakao(rest_api_key: str, query: str) -> tuple[float | None, float | None, str]:
    headers = {"Authorization": f"KakaoAK {rest_api_key}"}
    response = requests.get(KAKAO_URL, headers=headers, params={"query": query}, timeout=10)
    response.raise_for_status()
    payload = response.json()
    docs = payload.get("documents", [])
    if not docs:
        return None, None, ""
    # 카카오 응답: x=경도(lng), y=위도(lat)
    doc = docs[0]
    lng = float(doc["x"])
    lat = float(doc["y"])
    addr_name = ""
    road = doc.get("road_address") or {}
    if road.get("address_name"):
        addr_name = str(road.get("address_name"))
    elif doc.get("address", {}).get("address_name"):
        addr_name = str(doc.get("address", {}).get("address_name"))
    return lat, lng, addr_name


def escape_sql(value: str) -> str:
    return value.replace("'", "''")


def process_table(
    conn: pymysql.Connection,
    rest_api_key: str,
    table: str,
    pk_col: str,
    addr_col: str,
    sleep_sec: float,
    limit: int,
    only_miss: bool,
    max_consecutive_api_errors: int,
) -> tuple[int, int, int, int]:
    miss_table = f"{table}_geocode_miss"
    with conn.cursor() as cur:
        cur.execute(
            f"""
            CREATE TABLE IF NOT EXISTS {miss_table} (
              id BIGINT PRIMARY KEY,
              original_address VARCHAR(256) NOT NULL,
              last_query VARCHAR(256) NULL,
              candidate_count INT NOT NULL DEFAULT 0,
              miss_reason VARCHAR(32) NOT NULL,
              update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """
        )
        conn.commit()

    where = [f"{addr_col} IS NOT NULL", f"TRIM({addr_col}) <> ''", "(latitude IS NULL OR longitude IS NULL)"]
    if only_miss:
        where.append(f"{pk_col} IN (SELECT id FROM {miss_table})")
    where_sql = " AND ".join(where)
    limit_sql = f" LIMIT {limit}" if limit > 0 else ""

    with conn.cursor() as cur:
        cur.execute(
            f"""
            SELECT {pk_col} AS id, {addr_col} AS address
            FROM {table}
            WHERE {where_sql}
            ORDER BY {pk_col} ASC
            {limit_sql}
            """
        )
        rows = cur.fetchall()

    success = 0
    miss = 0
    fail = 0
    mismatch = 0
    consecutive_api_errors = 0

    for row in rows:
        row_id = row["id"]
        raw_address = str(row["address"])
        candidates = build_query_candidates(raw_address)
        if not candidates:
            miss += 1
            with conn.cursor() as cur:
                cur.execute(
                    f"""
                    REPLACE INTO {miss_table}(id, original_address, last_query, candidate_count, miss_reason)
                    VALUES(%s, %s, %s, %s, %s)
                    """,
                    (row_id, raw_address, "", 0, "EMPTY_CANDIDATE"),
                )
            conn.commit()
            continue

        resolved = False
        last_query = candidates[-1]
        saw_mismatch = False
        mismatch_result_address = ""

        try:
            for query in candidates:
                last_query = query
                lat, lng, result_address = geocode_kakao(rest_api_key, query)
                if lat is None or lng is None:
                    continue
                if not is_region_match(raw_address, result_address):
                    saw_mismatch = True
                    mismatch_result_address = result_address
                    continue
                with conn.cursor() as cur:
                    cur.execute(
                        f"UPDATE {table} SET latitude=%s, longitude=%s WHERE {pk_col}=%s",
                        (lat, lng, row_id),
                    )
                    cur.execute(f"DELETE FROM {miss_table} WHERE id=%s", (row_id,))
                conn.commit()
                success += 1
                resolved = True
                break
            consecutive_api_errors = 0
            time.sleep(sleep_sec)
        except Exception as exc:  # noqa: BLE001
            fail += 1
            consecutive_api_errors += 1
            print(f"[FAIL] {table}:{row_id} -> {last_query} ({exc})")
            with conn.cursor() as cur:
                cur.execute(
                    f"""
                    REPLACE INTO {miss_table}(id, original_address, last_query, candidate_count, miss_reason)
                    VALUES(%s, %s, %s, %s, %s)
                    """,
                    (row_id, raw_address, last_query, len(candidates), "API_FAIL"),
                )
            conn.commit()
            if max_consecutive_api_errors > 0 and consecutive_api_errors >= max_consecutive_api_errors:
                print(
                    f"[ABORT] 연속 API 오류 {consecutive_api_errors}회 발생으로 중단합니다. "
                    f"(threshold={max_consecutive_api_errors})"
                )
                break
            continue

        if resolved:
            continue

        if saw_mismatch:
            mismatch += 1
            print(
                f"[SKIP_MISMATCH] {table}:{row_id} -> region mismatch, "
                f"candidate_count={len(candidates)}, last_query='{last_query}', "
                f"result='{mismatch_result_address}'"
            )
            reason = "REGION_MISMATCH"
        else:
            miss += 1
            print(
                f"[MISS] {table}:{row_id} -> candidate_count={len(candidates)}, "
                f"last_query='{last_query}'"
            )
            reason = "MISS"
        with conn.cursor() as cur:
            cur.execute(
                f"""
                REPLACE INTO {miss_table}(id, original_address, last_query, candidate_count, miss_reason)
                VALUES(%s, %s, %s, %s, %s)
                """,
                (row_id, raw_address, last_query, len(candidates), reason),
            )
        conn.commit()
    return success, miss, fail, mismatch


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Kakao geocoding batch updater")
    parser.add_argument("--db-url", default="", help="override DB_URL")
    parser.add_argument("--db-user", default="", help="override DB_USER")
    parser.add_argument("--db-pass", default="", help="override DB_PASS")
    parser.add_argument("--sleep", type=float, default=0.12, help="seconds between API calls")
    parser.add_argument(
        "--table",
        choices=["all", "circular_buyer", "infrastructure"],
        default="all",
        help="target table",
    )
    parser.add_argument("--limit", type=int, default=0, help="max rows per table. 0 means no limit")
    parser.add_argument(
        "--only-miss",
        action="store_true",
        help="process only rows that are recorded in <table>_geocode_miss",
    )
    parser.add_argument(
        "--max-consecutive-api-errors",
        type=int,
        default=5,
        help="abort when this many API exceptions happen consecutively (0 disables)",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    rest_api_key = os.getenv("KAKAO_REST_API_KEY", "").strip()
    if not rest_api_key:
        print("KAKAO_REST_API_KEY 가 필요합니다.")
        return 1

    try:
        db = load_db_config(args)
    except Exception as exc:  # noqa: BLE001
        print(f"DB 설정 오류: {exc}")
        return 1

    conn = pymysql.connect(
        host=db.host,
        port=db.port,
        user=db.user,
        password=db.password,
        database=db.database,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=False,
    )

    total = {
        "circular_buyer": {"success": 0, "miss": 0, "fail": 0, "mismatch": 0},
        "infrastructure": {"success": 0, "miss": 0, "fail": 0, "mismatch": 0},
    }
    targets = (
        ["circular_buyer", "infrastructure"]
        if args.table == "all"
        else [args.table]
    )

    try:
        for target in targets:
            ok, miss, fail, mismatch = process_table(
                conn=conn,
                rest_api_key=rest_api_key,
                table=target,
                pk_col="id",
                addr_col="address",
                sleep_sec=args.sleep,
                limit=args.limit,
                only_miss=args.only_miss,
                max_consecutive_api_errors=args.max_consecutive_api_errors,
            )
            total[target]["success"] = ok
            total[target]["miss"] = miss
            total[target]["fail"] = fail
            total[target]["mismatch"] = mismatch
    except KeyboardInterrupt:
        print("\n[INTERRUPTED] 수동 중단됨. 현재까지 집계:")
    finally:
        conn.close()

    for target in ["circular_buyer", "infrastructure"]:
        if args.table != "all" and target != args.table:
            continue
        print(
            f"[{target}] success={total[target]['success']}, "
            f"miss={total[target]['miss']}, fail={total[target]['fail']}, "
            f"mismatch={total[target]['mismatch']}"
        )
    return 0


if __name__ == "__main__":
    sys.exit(main())
