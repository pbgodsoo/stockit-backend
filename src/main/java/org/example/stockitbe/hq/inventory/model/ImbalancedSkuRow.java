package org.example.stockitbe.hq.inventory.model;

public interface ImbalancedSkuRow {
    String getSkuCode();
    String getItemCode();
    String getItemName();
    String getCategory();
    String getColor();
    String getSize();
    Integer getTotalOnHand();
    Integer getTotalAvailable();
    Integer getShortageWarehouseCount();
    Integer getTotalShortageQty();
}
