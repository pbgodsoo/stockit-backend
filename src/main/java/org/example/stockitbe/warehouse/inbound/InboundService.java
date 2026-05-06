package org.example.stockitbe.warehouse.inbound;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.hq.purchaseorder.PurchaseOrderService;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderDto;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * WHS-005/007/008 — 창고 관리자 입고 도메인 (thin layer).
 *
 * 별 테이블 만들지 않고 PurchaseOrder + statusHistory 단일 진실 원천 활용 (ADR-015).
 * PurchaseOrderService 메소드를 위임 호출만 한다.
 */
@Service
@RequiredArgsConstructor
public class InboundService {

    private final PurchaseOrderService purchaseOrderService;

    /**
     * 입고 목록 — 창고 관심사는 DELIVERED(도착됨)/COMPLETED(입고완료) 만.
     * SHIPPING 은 공급처 단계(운송중)라 창고 화면에서 안 보임 — 본사 발주 화면에서 진행 상황만 확인.
     * status 미지정 시 두 상태 모두, 지정 시 그 status 만 (DELIVERED/COMPLETED 외엔 빈 결과).
     */
    @Transactional(readOnly = true)
    public List<PurchaseOrderDto.ListRes> findAll(PurchaseOrderStatus status,
                                                    Long warehouseId,
                                                    LocalDate from, LocalDate to) {
        // PurchaseOrderService.findAll 의 시그니처: (vendorCode, status, from, to).
        // vendorCode 는 창고 관심사 아니므로 null. status 가 비-입고 상태면 isInboundStatus 필터로 빈 결과 보장.
        List<PurchaseOrderDto.ListRes> all = purchaseOrderService.findAll(null, status, from, to);
        return all.stream()
                .filter(po -> isInboundStatus(po.getStatus()))
                .filter(po -> warehouseId == null || warehouseId.equals(po.getWarehouseId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDto.DetailRes findByCode(String code) {
        return purchaseOrderService.findByCode(code);
    }

    /**
     * 입고 확정 (WHS-007) — DELIVERED → COMPLETED.
     * 상태 검증·전환·history append 는 PurchaseOrder.markCompleted + Service.appendHistory 가 처리.
     */
    public PurchaseOrderDto.DetailRes confirm(String code) {
        return purchaseOrderService.complete(code);
    }

    private static boolean isInboundStatus(PurchaseOrderStatus s) {
        return s == PurchaseOrderStatus.DELIVERED || s == PurchaseOrderStatus.COMPLETED;
    }
}
