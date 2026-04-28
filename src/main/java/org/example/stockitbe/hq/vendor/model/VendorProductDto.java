package org.example.stockitbe.hq.vendor.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;

public class VendorProductDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        @NotBlank
        private String vendorCode;
        @NotBlank
        private String productCode;
        @NotBlank
        private String productName;
        @NotNull
        @Min(0)
        private Long unitPrice;
        @Min(1)
        private Integer moq;
        @Min(0)
        private Integer leadTimeDays;
        private LocalDate contractStart;
        private LocalDate contractEnd;

        // DTO → Entity 변환은 DTO 책임 (사용자 컨벤션)
        public VendorProduct toEntity(Vendor vendor, String code) {
            return VendorProduct.builder()
                    .code(code)
                    .vendorId(vendor.getId())
                    .productCode(this.productCode)
                    .productName(this.productName)
                    .unitPrice(this.unitPrice)
                    .moq(this.moq)
                    .leadTimeDays(this.leadTimeDays)
                    .contractStart(this.contractStart)
                    .contractEnd(this.contractEnd)
                    .status(VendorProductStatus.ACTIVE)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateReq {
        private String productName;
        @Min(0)
        private Long unitPrice;
        @Min(1)
        private Integer moq;
        @Min(0)
        private Integer leadTimeDays;
        private LocalDate contractStart;
        private LocalDate contractEnd;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusUpdateReq {
        @NotNull
        private VendorProductStatus status;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class ListRes {
        private String code;
        private String vendorCode;
        private String vendorName;
        private String productCode;
        private String productName;
        private Long unitPrice;
        private Integer moq;
        private Integer leadTimeDays;
        private LocalDate contractStart;
        private LocalDate contractEnd;
        private VendorProductStatus status;

        public static ListRes from(VendorProduct vp, Vendor vendor) {
            return ListRes.builder()
                    .code(vp.getCode())
                    .vendorCode(vendor.getCode())
                    .vendorName(vendor.getName())
                    .productCode(vp.getProductCode())
                    .productName(vp.getProductName())
                    .unitPrice(vp.getUnitPrice())
                    .moq(vp.getMoq())
                    .leadTimeDays(vp.getLeadTimeDays())
                    .contractStart(vp.getContractStart())
                    .contractEnd(vp.getContractEnd())
                    .status(vp.getStatus())
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
        private String productCode;
        private String productName;
        private Long unitPrice;
        private Integer moq;
        private Integer leadTimeDays;
        private LocalDate contractStart;
        private LocalDate contractEnd;
        private VendorProductStatus status;
        private Date createdAt;
        private Date updatedAt;

        public static DetailRes from(VendorProduct vp, Vendor vendor) {
            return DetailRes.builder()
                    .code(vp.getCode())
                    .vendorCode(vendor.getCode())
                    .vendorName(vendor.getName())
                    .productCode(vp.getProductCode())
                    .productName(vp.getProductName())
                    .unitPrice(vp.getUnitPrice())
                    .moq(vp.getMoq())
                    .leadTimeDays(vp.getLeadTimeDays())
                    .contractStart(vp.getContractStart())
                    .contractEnd(vp.getContractEnd())
                    .status(vp.getStatus())
                    .createdAt(vp.getCreatedAt())
                    .updatedAt(vp.getUpdatedAt())
                    .build();
        }
    }
}
