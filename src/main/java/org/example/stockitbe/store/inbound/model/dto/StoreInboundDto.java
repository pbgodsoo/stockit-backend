package org.example.stockitbe.store.inbound.model.dto;

import lombok.Builder;
import lombok.Getter;
import org.example.stockitbe.store.inbound.model.StoreInboundStatus;
import org.example.stockitbe.store.inbound.model.entity.StoreInboundHeader;
import org.example.stockitbe.store.inbound.model.entity.StoreInboundItem;
import org.example.stockitbe.store.inbound.model.entity.StoreInboundStatusHistory;
import org.example.stockitbe.warehouse.outbound.model.OutboundStatus;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundStatusHistory;

import java.util.Date;
import java.util.List;

public class StoreInboundDto {

    // 입고 내역 목록 조회 응답 dto
    @Getter
    @Builder
    public static class ListRes {
        private String inboundNo;
        private String sourceRefNo;
        private String outboundNo;
        private OutboundStatus outboundStatus;
        private Long fromWarehouseId;
        private String fromWarehouseName;
        private StoreInboundStatus status;
        private Date expectedArrivalAt;
        private Integer totalExpectedQuantity;
        private Date requestedAt;

        public static ListRes from(StoreInboundHeader header, OutboundStatus outboundStatus, String fromWarehouseName) {
            return ListRes.builder()
                    .inboundNo(header.getInboundNo())
                    .sourceRefNo(header.getSourceRefNo())
                    .outboundNo(header.getOutboundNo())
                    .outboundStatus(outboundStatus)
                    .fromWarehouseId(header.getFromWarehouseId())
                    .fromWarehouseName(fromWarehouseName)
                    .status(header.getStatus())
                    .expectedArrivalAt(header.getExpectedArrivalAt())
                    .totalExpectedQuantity(header.getTotalExpectedQuantity())
                    .requestedAt(header.getRequestedAt())
                    .build();
        }
    }

    //입고 내역 상세 조회 응답 dto
    @Getter
    @Builder
    public static class DetailRes {
        private String inboundNo;
        private String sourceRefNo;
        private Long sourceRefId;
        private String outboundNo;
        private Long storeId;
        private Long fromWarehouseId;
        private String fromWarehouseName;
        private StoreInboundStatus status;
        private Integer totalSkuCount;
        private Integer totalExpectedQuantity;
        private Date expectedArrivalAt;
        private Date requestedAt;
        private Date receivedAt;
        private String requestedByMemberId;
        private String requestedByName;
        private String receivedByMemberId;
        private String receivedByName;
        private String deliveryGroupNo;
        private String memo;
        private OutboundSummaryRes outbound;
        private List<OutboundStatusHistoryRes> outboundStatusHistory;
        private List<ItemRes> items;
        private List<StatusHistoryRes> statusHistory;

        public static DetailRes of(
                StoreInboundHeader header,
                List<StoreInboundItem> items,
                List<StoreInboundStatusHistory> history,
                String fromWarehouseName,
                OutboundSummaryRes outbound,
                List<WhOutboundStatusHistory> outboundHistory
        ) {
            return DetailRes.builder()
                    .inboundNo(header.getInboundNo())
                    .sourceRefNo(header.getSourceRefNo())
                    .sourceRefId(header.getSourceRefId())
                    .outboundNo(header.getOutboundNo())
                    .storeId(header.getStoreId())
                    .fromWarehouseId(header.getFromWarehouseId())
                    .fromWarehouseName(fromWarehouseName)
                    .status(header.getStatus())
                    .totalSkuCount(header.getTotalSkuCount())
                    .totalExpectedQuantity(header.getTotalExpectedQuantity())
                    .expectedArrivalAt(header.getExpectedArrivalAt())
                    .requestedAt(header.getRequestedAt())
                    .receivedAt(header.getReceivedAt())
                    .requestedByMemberId(header.getRequestedByMemberId())
                    .requestedByName(header.getRequestedByName())
                    .receivedByMemberId(header.getReceivedByMemberId())
                    .receivedByName(header.getReceivedByName())
                    .deliveryGroupNo(header.getDeliveryGroupNo())
                    .memo(header.getMemo())
                    .outbound(outbound)
                    .outboundStatusHistory(outboundHistory.stream().map(OutboundStatusHistoryRes::from).toList())
                    .items(items.stream().map(ItemRes::from).toList())
                    .statusHistory(history.stream().map(StatusHistoryRes::from).toList())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ItemRes {
        private Long id;
        private Long skuId;
        private String skuCode;
        private String productCode;
        private String productName;
        private String mainCategory;
        private String subCategory;
        private String color;
        private String size;
        private Long unitPrice;
        private Integer expectedQuantity;
        private String memo;

        public static ItemRes from(StoreInboundItem item) {
            return ItemRes.builder()
                    .id(item.getId())
                    .skuId(item.getSkuId())
                    .skuCode(item.getSkuCode())
                    .productCode(item.getProductCode())
                    .productName(item.getProductName())
                    .mainCategory(item.getMainCategory())
                    .subCategory(item.getSubCategory())
                    .color(item.getColor())
                    .size(item.getSize())
                    .unitPrice(item.getUnitPrice())
                    .expectedQuantity(item.getExpectedQuantity())
                    .memo(item.getMemo())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class StatusHistoryRes {
        private Long id;
        private StoreInboundStatus status;
        private Date changedAt;
        private String changedByMemberId;
        private String changedByName;
        private String reason;

        public static StatusHistoryRes from(StoreInboundStatusHistory history) {
            return StatusHistoryRes.builder()
                    .id(history.getId())
                    .status(history.getStatus())
                    .changedAt(history.getChangedAt())
                    .changedByMemberId(history.getChangedByMemberId())
                    .changedByName(history.getChangedByName())
                    .reason(history.getReason())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class OutboundSummaryRes {
        private String outboundNo;
        private OutboundStatus outboundStatus;
    }

    @Getter
    @Builder
    public static class OutboundStatusHistoryRes {
        private Long id;
        private OutboundStatus status;
        private Date changedAt;
        private String changedByMemberId;
        private String changedByName;
        private String reason;

        public static OutboundStatusHistoryRes from(WhOutboundStatusHistory history) {
            return OutboundStatusHistoryRes.builder()
                    .id(history.getId())
                    .status(history.getStatus())
                    .changedAt(history.getChangedAt())
                    .changedByMemberId(history.getChangedByMemberId())
                    .changedByName(history.getChangedByName())
                    .reason(history.getReason())
                    .build();
        }
    }
}

