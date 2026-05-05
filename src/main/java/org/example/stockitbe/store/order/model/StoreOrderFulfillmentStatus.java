package org.example.stockitbe.store.order.model;

public enum StoreOrderFulfillmentStatus {
    READY_TO_SHIP,  // 배송 준비 중
    IN_TRANSIT,     // 배송 중
    ARRIVED,        // 매장 도착
    RECEIVED        // 입고 완료
}

