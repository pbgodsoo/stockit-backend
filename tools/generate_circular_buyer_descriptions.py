#!/usr/bin/env python3
"""
Generate recommendation-oriented circular buyer descriptions from public CSV data.

The script intentionally uses only Python's standard library so it can run in
restricted environments without pandas. It rewrites only the "설명" column and
keeps every other column/value as-is.
"""

from __future__ import annotations

import argparse
import csv
import re
from pathlib import Path


REQUIRED_COLUMNS = ["생산품", "주소", "설명", "소재분류", "산업군"]

DEFAULT_INPUT = "/Users/pbgodsoo/Downloads/factory_ai_input.csv"
DEFAULT_OUTPUT = "factory_ai_output.csv"


INDUSTRY_DEFAULT_KEYWORDS = {
    "의복 제조업": ["봉제 의류", "의류 제품", "유니폼"],
    "섬유 제품 제조업": ["섬유 제품", "원단류", "섬유 소재"],
    "가죽/가방/신발 제조업": ["피혁 제품", "잡화류", "봉제 제품"],
    "플라스틱 제조업": ["플라스틱 제품", "합성수지류", "포장재"],
    "자동차 부품 제조업": ["자동차 부품", "수송기계 부품", "내장재 부품"],
    "가구 제조업": ["가구류", "목재 제품", "인테리어 제품"],
    "건축자제 제조업": ["건축 자재", "건설용 자재", "산업용 자재"],
}

INDUSTRY_DEFAULT_CRITERIA = {
    "의복 제조업": ["봉제 의류 잔재고", "의류 제품 잉여 재고"],
    "섬유 제품 제조업": ["원단류 잔재고", "섬유 소재 잉여 재고"],
    "가죽/가방/신발 제조업": ["피혁 제품 잔여 재고", "잡화류 잉여 재고"],
    "플라스틱 제조업": ["플라스틱 제품 잉여 재고", "합성수지류 잔여 재고"],
    "자동차 부품 제조업": ["자동차 부품류 잉여 재고", "내장재 부품 잔여 재고"],
    "가구 제조업": ["가구 제품 잉여 재고", "목재 제품 잔여 재고"],
    "건축자제 제조업": ["건축 자재 잉여 재고", "건설용 자재 잔여 재고"],
}

MATERIAL_CRITERIA = {
    "natural-single": ["천연 단일 소재 재고", "면·린넨·울 계열 잔재고"],
    "synthetic": ["합성 소재 재고", "폴리에스터·나일론·PET 계열 잔재고"],
    "blended": ["혼방 소재 재고", "복합 소재 섬유 잔재고"],
}

