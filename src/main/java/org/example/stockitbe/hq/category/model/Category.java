package org.example.stockitbe.hq.category.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

@Entity
@Table(name = "category", uniqueConstraints = {
        @UniqueConstraint(name = "uk_category_code", columnNames = "code")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 16)
    private CategoryLevel level;

    @Column(name = "parent_id")
    private Long parentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CategoryStatus status;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Builder
    private Category(String code, String name, CategoryLevel level, Long parentId,
                     CategoryStatus status, Integer sortOrder) {
        this.code = code;
        this.name = name;
        this.level = level;
        this.parentId = parentId;
        this.status = status == null ? CategoryStatus.ACTIVE : status;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
    }
}
