package org.example.stockitbe.warehouse.inbound.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundHeader;
import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundItem;
import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundStatusHistory;

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
        private String status;
        private Long totalQuantity;
        private Long totalAmount;
        private List<String> productNames;
        private Date createdAt;
        private Date arrivedAt;
        private Date completedAt;

        public static ListRes from(WhInboundHeader header, List<WhInboundItem> items) {
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
                    .status(header.getStatus().name())
                    .totalQuantity(header.getTotalQuantity())
                    .totalAmount(header.getTotalAmount())
                    .productNames(names)
                    .createdAt(header.getCreatedAt())
                    .arrivedAt(header.getArrivedAt())
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
        private String displayOption;   // color + "/" + size 합성 (둘 다 비어있으면 빈 문자열)
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

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusHistoryRes {
        private String status;
        private Date at;            // FE 호환 — changedAt → at
        private String byName;      // FE 호환 — changedByName → byName
        private String note;

        public static StatusHistoryRes from(WhInboundStatusHistory h) {
            return StatusHistoryRes.builder()
                    .status(h.getStatus().name())
                    .at(h.getChangedAt())
                    .byName(h.getChangedByName())
                    .note(h.getNote())
                    .build();
        }
    }

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
        private String status;
        private Long totalQuantity;
        private Long totalAmount;
        private Date createdAt;
        private Date arrivedAt;
        private Date completedAt;
        private String confirmedByName;
        private String memo;
        private List<ItemRes> items;
        private List<StatusHistoryRes> statusHistory;

        public static DetailRes from(WhInboundHeader header,
                                     List<WhInboundItem> items,
                                     List<WhInboundStatusHistory> history) {
            return DetailRes.builder()
                    .inboundCode(header.getInboundCode())
                    .inboundType(header.getInboundType().name())
                    .sourceRefNo(header.getSourceRefNo())
                    .sourceName(header.getSourceName())
                    .warehouseId(header.getWarehouseId())
                    .warehouseName(header.getWarehouseName())
                    .status(header.getStatus().name())
                    .totalQuantity(header.getTotalQuantity())
                    .totalAmount(header.getTotalAmount())
                    .createdAt(header.getCreatedAt())
                    .arrivedAt(header.getArrivedAt())
                    .completedAt(header.getCompletedAt())
                    .confirmedByName(header.getConfirmedByName())
                    .memo(header.getMemo())
                    .items(items.stream().map(ItemRes::from).collect(Collectors.toList()))
                    .statusHistory(history.stream().map(StatusHistoryRes::from).collect(Collectors.toList()))
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
