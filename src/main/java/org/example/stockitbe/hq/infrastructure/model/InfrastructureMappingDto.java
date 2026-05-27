package org.example.stockitbe.hq.infrastructure.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Date;
import java.util.List;

public class InfrastructureMappingDto {

    @Schema(description = "매장별 창고 매핑 행")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StoreMappingItem {
        @Schema(description = "매장 내부 ID", example = "1")
        private Long storeId;
        @Schema(description = "매장 코드", example = "ST-SL-0001")
        private String storeCode;
        @Schema(description = "매장명", example = "강남 플래그십 매장")
        private String storeName;
        @Schema(description = "지역", example = "서울")
        private String region;
        @Schema(description = "매장 상태", example = "ACTIVE")
        private InfraStatus status;
        @Schema(description = "주 창고 코드", example = "WH-SL-0001")
        private String primaryWarehouseCode;
        @Schema(description = "주 창고명", example = "서울 물류창고")
        private String primaryWarehouseName;
        @Schema(description = "예비 창고 코드", example = "WH-GG-0001")
        private String backupWarehouseCode;
        @Schema(description = "예비 창고명", example = "경기 물류창고")
        private String backupWarehouseName;
        @Schema(description = "매장 정보 수정 일시")
        private Date updatedAt;
    }

    @Schema(description = "매장 창고 매핑 저장 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpsertReq {
        @Schema(description = "주 창고 코드", example = "WH-SL-0001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String primaryWarehouseCode;
        @Schema(description = "예비 창고 코드", example = "WH-GG-0001")
        private String backupWarehouseCode;
    }

    @Schema(description = "매핑 선택 옵션 항목")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionItem {
        @Schema(description = "매장/창고 코드", example = "WH-SL-0001")
        private String code;
        @Schema(description = "매장/창고명", example = "서울 물류창고")
        private String name;
    }

    @Schema(description = "매장-창고 매핑 선택 옵션 응답")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionsRes {
        @Schema(description = "매장 옵션 목록")
        private List<OptionItem> stores;
        @Schema(description = "창고 옵션 목록")
        private List<OptionItem> warehouses;
    }
}
