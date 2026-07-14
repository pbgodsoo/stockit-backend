package org.example.stockitbe.hq.inventory.model;

import java.util.Date;

// 전사 재고(SKU 단위) MariaDB fallback native @Query 결과 매핑용 projection
public interface CompanyWideSkuInventoryRow {
    String getSkuCode();
    String getItemCode();
    String getItemName();
    String getParentCategory();
    String getChildCategory();
    String getColor();
    String getSize();
    Long getUnitPrice();
    Integer getActualStock();
    Integer getAvailableStock();
    Integer getSafetyStock();
    String getStatus();
    Date getUpdatedAt();
}
