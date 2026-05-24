package org.example.stockitbe.hq.purchaseorder.model;

/**
 * 본사 발주 상태머신 — 7단계 단일 enum.
 *
 * 흐름: REQUESTED → APPROVED → READY_TO_SHIP → IN_TRANSIT → ARRIVED → COMPLETED
 *      └─(REQUESTED 에서만)─→ CANCELLED
 *
 * 책임 분리:
 *  - REQUESTED: 본사 관리자가 발주 작성 (인증 사용자)
 *  - APPROVED / READY_TO_SHIP / IN_TRANSIT / ARRIVED: SYS-001 배치 자동 전환 (거래처 책임 단계, 30분 경과)
 *  - COMPLETED: 창고 [입고 확정] 매뉴얼 (인증 사용자)
 *  - CANCELLED: 본사 [취소] 매뉴얼 — REQUESTED 단계에서만 가능
 *
 * 라벨 분기 (FE view 책임):
 *  - 본사 화면: COMPLETED = "종료"
 *  - 창고 화면: COMPLETED = "입고 완료"
 *  - 그 외 상태는 권한군 무관 단일 라벨
 *
 * 인벤토리 hook (PR #173):
 *  - IN_TRANSIT 진입 시 → 가용재고 +
 *  - COMPLETED 진입 시 → 가용 → 실재고 이동
 */
public enum PurchaseOrderStatus {
    REQUESTED,
    APPROVED,
    READY_TO_SHIP,
    IN_TRANSIT,
    ARRIVED,
    COMPLETED,
    CANCELLED
}