# Keyword and matching rules are ordered deliberately. Do not convert them to a
# set; deterministic ordering keeps embedding input reproducible.
PRODUCT_RULES = [
    {
        "contains": ["기능성 티셔츠"],
        "keywords": ["기능성 의류", "티셔츠류", "상의류", "봉제 의류"],
        "criteria": ["기능성 의류 재고", "티셔츠류 재고", "상의류 잔재고"],
    },
    {
        "contains": ["반팔 티셔츠"],
        "keywords": ["티셔츠류", "반팔 의류", "상의류", "봉제 의류"],
        "criteria": ["티셔츠류 재고", "반팔 의류 재고", "상의류 잔재고"],
    },
    {
        "contains": ["린넨 셔츠"],
        "keywords": ["셔츠류", "상의류", "린넨 의류", "봉제 의류"],
        "criteria": ["셔츠류 재고", "상의류 잔재고", "린넨 계열 의류 재고"],
    },
    {
        "contains": ["긴팔 셔츠"],
        "keywords": ["셔츠류", "긴팔 의류", "상의류", "봉제 의류"],
        "criteria": ["셔츠류 재고", "긴팔 의류 재고", "상의류 잔재고"],
    },
    {
        "contains": ["작업용 점퍼"],
        "keywords": ["점퍼류", "작업복류", "아우터류", "봉제 의류"],
        "criteria": ["점퍼류 재고", "작업복류 재고", "아우터류 잔재고"],
    },
    {
        "contains": ["면바지"],
        "keywords": ["바지류", "하의류", "면 의류", "봉제 의류"],
        "criteria": ["바지류 재고", "하의류 잔재고", "면 계열 의류 재고"],
    },
    {
        "contains": ["단체복"],
        "keywords": ["단체복류", "유니폼", "작업복류", "봉제 의류"],
        "criteria": ["단체복류 재고", "유니폼 재고", "작업복류 잔재고"],
    },
    {
        "contains": ["특수 직물"],
        "keywords": ["특수 원단", "직물류", "섬유 소재", "기능성 원단"],
        "criteria": ["특수 원단 재고", "직물류 잔재고", "섬유 소재 잉여 재고"],
    },
    {
        "contains": ["혼방 원단"],
        "keywords": ["혼방 원단", "직물류", "섬유 소재", "복합 소재 원단"],
        "criteria": ["혼방 원단 재고", "복합 소재 섬유 재고", "직물류 잔재고"],
    },
    {
        "contains": ["면 직물"],
        "keywords": ["면 직물", "천연 원단", "직물류", "섬유 소재"],
        "criteria": ["면 직물 재고", "천연 원단 잔재고", "직물류 재고"],
    },
    {
        "contains": ["니트 편조물"],
        "keywords": ["니트류", "편조물", "섬유 제품", "원단류"],
        "criteria": ["니트류 잔재고", "편조물 재고", "섬유 제품 잉여 재고"],
    },
    {
        "contains": ["부직포"],
        "keywords": ["부직포", "산업용 원단", "섬유 소재", "기능성 소재"],
        "criteria": ["부직포 재고", "산업용 원단 잔재고", "섬유 소재 잉여 재고"],
    },
    {
        "contains": ["나일론 실"],
        "keywords": ["나일론 원사", "합성 섬유", "실류", "섬유 소재"],
        "criteria": ["나일론 계열 재고", "실류 잔재고", "합성 섬유 소재 재고"],
    },
    {
        "contains": ["재생 폴리에스터 원사"],
        "keywords": ["재생 원사", "폴리에스터 원사", "합성 섬유", "섬유 소재"],
        "criteria": ["폴리에스터 계열 재고", "원사류 잔재고", "합성 섬유 소재 재고"],
    },
    {
        "contains": ["PET 칩"],
        "keywords": ["PET 소재", "플라스틱 원료", "합성수지류", "재생 플라스틱"],
        "criteria": ["PET 계열 재고", "플라스틱 원료 잔여 재고", "합성수지류 재고"],
    },
    {
        "contains": ["재생 펠릿"],
        "keywords": ["재생 펠릿", "플라스틱 원료", "합성수지류", "재생 플라스틱"],
        "criteria": ["재생 펠릿 재고", "플라스틱 원료 잔여 재고", "합성수지류 재고"],
    },
    {
        "contains": ["포장용 필름"],
        "keywords": ["포장재", "필름류", "플라스틱 필름", "합성수지 제품"],
        "criteria": ["포장재 잉여 재고", "필름류 잔여 재고", "플라스틱 필름 재고"],
    },
    {
        "contains": ["플라스틱 팔레트"],
        "keywords": ["플라스틱 팔레트", "물류 자재", "플라스틱 제품", "산업용 자재"],
        "criteria": ["플라스틱 팔레트 재고", "물류 자재 잉여 재고", "플라스틱 제품 재고"],
    },
    {
        "contains": ["플라스틱 용기"],
        "keywords": ["플라스틱 용기", "포장 용기", "플라스틱 제품", "포장재"],
        "criteria": ["플라스틱 용기 재고", "포장 용기 잔여 재고", "플라스틱 제품 재고"],
    },
    {
        "contains": ["산업용 케이스"],
        "keywords": ["산업용 케이스", "플라스틱 케이스", "산업용 자재", "보관 용기"],
        "criteria": ["산업용 케이스 재고", "플라스틱 케이스 잔여 재고", "산업용 자재 재고"],
    },
    {
        "contains": ["범퍼 가드"],
        "keywords": ["범퍼 부품", "자동차 외장 부품", "보호 부품", "수송기계 부품"],
        "criteria": ["범퍼 부품 잉여 재고", "자동차 외장 부품 재고", "보호 부품 잔여 재고"],
    },
    {
        "contains": ["자동차 도어 트림"],
        "keywords": ["도어 트림", "자동차 내장재", "수송기계 부품", "내장재 부품"],
        "criteria": ["도어 트림 재고", "자동차 내장재 잔여 재고", "수송기계 부품 재고"],
    },
    {
        "contains": ["대시보드 부품"],
        "keywords": ["대시보드 부품", "자동차 내장재", "수송기계 부품", "내장재 부품"],
        "criteria": ["대시보드 부품 재고", "자동차 내장재 잔여 재고", "수송기계 부품 재고"],
    },
    {
        "contains": ["헤드라이트 케이스"],
        "keywords": ["헤드라이트 케이스", "자동차 외장 부품", "플라스틱 부품", "수송기계 부품"],
        "criteria": ["헤드라이트 케이스 재고", "자동차 외장 부품 재고", "플라스틱 부품 잔여 재고"],
    },
    {
        "contains": ["자동차용 클립"],
        "keywords": ["자동차용 클립", "체결 부품", "소형 부품", "수송기계 부품"],
        "criteria": ["자동차용 클립 재고", "체결 부품 잔여 재고", "소형 부품 재고"],
    },
    {
        "contains": ["흡음재"],
        "keywords": ["흡음재", "자동차 내장재", "완충 소재", "산업용 소재"],
        "criteria": ["흡음재 재고", "완충 소재 잔여 재고", "자동차 내장재 재고"],
    },
    {
        "contains": ["여성용 핸드백", "가죽 가방"],
        "keywords": ["가방류", "피혁 제품", "잡화류", "봉제 제품"],
        "criteria": ["가방류 재고", "피혁 제품 잔여 재고", "잡화류 잉여 재고"],
    },
    {
        "contains": ["가죽 구두", "스포츠화", "스니커즈"],
        "keywords": ["제화류", "신발류", "피혁 제품", "잡화류"],
        "criteria": ["제화류 재고", "신발류 잔여 재고", "피혁 제품 재고"],
    },
    {
        "contains": ["가죽 벨트"],
        "keywords": ["벨트류", "피혁 제품", "잡화류", "가죽 부자재"],
        "criteria": ["벨트류 재고", "피혁 제품 잔여 재고", "가죽 부자재 재고"],
    },
    {
        "contains": ["산업용 부품"],
        "keywords": ["산업용 부품", "기계 부품", "제조 부품", "산업용품"],
        "criteria": ["산업용 부품 잉여 재고", "기계 부품 잔여 재고", "제조 부품 재고"],
    },
    {
        "contains": ["표준 규격품"],
        "keywords": ["표준 규격품", "산업용품", "표준 자재", "제조 자재"],
        "criteria": ["표준 규격품 잉여 재고", "표준 자재 잔여 재고", "산업용품 재고"],
    },
    {
        "contains": ["범용 자재"],
        "keywords": ["범용 자재", "산업용 자재", "제조 자재", "공통 자재"],
        "criteria": ["범용 자재 잉여 재고", "산업용 자재 잔여 재고", "제조 자재 재고"],
    },
    {
        "contains": ["소모품"],
        "keywords": ["소모품", "산업용 소모품", "제조 소모품", "공정 자재"],
        "criteria": ["소모품 잉여 재고", "산업용 소모품 재고", "공정 자재 잔여 재고"],
    },
    {
        "contains": ["맞춤형 제조물"],
        "keywords": ["맞춤형 제조물", "주문 제작품", "산업용품", "제조 자재"],
        "criteria": ["맞춤형 제조물 잉여 재고", "주문 제작품 잔여 재고", "산업용품 재고"],
    },
]


