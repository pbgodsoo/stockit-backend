package org.example.stockitbe.warehouse.inbound.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderStatusHistory;
import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundHeader;
import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundItem;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundStatusHistory;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class WhInboundDto {

    @Schema(description = "입고 확정 요청 — 메모만 옵션으로 받음")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConfirmReq {
        @Schema(description = "확정 메모 (옵션)", example = "검수 완료, 파손 없음")
        private String memo;
    }

    /**
     * 입고 목록 응답. status 필드는 source 도메인의 진실 원천 join 결과 —
     * inbound.completedAt!=null 면 "COMPLETED", else PO.status 그대로.
     * 산출은 Service.findAll 의 resolveEffectiveStatus 가 책임.
     */
    @Schema(description = "입고 목록 한 행 — status 는 source 도메인(PO/outbound) join 결과")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListRes {
        @Schema(description = "입고 코드", example = "WIB-20260527-00001")
        private String inboundCode;
        @Schema(description = "입고 유형 (PURCHASE_ORDER/WAREHOUSE_TRANSFER)", example = "PURCHASE_ORDER")
        private String inboundType;
        @Schema(description = "원천 참조번호 (PO 코드 또는 transferNo)", example = "PO-20260520-00001")
        private String sourceRefNo;
        @Schema(description = "원천 거래처/창고 이름 (스냅샷)", example = "(주)테크서플라이")
        private String sourceName;
        @Schema(description = "도착 창고 이름", example = "강원 강릉 동해안 물류허브")
        private String warehouseName;
        @Schema(description = "현재 상태 — inbound.completedAt!=null 면 COMPLETED, else PO/outbound status",
                example = "ARRIVED", allowableValues = {"READY_TO_SHIP","IN_TRANSIT","ARRIVED","COMPLETED"})
        private String status;
        @Schema(description = "총 입고 수량", example = "300")
        private Long totalQuantity;
        @Schema(description = "총 입고 금액 (KRW)", example = "6330000")
        private Long totalAmount;
        @Schema(description = "라인 품목명 (최대 5개)", example = "[\"코튼 에센셜 크루 반팔\",\"드라이핏 액티브 반팔\"]")
        private List<String> productNames;
        @Schema(description = "입고 생성 시각", example = "2026-05-20T10:01:23.000+09:00")
        private Date createdAt;
        @Schema(description = "입고 확정 시각 (확정 전이면 null)", example = "2026-05-22T14:30:00.000+09:00", nullable = true)
        private Date completedAt;

        public static ListRes from(WhInboundHeader header, List<WhInboundItem> items, String status) {
            List<String> names = items.stream()
                    .map(WhInboundItem::getProductName)
                    .distinct()
                    .limit(5)
                    .collect(Collectors.toList());
            return ListRes.builder()
                    .inboundCode(header.getInboundCode())
                    .inboundType(header.getInboundType().name())
                    .sourceRefNo(header.getSourceRefNo())
                    .sourceName(header.getSourceName())
                    .warehouseName(header.getWarehouseName())
                    .status(status)
                    .totalQuantity(header.getTotalQuantity())
                    .totalAmount(header.getTotalAmount())
                    .productNames(names)
                    .createdAt(header.getCreatedAt())
                    .completedAt(header.getCompletedAt())
                    .build();
        }
    }

    @Schema(description = "입고 라인 응답")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemRes {
        @Schema(description = "본사 상품 코드", example = "PRD-TOP-SS-001")
        private String productCode;
        @Schema(description = "상품명 (스냅샷)", example = "코튼 에센셜 크루 반팔")
        private String productName;
        @Schema(description = "SKU 코드", example = "PRD-TOP-SS-001-BLK-M")
        private String skuCode;
        @Schema(description = "색상", example = "BLK")
        private String color;
        @Schema(description = "사이즈", example = "M")
        private String size;
        @Schema(description = "옵션 표기 (color/size)", example = "BLK/M")
        private String displayOption;
        @Schema(description = "입고 수량", example = "100")
        private Integer quantity;
        @Schema(description = "라인 단가 (KRW)", example = "21100")
        private Long unitPrice;
        @Schema(description = "라인 소계", example = "2110000")
        private Long subtotal;

        public static ItemRes from(WhInboundItem item) {
            String c = item.getColor() == null ? "" : item.getColor();
            String s = item.getSize() == null ? "" : item.getSize();
            String displayOption;
            if (!c.isEmpty() && !s.isEmpty()) displayOption = c + "/" + s;
            else if (!c.isEmpty()) displayOption = c;
            else if (!s.isEmpty()) displayOption = s;
            else displayOption = "";
            return ItemRes.builder()
                    .productCode(item.getProductCode())
                    .productName(item.getProductName())
                    .skuCode(item.getSkuCode())
                    .color(item.getColor())
                    .size(item.getSize())
                    .displayOption(displayOption)
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .subtotal(item.getSubtotal())
                    .build();
        }
    }

    /**
     * 입고 상세 응답. status 필드는 ListRes 와 동일 룰 (source join).
     * statusHistory 는 source 도메인 history join — PURCHASE_ORDER 면 PO history,
     * WAREHOUSE_TRANSFER 면 outbound history. completedAt!=null 이면 service 가
     * 마지막 COMPLETED 항목을 append 한다 (4단계째).
     */
    @Schema(description = "입고 상세 응답 — 헤더 + 라인 + source 도메인 statusHistory")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetailRes {
        @Schema(description = "입고 코드", example = "WIB-20260527-00001")
        private String inboundCode;
        @Schema(description = "입고 유형", example = "PURCHASE_ORDER")
        private String inboundType;
        @Schema(description = "원천 참조번호", example = "PO-20260520-00001")
        private String sourceRefNo;
        @Schema(description = "원천 거래처/창고명 (스냅샷)", example = "(주)테크서플라이")
        private String sourceName;
        @Schema(description = "도착 창고 PK", example = "21")
        private Long warehouseId;
        @Schema(description = "도착 창고 이름", example = "강원 강릉 동해안 물류허브")
        private String warehouseName;
        @Schema(description = "현재 상태 (source join)", example = "ARRIVED")
        private String status;
        @Schema(description = "총 입고 수량", example = "300")
        private Long totalQuantity;
        @Schema(description = "총 입고 금액", example = "6330000")
        private Long totalAmount;
        @Schema(description = "입고 생성 시각", example = "2026-05-20T10:01:23.000+09:00")
        private Date createdAt;
        @Schema(description = "입고 확정 시각", example = "2026-05-22T14:30:00.000+09:00", nullable = true)
        private Date completedAt;
        @Schema(description = "확정 처리자 이름", example = "박범수", nullable = true)
        private String confirmedByName;
        @Schema(description = "확정 메모", example = "검수 완료", nullable = true)
        private String memo;
        @Schema(description = "입고 라인 목록")
        private List<ItemRes> items;
        @Schema(description = "source 도메인 상태 히스토리 (PO 면 PO history, transfer 면 outbound history)")
        private List<StatusHistoryRes> statusHistory;

        public static DetailRes from(WhInboundHeader header, List<WhInboundItem> items,
                                     String status, List<StatusHistoryRes> statusHistory) {
            return DetailRes.builder()
                    .inboundCode(header.getInboundCode())
                    .inboundType(header.getInboundType().name())
                    .sourceRefNo(header.getSourceRefNo())
                    .sourceName(header.getSourceName())
                    .warehouseId(header.getWarehouse().getId())
                    .warehouseName(header.getWarehouseName())
                    .status(status)
                    .totalQuantity(header.getTotalQuantity())
                    .totalAmount(header.getTotalAmount())
                    .createdAt(header.getCreatedAt())
                    .completedAt(header.getCompletedAt())
                    .confirmedByName(header.getConfirmedByName())
                    .memo(header.getMemo())
                    .items(items.stream().map(ItemRes::from).collect(Collectors.toList()))
                    .statusHistory(statusHistory == null ? List.of() : statusHistory)
                    .build();
        }
    }

    /**
     * statusHistory 통일 shape — PO/outbound history 둘 다 같은 shape 으로 노출.
     *   - PO history → status / changedAt / changedByName / note
     *   - outbound history → status / changedAt / changedByName / reason
     */
    @Schema(description = "통일 상태 히스토리 1건 — PO/outbound history 둘 다 같은 shape")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusHistoryRes {
        @Schema(description = "전이된 상태", example = "ARRIVED")
        private String status;
        @Schema(description = "전이 시각", example = "2026-05-22T14:30:00.000+09:00")
        private Date at;
        @Schema(description = "전이를 일으킨 담당자 이름", example = "박범수")
        private String byName;
        @Schema(description = "전이 메모/사유", example = "정상 도착", nullable = true)
        private String note;

        public static StatusHistoryRes fromPo(PurchaseOrderStatusHistory h) {
            return StatusHistoryRes.builder()
                    .status(h.getStatus().name())
                    .at(h.getChangedAt())
                    .byName(h.getChangedByName())
                    .note(h.getNote())
                    .build();
        }

        public static StatusHistoryRes fromOutbound(WhOutboundStatusHistory h) {
            return StatusHistoryRes.builder()
                    .status(h.getStatus().name())
                    .at(h.getChangedAt())
                    .byName(h.getChangedByName())
                    .note(h.getReason())
                    .build();
        }

        public static StatusHistoryRes completed(Date at, String byName) {
            return StatusHistoryRes.builder()
                    .status("COMPLETED")
                    .at(at)
                    .byName(byName)
                    .note(null)
                    .build();
        }
    }

    @Schema(description = "백필 결과 — 누락된 입고 헤더 일괄 생성 응답")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BackfillRes {
        @Schema(description = "새로 생성된 입고 헤더 수", example = "12")
        private int createdCount;
        @Schema(description = "이미 존재해 스킵된 수", example = "3")
        private int skippedCount;
        @Schema(description = "생성된 입고 코드 목록", example = "[\"WIB-20260527-00001\",\"WIB-20260527-00002\"]")
        private List<String> createdInboundCodes;
    }
}
