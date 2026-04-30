#!/usr/bin/env python3
"""
Create GitHub issues from a CSV requirements document.

Example:
    export GITHUB_TOKEN=ghp_xxx
    python3 tools/create_github_issues_from_csv.py \
        --csv docs/requirements/issues.csv \
        --repo beyond-sw-camp/be24-fin-Stockers-STOCKIT-BE \
        --dry-run
"""

from __future__ import annotations

import argparse
import csv
import json
import os
import time
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass


DEFAULT_LABELS = {
    "feat": "기능",
    "feature": "기능",
    "refactor": "리팩토링",
    "fix": "오류 해결",
    "bug": "오류 해결",
}


@dataclass
class IssueDraft:
    title: str
    body: str
    labels: list[str]
    assignees: list[str]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Create GitHub issues from a CSV file."
    )
    parser.add_argument("--csv", required=True, help="Path to the input CSV file.")
    parser.add_argument(
        "--repo",
        required=True,
        help="GitHub repository in the form owner/repo.",
    )
    parser.add_argument(
        "--token",
        default=os.getenv("GITHUB_TOKEN"),
        help="GitHub token. Defaults to GITHUB_TOKEN environment variable.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print issue drafts without creating them.",
    )
    parser.add_argument(
        "--encoding",
        default="utf-8-sig",
        help="CSV encoding. Defaults to utf-8-sig.",
    )
    parser.add_argument(
        "--start-index",
        type=int,
        default=1,
        help="1-based CSV row index to start creating issues from.",
    )
    parser.add_argument(
        "--delay-seconds",
        type=float,
        default=1.5,
        help="Delay between issue creations to reduce GitHub secondary rate limits.",
    )
    parser.add_argument(
        "--max-retries",
        type=int,
        default=5,
        help="Maximum retries for secondary rate limit errors.",
    )
    return parser.parse_args()


def require_value(row: dict[str, str], *keys: str) -> str:
    for key in keys:
        value = row.get(key, "").strip()
        if value:
            return value
    raise ValueError(f"Missing required column value. Expected one of: {', '.join(keys)}")


def split_multiline(value: str) -> list[str]:
    items = []
    for raw in value.replace("\\n", "\n").splitlines():
        item = raw.strip().lstrip("-").strip()
        if item:
            items.append(item)
    return items


def list_block(items: list[str], checkbox: bool = False) -> str:
    if not items:
        return "- "
    prefix = "- [ ] " if checkbox else "- "
    return "\n".join(f"{prefix}{item}" for item in items)


def normalize_type(raw_type: str) -> str:
    issue_type = raw_type.strip().lower()
    if issue_type in {"feature"}:
        return "feat"
    if issue_type in {"bug"}:
        return "fix"
    return issue_type


def build_issue_body(row: dict[str, str]) -> IssueDraft:
    issue_type = normalize_type(require_value(row, "type", "issue_type"))
    title = require_value(row, "title")
    summary = row.get("summary", "").strip() or row.get("description", "").strip()
    requirement = require_value(row, "requirement", "requirement_id", "requirement_name")
    todos = split_multiline(row.get("todo", ""))
    labels = split_csv_items(row.get("labels", ""))
    assignees = split_csv_items(row.get("assignees", ""))

    if not labels and issue_type in DEFAULT_LABELS:
        labels = [DEFAULT_LABELS[issue_type]]

    if issue_type == "feat":
        body = f"""## 📝 작업 내용
> 수행해야 할 작업 혹은 구현할 기능에 대해 간략히 설명해주세요.
{list_block([summary] if summary else [])}

<br><br>

## ✔️ 관련 기능
> 어떤 기능과 관련되어 있는 작업인가요? (요구사항 명세서)
{list_block([requirement])}

<br><br>

## 🛠 세부 할 일 (To-Do)
{list_block(todos, checkbox=True)}

<br><br>
"""
        issue_title = f"feat: {title}"
    elif issue_type == "refactor":
        background = row.get("background", "").strip()
        expected = split_multiline(row.get("expected_effect", ""))
        improvement_flags = split_multiline(row.get("improvement_type", ""))
        if not improvement_flags:
            improvement_flags = ["기존 기능 개선"]

        body = f"""## 📝 작업 내용
> 리팩토링 할 내용에 대해 간략히 설명해주세요. (+ 개선 종류 체크)
{list_block(improvement_flags, checkbox=True)}
{f"- {summary}" if summary else "- "}

<br><br>

## ✔️ 관련 기능
> 어떤 기능과 관련되어 있는 작업인가요? (요구사항 명세서)
{list_block([requirement])}

<br><br>

## ❓ 리팩토링을 하게 된 이유/배경은 무엇인가요?
> 어떤 상황에서 문제가 발생했는지, 왜 리팩토링이 필요하게 되었는지 설명해주세요
{list_block([background] if background else [])}

<br><br>

## 🙆‍♀️ 리팩토링/성능개선 기대 효과

> 리팩토링 후 예상하는 결과가 어떤 것인지 설명해주세요
{list_block(expected)}

<br><br>

## 🛠 세부 할 일 (To-Do)
{list_block(todos, checkbox=True)}

<br><br>
"""
        issue_title = f"refactor: {title}"
    elif issue_type == "fix":
        bug = row.get("bug", "").strip() or summary
        scenario = row.get("scenario", "").strip()
        expected_result = row.get("expected_result", "").strip()

        body = f"""## 🐞어떤 버그인가요?

> 어떤 버그/오류 인지 간결하게 설명해주세요
{list_block([bug] if bug else [])}

<br><br>

## ❓어떤 상황에서 발생한 버그인가요?

> (가능하면) Given-When-Then 형식으로 서술해주세요
{list_block([scenario] if scenario else [])}

<br><br>

## 🙆‍♀️예상 결과

> 예상했던 정상적인 결과가 어떤 것이었는지 설명해주세요
{list_block([expected_result] if expected_result else [])}

<br><br>
"""
        issue_title = f"fix: {title}"
    else:
        raise ValueError(
            f"Unsupported issue type '{issue_type}'. Use feat, refactor, or fix."
        )

    return IssueDraft(
        title=issue_title,
        body=body,
        labels=labels,
        assignees=assignees,
    )


