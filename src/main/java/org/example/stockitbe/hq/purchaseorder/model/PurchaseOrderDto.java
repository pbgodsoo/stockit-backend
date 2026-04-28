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
        private String warehouseId;
        private String warehouseName;
        // 인증 미정 — placeholder OK
        private String memberId;
        private String memberName;
        @Valid
        @NotEmpty
        private List<ItemReq> items;

        public PurchaseOrder toEntity(Vendor vendor, String code, Long totalAmount) {
            return PurchaseOrder.builder()
                    .code(code)
                    .vendorId(vendor.getId())
                    .vendorName(vendor.getName())
                    .warehouseId(this.warehouseId)
                    .warehouseName(this.warehouseName)
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
        @NotNull
        @Min(1)
        private Integer quantity;

        public PurchaseOrderItem toEntity(Long purchaseOrderId, VendorProduct vp) {
            return PurchaseOrderItem.builder()
                    .purchaseOrderId(purchaseOrderId)
                    .vendorProductId(vp.getId())
                    .productCode(vp.getProductCode())
                    .productName(vp.getProductName())
                    .unitPrice(vp.getUnitPrice())
                    .quantity(this.quantity)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateReq {
        private String warehouseId;
        private String warehouseName;
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
        private String warehouseId;
        private String warehouseName;
        private String memberName;
        private PurchaseOrderStatus status;
        private Long totalAmount;
        private Integer itemCount;
        private Date createdAt;
        private Date updatedAt;

        public static ListRes from(PurchaseOrder po, Vendor vendor, int itemCount) {
            return ListRes.builder()
                    .code(po.getCode())
                    .vendorCode(vendor.getCode())
                    .vendorName(po.getVendorName())
                    .warehouseId(po.getWarehouseId())
                    .warehouseName(po.getWarehouseName())
                    .memberName(po.getMemberName())
                    .status(po.getStatus())
                    .totalAmount(po.getTotalAmount())
                    .itemCount(itemCount)
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
        private String warehouseId;
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

        public static DetailRes from(PurchaseOrder po, Vendor vendor,
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
        private Long unitPrice;
        private Integer quantity;
        private Long subtotal;

        public static ItemRes from(PurchaseOrderItem item, String vendorProductCode) {
            return ItemRes.builder()
                    .vendorProductId(item.getVendorProductId())
                    .vendorProductCode(vendorProductCode)
                    .productCode(item.getProductCode())
                    .productName(item.getProductName())
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
