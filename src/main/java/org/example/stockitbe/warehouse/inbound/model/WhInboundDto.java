package org.example.stockitbe.warehouse.inbound.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundHeader;
import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundItem;

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
     * statusHistory 는 폐기 — 진행 단계 history 는 PO.statusHistory 가 진실 원천이라
     * 이번 사이클에선 빈 배열 노출 (FE 의 v-if 가드로 자동 hidden).
     * 후속 사이클에서 PO.statusHistory 매핑 추가 가능.
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

        public static DetailRes from(WhInboundHeader header, List<WhInboundItem> items, String status) {
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
                    .statusHistory(List.of())
                    .build();
        }
    }

    /**
     * statusHistory 응답 형태 — 후속 사이클에서 PO.statusHistory 매핑할 때 사용.
     * 이번 사이클은 DetailRes 에서 빈 배열만 노출.
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
