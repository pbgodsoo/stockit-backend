package org.example.stockitbe.hq.circularsale.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.hq.circularsale.model.CircularSaleStatus;
import org.example.stockitbe.hq.circularsale.model.entity.CircularSaleHeader;
import org.example.stockitbe.hq.circularsale.model.entity.CircularSaleItem;
import org.example.stockitbe.hq.circularsale.model.entity.CircularSaleItemMaterial;
import org.example.stockitbe.hq.circularsale.model.entity.CircularSaleStatusHistory;
import org.example.stockitbe.warehouse.outbound.model.OutboundStatus;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class CircularSaleDto {

    // 순환재고 판매 요청 DTO
    // 거래처/소재구분/메모와 판매 라인 목록을 전달
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        @NotBlank
        private String buyerCode;
        @NotBlank
        private String materialType;
        private String memo;
        @Valid
        @NotEmpty
        private List<CreateLineReq> items;
    }

    // 순환재고 판매 응답 DTO
    // 생성된 판매 헤더 요약, 출고 연계 상태, 라인 목록을 반환
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRes {
        private Long saleId;
        private String saleNo;
        private CircularSaleStatus status;
        private String outboundNo;
        private OutboundStatus outboundStatus;
        private Date soldAt;
        private Date completedAt;
        private String buyerCode;
        private String buyerName;
        private String materialType;
        private Integer totalSkuCount;
        private BigDecimal totalRequestedWeightKg;
        private BigDecimal totalActualWeightKg;
        private Integer totalSoldQuantity;
        private Long totalAmount;
        private String memo;
        private List<LineRes> items;

        public static CreateRes from(CircularSaleHeader header, String buyerCode, String buyerName,
                                     String outboundNo, OutboundStatus outboundStatus, List<LineRes> items) {
            return CreateRes.builder()
                    .saleId(header.getId())
                    .saleNo(header.getSaleNo())
                    .status(header.getStatus())
                    .outboundNo(outboundNo)
                    .outboundStatus(outboundStatus)
                    .soldAt(header.getSoldAt())
                    .completedAt(header.getCompletedAt())
                    .buyerCode(buyerCode)
                    .buyerName(buyerName)
                    .materialType(header.getMaterialType())
                    .totalSkuCount(header.getTotalSkuCount())
                    .totalRequestedWeightKg(header.getTotalRequestedWeightKg())
                    .totalActualWeightKg(header.getTotalActualWeightKg())
                    .totalSoldQuantity(header.getTotalSoldQuantity())
                    .totalAmount(header.getTotalAmount())
                    .memo(header.getMemo())
                    .items(items)
                    .build();
        }
    }

    // 순환재고 판매 생성 라인 요청 DTO
    // 재고/상품 식별자와 중량/수량/단가/금액 정보를 전달
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateLineReq {
        @Min(1)
        private Long inventoryId;
        @NotBlank
        private String skuCode;
        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal requestedWeightKg;
        @Min(1)
        private Integer soldQuantity;
        @DecimalMin(value = "0.0")
        private BigDecimal actualWeightKg;
        @DecimalMin(value = "0.0")
        private BigDecimal estimatedQuantity;
        @Min(0)
        private Long unitPrice;
        @Min(0)
        private Long lineAmount;
        private String memo;
    }

    // 순환재고 판매 라인 응답 DTO
    // 판매 상세 품목 정보와 소재 스냅샷 목록을 반환
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LineRes {
        private Long itemId;
        private Long inventoryId;
        private String skuCode;
        private String productCode;
        private String productName;
        private String mainCategory;
        private String subCategory;
        private String color;
        private String size;
        private String materialType;
        private BigDecimal requestedWeightKg;
        private BigDecimal actualWeightKg;
        private BigDecimal estimatedQuantity;
        private Integer soldQuantity;
        private Long unitPrice;
        private Long lineAmount;
        private String memo;
        private List<MaterialRes> materials;

        public static LineRes from(CircularSaleItem item, List<MaterialRes> materials) {
            return LineRes.builder()
                    .itemId(item.getId())
                    .inventoryId(item.getInventoryId())
                    .skuCode(item.getSkuCode())
                    .productCode(item.getProductCode())
                    .productName(item.getProductName())
                    .mainCategory(item.getMainCategory())
                    .subCategory(item.getSubCategory())
                    .color(item.getColor())
                    .size(item.getSize())
                    .materialType(item.getMaterialType())
                    .requestedWeightKg(item.getRequestedWeightKg())
                    .actualWeightKg(item.getActualWeightKg())
                    .estimatedQuantity(item.getEstimatedQuantity())
                    .soldQuantity(item.getSoldQuantity())
                    .unitPrice(item.getUnitPrice())
                    .lineAmount(item.getLineAmount())
                    .memo(item.getMemo())
                    .materials(materials)
                    .build();
        }
    }

    // 순환재고 판매 품목 소재 응답 DTO
    // ESG 추적용 소재 스냅샷(material code/name/ratio)을 반환
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MaterialRes {
        private String materialCode;
        private String materialName;
        private Integer ratio;
        private Integer sortOrder;

        public static MaterialRes from(CircularSaleItemMaterial row) {
            return MaterialRes.builder()
                    .materialCode(row.getMaterialCode())
                    .materialName(row.getMaterialName())
                    .ratio(row.getRatio())
                    .sortOrder(row.getSortOrder())
                    .build();
        }
    }

    // 순환재고 판매 목록 List 페이지 응답 DTO
    // 페이지 메타정보와 목록 행 데이터를 함께 반환
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListPageRes {
        private int page;
        private int size;
        private int totalPages;
        private long totalElements;
        private boolean hasNext;
        private boolean hasPrevious;
        private List<ListRowRes> content;

        public static ListPageRes from(Page<ListRowRes> page) {
            return ListPageRes.builder()
                    .page(page.getNumber())
                    .size(page.getSize())
                    .totalPages(page.getTotalPages())
                    .totalElements(page.getTotalElements())
                    .hasNext(page.hasNext())
                    .hasPrevious(page.hasPrevious())
                    .content(page.getContent())
                    .build();
        }
    }

    // 순환재고 판매 목록 행 응답 DTO
    // 목록 화면에 필요한 판매 요약/상태/출고상태 정보를 반환
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListRowRes {
        private Long saleId;
        private String saleNo;
        private CircularSaleStatus status;
        private String outboundNo;
        private OutboundStatus outboundStatus;
        private Date soldAt;
        private Date completedAt;
        private String buyerCode;
        private String buyerName;
        private String materialType;
        private Integer totalSkuCount;
        private BigDecimal totalActualWeightKg;
        private Integer totalSoldQuantity;
        private Long totalAmount;
        private String headline;
    }

    // 순환재고 판매 상세 Detail 응답 DTO
    // 판매 헤더/라인/상태이력 전체를 상세 화면용으로 반환
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetailRes {
        private Long saleId;
        private String saleNo;
        private CircularSaleStatus status;
        private String outboundNo;
        private OutboundStatus outboundStatus;
        private Date soldAt;
        private Date completedAt;
        private String soldByMemberId;
        private String soldByName;
        private Long outboundHeaderId;
        private String buyerCode;
        private String buyerName;
        private String buyerIndustryGroup;
        private String materialType;
        private Integer totalSkuCount;
        private BigDecimal totalRequestedWeightKg;
        private BigDecimal totalActualWeightKg;
        private Integer totalSoldQuantity;
        private Long totalAmount;
        private String memo;
        private List<LineRes> items;
        private List<StatusHistoryRes> statusHistory;
    }

    // 순환재고 판매 상태이력 응답 DTO
    // 상태 전이(from/to), 변경 시각, 변경 주체, 사유를 반환
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusHistoryRes {
        private CircularSaleStatus fromStatus;
        private CircularSaleStatus status;
        private Date changedAt;
        private String changedByMemberId;
        private String changedByName;
        private String reason;

        public static StatusHistoryRes from(CircularSaleStatusHistory row) {
            return StatusHistoryRes.builder()
                    .fromStatus(row.getFromStatus())
                    .status(row.getStatus())
                    .changedAt(row.getChangedAt())
                    .changedByMemberId(row.getChangedByMemberId())
                    .changedByName(row.getChangedByName())
                    .reason(row.getReason())
                    .build();
        }
    }
}

