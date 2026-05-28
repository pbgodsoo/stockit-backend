package org.example.stockitbe.hq.infrastructure.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Date;

public class InfrastructureDto {

    @Schema(description = "매장/창고 등록 또는 수정 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpsertReq {
        @Schema(description = "거점 유형. 허용값: STORE, WAREHOUSE", example = "STORE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull private LocationType locationType;
        @Schema(description = "매장/창고명", example = "강남 플래그십 매장", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank private String name;
        @Schema(description = "지역명 또는 지역 코드. 예: 서울/SL, 경기/GG, 인천/IC, 부산/BS, 대전/DJ, 광주/GJ, 강원/GW, 제주/JJ, 충청/CN, 영남/YN, 호남/HN", example = "서울", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank private String region;
        @Schema(description = "담당자명", example = "김담당", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank private String managerName;
        @Schema(description = "담당자 연락처", example = "010-1234-5678", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank private String contact;
        @Schema(description = "주소", example = "서울특별시 강남구 테헤란로 123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank private String address;
        @Schema(description = "거점 상태. 허용값: ACTIVE, INACTIVE, SUSPENDED", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull private InfraStatus status;

        public Infrastructure toEntity(String code) {
            return Infrastructure.builder()
                    .code(code)
                    .locationType(locationType)
                    .name(name.trim())
                    .region(region.trim())
                    .managerName(managerName.trim())
                    .contact(contact.trim())
                    .address(address.trim())
                    .status(status)
                    .build();
        }
    }

    @Schema(description = "매장/창고 관리 응답")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class Res {
        // FE 가 warehouseId/locationId 가 필요한 BE 엔드포인트(예: 새 발주 카탈로그
        // /api/hq/purchase-orders/catalog?warehouseId=) 에 전달하기 위해 노출.
        // 외부 식별자는 code 가 정석이지만 catalog query 는 inventory.location_id (Long) 매핑이라
        // id 가 필요. 다른 화면들은 기존처럼 code 만 써도 무방.
        @Schema(description = "거점 내부 ID", example = "1")
        private Long id;
        @Schema(description = "매장/창고 코드", example = "ST-SL-0001")
        private String code;
        @Schema(description = "거점 유형", example = "STORE")
        private LocationType locationType;
        @Schema(description = "매장/창고명", example = "강남 플래그십 매장")
        private String name;
        @Schema(description = "지역", example = "서울")
        private String region;
        @Schema(description = "담당자명", example = "김담당")
        private String managerName;
        @Schema(description = "담당자 연락처", example = "010-1234-5678")
        private String contact;
        @Schema(description = "주소", example = "서울특별시 강남구 테헤란로 123")
        private String address;
        @Schema(description = "거점 상태", example = "ACTIVE")
        private InfraStatus status;
        @Schema(description = "창고에 매핑된 매장 수. 창고 응답에서 사용", example = "3")
        private Long mappedStoreCount;
        @Schema(description = "매장에 매핑된 주 창고 코드. 매장 응답에서 사용", example = "WH-SL-0001")
        private String primaryWarehouseCode;
        @Schema(description = "매장에 매핑된 주 창고명. 매장 응답에서 사용", example = "서울 물류창고")
        private String primaryWarehouseName;
        @Schema(description = "수정 일시", example = "2026-05-27T09:00:00.000+09:00")
        private Date updatedAt;

        public static Res from(Infrastructure infra, Long mappedStoreCount, StoreWarehouseMap primaryWarehouseMap) {
            Infrastructure primaryWarehouse = primaryWarehouseMap == null ? null : primaryWarehouseMap.getWarehouse();
            return Res.builder()
                    .id(infra.getId())
                    .code(infra.getCode())
                    .locationType(infra.getLocationType())
                    .name(infra.getName())
                    .region(infra.getRegion())
                    .managerName(infra.getManagerName())
                    .contact(infra.getContact())
                    .address(infra.getAddress())
                    .status(infra.getStatus())
                    .mappedStoreCount(mappedStoreCount)
                    .primaryWarehouseCode(primaryWarehouse == null ? null : primaryWarehouse.getCode())
                    .primaryWarehouseName(primaryWarehouse == null ? null : primaryWarehouse.getName())
                    .updatedAt(infra.getUpdatedAt())
                    .build();
        }
    }

    @Schema(description = "회원가입용 매장/창고 공개 응답 — 비로그인 상태에서 노출")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class PublicRes {
        @Schema(description = "거점 코드", example = "WH-GW-0001")
        private String code;
        @Schema(description = "거점 유형", example = "WAREHOUSE", allowableValues = {"STORE","WAREHOUSE"})
        private LocationType locationType;
        @Schema(description = "거점 이름", example = "강원 강릉 동해안 물류허브")
        private String name;
        @Schema(description = "지역 한글명", example = "강원")
        private String region;
    }
}
