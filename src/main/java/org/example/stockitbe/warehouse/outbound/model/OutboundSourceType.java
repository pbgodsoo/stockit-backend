package org.example.stockitbe.warehouse.outbound.model;

public enum OutboundSourceType {
    STORE_ORDER,        // 매장 발주
    WAREHOUSE_TRANSFER, // 물류창고간 재고이동
    CIRCULAR_SALE       // 순환재고 판매
}

