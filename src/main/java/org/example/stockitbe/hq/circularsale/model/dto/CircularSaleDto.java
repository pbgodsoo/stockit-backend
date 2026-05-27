package org.example.stockitbe.hq.circularsale.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "순환재고 판매 생성 요청 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        @Schema(description = "거래처 코드", example = "BUYER-001")
        @NotBlank
        private String buyerCode;
        @Schema(description = "소재 구분", example = "POLYESTER")
        @NotBlank
        private String materialType;
        @Schema(description = "메모")
        private String memo;
        @Valid
        @NotEmpty
        @Schema(description = "판매 SKU 라인 목록")
        private List<CreateLineReq> items;
    }

    @Schema(description = "순환재고 판매 생성 응답 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRes {
        @Schema(description = "판매 ID")
        private Long saleId;
        @Schema(description = "판매번호", example = "CSALE-20240101-001")
        private String saleNo;
        @Schema(description = "판매 상태")
        private CircularSaleStatus status;
        @Schema(description = "연계 출고번호", example = "OUT-20240101-001")
        private String outboundNo;
        @Schema(description = "출고 상태")
        private OutboundStatus outboundStatus;
        @Schema(description = "판매 일시")
        private Date soldAt;
        @Schema(description = "완료 일시")
        private Date completedAt;
        @Schema(description = "거래처 코드", example = "BUYER-001")
        private String buyerCode;
        @Schema(description = "거래처명", example = "(주)리사이클코리아")
        private String buyerName;
        @Schema(description = "소재 구분", example = "POLYESTER")
        private String materialType;
        @Schema(description = "총 SKU 종류 수", example = "2")
        private Integer totalSkuCount;
        @Schema(description = "총 요청 중량 (kg)", example = "150.5")
        private BigDecimal totalRequestedWeightKg;
        @Schema(description = "총 실제 중량 (kg)", example = "148.3")
        private BigDecimal totalActualWeightKg;
        @Schema(description = "총 판매 수량", example = "300")
        private Integer totalSoldQuantity;
        @Schema(description = "총 판매 금액 (원)", example = "1500000")
        private Long totalAmount;
        @Schema(description = "메모")
        private String memo;
        @Schema(description = "판매 SKU 라인 목록")
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

    @Schema(description = "순환재고 판매 SKU 라인 생성 요청 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateLineReq {
        @Schema(description = "재고 ID", example = "1")
        @Min(1)
        private Long inventoryId;
        @Schema(description = "SKU 코드", example = "SKU-RED-M")
        @NotBlank
        private String skuCode;
        @Schema(description = "요청 중량 (kg)", example = "50.0")
        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal requestedWeightKg;
        @Schema(description = "판매 수량", example = "100")
        @Min(1)
        private Integer soldQuantity;
        @Schema(description = "실제 중량 (kg, 0 허용 — 측정 전 임시값 용도)", example = "49.5")
        @DecimalMin(value = "0.0")
        private BigDecimal actualWeightKg;
        @Schema(description = "추정 수량", example = "99.0")
        @DecimalMin(value = "0.0")
        private BigDecimal estimatedQuantity;
        @Schema(description = "단가 (원)", example = "5000")
        @Min(0)
        private Long unitPrice;
        @Schema(description = "라인 금액 (원)", example = "500000")
        @Min(0)
        private Long lineAmount;
        @Schema(description = "메모")
        private String memo;
    }

    @Schema(description = "순환재고 판매 SKU 라인 응답 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LineRes {
        @Schema(description = "아이템 ID")
        private Long itemId;
        @Schema(description = "재고 ID")
        private Long inventoryId;
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
        @Schema(description = "소재 구분", example = "POLYESTER")
        private String materialType;
        @Schema(description = "요청 중량 (kg)", example = "50.0")
        private BigDecimal requestedWeightKg;
        @Schema(description = "실제 중량 (kg)", example = "49.5")
        private BigDecimal actualWeightKg;
        @Schema(description = "추정 수량", example = "99.0")
        private BigDecimal estimatedQuantity;
        @Schema(description = "판매 수량", example = "100")
        private Integer soldQuantity;
        @Schema(description = "가용 수량 스냅샷", example = "200")
        private Integer availableQuantity;
        @Schema(description = "가용 중량 스냅샷 (kg)", example = "100.0")
        private BigDecimal availableWeightKg;
        @Schema(description = "창고 코드")
        private String warehouseCode;
        @Schema(description = "창고명")
        private String warehouseName;
        @Schema(description = "단가 (원)", example = "5000")
        private Long unitPrice;
        @Schema(description = "라인 금액 (원)", example = "500000")
        private Long lineAmount;
        @Schema(description = "메모")
        private String memo;
        @Schema(description = "소재 스냅샷 목록")
        private List<MaterialRes> materials;

        public static LineRes from(CircularSaleItem item, List<MaterialRes> materials, String warehouseCode, String warehouseName) {
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
                    .availableQuantity(item.getStockQuantitySnapshot())
                    .availableWeightKg(item.getStockWeightKgSnapshot())
                    .warehouseCode(warehouseCode)
                    .warehouseName(warehouseName)
                    .unitPrice(item.getUnitPrice())
                    .lineAmount(item.getLineAmount())
                    .memo(item.getMemo())
                    .materials(materials)
                    .build();
        }
    }

    @Schema(description = "소재 스냅샷 응답 DTO (ESG 추적용)")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MaterialRes {
        @Schema(description = "소재 코드", example = "MAT-POLY")
        private String materialCode;
        @Schema(description = "소재명", example = "폴리에스터")
        private String materialName;
        @Schema(description = "소재 비율 (%)", example = "80")
        private Integer ratio;
        @Schema(description = "정렬 순서", example = "1")
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

    @Schema(description = "순환재고 판매 목록 페이지 응답 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListPageRes {
        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        private int page;
        @Schema(description = "페이지 크기", example = "20")
        private int size;
        @Schema(description = "총 페이지 수", example = "5")
        private int totalPages;
        @Schema(description = "총 요소 수", example = "100")
        private long totalElements;
        @Schema(description = "다음 페이지 존재 여부")
        private boolean hasNext;
        @Schema(description = "이전 페이지 존재 여부")
        private boolean hasPrevious;
        @Schema(description = "판매 목록")
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

    @Schema(description = "순환재고 판매 목록 행 응답 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListRowRes {
        @Schema(description = "판매 ID")
        private Long saleId;
        @Schema(description = "판매번호", example = "CSALE-20240101-001")
        private String saleNo;
        @Schema(description = "판매 상태")
        private CircularSaleStatus status;
        @Schema(description = "연계 출고번호", example = "OUT-20240101-001")
        private String outboundNo;
        @Schema(description = "출고 창고 코드")
        private String outboundWarehouseCode;
        @Schema(description = "출고 창고명")
        private String outboundWarehouseName;
        @Schema(description = "출고 상태")
        private OutboundStatus outboundStatus;
        @Schema(description = "판매 일시")
        private Date soldAt;
        @Schema(description = "완료 일시")
        private Date completedAt;
        @Schema(description = "거래처 코드", example = "BUYER-001")
        private String buyerCode;
        @Schema(description = "거래처명", example = "(주)리사이클코리아")
        private String buyerName;
        @Schema(description = "거래처 산업군")
        private String buyerIndustryGroup;
        @Schema(description = "소재 구분", example = "POLYESTER")
        private String materialType;
        @Schema(description = "총 SKU 종류 수", example = "2")
        private Integer totalSkuCount;
        @Schema(description = "총 실제 중량 (kg)", example = "148.3")
        private BigDecimal totalActualWeightKg;
        @Schema(description = "총 판매 수량", example = "300")
        private Integer totalSoldQuantity;
        @Schema(description = "총 판매 금액 (원)", example = "1500000")
        private Long totalAmount;
        @Schema(description = "헤드라인 (첫 상품명 외 n건)", example = "반팔 티셔츠 외 1건")
        private String headline;
    }

    @Schema(description = "순환재고 판매 상세 응답 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetailRes {
        @Schema(description = "판매 ID")
        private Long saleId;
        @Schema(description = "판매번호", example = "CSALE-20240101-001")
        private String saleNo;
        @Schema(description = "판매 상태")
        private CircularSaleStatus status;
        @Schema(description = "연계 출고번호", example = "OUT-20240101-001")
        private String outboundNo;
        @Schema(description = "출고 상태")
        private OutboundStatus outboundStatus;
        @Schema(description = "판매 일시")
        private Date soldAt;
        @Schema(description = "완료 일시")
        private Date completedAt;
        @Schema(description = "판매자 사번", example = "EMP-001")
        private String soldByMemberId;
        @Schema(description = "판매자 이름", example = "홍길동")
        private String soldByName;
        @Schema(description = "출고 헤더 ID")
        private Long outboundHeaderId;
        @Schema(description = "출고 창고 코드")
        private String outboundWarehouseCode;
        @Schema(description = "출고 창고명")
        private String outboundWarehouseName;
        @Schema(description = "거래처 코드", example = "BUYER-001")
        private String buyerCode;
        @Schema(description = "거래처명", example = "(주)리사이클코리아")
        private String buyerName;
        @Schema(description = "거래처 산업군")
        private String buyerIndustryGroup;
        @Schema(description = "소재 구분", example = "POLYESTER")
        private String materialType;
        @Schema(description = "총 SKU 종류 수", example = "2")
        private Integer totalSkuCount;
        @Schema(description = "총 요청 중량 (kg)", example = "150.5")
        private BigDecimal totalRequestedWeightKg;
        @Schema(description = "총 실제 중량 (kg)", example = "148.3")
        private BigDecimal totalActualWeightKg;
        @Schema(description = "총 판매 수량", example = "300")
        private Integer totalSoldQuantity;
        @Schema(description = "총 판매 금액 (원)", example = "1500000")
        private Long totalAmount;
        @Schema(description = "메모")
        private String memo;
        @Schema(description = "판매 SKU 라인 목록")
        private List<LineRes> items;
        @Schema(description = "상태이력 목록")
        private List<StatusHistoryRes> statusHistory;
    }

    @Schema(description = "순환재고 판매 상태이력 응답 DTO")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusHistoryRes {
        @Schema(description = "이전 상태")
        private CircularSaleStatus fromStatus;
        @Schema(description = "현재 상태")
        private CircularSaleStatus status;
        @Schema(description = "변경 일시")
        private Date changedAt;
        @Schema(description = "변경자 사번", example = "EMP-001")
        private String changedByMemberId;
        @Schema(description = "변경자 이름", example = "홍길동")
        private String changedByName;
        @Schema(description = "변경 사유")
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
