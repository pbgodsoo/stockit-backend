package org.example.stockitbe.hq.category.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CategoryDto {

    @Schema(description = "카테고리 등록 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        @Schema(description = "카테고리명", example = "자켓", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String name;
        @Schema(description = "카테고리 레벨. 허용값: ROOT, CHILD", example = "CHILD", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private CategoryLevel level;
        @Schema(description = "상위 카테고리 코드. CHILD 등록 시 필수, ROOT 등록 시 비워야 한다.", example = "CAT-L1-OUT")
        private String parentCode;
        @Schema(description = "카테고리 상태. 허용값: ACTIVE, SUSPENDED, INACTIVE", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private CategoryStatus status;

        public Category toEntity(String code, Long parentId, Integer sortOrder) {
            return Category.builder()
                    .code(code)
                    .name(this.name.trim())
                    .level(this.level)
                    .parentId(parentId)
                    .status(this.status)
                    .sortOrder(sortOrder)
                    .build();
        }
    }

    @Schema(description = "카테고리 수정 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateReq {
        @Schema(description = "카테고리명", example = "자켓", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String name;
        @Schema(description = "카테고리 상태. 허용값: ACTIVE, SUSPENDED, INACTIVE", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private CategoryStatus status;
    }

    @Schema(description = "카테고리 트리 응답")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class TreeRes {
        @Schema(description = "카테고리 코드", example = "CAT-L2-OUT-JK")
        private String code;
        @Schema(description = "카테고리명", example = "자켓")
        private String name;
        @Schema(description = "카테고리 레벨", example = "CHILD")
        private CategoryLevel level;
        @Schema(description = "카테고리 상태", example = "ACTIVE")
        private CategoryStatus status;
        @Schema(description = "상위 카테고리 코드", example = "CAT-L1-OUT")
        private String parentCode;
        @Schema(description = "수정 일시", example = "2026-05-27T09:00:00.000+09:00")
        private Date updatedAt;
        @Schema(description = "하위 카테고리 목록")
        private List<TreeRes> children;

        public static TreeRes from(Category category, String parentCode, List<TreeRes> children) {
            return TreeRes.builder()
                    .code(category.getCode())
                    .name(category.getName())
                    .level(category.getLevel())
                    .status(category.getStatus())
                    .parentCode(parentCode)
                    .updatedAt(category.getUpdatedAt())
                    .children(children == null ? Collections.emptyList() : children)
                    .build();
        }
    }

    @Schema(description = "카테고리 상세 응답")
    @Getter
    @AllArgsConstructor
    @Builder
    public static class DetailRes {
        @Schema(description = "카테고리 코드", example = "CAT-L2-OUT-JK")
        private String code;
        @Schema(description = "카테고리명", example = "자켓")
        private String name;
        @Schema(description = "카테고리 레벨", example = "CHILD")
        private CategoryLevel level;
        @Schema(description = "카테고리 상태", example = "ACTIVE")
        private CategoryStatus status;
        @Schema(description = "상위 카테고리 코드", example = "CAT-L1-OUT")
        private String parentCode;
        @Schema(description = "등록 일시", example = "2026-05-27T09:00:00.000+09:00")
        private Date createdAt;
        @Schema(description = "수정 일시", example = "2026-05-27T09:00:00.000+09:00")
        private Date updatedAt;

        public static DetailRes from(Category category, String parentCode) {
            return DetailRes.builder()
                    .code(category.getCode())
                    .name(category.getName())
                    .level(category.getLevel())
                    .status(category.getStatus())
                    .parentCode(parentCode)
                    .createdAt(category.getCreatedAt())
                    .updatedAt(category.getUpdatedAt())
                    .build();
        }
    }
}
