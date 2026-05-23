package org.example.stockitbe.hq.esg.emissionquota;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.hq.esg.emissionquota.model.EmissionQuota;
import org.example.stockitbe.hq.esg.emissionquota.model.EmissionQuotaDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmissionQuotaService {

    private final EmissionQuotaRepository repository;

    /** GET — 없으면 기본값 새로 생성해서 반환 (영속화) */
    @Transactional
    public EmissionQuotaDto.Response getQuota(Integer year) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        EmissionQuota quota = repository.findByFiscalYear(targetYear)
                .orElseGet(() -> repository.save(EmissionQuota.builder()
                        .fiscalYear(targetYear)
                        .yearlyAllocation(BigDecimal.ZERO)
                        .warnThresholdPct(75)
                        .build()));
        return toResponse(quota);
    }

    /** PUT — 갱신 (없으면 새로 생성하고 갱신) */
    @Transactional
    public EmissionQuotaDto.Response updateQuota(Integer year,
                                                 EmissionQuotaDto.UpdateRequest req,
                                                 String employeeCode) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        EmissionQuota quota = repository.findByFiscalYear(targetYear)
                .orElseGet(() -> EmissionQuota.builder()
                        .fiscalYear(targetYear)
                        .yearlyAllocation(BigDecimal.ZERO)
                        .warnThresholdPct(75)
                        .build());

        // 2026-05-22: 탄소 배출 한도 알림(ESG_QUOTA_WARNING/EXCEEDED) 발송 로직 제거 — 알림 종류 4종 정리.
        //   EmissionQuota 도메인의 한도 조회/저장 기능은 유지하되, 임계 전이 알림은 더 이상 발송하지 않음.
        quota.update(
                req.getYearlyAllocation(),
                req.getMonthlyEmissions(),
                req.getWarnThresholdPct(),
                employeeCode
        );
        EmissionQuota saved = repository.save(quota);
        return toResponse(saved);
    }

    /** Entity → Response (분기/YTD 자동 합계) */
    private EmissionQuotaDto.Response toResponse(EmissionQuota q) {
        BigDecimal[] monthly = q.getMonthlyEmissionsArray();   // 길이 12, null 가능

        // 분기별 합계 (null 은 0 으로)
        BigDecimal[] quarterly = new BigDecimal[4];
        for (int qi = 0; qi < 4; qi++) {
            BigDecimal sum = BigDecimal.ZERO;
            for (int m = qi * 3; m < qi * 3 + 3; m++) {
                if (monthly[m] != null) sum = sum.add(monthly[m]);
            }
            quarterly[qi] = sum;
        }

        // YTD (12개월 합계)
        BigDecimal ytd = BigDecimal.ZERO;
        for (BigDecimal v : monthly) if (v != null) ytd = ytd.add(v);

        BigDecimal allocation = q.getYearlyAllocation();
        BigDecimal remaining  = allocation.subtract(ytd);

        BigDecimal utilizationPct = BigDecimal.ZERO;
        if (allocation.compareTo(BigDecimal.ZERO) > 0) {
            utilizationPct = ytd.multiply(BigDecimal.valueOf(100))
                    .divide(allocation, 1, RoundingMode.HALF_UP);
        }
        boolean warning = utilizationPct.compareTo(BigDecimal.valueOf(q.getWarnThresholdPct())) >= 0;

        return EmissionQuotaDto.Response.builder()
                .fiscalYear(q.getFiscalYear())
                .yearlyAllocation(allocation)
                .monthlyEmissions(monthly)
                .quarterlyEmissions(quarterly)
                .ytdEmissions(ytd)
                .remaining(remaining)
                .utilizationPct(utilizationPct)
                .warning(warning)
                .warnThresholdPct(q.getWarnThresholdPct())
                .updatedBy(q.getUpdatedBy())
                .updatedAt(q.getUpdatedAt())
                .build();
    }
}