def add_unique(items: list[str], values: list[str] | tuple[str, ...]) -> None:
    for value in values:
        value = normalize_space(value)
        if value and value not in items:
            items.append(value)


def normalize_space(value: object) -> str:
    return re.sub(r"\s+", " ", str(value or "")).strip()


def read_csv(path: Path) -> tuple[list[str], list[dict[str, str]], str]:
    last_error: Exception | None = None
    for encoding in ("utf-8-sig", "utf-8", "cp949", "euc-kr"):
        try:
            with path.open("r", encoding=encoding, newline="") as f:
                reader = csv.DictReader(f)
                rows = list(reader)
                if not reader.fieldnames:
                    raise ValueError("CSV header not found")
                return reader.fieldnames, rows, encoding
        except UnicodeDecodeError as e:
            last_error = e
    raise RuntimeError(f"CSV encoding detection failed: {last_error}")


def validate_columns(fieldnames: list[str]) -> None:
    missing = [col for col in REQUIRED_COLUMNS if col not in fieldnames]
    if missing:
        raise ValueError(f"Missing required columns: {', '.join(missing)}")


def get_region(address: str) -> str:
    address = normalize_space(address)
    if not address:
        return "지역 미확인"

    parts = address.split()
    if not parts:
        return "지역 미확인"
    if len(parts) == 1:
        return parts[0]

    # Recommendation distance is handled separately, so avoid full address text.
    # City/province + city/county/district is enough for a public-data profile.
    return f"{parts[0]} {parts[1]}"


