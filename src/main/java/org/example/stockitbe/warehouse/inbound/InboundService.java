package org.example.stockitbe.warehouse.inbound;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.hq.purchaseorder.PurchaseOrderService;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderDto;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderStatus;
import org.example.stockitbe.user.model.AuthUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * WHS-005/007/008 — 창고 관리자 입고 도메인 (thin layer).
 *
 * 별 테이블 만들지 않고 PurchaseOrder + statusHistory 단일 진실 원천 활용 (ADR-015).
 * PurchaseOrderService 메소드를 위임 호출만 한다.
 *
 * 인증 사용자의 locationCode 를 받아 자기 창고 row 만 노출 — 다른 창고 발주 직접 URL
 * 접근 시 PURCHASE_ORDER_NOT_FOUND 으로 차단.
 */
@Service
@RequiredArgsConstructor
public class InboundService {

    private final PurchaseOrderService purchaseOrderService;
    private final InfrastructureRepository infrastructureRepository;

    /**
     * 입고 목록 — 창고 관심사는 READY_TO_SHIP/IN_TRANSIT/ARRIVED/COMPLETED 4상태.
     * REQUESTED/APPROVED 는 본사 승인 단계라 창고 화면에서 숨김. CANCELLED 는 취소된 발주.
     * status 미지정 시 4상태 모두, 지정 시 그 status 만 (4상태 외엔 빈 결과).
     */
    @Transactional(readOnly = true)
    public List<PurchaseOrderDto.ListRes> findAll(String locationCode,
                                                    PurchaseOrderStatus status,
                                                    LocalDate from, LocalDate to) {
        Long warehouseId = resolveWarehouseId(locationCode);
        // PurchaseOrderService.findAll 의 시그니처: (vendorCode, status, from, to).
        // vendorCode 는 창고 관심사 아니므로 null. status 가 비-입고 상태면 isInboundStatus 필터로 빈 결과 보장.
        List<PurchaseOrderDto.ListRes> all = purchaseOrderService.findAll(null, status, from, to);
        return all.stream()
                .filter(po -> isInboundStatus(po.getStatus()))
                .filter(po -> warehouseId.equals(po.getWarehouseId()))
                .toList();
    }

    /**
     * 입고 상세 — 자기 창고 발주만 조회 가능. 다른 창고 코드로 직접 URL 접근하면
     * PURCHASE_ORDER_NOT_FOUND 으로 차단 (창고 존재 사실 노출 회피).
     */
    @Transactional(readOnly = true)
    public PurchaseOrderDto.DetailRes findByCode(String code, String locationCode) {
        Long warehouseId = resolveWarehouseId(locationCode);
        PurchaseOrderDto.DetailRes detail = purchaseOrderService.findByCode(code);
        if (!warehouseId.equals(detail.getWarehouseId())) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_NOT_FOUND);
        }
        return detail;
    }

    /**
     * 입고 확정 (WHS-007) — DELIVERED → COMPLETED.
     * 상태 검증·전환·history append 는 PurchaseOrder.markCompleted + Service.appendHistory 가 처리.
     * me 는 인증된 창고 관리자 — 진행 이력 changedByName 에 사용자 실명 박는 데 사용.
     */
    public PurchaseOrderDto.DetailRes confirm(String code, AuthUserDetails me) {
        return purchaseOrderService.complete(code, me);
    }

    private Long resolveWarehouseId(String locationCode) {
        return infrastructureRepository
                .findByCodeAndLocationType(locationCode, LocationType.WAREHOUSE)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.WAREHOUSE_NOT_FOUND))
                .getId();
    }

    private static boolean isInboundStatus(PurchaseOrderStatus s) {
        return s == PurchaseOrderStatus.READY_TO_SHIP
                || s == PurchaseOrderStatus.IN_TRANSIT
                || s == PurchaseOrderStatus.ARRIVED
                || s == PurchaseOrderStatus.COMPLETED;
    }
}
