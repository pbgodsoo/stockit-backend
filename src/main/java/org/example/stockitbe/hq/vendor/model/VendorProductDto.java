package org.example.stockitbe.hq.vendor.model;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "공급처 제품 매핑 등록 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        @Schema(description = "공급처 코드", example = "VND-001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String vendorCode;
        @Schema(description = "본사 제품 코드", example = "PRD-TOP-SS-001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String productCode;
        @Schema(description = "계약 단가 (KRW)", example = "21100", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Min(0)
        private Long unitPrice;
        @Schema(description = "최소 발주 수량 MOQ", example = "50", minimum = "1")
        @Min(1)
        private Integer moq;
        @Schema(description = "납기 리드타임 (일)", example = "6", minimum = "0")
        @Min(0)
        private Integer leadTimeDays;
        @Schema(description = "계약 시작일", example = "2026-04-01")
        private LocalDate contractStart;
        @Schema(description = "계약 종료일", example = "2027-03-31")
        private LocalDate contractEnd;

        // DTO → Entity 변환은 DTO 책임 (사용자 컨벤션).
        // productName 은 ProductMaster.name 시점 복사 — Service 가 lookup 결과 전달.
        public VendorProduct toEntity(Vendor vendor, String code, String productName) {
            return VendorProduct.builder()
                    .code(code)
                    .vendor(vendor)
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

    @Schema(description = "공급처 제품 매핑 수정 요청 — 계약 조건만 변경, vendor/product 변경 불가")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateReq {
        @Schema(description = "계약 단가 (KRW)", example = "22000", minimum = "0")
        @Min(0)
        private Long unitPrice;
        @Schema(description = "최소 발주 수량 MOQ", example = "60", minimum = "1")
        @Min(1)
        private Integer moq;
        @Schema(description = "납기 리드타임 (일)", example = "7", minimum = "0")
        @Min(0)
        private Integer leadTimeDays;
        @Schema(description = "계약 시작일", example = "2026-04-01")
        private LocalDate contractStart;
        @Schema(description = "계약 종료일", example = "2027-03-31")
        private LocalDate contractEnd;
    }

    @Schema(description = "공급처 제품 매핑 상태 전환 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusUpdateReq {
        @Schema(description = "전환 후 상태", example = "INACTIVE", allowableValues = {"ACTIVE","INACTIVE","SUSPENDED","EXPIRED","DELETED"}, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private VendorProductStatus status;
    }

    @Schema(description = "공급처 제품 매핑 목록 행")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ListRes {
        @Schema(description = "공급처 제품 코드", example = "VP-TOP-SS-001-V00")
        private String code;
        @Schema(description = "공급처 코드", example = "VND-001")
        private String vendorCode;
        @Schema(description = "공급처 이름", example = "(주)테크서플라이")
        private String vendorName;
        @Schema(description = "본사 제품 코드", example = "PRD-TOP-SS-001")
        private String productCode;
        @Schema(description = "본사 제품명 (스냅샷)", example = "코튼 에센셜 크루 반팔")
        private String productName;
        @Schema(description = "계약 단가 (KRW)", example = "21100")
        private Long unitPrice;
        @Schema(description = "최소 발주 수량", example = "50")
        private Integer moq;
        @Schema(description = "납기 리드타임 (일)", example = "6")
        private Integer leadTimeDays;
        @Schema(description = "계약 시작일", example = "2026-04-01")
        private LocalDate contractStart;
        @Schema(description = "계약 종료일", example = "2027-03-31")
        private LocalDate contractEnd;
        @Schema(description = "매핑 상태", example = "ACTIVE")
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
    @Schema(description = "공급처 계약 표 한 행 — ProductMaster + (옵션) VendorProduct join, contracted=false 면 미정 행")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ContractRowRes {
        // ProductMaster 측 (필수)
        @Schema(description = "본사 제품 코드", example = "PRD-TOP-SS-001")
        private String productCode;
        @Schema(description = "본사 제품명", example = "코튼 에센셜 크루 반팔")
        private String productName;
        @Schema(description = "카테고리 코드", example = "CAT-L2-TOP-SS")
        private String categoryCode;
        @Schema(description = "본사 기준 가격 (KRW)", example = "21100")
        private Long basePrice;

        // VendorProduct 측 (계약 미정이면 null)
        @Schema(description = "공급처 제품 코드 (미정이면 null)", example = "VP-TOP-SS-001-V00", nullable = true)
        private String vendorProductCode;
        @Schema(description = "계약 단가 (미정이면 null, KRW)", example = "21100", nullable = true)
        private Long contractUnitPrice;
        @Schema(description = "최소 발주 수량 (미정이면 null)", example = "50", nullable = true)
        private Integer moq;
        @Schema(description = "납기 리드타임 (미정이면 null, 일)", example = "6", nullable = true)
        private Integer leadTimeDays;
        @Schema(description = "계약 시작일", example = "2026-04-01", nullable = true)
        private LocalDate contractStart;
        @Schema(description = "계약 종료일", example = "2027-03-31", nullable = true)
        private LocalDate contractEnd;
        @Schema(description = "매핑 상태", example = "ACTIVE", nullable = true)
        private VendorProductStatus status;
        @Schema(description = "계약 체결 여부 — false 면 ProductMaster 만 있고 계약 정보 없음", example = "true")
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

    @Schema(description = "공급처 제품 매핑 상세 응답")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class DetailRes {
        @Schema(description = "공급처 제품 코드", example = "VP-TOP-SS-001-V00")
        private String code;
        @Schema(description = "공급처 코드", example = "VND-001")
        private String vendorCode;
        @Schema(description = "공급처 이름", example = "(주)테크서플라이")
        private String vendorName;
        @Schema(description = "본사 제품 코드", example = "PRD-TOP-SS-001")
        private String productCode;
        @Schema(description = "본사 제품명 (스냅샷)", example = "코튼 에센셜 크루 반팔")
        private String productName;
        @Schema(description = "계약 단가 (KRW)", example = "21100")
        private Long unitPrice;
        @Schema(description = "최소 발주 수량", example = "50")
        private Integer moq;
        @Schema(description = "납기 리드타임 (일)", example = "6")
        private Integer leadTimeDays;
        @Schema(description = "계약 시작일", example = "2026-04-01")
        private LocalDate contractStart;
        @Schema(description = "계약 종료일", example = "2027-03-31")
        private LocalDate contractEnd;
        @Schema(description = "매핑 상태", example = "ACTIVE")
        private VendorProductStatus status;
        @Schema(description = "매핑 생성 시각", example = "2026-05-22T05:23:02.000+09:00")
        private Date createdAt;
        @Schema(description = "매핑 마지막 수정 시각", example = "2026-05-22T05:23:02.000+09:00")
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
