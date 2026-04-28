package org.example.stockitbe.hq.category.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CategoryDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        @NotBlank
        private String name;
        @NotNull
        private CategoryLevel level;
        private String parentCode;
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

    @Getter
    @AllArgsConstructor
    @Builder
    public static class TreeRes {
        private String code;
        private String name;
        private CategoryLevel level;
        private CategoryStatus status;
        private String parentCode;
        private Date updatedAt;
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

    @Getter
    @AllArgsConstructor
    @Builder
    public static class DetailRes {
        private String code;
        private String name;
        private CategoryLevel level;
        private CategoryStatus status;
        private String parentCode;
        private Date createdAt;
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
