package org.example.stockitbe.hq.infrastructure.model;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Date;
import java.util.List;

public class InfrastructureMappingDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StoreMappingItem {
        private Long storeId;
        private String storeCode;
        private String storeName;
        private String region;
        private InfraStatus status;
        private String primaryWarehouseCode;
        private String primaryWarehouseName;
        private String backupWarehouseCode;
        private String backupWarehouseName;
        private Date updatedAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpsertReq {
        @NotBlank
        private String primaryWarehouseCode;
        private String backupWarehouseCode;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionItem {
        private String code;
        private String name;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionsRes {
        private List<OptionItem> stores;
        private List<OptionItem> warehouses;
    }
}
