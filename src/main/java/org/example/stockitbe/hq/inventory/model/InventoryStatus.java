package org.example.stockitbe.hq.inventory.model;

public enum InventoryStatus {
    // 일반 판매/출고/입고에 사용되는 기본 재고 상태
    NORMAL,
    // 순환재고 후보 상태
    CIRCULAR_CANDIDATE,
    // 순환재고 확정 상태
    CIRCULAR
}
