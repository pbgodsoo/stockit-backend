package org.example.stockitbe.hq.inventory.model;

import java.util.Date;

// 전사 재고(품목 단위) 집계 페이지네이션 native @Query 결과 매핑용 interface projection.
// safetyStock 은 별도 native query (ItemSafetyStockRow) 로 한 번에 집계.
public interface CompanyWideAggregateRow {
    String getItemCode();
    String getItemName();
    String getParentCategory();
    String getChildCategory();
    Integer getActualStock();
    Integer getAvailableStock();
    Date getUpdatedAt();
}
