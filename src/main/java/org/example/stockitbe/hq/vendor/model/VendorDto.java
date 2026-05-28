package org.example.stockitbe.hq.vendor.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class VendorDto {

    @Schema(description = "공급처 행 — 목록·상세 공통")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ListRes {
        @Schema(description = "공급처 코드", example = "VND-001")
        private String code;
        @Schema(description = "공급처 이름", example = "(주)테크서플라이")
        private String name;
        @Schema(description = "담당자 이름", example = "이공급")
        private String contactName;
        @Schema(description = "담당자 연락처", example = "02-1111-1111")
        private String contactPhone;
        @Schema(description = "담당자 이메일", example = "lee@techsupply.co.kr")
        private String contactEmail;
        @Schema(description = "공급처 상태", example = "ACTIVE", allowableValues = {"ACTIVE","INACTIVE"})
        private VendorStatus status;

        public static ListRes from(Vendor vendor) {
            return ListRes.builder()
                    .code(vendor.getCode())
                    .name(vendor.getName())
                    .contactName(vendor.getContactName())
                    .contactPhone(vendor.getContactPhone())
                    .contactEmail(vendor.getContactEmail())
                    .status(vendor.getStatus())
                    .build();
        }
    }
}
