package org.example.stockitbe.hq.esg.emissionquota;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.hq.esg.emissionquota.model.EmissionQuota;
import org.example.stockitbe.hq.esg.emissionquota.model.EmissionQuotaDto;
// Phase 2 알림 트리거 — 배출권 사용률 임계 (75%/100%) 전이 시 본사 알림
import org.example.stockitbe.notification.event.NotificationEvent;
import org.example.stockitbe.notification.model.entity.NotificationSeverity;
import org.example.stockitbe.notification.model.entity.NotificationType;
import org.example.stockitbe.user.model.entity.UserRole;
import org.springframework.context.ApplicationEventPublisher;
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
    // Phase 2 — 도메인 이벤트 발행자. updateQuota 직후 임계 전이 감지 시 사용
    private final ApplicationEventPublisher eventPublisher;

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

        // Phase 2 — 임계 전이 감지를 위해 갱신 *이전* 사용률 스냅샷을 저장
        //         이렇게 해야 75% 임박/100% 초과를 "처음 도달한 순간" 만 1회 알림 가능
        BigDecimal beforePct = toResponse(quota).getUtilizationPct();

        quota.update(
                req.getYearlyAllocation(),
                req.getMonthlyEmissions(),
                req.getWarnThresholdPct(),
                employeeCode
        );
        EmissionQuota saved = repository.save(quota);

        EmissionQuotaDto.Response result = toResponse(saved);
        BigDecimal afterPct = result.getUtilizationPct();
        BigDecimal warnThreshold = BigDecimal.valueOf(saved.getWarnThresholdPct()); // 보통 75%
        BigDecimal hundred = BigDecimal.valueOf(100);

        // (a) 임박 전이 — 이전 < 임계 && 이후 >= 임계 (예: 70% → 80%)
        //     동일 이상 유지 (예: 80% → 90%) 시 알림 안 나옴 — 스팸 방지
        if (beforePct.compareTo(warnThreshold) < 0 && afterPct.compareTo(warnThreshold) >= 0) {
            eventPublisher.publishEvent(NotificationEvent.builder()
                    .type(NotificationType.ESG_QUOTA_WARNING)
                    .severity(NotificationSeverity.WARNING)
                    .title("배출권 사용률 임계 임박")
                    .message(targetYear + "년 배출권 사용률 " + afterPct + "% — 경고 임계(" + warnThreshold + "%) 도달")
                    .targetRole(UserRole.HQ)
                    .refType("EMISSION_QUOTA")
                    .refId(String.valueOf(targetYear))
                    .build());
        }
        // (b) 초과 전이 — 이전 < 100 && 이후 >= 100
        //     초과는 CRITICAL 로 발행 (운영자 즉각 대응 필요)
        if (beforePct.compareTo(hundred) < 0 && afterPct.compareTo(hundred) >= 0) {
            eventPublisher.publishEvent(NotificationEvent.builder()
                    .type(NotificationType.ESG_QUOTA_EXCEEDED)
                    .severity(NotificationSeverity.CRITICAL)
                    .title("배출권 할당량 초과")
                    .message(targetYear + "년 배출권 사용률 " + afterPct + "% — 할당량을 초과했습니다.")
                    .targetRole(UserRole.HQ)
                    .refType("EMISSION_QUOTA")
                    .refId(String.valueOf(targetYear))
                    .build());
        }

        return result;
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
