package org.example.stockitbe.hq.vendor.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

import java.time.LocalDate;

@Entity
@Table(name = "vendor_product",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_vendor_product_code", columnNames = "code")
    },
    // (product_code, status) 복합 인덱스 — 새 발주 카탈로그 4-way JOIN 의
    // ProductSku ↔ VendorProduct nested loop 풀스캔 회피 (ADR-027).
    indexes = {
        @Index(name = "idx_vendor_product_product_code_status", columnList = "product_code, status")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VendorProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    // 외부 도메인 — JPA 정석 매핑 (ADR-024).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    // 마스터 제품 자연 키 — ProductMaster.code (String 자연 키, 매핑 안 박음).
    @Column(name = "product_code", nullable = false, length = 64)
    private String productCode;

    // 계약 당시 제품명 (시점 복사)
    @Column(name = "product_name", nullable = false, length = 256)
    private String productName;

    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Column(name = "moq")
    private Integer moq;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(name = "contract_start")
    private LocalDate contractStart;

    @Column(name = "contract_end")
    private LocalDate contractEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private VendorProductStatus status;

    @Builder
    private VendorProduct(String code, Vendor vendor, String productCode, String productName,
                          Long unitPrice, Integer moq, Integer leadTimeDays,
                          LocalDate contractStart, LocalDate contractEnd, VendorProductStatus status) {
        this.code = code;
        this.vendor = vendor;
        this.productCode = productCode;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.moq = moq;
        this.leadTimeDays = leadTimeDays;
        this.contractStart = contractStart;
        this.contractEnd = contractEnd;
        this.status = status == null ? VendorProductStatus.ACTIVE : status;
    }

    /**
     * 계약 정보 수정 (단가·MOQ·납기·계약기간).
     * productName 은 ProductMaster 가 진실 원천이라 VendorProduct 측에서 변경 액션 폐기 — 컬럼은 시점 복사로 발주 이력 보호용 유지.
     * status 변경은 changeStatus() 통해서만.
     */
    public void updateContract(Long unitPrice, Integer moq, Integer leadTimeDays,
                                LocalDate contractStart, LocalDate contractEnd) {
        if (unitPrice != null) this.unitPrice = unitPrice;
        if (moq != null) this.moq = moq;
        if (leadTimeDays != null) this.leadTimeDays = leadTimeDays;
        if (contractStart != null) this.contractStart = contractStart;
        if (contractEnd != null) this.contractEnd = contractEnd;
    }

    public void changeStatus(VendorProductStatus newStatus) {
        this.status = newStatus;
    }

    public void softDelete() {
        this.status = VendorProductStatus.DELETED;
    }
}
