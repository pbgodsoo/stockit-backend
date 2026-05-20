package org.example.stockitbe.hq.inventory.model;

public interface CircularInventoryPageRow {
    Long getInventoryId();
    String getSkuCode();
    String getItemCode();
    String getItemName();
    String getWarehouseCode();
    String getWarehouseName();
    String getParentCategory();
    String getChildCategory();
    String getColor();
    String getSize();
    Integer getAvailableQuantity();
}

