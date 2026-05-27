package org.example.stockitbe.store.inbound.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.store.inbound.model.StoreInboundStatus;
import org.example.stockitbe.store.inbound.model.entity.StoreInboundHeader;
import org.example.stockitbe.store.inbound.model.entity.StoreInboundItem;
import org.example.stockitbe.store.inbound.model.entity.StoreInboundStatusHistory;
import org.example.stockitbe.warehouse.outbound.model.OutboundStatus;
import org.example.stockitbe.warehouse.outbound.model.entity.WhOutboundStatusHistory;

import java.util.Date;
import java.util.List;

public class StoreInboundDto {

    @Schema(description = "입고 액션 요청 DTO (확정 공용)")
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionReq {
        @Schema(description = "처리 사유 (선택)")
        private String reason;
    }

    @Schema(description = "입고 목록 조회 응답 DTO")
    @Getter
    @Builder
    public static class ListRes {
        @Schema(description = "입고번호", example = "INB-20240101-001")
        private String inboundNo;
        @Schema(description = "소스 참조번호 (발주번호 등)", example = "ORD-20240101-001")
        private String sourceRefNo;
        @Schema(description = "연계 출고번호", example = "OUT-20240101-001")
        private String outboundNo;
        @Schema(description = "출고 상태")
        private OutboundStatus outboundStatus;
        @Schema(description = "출고 창고 ID")
        private Long fromWarehouseId;
        @Schema(description = "출고 창고명", example = "경기 물류센터")
        private String fromWarehouseName;
        @Schema(description = "입고 상태")
        private StoreInboundStatus status;
        @Schema(description = "예상 도착일")
        private Date expectedArrivalAt;
        @Schema(description = "총 예정 수량", example = "50")
        private Integer totalExpectedQuantity;
        @Schema(description = "발주 요청 일시")
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

    @Schema(description = "입고 상세 조회 응답 DTO")
    @Getter
    @Builder
    public static class DetailRes {
        @Schema(description = "입고번호", example = "INB-20240101-001")
        private String inboundNo;
        @Schema(description = "소스 참조번호", example = "ORD-20240101-001")
        private String sourceRefNo;
        @Schema(description = "소스 참조 ID")
        private Long sourceRefId;
        @Schema(description = "연계 출고번호", example = "OUT-20240101-001")
        private String outboundNo;
        @Schema(description = "매장 ID")
        private Long storeId;
        @Schema(description = "출고 창고 ID")
        private Long fromWarehouseId;
        @Schema(description = "출고 창고명", example = "경기 물류센터")
        private String fromWarehouseName;
        @Schema(description = "입고 상태")
        private StoreInboundStatus status;
        @Schema(description = "총 SKU 종류 수", example = "3")
        private Integer totalSkuCount;
        @Schema(description = "총 예정 수량", example = "50")
        private Integer totalExpectedQuantity;
        @Schema(description = "예상 도착일")
        private Date expectedArrivalAt;
        @Schema(description = "발주 요청 일시")
        private Date requestedAt;
        @Schema(description = "입고 확정 일시")
        private Date receivedAt;
        @Schema(description = "요청자 사번", example = "EMP-001")
        private String requestedByMemberId;
        @Schema(description = "요청자 이름", example = "홍길동")
        private String requestedByName;
        @Schema(description = "입고 확정자 사번", example = "EMP-002")
        private String receivedByMemberId;
        @Schema(description = "입고 확정자 이름", example = "김영희")
        private String receivedByName;
        @Schema(description = "배송 그룹 번호")
        private String deliveryGroupNo;
        @Schema(description = "메모")
        private String memo;
        @Schema(description = "연계 출고 요약")
        private OutboundSummaryRes outbound;
        @Schema(description = "출고 상태이력 목록")
        private List<OutboundStatusHistoryRes> outboundStatusHistory;
        @Schema(description = "입고 아이템 목록")
        private List<ItemRes> items;
        @Schema(description = "입고 상태이력 목록")
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

    @Schema(description = "입고 아이템 응답 DTO")
    @Getter
    @Builder
    public static class ItemRes {
        @Schema(description = "아이템 ID")
        private Long id;
        @Schema(description = "SKU ID")
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
        @Schema(description = "예정 수량", example = "10")
        private Integer expectedQuantity;
        @Schema(description = "메모")
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

    @Schema(description = "입고 상태이력 응답 DTO")
    @Getter
    @Builder
    public static class StatusHistoryRes {
        @Schema(description = "이력 ID")
        private Long id;
        @Schema(description = "입고 상태")
        private StoreInboundStatus status;
        @Schema(description = "변경 일시")
        private Date changedAt;
        @Schema(description = "변경자 사번", example = "EMP-001")
        private String changedByMemberId;
        @Schema(description = "변경자 이름", example = "홍길동")
        private String changedByName;
        @Schema(description = "변경 사유")
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

    @Schema(description = "연계 출고 요약 응답 DTO")
    @Getter
    @Builder
    public static class OutboundSummaryRes {
        @Schema(description = "출고번호", example = "OUT-20240101-001")
        private String outboundNo;
        @Schema(description = "출고 상태")
        private OutboundStatus outboundStatus;
    }

    @Schema(description = "출고 상태이력 응답 DTO")
    @Getter
    @Builder
    public static class OutboundStatusHistoryRes {
        @Schema(description = "이력 ID")
        private Long id;
        @Schema(description = "출고 상태")
        private OutboundStatus status;
        @Schema(description = "변경 일시")
        private Date changedAt;
        @Schema(description = "변경자 사번", example = "EMP-001")
        private String changedByMemberId;
        @Schema(description = "변경자 이름", example = "홍길동")
        private String changedByName;
        @Schema(description = "변경 사유")
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