def split_csv_items(value: str) -> list[str]:
    return [item.strip() for item in value.split(",") if item.strip()]


def load_issue_drafts(csv_path: str, encoding: str) -> list[IssueDraft]:
    with open(csv_path, "r", encoding=encoding, newline="") as csv_file:
        reader = csv.DictReader(csv_file)
        if not reader.fieldnames:
            raise ValueError("CSV header is missing.")
        drafts = [build_issue_body(row) for row in reader]

    if not drafts:
        raise ValueError("CSV does not contain any data rows.")
    return drafts


def create_issue(repo: str, token: str, draft: IssueDraft) -> str:
    payload = {
        "title": draft.title,
        "body": draft.body,
        "labels": draft.labels,
        "assignees": draft.assignees,
    }

    request = urllib.request.Request(
        url=f"https://api.github.com/repos/{repo}/issues",
        data=json.dumps(payload).encode("utf-8"),
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "stockit-issue-bulk-creator",
        },
        method="POST",
    )

    with urllib.request.urlopen(request) as response:
        data = json.loads(response.read().decode("utf-8"))
        return data["html_url"]


def is_secondary_rate_limit(error: urllib.error.HTTPError, details: str) -> bool:
    return error.code == 403 and "secondary rate limit" in details.lower()


def create_issue_with_retry(
    repo: str,
    token: str,
    draft: IssueDraft,
    max_retries: int,
) -> str:
    attempt = 0
    wait_seconds = 30

    while True:
        try:
            return create_issue(repo, token, draft)
        except urllib.error.HTTPError as error:
            details = error.read().decode("utf-8", errors="replace")
            if not is_secondary_rate_limit(error, details) or attempt >= max_retries:
                raise RuntimeError(f"{error.code} {details}") from error

            attempt += 1
            print(
                f"Secondary rate limit hit for '{draft.title}'. "
                f"Retrying in {wait_seconds:.0f}s "
                f"(attempt {attempt}/{max_retries})...",
                file=sys.stderr,
            )
            time.sleep(wait_seconds)
            wait_seconds *= 2


def print_draft(index: int, draft: IssueDraft) -> None:
    print(f"[{index}] {draft.title}")
    if draft.labels:
        print(f"labels: {', '.join(draft.labels)}")
    if draft.assignees:
        print(f"assignees: {', '.join(draft.assignees)}")
    print(draft.body)
    print("=" * 80)


def main() -> int:
    args = parse_args()

    try:
        drafts = load_issue_drafts(args.csv, args.encoding)
    except Exception as error:  # noqa: BLE001
        print(f"Failed to read CSV: {error}", file=sys.stderr)
        return 1

    if args.start_index < 1 or args.start_index > len(drafts):
        print(
            f"--start-index must be between 1 and {len(drafts)}.",
            file=sys.stderr,
        )
        return 1

    if args.dry_run:
        for index, draft in enumerate(drafts, start=1):
            if index < args.start_index:
                continue
            print_draft(index, draft)
        return 0

    if not args.token:
        print(
            "GitHub token is required. Set GITHUB_TOKEN or pass --token.",
            file=sys.stderr,
        )
        return 1

    for index, draft in enumerate(drafts, start=1):
        if index < args.start_index:
            continue

        try:
            issue_url = create_issue_with_retry(
                args.repo,
                args.token,
                draft,
                args.max_retries,
            )
            print(f"[{index}] Created: {issue_url}")
            if args.delay_seconds > 0:
                time.sleep(args.delay_seconds)
        except urllib.error.HTTPError as error:
            details = error.read().decode("utf-8", errors="replace")
            print(
                f"[{index}] Failed to create issue '{draft.title}': {error.code} {details}",
                file=sys.stderr,
            )
            return 1
        except Exception as error:  # noqa: BLE001
            print(
                f"[{index}] Failed to create issue '{draft.title}': {error}",
                file=sys.stderr,
            )
            return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
