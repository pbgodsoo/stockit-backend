#!/usr/bin/env python3
"""
geocode miss 테이블 CSV export 도구.

기본 대상:
- circular_buyer_geocode_miss

환경변수:
- DB_URL / DB_USER / DB_PASS
"""

from __future__ import annotations

import argparse
import csv
import os
from datetime import datetime
from urllib.parse import urlparse

import pymysql


def parse_db_url(db_url: str) -> tuple[str, int, str]:
    if db_url.startswith("jdbc:"):
        db_url = db_url[5:]
    parsed = urlparse(db_url)
    host = parsed.hostname or "127.0.0.1"
    port = parsed.port or 3306
    database = parsed.path.lstrip("/")
    if not database:
        raise ValueError("DB_URL 에 database 이름이 없습니다.")
    return host, port, database


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Export geocode miss rows to CSV")
    parser.add_argument("--db-url", default=os.getenv("DB_URL", ""), help="override DB_URL")
    parser.add_argument("--db-user", default=os.getenv("DB_USER", ""), help="override DB_USER")
    parser.add_argument("--db-pass", default=os.getenv("DB_PASS", ""), help="override DB_PASS")
    parser.add_argument("--table", default="circular_buyer_geocode_miss", help="miss table name")
    parser.add_argument("--limit", type=int, default=0, help="row limit (0 means all)")
    parser.add_argument(
        "--out",
        default="",
        help="output csv path (default: tools/out/<table>_YYYYmmdd_HHMMSS.csv)",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if not args.db_url or not args.db_user:
        raise SystemExit("DB_URL/DB_USER 가 필요합니다.")

    host, port, database = parse_db_url(args.db_url)
    out_path = args.out
    if not out_path:
        ts = datetime.now().strftime("%Y%m%d_%H%M%S")
        out_dir = os.path.join(os.path.dirname(__file__), "out")
        os.makedirs(out_dir, exist_ok=True)
        out_path = os.path.join(out_dir, f"{args.table}_{ts}.csv")

    conn = pymysql.connect(
        host=host,
        port=port,
        user=args.db_user,
        password=args.db_pass,
        database=database,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )
    try:
        limit_sql = f" LIMIT {args.limit}" if args.limit > 0 else ""
        query = f"""
            SELECT id, original_address, last_query, candidate_count, miss_reason, update_date
            FROM {args.table}
            ORDER BY id ASC
            {limit_sql}
        """
        with conn.cursor() as cur:
            cur.execute(query)
            rows = cur.fetchall()

        with open(out_path, "w", newline="", encoding="utf-8-sig") as fp:
            writer = csv.writer(fp)
            writer.writerow(
                ["id", "original_address", "last_query", "candidate_count", "miss_reason", "update_date"]
            )
            for row in rows:
                writer.writerow(
                    [
                        row.get("id"),
                        row.get("original_address"),
                        row.get("last_query"),
                        row.get("candidate_count"),
                        row.get("miss_reason"),
                        row.get("update_date"),
                    ]
                )
        print(f"exported rows={len(rows)} -> {out_path}")
        return 0
    finally:
        conn.close()


if __name__ == "__main__":
    raise SystemExit(main())
