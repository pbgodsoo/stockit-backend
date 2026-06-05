package org.example.stockitbe.hq.purchaseorder.model;

public interface SkuRowProjection {
    String getVendorCode();
    String getVendorName();
    String getVendorProductCode();
    String getProductCode();
    String getProductName();
    String getSkuCode();
    String getColor();
    String getSize();
    String getDisplayOption();
    Long getUnitPrice();
    Long getContractUnitPrice();
    Long getAvailableQty();
    Long getWarehouseSafetyStock();
}
