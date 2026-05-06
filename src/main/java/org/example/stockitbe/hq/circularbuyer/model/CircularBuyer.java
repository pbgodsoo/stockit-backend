package org.example.stockitbe.hq.circularbuyer.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "circular_buyer", uniqueConstraints = {
        @UniqueConstraint(name = "uk_circular_buyer_code", columnNames = "code")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CircularBuyer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "company_name", nullable = false, length = 128)
    private String companyName;

    @Column(name = "industry_group", nullable = false, length = 64)
    private String industryGroup;

    // 생산품 키워드 배열 — JSON 컬럼 (정규화 X, 검색 빈도 낮음)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "product_types", columnDefinition = "json")
    private List<String> productTypes;

    @Column(name = "product_note", columnDefinition = "TEXT")
    private String productNote;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // 1층 SQL 룰 필터의 유일한 근거 (ADR-021). natural-single / synthetic / blended.
    @Column(name = "primary_material_fit", nullable = false, length = 32)
    private String primaryMaterialFit;

    @Column(name = "manager_name", nullable = false, length = 64)
    private String managerName;

    @Column(name = "phone", nullable = false, length = 32)
    private String phone;

    // 파트너 유형 — local_small / social_enterprise / general. 점수 가산 룰은 사이클 3 (거래 이력 합류 후) 에서.
    @Column(name = "partner_type", nullable = false, length = 32)
    @ColumnDefault("'general'")
    private String partnerType;

    // 임베딩 1536차원 — AI 추천 phase 에서 채움. NULL 허용 (ADR-021).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "embedding", columnDefinition = "json")
    private List<Double> embedding;

    @Builder
    private CircularBuyer(String code, String companyName, String industryGroup,
                           List<String> productTypes, String productNote, String description,
                           String primaryMaterialFit, String managerName, String phone,
                           String partnerType, List<Double> embedding) {
        this.code = code;
        this.companyName = companyName;
        this.industryGroup = industryGroup;
        this.productTypes = productTypes;
        this.productNote = productNote;
        this.description = description;
        this.primaryMaterialFit = primaryMaterialFit;
        this.managerName = managerName;
        this.phone = phone;
        this.partnerType = partnerType;
        this.embedding = embedding;
    }

    /**
     * 거래처 정보 수정. embedding 은 별도 메소드.
     * 의미 필드(companyName/industryGroup/productTypes/productNote/description/primaryMaterialFit) 변경 시 임베딩 재생성 룰은 Service 책임.
     * partnerType 은 의미 필드가 아님 — 임베딩 재생성 트리거 X.
     */
    public void updateProfile(String companyName, String industryGroup, List<String> productTypes,
                               String productNote, String description, String primaryMaterialFit,
                               String managerName, String phone, String partnerType) {
        if (companyName != null) this.companyName = companyName;
        if (industryGroup != null) this.industryGroup = industryGroup;
        if (productTypes != null) this.productTypes = productTypes;
        if (productNote != null) this.productNote = productNote;
        if (description != null) this.description = description;
        if (primaryMaterialFit != null) this.primaryMaterialFit = primaryMaterialFit;
        if (managerName != null) this.managerName = managerName;
        if (phone != null) this.phone = phone;
        if (partnerType != null) this.partnerType = partnerType;
    }

    public void updateEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }
}
