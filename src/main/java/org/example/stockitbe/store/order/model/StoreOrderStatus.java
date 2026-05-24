package org.example.stockitbe.store.order.model;

public enum StoreOrderStatus {
    REQUESTED,  // 승인 대기
    APPROVED,   // 승인 완료
    COMPLETED,  // 완료 (입고까지 완료 후 해당 발주 건 종료)
    CANCELLED   // 취소
}

