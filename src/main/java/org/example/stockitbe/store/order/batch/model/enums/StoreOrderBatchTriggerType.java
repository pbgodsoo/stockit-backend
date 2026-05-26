package org.example.stockitbe.store.order.batch.model.enums;

public enum StoreOrderBatchTriggerType {
    // 매일 자정 스케줄러가 전일 발주를 일괄 승인
    MIDNIGHT,
    // 본사 관리자가 HTTP API를 통해 직접 실행
    MANUAL
}
