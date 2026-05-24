package org.example.stockitbe.warehouse.inbound.model;

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

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConfirmReq {
        private String memo;
    }

    /**
     * 입고 목록 응답. status 필드는 source 도메인의 진실 원천 join 결과 —
     * inbound.completedAt!=null 면 "COMPLETED", else PO.status 그대로.
     * 산출은 Service.findAll 의 resolveEffectiveStatus 가 책임.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListRes {
        private String inboundCode;
        private String inboundType;
        private String sourceRefNo;
        private String sourceName;
        private String warehouseName;
        private String status;            // ← join 결과 (inbound 자체 컬럼 X)
        private Long totalQuantity;
        private Long totalAmount;
        private List<String> productNames;
        private Date createdAt;
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

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemRes {
        private String productCode;
        private String productName;
        private String skuCode;
        private String color;
        private String size;
        private String displayOption;
        private Integer quantity;
        private Long unitPrice;
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
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetailRes {
        private String inboundCode;
        private String inboundType;
        private String sourceRefNo;
        private String sourceName;
        private Long warehouseId;
        private String warehouseName;
        private String status;            // ← join 결과
        private Long totalQuantity;
        private Long totalAmount;
        private Date createdAt;
        private Date completedAt;
        private String confirmedByName;
        private String memo;
        private List<ItemRes> items;
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
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusHistoryRes {
        private String status;
        private Date at;
        private String byName;
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

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BackfillRes {
        private int createdCount;
        private int skippedCount;
        private List<String> createdInboundCodes;
    }
}
