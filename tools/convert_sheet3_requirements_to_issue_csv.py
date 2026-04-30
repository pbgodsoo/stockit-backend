#!/usr/bin/env python3
"""
Convert the provided requirements-definition CSV format into the issue-import CSV
consumed by tools/create_github_issues_from_csv.py.
"""

from __future__ import annotations

import argparse
import csv
from collections import OrderedDict
from pathlib import Path


OUTPUT_HEADERS = [
    "type",
    "title",
    "summary",
    "requirement",
    "todo",
    "labels",
    "assignees",
    "background",
    "expected_effect",
    "improvement_type",
    "bug",
    "scenario",
    "expected_result",
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Convert Sheet3-style requirements CSV to issue-import CSV."
    )
    parser.add_argument("--input", required=True, help="Source CSV path.")
    parser.add_argument("--output", required=True, help="Converted CSV path.")
    return parser.parse_args()


def clean_bullet_line(line: str) -> str:
    return line.strip().strip('"').lstrip("·•・-").strip().strip('"')


def split_lines(value: str) -> list[str]:
    return [clean_bullet_line(line) for line in value.splitlines() if clean_bullet_line(line)]


def dedupe_preserve_order(items: list[str]) -> list[str]:
    return list(OrderedDict.fromkeys(items))


def normalize_middle(middle: str) -> str:
    return middle.strip() or "미분류"


def main() -> int:
    args = parse_args()
    input_path = Path(args.input)
    output_path = Path(args.output)

    with input_path.open("r", encoding="utf-8-sig", newline="") as source_file:
        rows = list(csv.reader(source_file))

    current_major = ""
    current_middle = ""
    grouped_rows: OrderedDict[tuple[str, str], dict[str, list[str] | str]] = OrderedDict()

    for row in rows[8:]:
        if len(row) < 10:
            continue

        requirement_id = row[1].strip()
        if not requirement_id:
            continue

        if row[2].strip():
            current_major = row[2].strip()
        if row[3].strip():
            current_middle = row[3].strip()

        subcategory = row[4].strip()
        role = row[5].strip()
        requirement_text = row[6].strip()
        detail_text = row[7].strip()
        _owner = row[8].strip()
        _priority = row[9].strip()

        major = current_major.strip() or "미분류"
        middle = normalize_middle(current_middle)
        group_key = (major, middle)
        if group_key not in grouped_rows:
            grouped_rows[group_key] = {
                "requirement_ids": [],
                "todo_lines": [],
            }
        bucket = grouped_rows[group_key]
        bucket["requirement_ids"].append(requirement_id)

        todos: list[str] = []
        if requirement_text:
            todos.extend(split_lines(requirement_text))
        if detail_text:
            todos.extend(split_lines(detail_text))
        item_title = f"[{requirement_id}] {subcategory}" if subcategory else f"[{requirement_id}]"
        bucket["todo_lines"].append(item_title)
        bucket["todo_lines"].extend(todos)
        if role:
            bucket["todo_lines"].append(f"권한: {role}")

    converted_rows: list[dict[str, str]] = []
    for (major, middle), bucket in grouped_rows.items():
        requirement_ids = bucket["requirement_ids"]
        todo_lines = dedupe_preserve_order(bucket["todo_lines"])

        converted_rows.append(
            {
                "type": "feat",
                "title": f"[{major}] {middle}",
                "summary": f"{major} > {middle} 기능 묶음 ({len(requirement_ids)}건)",
                "requirement": f"{major} | {middle} | {', '.join(requirement_ids)}",
                "todo": "\n".join(todo_lines),
                "labels": "기능",
                "assignees": "",
                "background": "",
                "expected_effect": "",
                "improvement_type": "",
                "bug": "",
                "scenario": "",
                "expected_result": "",
            }
        )

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8-sig", newline="") as output_file:
        writer = csv.DictWriter(output_file, fieldnames=OUTPUT_HEADERS)
        writer.writeheader()
        writer.writerows(converted_rows)

    print(f"Converted {len(converted_rows)} rows to {output_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
