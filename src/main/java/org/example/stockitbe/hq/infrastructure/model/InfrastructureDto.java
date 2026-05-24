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

    @Getter
    @AllArgsConstructor
    @Builder
    public static class Res {
        // FE 가 warehouseId/locationId 가 필요한 BE 엔드포인트(예: 새 발주 카탈로그
        // /api/hq/purchase-orders/catalog?warehouseId=) 에 전달하기 위해 노출.
        // 외부 식별자는 code 가 정석이지만 catalog query 는 inventory.location_id (Long) 매핑이라
        // id 가 필요. 다른 화면들은 기존처럼 code 만 써도 무방.
        private Long id;
        private String code;
        private LocationType locationType;
        private String name;
        private String region;
        private String managerName;
        private String contact;
        private String address;
        private InfraStatus status;
        private Long mappedStoreCount;
        private Date updatedAt;

        public static Res from(Infrastructure infra, Long mappedStoreCount) {
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
                    .updatedAt(infra.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class PublicRes {
        private String code;            // WH-SL-0001
        private LocationType locationType;  // STORE | WAREHOUSE
        private String name;            // 서울 도심 풀필먼트 허브
        private String region;          // 서울
    }
}
