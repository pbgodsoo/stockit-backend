package org.example.stockitbe.store.inventory.model;

// 매장 재고(품목 단위) 집계 페이지네이션 native @Query 결과 매핑용 interface projection
public interface StoreItemRow {
    String getItemCode();
    String getItemName();
    String getParentCategory();
    String getChildCategory();
    Integer getActualStock();
    Integer getAvailableStock();
    Integer getSafetyStock();
    String getStatus();
}
