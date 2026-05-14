package org.example.stockitbe.warehouse.inventory.model;

// 창고 재고 SKU 단위 페이지네이션 native @Query 결과 매핑용 interface projection
public interface WarehouseSkuRow {
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
