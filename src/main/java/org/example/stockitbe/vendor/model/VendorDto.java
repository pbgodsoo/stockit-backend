package org.example.stockitbe.vendor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class VendorDto {

    @Getter
    @AllArgsConstructor
    @Builder
    public static class ListRes {
        private String code;
        private String name;
        private String contactName;
        private String contactPhone;
        private String contactEmail;
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
