package org.example.stockitbe.hq.infrastructure.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Date;

public class InfrastructureDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpsertReq {
        @NotNull private LocationType locationType;
        @NotBlank private String name;
        @NotBlank private String region;
        @NotBlank private String managerName;
        @NotBlank private String contact;
        @NotBlank private String address;
        @NotNull private InfraStatus status;

        private StoreType storeType;
        private String mappedWarehouseCode;
        private String capacity;

        public Infrastructure toEntity(String code, StoreType normalizedStoreType,
                                       String normalizedMappedWarehouseCode, String normalizedCapacity) {
            return Infrastructure.builder()
                    .code(code)
                    .locationType(locationType)
                    .name(name.trim())
                    .region(region.trim())
                    .managerName(managerName.trim())
                    .contact(contact.trim())
                    .address(address.trim())
                    .status(status)
                    .storeType(normalizedStoreType)
                    .mappedWarehouseCode(normalizedMappedWarehouseCode)
                    .capacity(normalizedCapacity)
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class Res {
        private String code;
        private LocationType locationType;
        private String name;
        private String region;
        private String managerName;
        private String contact;
        private String address;
        private InfraStatus status;
        private StoreType storeType;
        private String mappedWarehouseCode;
        private String capacity;
        private Long mappedStoreCount;
        private Date updatedAt;

        public static Res from(Infrastructure infra, Long mappedStoreCount) {
            return Res.builder()
                    .code(infra.getCode())
                    .locationType(infra.getLocationType())
                    .name(infra.getName())
                    .region(infra.getRegion())
                    .managerName(infra.getManagerName())
                    .contact(infra.getContact())
                    .address(infra.getAddress())
                    .status(infra.getStatus())
                    .storeType(infra.getStoreType())
                    .mappedWarehouseCode(infra.getMappedWarehouseCode())
                    .capacity(infra.getCapacity())
                    .mappedStoreCount(mappedStoreCount)
                    .updatedAt(infra.getUpdatedAt())
                    .build();
        }
    }
}
