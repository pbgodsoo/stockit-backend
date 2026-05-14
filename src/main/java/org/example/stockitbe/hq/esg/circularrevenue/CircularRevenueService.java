package org.example.stockitbe.hq.esg.circularrevenue;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.hq.circularbuyer.repository.CircularBuyerTransactionRepository;
import org.example.stockitbe.hq.esg.circularrevenue.model.CircularRevenueDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CircularRevenueService {

    private final CircularBuyerTransactionRepository txRepo;

    /**
     * 지정 연도의 순환재고 월별 판매 수익 집계.
     *  - year 미지정 시 현재 연도 사용
     *  - 12개월 버킷으로 채워서 응답 (거래 없는 월은 revenue=0, count=0)
     */
    public CircularRevenueDto.Response getMonthlyRevenue(Integer year) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();

        // [{ m, revenue, count }, ...] 형태로 BE 에서 받음 (거래 있는 월만)
        List<Object[]> rows = txRepo.aggregateMonthlyRevenue(targetYear);

        // 12개월 버킷 초기화
        long[] revByMonth = new long[12];
        long[] countByMonth = new long[12];
        for (Object[] r : rows) {
            int m = ((Number) r[0]).intValue();
            if (m < 1 || m > 12) continue;
            revByMonth[m - 1]   = ((Number) r[1]).longValue();
            countByMonth[m - 1] = ((Number) r[2]).longValue();
        }

        List<CircularRevenueDto.MonthlyPoint> monthly = new ArrayList<>(12);
        long total = 0L;
        long totalCnt = 0L;
        int monthsWithData = 0;
        for (int i = 0; i < 12; i++) {
            monthly.add(CircularRevenueDto.MonthlyPoint.builder()
                    .month(i + 1)
                    .revenue(revByMonth[i])
                    .count(countByMonth[i])
                    .build());
            total    += revByMonth[i];
            totalCnt += countByMonth[i];
            if (revByMonth[i] > 0) monthsWithData++;
        }
        long avg = monthsWithData > 0 ? Math.round((double) total / monthsWithData) : 0L;

        return CircularRevenueDto.Response.builder()
                .year(targetYear)
                .monthly(monthly)
                .totalRevenue(total)
                .totalCount(totalCnt)
                .monthsWithData(monthsWithData)
                .avgMonthly(avg)
                .build();
    }
}
