package org.example.stockitbe.hq.inventory.model;

public interface CircularInventoryCompositionRow {
    String getItemCode();
    String getMaterialCode();
    String getMaterialNameKo();
    String getMaterialGroup();
    Integer getRatio();
    Integer getCompositionOrder();
}

