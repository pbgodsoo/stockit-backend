package org.example.stockitbe.warehouse.outbound.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.store.inbound.model.StoreInboundStatus;
import org.example.stockitbe.warehouse.outbound.model.OutboundStatus;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundHeader;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundItem;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundStatusHistory;

import java.util.Date;
import java.util.List;

public class WhOutboundDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActionReq {
        private String reason;
    }

    // 발주 내역 목록 조회 응답 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListRes {
        private String outboundNo;
        private String sourceType;
        private String sourceRefNo;
        private Integer sourceRefSeq;
        private Long warehouseId;
        private String warehouseCode;
        private String warehouseName;
        private String destinationType;
        private Long destinationId;
        private OutboundStatus status;
        private Integer totalRequestedQuantity;
        private Date requestedAt;
    }

    // 발주 내역 상세 조회 응답 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetailRes {
        private String outboundNo;
        private String sourceType;
        private String sourceRefNo;
        private Integer sourceRefSeq;
        private Long sourceRefId;
        private Long warehouseId;
        private String warehouseCode;
        private String warehouseName;
        private String destinationType;
        private Long destinationId;
        private OutboundStatus status;
        private Integer totalRequestedQuantity;
        private Date requestedAt;
        private Date confirmedAt;
        private Date departedAt;
        private Date arrivedAt;
        private String requestedByMemberId;
        private String requestedByName;
        private String memo;
        private List<ItemRes> items;
        private List<StatusHistoryRes> statusHistory;
        private InboundSummaryRes inbound;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemRes {
        private Long id;
        private Long sourceLineRefId;
        private Long skuId;
        private String skuCode;
        private String productCode;
        private String productName;
        private String mainCategory;
        private String subCategory;
        private String color;
        private String size;
        private Long unitPrice;
        private Integer requestedQuantity;
        private String memo;

        public static ItemRes from(WhOutboundItem item) {
            return ItemRes.builder()
                    .id(item.getId())
                    .sourceLineRefId(item.getSourceLineRefId())
                    .skuId(item.getSkuId())
                    .skuCode(item.getSkuCode())
                    .productCode(item.getProductCode())
                    .productName(item.getProductName())
                    .mainCategory(item.getMainCategory())
                    .subCategory(item.getSubCategory())
                    .color(item.getColor())
                    .size(item.getSize())
                    .unitPrice(item.getUnitPrice())
                    .requestedQuantity(item.getRequestedQuantity())
                    .memo(item.getMemo())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusHistoryRes {
        private OutboundStatus status;
        private Date changedAt;
        private String changedByMemberId;
        private String changedByName;
        private String reason;

        public static StatusHistoryRes from(WhOutboundStatusHistory history) {
            return StatusHistoryRes.builder()
                    .status(history.getStatus())
                    .changedAt(history.getChangedAt())
                    .changedByMemberId(history.getChangedByMemberId())
                    .changedByName(history.getChangedByName())
                    .reason(history.getReason())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InboundSummaryRes {
        private String inboundNo;
        private StoreInboundStatus inboundStatus;
    }

    public static ListRes toListRes(WhOutboundHeader header, String warehouseCode, String warehouseName) {
        return ListRes.builder()
                .outboundNo(header.getOutboundNo())
                .sourceType(header.getSourceType().name())
                .sourceRefNo(header.getSourceRefNo())
                .sourceRefSeq(header.getSourceRefSeq())
                .warehouseId(header.getWarehouseId())
                .warehouseCode(warehouseCode)
                .warehouseName(warehouseName)
                .destinationType(header.getDestinationType().name())
                .destinationId(header.getDestinationId())
                .status(header.getStatus())
                .totalRequestedQuantity(header.getTotalRequestedQuantity())
                .requestedAt(header.getRequestedAt())
                .build();
    }
}
