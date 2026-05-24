package org.example.stockitbe.notification.model.entity;

// 알림 종류 (총 4종)
//  - INVENTORY_SHORTAGE       : 재고 부족
//  - INVENTORY_OUT_OF_STOCK   : 재고 소진
//  - USER_SIGNUP_PENDING      : 회원가입 승인 대기
//  - CIRCULAR_CANDIDATE       : 순환 재고 후보
//
// 2026-05-22 폐기:
//  - ESG_QUOTA_WARNING / ESG_QUOTA_EXCEEDED — 탄소 배출 한도 알림 (탄소 배출 관리 기능 폐기에 따른 정리)
public enum NotificationType {
    INVENTORY_SHORTAGE,
    INVENTORY_OUT_OF_STOCK,
    USER_SIGNUP_PENDING,
    CIRCULAR_CANDIDATE
}
