package org.example.stockitbe.warehouse.inventory.model;

import java.util.Date;

// 창고 재고(품목 단위) 집계 페이지네이션 native @Query 결과 매핑용 interface projection
public interface WarehouseAggregateRow {
    String getItemCode();
    String getItemName();
    String getParentCategory();
    String getChildCategory();
    Integer getActualStock();
    Integer getAvailableStock();
    Integer getSafetyStock();
    String getStatus();
    Date getUpdatedAt();
}
