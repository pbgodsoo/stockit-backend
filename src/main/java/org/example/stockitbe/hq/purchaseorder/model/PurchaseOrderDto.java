package org.example.stockitbe.hq.purchaseorder.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.product.model.ProductSku;
import org.example.stockitbe.hq.vendor.model.Vendor;
import org.example.stockitbe.hq.vendor.model.VendorProduct;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class PurchaseOrderDto {

    @Schema(description = "발주 단건 생성 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        @Schema(description = "거래처 코드", example = "VND-001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String vendorCode;
        @Schema(description = "입고 창고 코드", example = "WH-GW-0001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String warehouseCode;
        // warehouseName 필드 폐기 — 서버가 lookupWarehouse 후 name 박음 (vendor 패턴 일관)
        // 인증 미정 — placeholder OK
        @Schema(description = "발주 담당 멤버 ID (인증 정착 전 임시 필드)", example = "hq-buyer-01")
        private String memberId;
        @Schema(description = "발주 담당 멤버 이름", example = "이선엽")
        private String memberName;
        @Schema(description = "발주 라인 (1개 이상)", requiredMode = Schema.RequiredMode.REQUIRED)
        @Valid
        @NotEmpty
        private List<ItemReq> items;

        public PurchaseOrder toEntity(Vendor vendor, Infrastructure warehouse, String code, Long totalAmount) {
            return PurchaseOrder.builder()
                    .code(code)
                    .vendor(vendor)
                    .vendorName(vendor.getName())
                    .vendorContactName(vendor.getContactName())
                    .warehouse(warehouse)
                    .warehouseName(warehouse.getName())
                    .memberId(this.memberId)
                    .memberName(this.memberName)
                    .totalAmount(totalAmount)
                    .build();
        }
    }

    @Schema(description = "발주 라인 1건")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemReq {
        @Schema(description = "거래처 상품 코드 (vendor_product.code)", example = "VP-TOP-SS-001-V00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String vendorProductCode;
        @Schema(description = "SKU 코드", example = "PRD-TOP-SS-001-BLK-M", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String skuCode;
        @Schema(description = "발주 수량 (1 이상)", example = "100", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Min(1)
        private Integer quantity;

        // purchaseOrder 인자 X — PurchaseOrder.replaceItems 안에서 linkToParent 로 부모-자식 동기화.
        public PurchaseOrderItem toEntity(VendorProduct vp, ProductSku sku) {
            return PurchaseOrderItem.builder()
                    .vendorProduct(vp)
                    .productCode(vp.getProductCode())
                    .productName(vp.getProductName())
                    .skuCode(sku.getSkuCode())
                    .color(sku.getColor())
                    .size(sku.getSize())
                    .unitPrice(sku.getUnitPrice())  // sku 단가 우선 (옵션별 차등 가능)
                    .quantity(this.quantity)
                    .build();
        }
    }

    @Schema(description = "발주 일괄 생성 요청 — 선택한 SKU 들을 거래처별로 묶어 발주 N건 생성")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BatchCreateReq {
        @Schema(description = "공통 입고 창고 코드", example = "WH-GW-0001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String warehouseCode;
        @Schema(description = "발주 담당 멤버 ID", example = "hq-buyer-01")
        private String memberId;
        @Schema(description = "발주 담당 멤버 이름", example = "이선엽")
        private String memberName;
        @Schema(description = "발주 라인들 — 서버가 vendor 기준으로 자동 분할", requiredMode = Schema.RequiredMode.REQUIRED)
        @Valid
        @NotEmpty
        private List<ItemReq> items;
    }

    @Schema(description = "발주 일괄 생성 결과")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class BatchCreateRes {
        @Schema(description = "생성된 발주 목록 (vendorName ASC 정렬)")
        private List<DetailRes> orders;
        @Schema(description = "분할된 거래처 수", example = "3")
        private Integer vendorCount;
        @Schema(description = "총 라인 수", example = "12")
        private Integer itemCount;
        @Schema(description = "총 발주 금액 (KRW)", example = "5460000")
        private Long totalAmount;
    }

    @Schema(description = "발주 수정 요청 — PENDING 상태에서만 가능")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateReq {
        @Schema(description = "입고 창고 코드", example = "WH-GW-0001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String warehouseCode;
        // warehouseName 필드 폐기 — 서버가 lookupWarehouse 후 name 박음
        @Schema(description = "전체 교체될 라인 목록 (기존 라인은 모두 삭제 후 재구성)", requiredMode = Schema.RequiredMode.REQUIRED)
        @Valid
        @NotEmpty
        private List<ItemReq> items;
    }

    @Schema(description = "발주 취소 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CancelReq {
        @Schema(description = "취소 사유", example = "거래처 재고 부족으로 납기 불가", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String cancelReason;
    }

    @Schema(description = "발주 목록 행 — 목록 화면에서 한 줄로 표시되는 단위")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ListRes {
        @Schema(description = "발주 코드", example = "PO-20260520-00001")
        private String code;
        @Schema(description = "거래처 코드", example = "VND-001")
        private String vendorCode;
        @Schema(description = "거래처 이름 (발주 시점 스냅샷)", example = "(주)테크서플라이")
        private String vendorName;
        @Schema(description = "창고 PK", example = "12")
        private Long warehouseId;
        @Schema(description = "창고 코드", example = "WH-GW-0001")
        private String warehouseCode;
        @Schema(description = "창고 이름 (발주 시점 스냅샷)", example = "강원 강릉 동해안 물류허브")
        private String warehouseName;
        @Schema(description = "발주 담당자 이름", example = "이선엽")
        private String memberName;
        @Schema(description = "발주 상태 머신 (PENDING/APPROVED/READY_TO_SHIP/IN_TRANSIT/ARRIVED/COMPLETED/CANCELLED)", example = "PENDING")
        private PurchaseOrderStatus status;
        @Schema(description = "총 발주 금액 (KRW)", example = "2110000")
        private Long totalAmount;
        @Schema(description = "라인 수", example = "5")
        private Integer itemCount;
        // 발주의 모든 품목명 (입력 순서). FE 가 첫 품목명 + "외 N건" 표시 + 품목명 검색 매칭에 활용.
        @Schema(description = "라인 품목명 목록 (입력 순서). FE 가 첫 품목명 + \"외 N건\" 표기·검색 매칭에 사용",
                example = "[\"코튼 에센셜 크루 반팔\",\"드라이핏 액티브 반팔\"]")
        private List<String> productNames;
        @Schema(description = "발주 생성 시각 (ISO-8601)", example = "2026-05-20T09:15:00.000+09:00")
        private Date createdAt;
        @Schema(description = "발주 마지막 수정 시각", example = "2026-05-20T11:42:18.000+09:00")
        private Date updatedAt;

        public static ListRes from(PurchaseOrder po, Vendor vendor, String warehouseCode,
                                   int itemCount, List<String> productNames) {
            return ListRes.builder()
                    .code(po.getCode())
                    .vendorCode(vendor.getCode())
                    .vendorName(po.getVendorName())
                    .warehouseId(po.getWarehouse().getId())
                    .warehouseCode(warehouseCode)
                    .warehouseName(po.getWarehouseName())
                    .memberName(po.getMemberName())
                    .status(po.getStatus())
                    .totalAmount(po.getTotalAmount())
                    .itemCount(itemCount)
                    .productNames(productNames)
                    .createdAt(po.getCreatedAt())
                    .updatedAt(po.getUpdatedAt())
                    .build();
        }
    }

    @Schema(description = "발주 상세 — 헤더 + 라인 + 상태 히스토리 묶음")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class DetailRes {
        @Schema(description = "발주 코드", example = "PO-20260520-00001")
        private String code;
        @Schema(description = "거래처 코드", example = "VND-001")
        private String vendorCode;
        @Schema(description = "거래처 이름 (스냅샷)", example = "(주)테크서플라이")
        private String vendorName;
        @Schema(description = "창고 PK", example = "12")
        private Long warehouseId;
        @Schema(description = "창고 코드", example = "WH-GW-0001")
        private String warehouseCode;
        @Schema(description = "창고 이름 (스냅샷)", example = "강원 강릉 동해안 물류허브")
        private String warehouseName;
        @Schema(description = "발주 담당자 ID", example = "hq-buyer-01")
        private String memberId;
        @Schema(description = "발주 담당자 이름", example = "이선엽")
        private String memberName;
        @Schema(description = "현재 상태", example = "PENDING")
        private PurchaseOrderStatus status;
        @Schema(description = "총 발주 금액 (KRW)", example = "2110000")
        private Long totalAmount;
        @Schema(description = "취소 사유 (CANCELLED 상태에서만 값 존재)", example = "거래처 재고 부족으로 납기 불가", nullable = true)
        private String cancelReason;
        @Schema(description = "발주 생성 시각", example = "2026-05-20T09:15:00.000+09:00")
        private Date createdAt;
        @Schema(description = "발주 마지막 수정 시각", example = "2026-05-20T11:42:18.000+09:00")
        private Date updatedAt;
        @Schema(description = "발주 라인 목록")
        private List<ItemRes> items;
        @Schema(description = "상태 전이 히스토리 (오래된 → 최신)")
        private List<HistoryRes> statusHistory;

        public static DetailRes from(PurchaseOrder po, Vendor vendor, String warehouseCode,
                                     List<PurchaseOrderItem> items,
                                     List<PurchaseOrderStatusHistory> history,
                                     Map<Long, String> vendorProductCodeById) {
            List<ItemRes> itemRes = items.stream()
                    .map(item -> ItemRes.from(item, vendorProductCodeById.get(item.getVendorProduct().getId())))
                    .toList();
            List<HistoryRes> historyRes = history.stream()
                    .map(HistoryRes::from)
                    .toList();
            return DetailRes.builder()
                    .code(po.getCode())
                    .vendorCode(vendor.getCode())
                    .vendorName(po.getVendorName())
                    .warehouseId(po.getWarehouse().getId())
                    .warehouseCode(warehouseCode)
                    .warehouseName(po.getWarehouseName())
                    .memberId(po.getMemberId())
                    .memberName(po.getMemberName())
                    .status(po.getStatus())
                    .totalAmount(po.getTotalAmount())
                    .cancelReason(po.getCancelReason())
                    .createdAt(po.getCreatedAt())
                    .updatedAt(po.getUpdatedAt())
                    .items(itemRes)
                    .statusHistory(historyRes)
                    .build();
        }
    }

    @Schema(description = "발주 라인 응답")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ItemRes {
        @Schema(description = "vendor_product PK", example = "1")
        private Long vendorProductId;
        @Schema(description = "거래처 상품 코드", example = "VP-TOP-SS-001-V00")
        private String vendorProductCode;
        @Schema(description = "본사 상품 코드 (스냅샷)", example = "PRD-TOP-SS-001")
        private String productCode;
        @Schema(description = "상품명 (스냅샷)", example = "코튼 에센셜 크루 반팔")
        private String productName;
        @Schema(description = "SKU 코드", example = "PRD-TOP-SS-001-BLK-M")
        private String skuCode;
        @Schema(description = "색상 (3자리 코드)", example = "BLK")
        private String color;
        @Schema(description = "사이즈", example = "M")
        private String size;
        @Schema(description = "화면 표기용 옵션 (color/size)", example = "BLK/M")
        private String displayOption;
        @Schema(description = "발주 단가 (SKU 단가 스냅샷, KRW)", example = "21100")
        private Long unitPrice;
        @Schema(description = "발주 수량", example = "100")
        private Integer quantity;
        @Schema(description = "라인 소계 = unitPrice × quantity", example = "2110000")
        private Long subtotal;

        public static ItemRes from(PurchaseOrderItem item, String vendorProductCode) {
            return ItemRes.builder()
                    .vendorProductId(item.getVendorProduct().getId())
                    .vendorProductCode(vendorProductCode)
                    .productCode(item.getProductCode())
                    .productName(item.getProductName())
                    .skuCode(item.getSkuCode())
                    .color(item.getColor())
                    .size(item.getSize())
                    .displayOption(item.getColor() + "/" + item.getSize())
                    .unitPrice(item.getUnitPrice())
                    .quantity(item.getQuantity())
                    .subtotal(item.getSubtotal())
                    .build();
        }
    }

    @Schema(description = "상태 전이 1건")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class HistoryRes {
        @Schema(description = "전이된 상태", example = "APPROVED")
        private PurchaseOrderStatus status;
        @Schema(description = "전이 시각", example = "2026-05-20T10:01:23.000+09:00")
        private Date changedAt;
        @Schema(description = "전이를 일으킨 담당자 이름", example = "김사라")
        private String changedByName;
        @Schema(description = "전이 메모 (취소 사유·자동 전환 사유 등)", example = "본사 승인 완료", nullable = true)
        private String note;

        public static HistoryRes from(PurchaseOrderStatusHistory h) {
            return HistoryRes.builder()
                    .status(h.getStatus())
                    .changedAt(h.getChangedAt())
                    .changedByName(h.getChangedByName())
                    .note(h.getNote())
                    .build();
        }
    }
}
