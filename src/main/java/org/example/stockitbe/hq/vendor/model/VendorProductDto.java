package org.example.stockitbe.hq.vendor.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.hq.product.model.ProductMaster;

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
        @NotNull
        @Min(0)
        private Long unitPrice;
        @Min(1)
        private Integer moq;
        @Min(0)
        private Integer leadTimeDays;
        private LocalDate contractStart;
        private LocalDate contractEnd;

        // DTO → Entity 변환은 DTO 책임 (사용자 컨벤션).
        // productName 은 ProductMaster.name 시점 복사 — Service 가 lookup 결과 전달.
        public VendorProduct toEntity(Vendor vendor, String code, String productName) {
            return VendorProduct.builder()
                    .code(code)
                    .vendorId(vendor.getId())
                    .productCode(this.productCode)
                    .productName(productName)
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

    /**
     * 공급처 계약 표 한 행 (E 안 — UX 친화 inline edit).
     * ProductMaster 가 mainVendorCode 매칭으로 무조건 노출되고, VendorProduct 매칭이 있으면 계약 디테일을 채워서 반환.
     * contracted=false 인 행 = "미정" 상태 (마스터 제품은 있지만 계약 정보 없음).
     */
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ContractRowRes {
        // ProductMaster 측 (필수)
        private String productCode;
        private String productName;
        private String categoryCode;
        private Long basePrice;

        // VendorProduct 측 (계약 미정이면 null)
        private String vendorProductCode;
        private Long contractUnitPrice;
        private Integer moq;
        private Integer leadTimeDays;
        private LocalDate contractStart;
        private LocalDate contractEnd;
        private VendorProductStatus status;
        private boolean contracted;

        public static ContractRowRes from(ProductMaster pm, VendorProduct vp) {
            boolean has = vp != null;
            return ContractRowRes.builder()
                    .productCode(pm.getCode())
                    .productName(pm.getName())
                    .categoryCode(pm.getCategoryCode())
                    .basePrice(pm.getBasePrice())
                    .vendorProductCode(has ? vp.getCode() : null)
                    .contractUnitPrice(has ? vp.getUnitPrice() : null)
                    .moq(has ? vp.getMoq() : null)
                    .leadTimeDays(has ? vp.getLeadTimeDays() : null)
                    .contractStart(has ? vp.getContractStart() : null)
                    .contractEnd(has ? vp.getContractEnd() : null)
                    .status(has ? vp.getStatus() : null)
                    .contracted(has)
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
