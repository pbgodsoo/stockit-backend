package org.example.stockitbe.hq.circularbuyer.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

public class CircularBuyerDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        @NotBlank
        private String code;
        @NotBlank
        private String companyName;
        @NotBlank
        private String industryGroup;
        private List<String> productTypes;
        private String productNote;
        private String description;
        @NotBlank
        private String primaryMaterialFit;
        @NotBlank
        private String managerName;
        @NotBlank
        private String phone;

        public CircularBuyer toEntity() {
            return CircularBuyer.builder()
                    .code(this.code)
                    .companyName(this.companyName)
                    .industryGroup(this.industryGroup)
                    .productTypes(this.productTypes)
                    .productNote(this.productNote)
                    .description(this.description)
                    .primaryMaterialFit(this.primaryMaterialFit)
                    .managerName(this.managerName)
                    .phone(this.phone)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateReq {
        private String companyName;
        private String industryGroup;
        private List<String> productTypes;
        private String productNote;
        private String description;
        private String primaryMaterialFit;
        private String managerName;
        private String phone;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class ListRes {
        private String code;
        private String companyName;
        private String industryGroup;
        private List<String> productTypes;
        private String primaryMaterialFit;
        private String managerName;
        private String phone;

        public static ListRes from(CircularBuyer v) {
            return ListRes.builder()
                    .code(v.getCode())
                    .companyName(v.getCompanyName())
                    .industryGroup(v.getIndustryGroup())
                    .productTypes(v.getProductTypes())
                    .primaryMaterialFit(v.getPrimaryMaterialFit())
                    .managerName(v.getManagerName())
                    .phone(v.getPhone())
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class DetailRes {
        private String code;
        private String companyName;
        private String industryGroup;
        private List<String> productTypes;
        private String productNote;
        private String description;
        private String primaryMaterialFit;
        private String managerName;
        private String phone;
        private Date createdAt;
        private Date updatedAt;

        public static DetailRes from(CircularBuyer v) {
            return DetailRes.builder()
                    .code(v.getCode())
                    .companyName(v.getCompanyName())
                    .industryGroup(v.getIndustryGroup())
                    .productTypes(v.getProductTypes())
                    .productNote(v.getProductNote())
                    .description(v.getDescription())
                    .primaryMaterialFit(v.getPrimaryMaterialFit())
                    .managerName(v.getManagerName())
                    .phone(v.getPhone())
                    .createdAt(v.getCreatedAt())
                    .updatedAt(v.getUpdatedAt())
                    .build();
        }
    }
}
