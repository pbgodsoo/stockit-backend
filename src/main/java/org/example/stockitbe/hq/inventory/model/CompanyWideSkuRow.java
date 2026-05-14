package org.example.stockitbe.hq.inventory.model;

// 전사 재고 SKU 단위 페이지네이션 native @Query 결과 매핑용 interface projection.
// status 라벨은 SQL CASE 로 계산 (창고재고 패턴과 동일 — 페이징 정확성 보장).
public interface CompanyWideSkuRow {
    String getSkuCode();
    String getItemCode();
    String getItemName();
    String getParentCategory();
    String getChildCategory();
    String getColor();
    String getSize();
    Integer getActualStock();
    Integer getAvailableStock();
    Integer getSafetyStock();
    String getStatus();
}
