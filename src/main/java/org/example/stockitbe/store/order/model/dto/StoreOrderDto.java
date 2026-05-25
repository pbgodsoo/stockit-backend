package org.example.stockitbe.store.order.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.store.order.model.enums.StoreOrderHistoryType;
import org.example.stockitbe.store.order.model.enums.StoreOrderStatus;
import org.example.stockitbe.store.inbound.model.StoreInboundStatus;
import org.example.stockitbe.store.order.model.entity.StoreOrderHeader;
import org.example.stockitbe.store.order.model.entity.StoreOrderItem;
import org.example.stockitbe.store.order.model.entity.StoreOrderStatusHistory;

import java.util.Date;
import java.util.List;

public class StoreOrderDto {

    // 발주 생성 요청 DTO
    // 매장 코드, 요청자 정보, 발주 SKU 라인 목록을 전달
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        private String requestedByMemberId;
        @NotBlank
        private String requestedByName;
        private String memo;
        @Valid
        @NotEmpty
        private List<CreateLineReq> items;

        public StoreOrderHeader toEntity(CreateHeaderContext context) {
            return StoreOrderHeader.builder()
                    .orderNo(context.getTemporaryOrderNo())
                    .storeId(context.getStoreId())
                    .warehouseId(context.getWarehouseId())
                    .requestedByMemberId(this.requestedByMemberId == null ? "" : this.requestedByMemberId)
                    .requestedByName(this.requestedByName)
                    .requestedAt(context.getRequestedAt())
                    .status(StoreOrderStatus.REQUESTED)
                    .totalSkuCount(context.getTotalSkuCount())
                    .totalRequestedQuantity(context.getTotalRequestedQuantity())
                    .memo(context.getMemo())
                    .cancelReason(null)
                    .build();
        }
    }

    // 발주 생성 응답 DTO
    // 생성된 발주 헤더/라인/상태 이력을 반환
    @Getter
    @AllArgsConstructor
    @Builder
    public static class CreateRes {
        private String orderId;
        private String storeCode;
        private String storeName;
        private Date requestedAt;
        private String requestedByMemberId;
        private String requestedBy;
        private StoreOrderStatus status;
        private Integer totalSkuCount;
        private Integer totalRequestedQuantity;
        private String memo;
        private String cancelReason;
        private Integer totalInboundCount;
        private Integer receivedInboundCount;
        private InboundProgress inboundProgress;
        private List<InboundSummaryRes> inboundSummaries;
        private List<CreateLineRes> items;
        private List<CreateHistoryRes> statusHistory;

        public static CreateRes from(StoreOrderHeader header, String storeCode, String storeName,
                                     Integer totalInboundCount, Integer receivedInboundCount, InboundProgress inboundProgress,
                                     List<InboundSummaryRes> inboundSummaries,
                                     List<CreateLineRes> items, List<CreateHistoryRes> history) {
            return CreateRes.builder()
                    .orderId(header.getOrderNo())
                    .storeCode(storeCode)
                    .storeName(storeName)
                    .requestedAt(header.getRequestedAt())
                    .requestedByMemberId(header.getRequestedByMemberId())
                    .requestedBy(header.getRequestedByName())
                    .status(header.getStatus())
                    .totalSkuCount(header.getTotalSkuCount())
                    .totalRequestedQuantity(header.getTotalRequestedQuantity())
                    .memo(header.getMemo())
                    .cancelReason(header.getCancelReason())
                    .totalInboundCount(totalInboundCount)
                    .receivedInboundCount(receivedInboundCount)
                    .inboundProgress(inboundProgress)
                    .inboundSummaries(inboundSummaries)
                    .items(items)
                    .statusHistory(history)
                    .build();
        }
    }

    // 발주 생성 라인 요청 DTO
    // SKU 코드와 요청 수량을 전달
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateLineReq {
        @NotBlank
        private String skuCode;
        @Min(1)
        private Integer requestedQuantity;

        public StoreOrderItem toEntity(CreateLineContext context) {
            return StoreOrderItem.builder()
                    .orderHeaderId(context.getOrderHeaderId())
                    .skuId(context.getSkuId())
                    .skuCode(this.skuCode)
                    .productCode(context.getProductCode())
                    .productName(context.getProductName())
                    .mainCategory(context.getMainCategory())
                    .subCategory(context.getSubCategory())
                    .color(context.getColor())
                    .size(context.getSize())
                    .unitPrice(context.getUnitPrice())
                    .requestedQuantity(this.requestedQuantity)
                    .build();
        }
    }

    // 발주 생성 라인 응답 DTO
    // SKU/상품/옵션 스냅샷과 수량/단가를 반환
    @Getter
    @AllArgsConstructor
    @Builder
    public static class CreateLineRes {
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

        public static CreateLineRes from(StoreOrderItem item) {
            return CreateLineRes.builder()
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
                    .build();
        }
    }

    // 발주 생성 상태이력 응답 DTO
    // 상태 전환 이력과 변경 주체/사유를 반환
    @Getter
    @AllArgsConstructor
    @Builder
    public static class CreateHistoryRes {
        private StoreOrderHistoryType historyType;
        private String status;
        private Date changedAt;
        private String changedByMemberId;
        private String changedByName;
        private String reason;

        public static CreateHistoryRes from(StoreOrderStatusHistory history) {
            return CreateHistoryRes.builder()
                    .historyType(history.getHistoryType())
                    .status(history.getStatus())
                    .changedAt(history.getChangedAt())
                    .changedByMemberId(history.getChangedByMemberId())
                    .changedByName(history.getChangedByName())
                    .reason(history.getReason())
                    .build();
        }
    }

    // 발주 수정 요청 DTO
    // 발주 메모와 수정 라인 목록을 전달
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateReq {
        private String memo;
        @Valid
        @NotEmpty
        private List<UpdateLineReq> items;
    }

    // 발주 수정 응답 DTO
    // 수정 반영된 발주 상세를 반환
    @Getter
    @AllArgsConstructor
    @Builder
    public static class UpdateRes {
        private CreateRes order;

        public static UpdateRes from(CreateRes order) {
            return UpdateRes.builder().order(order).build();
        }
    }

    // 발주 수정 라인 요청 DTO
    // SKU 코드와 요청 수량을 전달
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateLineReq {
        @NotBlank
        private String skuCode;
        @Min(1)
        private Integer requestedQuantity;

        public StoreOrderItem toEntity(UpdateLineContext context) {
            return StoreOrderItem.builder()
                    .orderHeaderId(context.getOrderHeaderId())
                    .skuId(context.getSkuId())
                    .skuCode(this.skuCode)
                    .productCode(context.getProductCode())
                    .productName(context.getProductName())
                    .mainCategory(context.getMainCategory())
                    .subCategory(context.getSubCategory())
                    .color(context.getColor())
                    .size(context.getSize())
                    .unitPrice(context.getUnitPrice())
                    .requestedQuantity(this.requestedQuantity)
                    .build();
        }
    }

    // 발주 취소 요청 DTO
    // 취소 사유와 취소자 정보를 전달
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CancelReq {
        @NotBlank
        private String cancelReason;
        private String cancelledByMemberId;
        private String cancelledByName;
    }

    // 발주 취소 응답 DTO
    // 취소 반영된 발주 상세를 반환
    @Getter
    @AllArgsConstructor
    @Builder
    public static class CancelRes {
        private CreateRes order;

        public static CancelRes from(CreateRes order) {
            return CancelRes.builder().order(order).build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApproveReq {
        private String approvedByMemberId;
        private String approvedByName;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class ApproveRes {
        private CreateRes order;

        public static ApproveRes from(CreateRes order) {
            return ApproveRes.builder().order(order).build();
        }
    }

    // 발주 목록 조회 응답 DTO
    // 목록 조회용 요약 정보와 헤드라인을 반환
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ListRes {
        private String orderId;
        private String storeCode;
        private String storeName;
        private Date requestedAt;
        private String requestedBy;
        private StoreOrderStatus status;
        private Integer totalSkuCount;
        private Integer totalRequestedQuantity;
        private String memo;
        private String cancelReason;
        private String headline;
        private Integer totalInboundCount;
        private Integer receivedInboundCount;
        private InboundProgress inboundProgress;

        public static ListRes from(StoreOrderHeader header, String storeCode, String storeName, String headline,
                                   Integer totalInboundCount, Integer receivedInboundCount, InboundProgress inboundProgress) {
            return ListRes.builder()
                    .orderId(header.getOrderNo())
                    .storeCode(storeCode)
                    .storeName(storeName)
                    .requestedAt(header.getRequestedAt())
                    .requestedBy(header.getRequestedByName())
                    .status(header.getStatus())
                    .totalSkuCount(header.getTotalSkuCount())
                    .totalRequestedQuantity(header.getTotalRequestedQuantity())
                    .memo(header.getMemo())
                    .cancelReason(header.getCancelReason())
                    .headline(headline)
                    .totalInboundCount(totalInboundCount)
                    .receivedInboundCount(receivedInboundCount)
                    .inboundProgress(inboundProgress)
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class InboundSummaryRes {
        private String inboundNo;
        private String outboundNo;
        private Long fromWarehouseId;
        private Date expectedArrivalAt;
        private Integer totalExpectedQuantity;
        private StoreInboundStatus status;
    }

    public enum InboundProgress {
        NOT_STARTED,
        PARTIAL,
        FULL
    }

    // 발주 상세 조회 응답 DTO
    // 단건 발주 상세를 래핑해 반환
    @Getter
    @AllArgsConstructor
    @Builder
    public static class DetailRes {
        private CreateRes order;

        public static DetailRes from(CreateRes order) {
            return DetailRes.builder().order(order).build();
        }
    }

    // 발주 분석 응답 DTO
    // 상태별 집계 및 SKU/카테고리 분석 결과를 반환
    @Getter
    @AllArgsConstructor
    @Builder
    public static class AnalyticsRes {
        private Integer totalOrders;
        private Integer totalRequestedQuantity;
        private Integer requestedCount;
        private Integer approvedCount;
        private Integer completedCount;
        private Integer cancelledCount;
        private List<AnalyticsSkuRes> topSkus;
        private List<AnalyticsCategoryRes> categoryBreakdown;
    }

    // 발주 SKU 분석 응답 DTO
    // SKU별 누적 요청 수량/건수를 반환
    @Getter
    @AllArgsConstructor
    @Builder
    public static class AnalyticsSkuRes {
        private String skuCode;
        private String productName;
        private String categoryLabel;
        private Integer requestedQuantity;
        private Integer orderCount;
    }

    // 발주 카테고리 분석 응답 DTO
    // 카테고리별 누적 요청 수량을 반환
    @Getter
    @AllArgsConstructor
    @Builder
    public static class AnalyticsCategoryRes {
        private String mainCategory;
        private String subCategory;
        private String label;
        private Integer requestedQuantity;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class CreateHeaderContext {
        private Long storeId;
        private Long warehouseId;
        private Date requestedAt;
        private Integer totalSkuCount;
        private Integer totalRequestedQuantity;
        private String memo;
        private String temporaryOrderNo;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class CreateLineContext {
        private Long orderHeaderId;
        private Long skuId;
        private String productCode;
        private String productName;
        private String mainCategory;
        private String subCategory;
        private String color;
        private String size;
        private Long unitPrice;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class UpdateLineContext {
        private Long orderHeaderId;
        private Long skuId;
        private String productCode;
        private String productName;
        private String mainCategory;
        private String subCategory;
        private String color;
        private String size;
        private Long unitPrice;
    }
}
