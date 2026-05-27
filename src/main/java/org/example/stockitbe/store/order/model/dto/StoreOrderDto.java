package org.example.stockitbe.store.order.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "발주 요청 생성 요청 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        @Schema(description = "요청자 사번", example = "EMP-001")
        private String requestedByMemberId;
        @Schema(description = "요청자 이름", example = "홍길동")
        @NotBlank
        private String requestedByName;
        @Schema(description = "발주 메모", example = "긴급 발주")
        private String memo;
        @Valid
        @NotEmpty
        @Schema(description = "발주 SKU 라인 목록")
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

    @Schema(description = "발주 생성/상세 응답 DTO")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class CreateRes {
        @Schema(description = "발주번호 (문자열 형식, 예: ORD-20240101-001)", example = "ORD-20240101-001")
        private String orderId;
        @Schema(description = "매장 코드", example = "STORE-001")
        private String storeCode;
        @Schema(description = "매장명", example = "강남점")
        private String storeName;
        @Schema(description = "발주 요청 일시")
        private Date requestedAt;
        @Schema(description = "요청자 사번", example = "EMP-001")
        private String requestedByMemberId;
        @Schema(description = "요청자 이름", example = "홍길동")
        private String requestedBy;
        @Schema(description = "발주 상태")
        private StoreOrderStatus status;
        @Schema(description = "총 SKU 종류 수", example = "3")
        private Integer totalSkuCount;
        @Schema(description = "총 요청 수량", example = "50")
        private Integer totalRequestedQuantity;
        @Schema(description = "발주 메모")
        private String memo;
        @Schema(description = "취소 사유")
        private String cancelReason;
        @Schema(description = "총 입고 건수", example = "2")
        private Integer totalInboundCount;
        @Schema(description = "입고 완료 건수", example = "1")
        private Integer receivedInboundCount;
        @Schema(description = "입고 진행 상태")
        private InboundProgress inboundProgress;
        @Schema(description = "입고 요약 목록")
        private List<InboundSummaryRes> inboundSummaries;
        @Schema(description = "발주 SKU 라인 목록")
        private List<CreateLineRes> items;
        @Schema(description = "상태 변경 이력")
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

    @Schema(description = "발주 SKU 라인 생성 요청 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateLineReq {
        @Schema(description = "SKU 코드", example = "SKU-RED-M")
        @NotBlank
        private String skuCode;
        @Schema(description = "요청 수량", example = "10")
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

    @Schema(description = "발주 SKU 라인 응답 DTO")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class CreateLineRes {
        @Schema(description = "SKU ID")
        private Long skuId;
        @Schema(description = "SKU 코드", example = "SKU-RED-M")
        private String skuCode;
        @Schema(description = "상품 코드", example = "ITEM-001")
        private String productCode;
        @Schema(description = "상품명", example = "반팔 티셔츠")
        private String productName;
        @Schema(description = "대분류 카테고리", example = "상의")
        private String mainCategory;
        @Schema(description = "소분류 카테고리", example = "티셔츠")
        private String subCategory;
        @Schema(description = "색상", example = "레드")
        private String color;
        @Schema(description = "사이즈", example = "M")
        private String size;
        @Schema(description = "단가 (원)", example = "15000")
        private Long unitPrice;
        @Schema(description = "요청 수량", example = "10")
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

    @Schema(description = "발주 상태이력 응답 DTO")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class CreateHistoryRes {
        @Schema(description = "이력 유형")
        private StoreOrderHistoryType historyType;
        @Schema(description = "상태", example = "REQUESTED")
        private String status;
        @Schema(description = "변경 일시")
        private Date changedAt;
        @Schema(description = "변경자 사번", example = "EMP-001")
        private String changedByMemberId;
        @Schema(description = "변경자 이름", example = "홍길동")
        private String changedByName;
        @Schema(description = "변경 사유")
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

    @Schema(description = "발주 수정 요청 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateReq {
        @Schema(description = "발주 메모")
        private String memo;
        @Valid
        @NotEmpty
        @Schema(description = "수정할 SKU 라인 목록")
        private List<UpdateLineReq> items;
    }

    @Schema(description = "발주 수정 응답 DTO")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class UpdateRes {
        @Schema(description = "수정된 발주 상세")
        private CreateRes order;

        public static UpdateRes from(CreateRes order) {
            return UpdateRes.builder().order(order).build();
        }
    }

    @Schema(description = "발주 수정 SKU 라인 요청 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateLineReq {
        @Schema(description = "SKU 코드", example = "SKU-RED-M")
        @NotBlank
        private String skuCode;
        @Schema(description = "요청 수량", example = "10")
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

    @Schema(description = "발주 취소 요청 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CancelReq {
        @Schema(description = "취소 사유", example = "재고 확인 후 재발주 예정")
        @NotBlank
        private String cancelReason;
        @Schema(description = "취소자 사번", example = "EMP-001")
        private String cancelledByMemberId;
        @Schema(description = "취소자 이름", example = "홍길동")
        private String cancelledByName;
    }

    @Schema(description = "발주 취소 응답 DTO")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class CancelRes {
        @Schema(description = "취소 처리된 발주 상세")
        private CreateRes order;

        public static CancelRes from(CreateRes order) {
            return CancelRes.builder().order(order).build();
        }
    }

    @Schema(description = "발주 승인 요청 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApproveReq {
        @Schema(description = "승인자 사번", example = "EMP-001")
        private String approvedByMemberId;
        @Schema(description = "승인자 이름", example = "홍길동")
        private String approvedByName;
    }

    @Schema(description = "발주 승인 응답 DTO")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ApproveRes {
        @Schema(description = "승인된 발주 상세")
        private CreateRes order;

        public static ApproveRes from(CreateRes order) {
            return ApproveRes.builder().order(order).build();
        }
    }

    @Schema(description = "발주 목록 조회 응답 DTO")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ListRes {
        @Schema(description = "발주번호 (문자열 형식, 예: ORD-20240101-001)", example = "ORD-20240101-001")
        private String orderId;
        @Schema(description = "매장 코드", example = "STORE-001")
        private String storeCode;
        @Schema(description = "매장명", example = "강남점")
        private String storeName;
        @Schema(description = "발주 요청 일시")
        private Date requestedAt;
        @Schema(description = "요청자 이름", example = "홍길동")
        private String requestedBy;
        @Schema(description = "발주 상태")
        private StoreOrderStatus status;
        @Schema(description = "총 SKU 종류 수", example = "3")
        private Integer totalSkuCount;
        @Schema(description = "총 요청 수량", example = "50")
        private Integer totalRequestedQuantity;
        @Schema(description = "메모")
        private String memo;
        @Schema(description = "취소 사유")
        private String cancelReason;
        @Schema(description = "발주 헤드라인 (첫 상품명 외 n건)", example = "반팔 티셔츠 외 2건")
        private String headline;
        @Schema(description = "총 입고 건수", example = "2")
        private Integer totalInboundCount;
        @Schema(description = "입고 완료 건수", example = "1")
        private Integer receivedInboundCount;
        @Schema(description = "입고 진행 상태")
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

    @Schema(description = "연계 입고 요약 응답 DTO")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class InboundSummaryRes {
        @Schema(description = "입고번호", example = "INB-20240101-001")
        private String inboundNo;
        @Schema(description = "출고번호", example = "OUT-20240101-001")
        private String outboundNo;
        @Schema(description = "출고 창고 ID")
        private Long fromWarehouseId;
        @Schema(description = "예상 도착일")
        private Date expectedArrivalAt;
        @Schema(description = "총 예정 수량", example = "50")
        private Integer totalExpectedQuantity;
        @Schema(description = "입고 상태")
        private StoreInboundStatus status;
    }

    @Schema(description = "입고 진행 상태")
    public enum InboundProgress {
        NOT_STARTED,
        PARTIAL,
        FULL
    }

    @Schema(description = "발주 상세 조회 응답 DTO")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class DetailRes {
        @Schema(description = "발주 상세 정보")
        private CreateRes order;

        public static DetailRes from(CreateRes order) {
            return DetailRes.builder().order(order).build();
        }
    }

    @Schema(description = "발주 분석 응답 DTO")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class AnalyticsRes {
        @Schema(description = "총 발주 건수", example = "20")
        private Integer totalOrders;
        @Schema(description = "총 요청 수량", example = "300")
        private Integer totalRequestedQuantity;
        @Schema(description = "요청 건수", example = "5")
        private Integer requestedCount;
        @Schema(description = "승인 건수", example = "8")
        private Integer approvedCount;
        @Schema(description = "완료 건수", example = "6")
        private Integer completedCount;
        @Schema(description = "취소 건수", example = "1")
        private Integer cancelledCount;
        @Schema(description = "발주 상위 SKU 목록")
        private List<AnalyticsSkuRes> topSkus;
        @Schema(description = "카테고리별 발주 분석")
        private List<AnalyticsCategoryRes> categoryBreakdown;
    }

    @Schema(description = "발주 SKU 분석 응답 DTO")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class AnalyticsSkuRes {
        @Schema(description = "SKU 코드", example = "SKU-RED-M")
        private String skuCode;
        @Schema(description = "상품명", example = "반팔 티셔츠")
        private String productName;
        @Schema(description = "카테고리 레이블", example = "상의 > 티셔츠")
        private String categoryLabel;
        @Schema(description = "누적 요청 수량", example = "100")
        private Integer requestedQuantity;
        @Schema(description = "발주 건수", example = "5")
        private Integer orderCount;
    }

    @Schema(description = "발주 카테고리 분석 응답 DTO")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class AnalyticsCategoryRes {
        @Schema(description = "대분류", example = "상의")
        private String mainCategory;
        @Schema(description = "소분류", example = "티셔츠")
        private String subCategory;
        @Schema(description = "카테고리 레이블", example = "상의 > 티셔츠")
        private String label;
        @Schema(description = "누적 요청 수량", example = "150")
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
