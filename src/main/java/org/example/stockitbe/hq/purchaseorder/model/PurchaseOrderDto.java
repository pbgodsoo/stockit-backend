package org.example.stockitbe.hq.purchaseorder.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.hq.infrastructure.model.Warehouse;
import org.example.stockitbe.hq.product.model.ProductSku;
import org.example.stockitbe.hq.vendor.model.Vendor;
import org.example.stockitbe.hq.vendor.model.VendorProduct;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class PurchaseOrderDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        @NotBlank
        private String vendorCode;
        @NotBlank
        private String warehouseCode;
        // warehouseName 필드 폐기 — 서버가 lookupWarehouse 후 name 박음 (vendor 패턴 일관)
        // 인증 미정 — placeholder OK
        private String memberId;
        private String memberName;
        @Valid
        @NotEmpty
        private List<ItemReq> items;

        public PurchaseOrder toEntity(Vendor vendor, Warehouse warehouse, String code, Long totalAmount) {
            return PurchaseOrder.builder()
                    .code(code)
                    .vendorId(vendor.getId())
                    .vendorName(vendor.getName())
                    .vendorContactName(vendor.getContactName())
                    .warehouseId(warehouse.getId())
                    .warehouseName(warehouse.getName())
                    .memberId(this.memberId)
                    .memberName(this.memberName)
                    .totalAmount(totalAmount)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemReq {
        @NotBlank
        private String vendorProductCode;
        @NotBlank
        private String skuCode;
        @NotNull
        @Min(1)
        private Integer quantity;

        public PurchaseOrderItem toEntity(Long purchaseOrderId, VendorProduct vp, ProductSku sku) {
            return PurchaseOrderItem.builder()
                    .purchaseOrderId(purchaseOrderId)
                    .vendorProductId(vp.getId())
                    .productCode(vp.getProductCode())
                    .productName(vp.getProductName())
                    .skuCode(sku.getSkuCode())
                    .optionName(sku.getOptionName())
                    .optionValue(sku.getOptionValue())
                    .unitPrice(sku.getUnitPrice())  // sku 단가 우선 (옵션별 차등 가능)
                    .quantity(this.quantity)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateReq {
        @NotBlank
        private String warehouseCode;
        // warehouseName 필드 폐기 — 서버가 lookupWarehouse 후 name 박음
        @Valid
        @NotEmpty
        private List<ItemReq> items;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CancelReq {
        @NotBlank
        private String cancelReason;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class ListRes {
        private String code;
        private String vendorCode;
        private String vendorName;
        private Long warehouseId;
        private String warehouseCode;
        private String warehouseName;
        private String memberName;
        private PurchaseOrderStatus status;
        private Long totalAmount;
        private Integer itemCount;
        // 발주의 모든 품목명 (입력 순서). FE 가 첫 품목명 + "외 N건" 표시 + 품목명 검색 매칭에 활용.
        private List<String> productNames;
        private Date createdAt;
        private Date updatedAt;

        public static ListRes from(PurchaseOrder po, Vendor vendor, String warehouseCode,
                                    int itemCount, List<String> productNames) {
            return ListRes.builder()
                    .code(po.getCode())
                    .vendorCode(vendor.getCode())
                    .vendorName(po.getVendorName())
                    .warehouseId(po.getWarehouseId())
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

    @Getter
    @AllArgsConstructor
    @Builder
    public static class DetailRes {
        private String code;
        private String vendorCode;
        private String vendorName;
        private Long warehouseId;
        private String warehouseCode;
        private String warehouseName;
        private String memberId;
        private String memberName;
        private PurchaseOrderStatus status;
        private Long totalAmount;
        private String cancelReason;
        private Date createdAt;
        private Date updatedAt;
        private List<ItemRes> items;
        private List<HistoryRes> statusHistory;

        public static DetailRes from(PurchaseOrder po, Vendor vendor, String warehouseCode,
                                      List<PurchaseOrderItem> items,
                                      List<PurchaseOrderStatusHistory> history,
                                      Map<Long, String> vendorProductCodeById) {
            List<ItemRes> itemRes = items.stream()
                    .map(item -> ItemRes.from(item, vendorProductCodeById.get(item.getVendorProductId())))
                    .toList();
            List<HistoryRes> historyRes = history.stream()
                    .map(HistoryRes::from)
                    .toList();
            return DetailRes.builder()
                    .code(po.getCode())
                    .vendorCode(vendor.getCode())
                    .vendorName(po.getVendorName())
                    .warehouseId(po.getWarehouseId())
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

    @Getter
    @AllArgsConstructor
    @Builder
    public static class ItemRes {
        private Long vendorProductId;
        private String vendorProductCode;
        private String productCode;
        private String productName;
        private String skuCode;
        private String optionName;
        private String optionValue;
        private Long unitPrice;
        private Integer quantity;
        private Long subtotal;

        public static ItemRes from(PurchaseOrderItem item, String vendorProductCode) {
            return ItemRes.builder()
                    .vendorProductId(item.getVendorProductId())
                    .vendorProductCode(vendorProductCode)
                    .productCode(item.getProductCode())
                    .productName(item.getProductName())
                    .skuCode(item.getSkuCode())
                    .optionName(item.getOptionName())
                    .optionValue(item.getOptionValue())
                    .unitPrice(item.getUnitPrice())
                    .quantity(item.getQuantity())
                    .subtotal(item.getSubtotal())
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class HistoryRes {
        private PurchaseOrderStatus status;
        private Date changedAt;
        private String changedByName;
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
