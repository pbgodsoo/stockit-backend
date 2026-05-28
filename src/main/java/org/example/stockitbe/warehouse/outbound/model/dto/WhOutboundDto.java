package org.example.stockitbe.warehouse.outbound.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "출고 액션 요청 DTO (확정·도착 공용)")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActionReq {
        @Schema(description = "처리 사유 (선택)", example = "샘플 메모")
        private String reason;
    }

    @Schema(description = "출고 목록 조회 응답 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListRes {
        @Schema(description = "출고번호", example = "OUT-20240101-001")
        private String outboundNo;
        @Schema(description = "소스 유형 (STORE_ORDER / CIRCULAR_SALE 등)", example = "STORE_ORDER")
        private String sourceType;
        @Schema(description = "소스 참조번호", example = "ORD-20240101-001")
        private String sourceRefNo;
        @Schema(description = "소스 참조 시퀀스", example = "1")
        private Integer sourceRefSeq;
        @Schema(description = "창고 ID", example = "1")
        private Long warehouseId;
        @Schema(description = "창고 코드", example = "WH-001")
        private String warehouseCode;
        @Schema(description = "창고명", example = "경기 물류센터")
        private String warehouseName;
        @Schema(description = "목적지 유형 (STORE / BUYER 등)", example = "STORE")
        private String destinationType;
        @Schema(description = "목적지 ID", example = "1")
        private Long destinationId;
        @Schema(description = "목적지명", example = "강남점")
        private String destinationName;
        @Schema(description = "출고 상태")
        private OutboundStatus status;
        @Schema(description = "총 요청 수량", example = "50")
        private Integer totalRequestedQuantity;
        @Schema(description = "출고 요청 일시", example = "2026-05-27T09:00:00.000+09:00")
        private Date requestedAt;
    }

    @Schema(description = "출고 상세 조회 응답 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetailRes {
        @Schema(description = "출고번호", example = "OUT-20240101-001")
        private String outboundNo;
        @Schema(description = "소스 유형", example = "STORE_ORDER")
        private String sourceType;
        @Schema(description = "소스 참조번호", example = "ORD-20240101-001")
        private String sourceRefNo;
        @Schema(description = "소스 참조 시퀀스", example = "1")
        private Integer sourceRefSeq;
        @Schema(description = "소스 참조 ID", example = "1")
        private Long sourceRefId;
        @Schema(description = "창고 ID", example = "1")
        private Long warehouseId;
        @Schema(description = "창고 코드", example = "WH-001")
        private String warehouseCode;
        @Schema(description = "창고명", example = "경기 물류센터")
        private String warehouseName;
        @Schema(description = "목적지 유형", example = "STORE")
        private String destinationType;
        @Schema(description = "목적지 ID", example = "1")
        private Long destinationId;
        @Schema(description = "목적지명", example = "강남점")
        private String destinationName;
        @Schema(description = "출고 상태")
        private OutboundStatus status;
        @Schema(description = "총 요청 수량", example = "50")
        private Integer totalRequestedQuantity;
        @Schema(description = "출고 요청 일시", example = "2026-05-27T09:00:00.000+09:00")
        private Date requestedAt;
        @Schema(description = "출고 확정 일시", example = "2026-05-27T09:00:00.000+09:00")
        private Date confirmedAt;
        @Schema(description = "출발 일시", example = "2026-05-27T09:00:00.000+09:00")
        private Date departedAt;
        @Schema(description = "도착 일시", example = "2026-05-27T09:00:00.000+09:00")
        private Date arrivedAt;
        @Schema(description = "요청자 사번", example = "EMP-001")
        private String requestedByMemberId;
        @Schema(description = "요청자 이름", example = "홍길동")
        private String requestedByName;
        @Schema(description = "메모", example = "샘플 메모")
        private String memo;
        @Schema(description = "출고 아이템 목록")
        private List<ItemRes> items;
        @Schema(description = "출고 상태이력 목록")
        private List<StatusHistoryRes> statusHistory;
        @Schema(description = "연계 입고 요약")
        private InboundSummaryRes inbound;
    }

    @Schema(description = "출고 아이템 응답 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemRes {
        @Schema(description = "아이템 ID", example = "1")
        private Long id;
        @Schema(description = "소스 라인 참조 ID", example = "1")
        private Long sourceLineRefId;
        @Schema(description = "SKU ID", example = "1")
        private Long skuId;
        @Schema(description = "SKU 코드", example = "SKU-RED-M")
        private String skuCode;
        @Schema(description = "상품 코드", example = "ITEM-001")
        private String productCode;
        @Schema(description = "상품명", example = "반팔 티셔츠")
        private String productName;
        @Schema(description = "대분류", example = "상의")
        private String mainCategory;
        @Schema(description = "소분류", example = "티셔츠")
        private String subCategory;
        @Schema(description = "색상", example = "레드")
        private String color;
        @Schema(description = "사이즈", example = "M")
        private String size;
        @Schema(description = "단가 (원)", example = "15000")
        private Long unitPrice;
        @Schema(description = "요청 수량", example = "10")
        private Integer requestedQuantity;
        @Schema(description = "메모", example = "샘플 메모")
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

    @Schema(description = "출고 상태이력 응답 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusHistoryRes {
        @Schema(description = "출고 상태")
        private OutboundStatus status;
        @Schema(description = "변경 일시", example = "2026-05-27T09:00:00.000+09:00")
        private Date changedAt;
        @Schema(description = "변경자 사번", example = "EMP-001")
        private String changedByMemberId;
        @Schema(description = "변경자 이름", example = "홍길동")
        private String changedByName;
        @Schema(description = "변경 사유", example = "샘플 메모")
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

    @Schema(description = "연계 입고 요약 응답 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InboundSummaryRes {
        @Schema(description = "입고번호", example = "INB-20240101-001")
        private String inboundNo;
        @Schema(description = "입고 상태")
        private StoreInboundStatus inboundStatus;
    }

    public static ListRes toListRes(
            WhOutboundHeader header,
            String warehouseCode,
            String warehouseName,
            String destinationName
    ) {
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
                .destinationName(destinationName)
                .status(header.getStatus())
                .totalRequestedQuantity(header.getTotalRequestedQuantity())
                .requestedAt(header.getRequestedAt())
                .build();
    }
}
