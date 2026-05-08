package org.example.stockitbe.hq.esg.quota.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

import java.math.BigDecimal;

@Entity
@Table(
        name = "emission_quota",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_emission_quota_fiscal_year",
                columnNames = "fiscal_year"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmissionQuota extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    @Column(name = "yearly_allocation", nullable = false, precision = 12, scale = 2)
    private BigDecimal yearlyAllocation;

    @Column(name = "warn_threshold_pct", nullable = false)
    private Integer warnThresholdPct;

    // ─── 월별 실효 배출 (NULL = 해당 월 미입력) ──────────────────────────
    @Column(name = "m1_emissions",  precision = 12, scale = 2) private BigDecimal m1Emissions;
    @Column(name = "m2_emissions",  precision = 12, scale = 2) private BigDecimal m2Emissions;
    @Column(name = "m3_emissions",  precision = 12, scale = 2) private BigDecimal m3Emissions;
    @Column(name = "m4_emissions",  precision = 12, scale = 2) private BigDecimal m4Emissions;
    @Column(name = "m5_emissions",  precision = 12, scale = 2) private BigDecimal m5Emissions;
    @Column(name = "m6_emissions",  precision = 12, scale = 2) private BigDecimal m6Emissions;
    @Column(name = "m7_emissions",  precision = 12, scale = 2) private BigDecimal m7Emissions;
    @Column(name = "m8_emissions",  precision = 12, scale = 2) private BigDecimal m8Emissions;
    @Column(name = "m9_emissions",  precision = 12, scale = 2) private BigDecimal m9Emissions;
    @Column(name = "m10_emissions", precision = 12, scale = 2) private BigDecimal m10Emissions;
    @Column(name = "m11_emissions", precision = 12, scale = 2) private BigDecimal m11Emissions;
    @Column(name = "m12_emissions", precision = 12, scale = 2) private BigDecimal m12Emissions;

    @Column(name = "updated_by", length = 32)
    private String updatedBy;

    @Builder
    private EmissionQuota(Integer fiscalYear, BigDecimal yearlyAllocation,
                          Integer warnThresholdPct, String updatedBy) {
        this.fiscalYear        = fiscalYear;
        this.yearlyAllocation  = yearlyAllocation == null ? BigDecimal.ZERO : yearlyAllocation;
        this.warnThresholdPct  = warnThresholdPct == null ? 75 : warnThresholdPct;
        this.updatedBy         = updatedBy;
    }

    /**
     * PUT 으로 들어온 필드만 갱신.
     * monthlyEmissions: 길이 12 배열 (index 0 = 1월, index 11 = 12월).
     *                   원소가 null 이면 그 달 "미입력" 상태로 저장.
     *                   배열 자체가 null 이면 월별 값 변경 안 함.
     */
    public void update(BigDecimal yearlyAllocation,
                       BigDecimal[] monthlyEmissions,
                       Integer warnThresholdPct,
                       String updatedBy) {
        if (yearlyAllocation != null)  this.yearlyAllocation = yearlyAllocation;
        if (warnThresholdPct != null)  this.warnThresholdPct = warnThresholdPct;
        if (updatedBy != null)         this.updatedBy = updatedBy;

        if (monthlyEmissions != null && monthlyEmissions.length == 12) {
            this.m1Emissions  = monthlyEmissions[0];
            this.m2Emissions  = monthlyEmissions[1];
            this.m3Emissions  = monthlyEmissions[2];
            this.m4Emissions  = monthlyEmissions[3];
            this.m5Emissions  = monthlyEmissions[4];
            this.m6Emissions  = monthlyEmissions[5];
            this.m7Emissions  = monthlyEmissions[6];
            this.m8Emissions  = monthlyEmissions[7];
            this.m9Emissions  = monthlyEmissions[8];
            this.m10Emissions = monthlyEmissions[9];
            this.m11Emissions = monthlyEmissions[10];
            this.m12Emissions = monthlyEmissions[11];
        }
    }

    /**
     * 12개월 배출량 배열로 반환 (Service 의 합계/분기 계산용 헬퍼).
     * index 0 = 1월, index 11 = 12월. null 은 미입력.
     */
    public BigDecimal[] getMonthlyEmissionsArray() {
        return new BigDecimal[] {
                m1Emissions, m2Emissions, m3Emissions,
                m4Emissions, m5Emissions, m6Emissions,
                m7Emissions, m8Emissions, m9Emissions,
                m10Emissions, m11Emissions, m12Emissions
        };
    }
}
