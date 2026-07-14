package org.example.stockitbe.hq.inventory.model;

import java.util.Date;

// 전사 재고(품목 단위) MariaDB fallback native @Query 결과 매핑용 projection
public interface CompanyWideInventoryRow {
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
