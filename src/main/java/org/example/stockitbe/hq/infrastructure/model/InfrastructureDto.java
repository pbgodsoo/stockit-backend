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
    public static class StoreUpsertReq {
        @NotBlank private String name;
        @NotBlank private String region;
        @NotNull private StoreType type;
        @NotBlank private String managerName;
        @NotBlank private String contact;
        @NotBlank private String address;
        @NotBlank private String warehouseCode;
        @NotNull private InfraStatus status;

        public Store toEntity(String code) {
            return Store.builder()
                    .code(code)
                    .name(name.trim())
                    .region(region.trim())
                    .type(type)
                    .managerName(managerName.trim())
                    .contact(contact.trim())
                    .address(address.trim())
                    .warehouseCode(warehouseCode.trim())
                    .status(status)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WarehouseUpsertReq {
        @NotBlank private String name;
        @NotBlank private String region;
        @NotBlank private String managerName;
        @NotBlank private String contact;
        @NotBlank private String address;
        @NotBlank private String capacity;
        @NotNull private InfraStatus status;

        public Warehouse toEntity(String code) {
            return Warehouse.builder()
                    .code(code)
                    .name(name.trim())
                    .region(region.trim())
                    .managerName(managerName.trim())
                    .contact(contact.trim())
                    .address(address.trim())
                    .capacity(capacity.trim())
                    .status(status)
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class StoreRes {
        private String code;
        private String name;
        private String region;
        private StoreType type;
        private String managerName;
        private String contact;
        private String address;
        private String warehouseCode;
        private InfraStatus status;
        private Date updatedAt;

        public static StoreRes from(Store store) {
            return StoreRes.builder()
                    .code(store.getCode())
                    .name(store.getName())
                    .region(store.getRegion())
                    .type(store.getType())
                    .managerName(store.getManagerName())
                    .contact(store.getContact())
                    .address(store.getAddress())
                    .warehouseCode(store.getWarehouseCode())
                    .status(store.getStatus())
                    .updatedAt(store.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class WarehouseRes {
        private String code;
        private String name;
        private String region;
        private String managerName;
        private String contact;
        private String address;
        private String capacity;
        private InfraStatus status;
        private long mappedStoreCount;
        private Date updatedAt;

        public static WarehouseRes from(Warehouse warehouse, long mappedStoreCount) {
            return WarehouseRes.builder()
                    .code(warehouse.getCode())
                    .name(warehouse.getName())
                    .region(warehouse.getRegion())
                    .managerName(warehouse.getManagerName())
                    .contact(warehouse.getContact())
                    .address(warehouse.getAddress())
                    .capacity(warehouse.getCapacity())
                    .status(warehouse.getStatus())
                    .mappedStoreCount(mappedStoreCount)
                    .updatedAt(warehouse.getUpdatedAt())
                    .build();
        }
    }
}
