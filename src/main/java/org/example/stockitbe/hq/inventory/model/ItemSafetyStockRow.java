package org.example.stockitbe.hq.inventory.model;

// 전사 재고 페이지 후 itemCode 별 safetyStock 집계 native @Query 결과 매핑용 interface projection.
// (sku, location) DISTINCT 별로 location_type 에 따라 store/warehouse safety_stock 을 합산한 값.
public interface ItemSafetyStockRow {
    String getItemCode();
    Integer getSafetyStock();
}