def split_products(product_text: str) -> list[str]:
    product_text = normalize_space(product_text)
    if not product_text:
        return []
    parts = re.split(r"[,/·|;\n]+", product_text)
    cleaned: list[str] = []
    for part in parts:
        item = normalize_space(part)
        item = item.replace(" 제조업", "").replace("제조업", "")
        item = item.replace(" 제조", "").replace("제조", "")
        if item and item not in cleaned:
            cleaned.append(item)
    return cleaned


def get_product_keywords(product_text: str, industry: str, limit: int = 6) -> str:
    keywords: list[str] = []
    normalized_products = split_products(product_text)
    joined = " ".join(normalized_products)

    for rule in PRODUCT_RULES:
        if any(token in joined for token in rule["contains"]):
            add_unique(keywords, rule["keywords"])

    add_unique(keywords, INDUSTRY_DEFAULT_KEYWORDS.get(normalize_space(industry), []))

    # Add short, clean product names only after generalized keywords. This keeps
    # embeddings from being dominated by noisy public-data product labels.
    for product in normalized_products:
        if 2 <= len(product) <= 12 and product not in {"등", "기타"}:
            add_unique(keywords, [product])

    return ", ".join(keywords[:limit]) if keywords else "기타 제품군"


def get_matching_criteria(material: str, product_text: str, industry: str, limit: int = 6) -> str:
    product_criteria: list[str] = []
    material = normalize_space(material)
    industry = normalize_space(industry)
    normalized_products = split_products(product_text)
    joined = " ".join(normalized_products)

    for rule in PRODUCT_RULES:
        if any(token in joined for token in rule["contains"]):
            add_unique(product_criteria, rule["criteria"])

    criteria: list[str] = []
    add_unique(criteria, product_criteria[:4])
    add_unique(criteria, MATERIAL_CRITERIA.get(material, [])[:2])
    add_unique(criteria, INDUSTRY_DEFAULT_CRITERIA.get(industry, []))
    add_unique(criteria, product_criteria[4:])

    return ", ".join(criteria[:limit]) if criteria else "기타 잔여 재고"


def build_description(row: dict[str, str]) -> str:
    region = get_region(row.get("주소", ""))
    industry = normalize_space(row.get("산업군", "")) or "산업군 미확인"
    product = normalize_space(row.get("생산품", "")) or "공개 데이터상 생산품 미확인"
    product_keywords = get_product_keywords(product, industry)
    matching_criteria = get_matching_criteria(row.get("소재분류", ""), product, industry)

    return (
        f"공개 데이터 기준 정보: {region} 소재 {industry} 사업장. "
        f"주요 생산품: {product}. "
        f"제품군 키워드: {product_keywords}. "
        f"순환재고 매칭 기준: {matching_criteria}."
    )


def write_csv(path: Path, fieldnames: list[str], rows: list[dict[str, str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Generate public-data-based recommendation descriptions for circular buyers."
    )
    parser.add_argument(
        "input",
        nargs="?",
        default=DEFAULT_INPUT,
        help=f"input CSV path (default: {DEFAULT_INPUT})",
    )
    parser.add_argument(
        "-o",
        "--output",
        default=DEFAULT_OUTPUT,
        help=f"output CSV path (default: {DEFAULT_OUTPUT})",
    )
    parser.add_argument(
        "--sample",
        type=int,
        default=0,
        help="write only the first N rows for inspection",
    )
    args = parser.parse_args()

    input_path = Path(args.input).expanduser()
    output_path = Path(args.output).expanduser()

    fieldnames, rows, encoding = read_csv(input_path)
    validate_columns(fieldnames)

    target_rows = rows[: args.sample] if args.sample and args.sample > 0 else rows
    for row in target_rows:
        row["설명"] = build_description(row)

    write_csv(output_path, fieldnames, target_rows)
    print(
        f"완료: {len(target_rows):,}행 설명 생성 "
        f"(input_encoding={encoding}, output={output_path})"
    )


if __name__ == "__main__":
    main()
