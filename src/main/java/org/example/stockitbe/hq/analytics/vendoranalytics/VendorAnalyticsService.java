package org.example.stockitbe.hq.analytics.vendoranalytics;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.hq.analytics.vendoranalytics.model.VendorAnalyticsDto;
import org.example.stockitbe.hq.analytics.vendoranalytics.model.VendorPeriod;
import org.example.stockitbe.hq.circularbuyer.repository.CircularBuyerTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorAnalyticsService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final CircularBuyerTransactionRepository txRepo;

    public VendorAnalyticsDto.Res getVendorAnalytics(
            VendorPeriod period, LocalDate from, LocalDate to) {

        Date fromDate = toDate(from.atStartOfDay());
        Date toDateExcl = toDate(to.plusDays(1).atStartOfDay());

        // KPI
        List<Object[]> kpiRows = txRepo.kpiAggregate(fromDate, toDateExcl);
        Object[] kpiRow = kpiRows.isEmpty() ? null : kpiRows.get(0);
        long totalSales    = kpiRow == null ? 0L : ((Number) kpiRow[0]).longValue();
        int  activeVendors = kpiRow == null ? 0  : ((Number) kpiRow[1]).intValue();
        int  activeMats    = kpiRow == null ? 0  : ((Number) kpiRow[2]).intValue();

        List<Object[]> topVList = txRepo.topVendor(fromDate, toDateExcl);
        Object[] topV = topVList.isEmpty() ? null : topVList.get(0);
        String topVendorName = topV != null ? (String) topV[0] : "";
        long   topVendorAmt  = topV != null ? ((Number) topV[1]).longValue() : 0L;

        // topMaterial: 매출 기준 → 판매량(kg) 기준으로 전환. r[1] 의미가 amount → weight.
        List<Object[]> topMList = txRepo.topMaterial(fromDate, toDateExcl);
        Object[] topM = topMList.isEmpty() ? null : topMList.get(0);
        String topMaterialName   = topM != null ? (String) topM[0] : "";
        long   topMaterialWeight = topM != null ? ((Number) topM[1]).longValue() : 0L;

        VendorAnalyticsDto.KpiSummary kpi = VendorAnalyticsDto.KpiSummary.builder()
                .activeVendorCount(activeVendors)
                .activeMaterialCount(activeMats)
                .topVendorName(topVendorName)
                .topVendorAmount(BigDecimal.valueOf(topVendorAmt))
                .topMaterialName(topMaterialName)
                .topMaterialWeight(topMaterialWeight)
                .totalSalesAmount(BigDecimal.valueOf(totalSales))
                .build();

        // 거래처 상세
        List<VendorAnalyticsDto.VendorStats> vendors = txRepo.aggregateByVendor(fromDate, toDateExcl)
                .stream().map(r -> VendorAnalyticsDto.VendorStats.builder()
                        .name((String) r[0])
                        .material(r[1] == null ? "" : (String) r[1])
                        .unitPrice(r[2] == null ? 0 : ((Number) r[2]).intValue())
                        .orderWeight(((Number) r[3]).longValue())
                        .orderValue(BigDecimal.valueOf(((Number) r[4]).longValue()))
                        .build())
                .toList();

        // 소재 상세
        List<VendorAnalyticsDto.MaterialStats> materials = txRepo.aggregateByMaterial(fromDate, toDateExcl)
                .stream().map(r -> {
                    String group = (String) r[2];
                    String materialType = mapMaterialType(group);
                    return VendorAnalyticsDto.MaterialStats.builder()
                            .materialCode((String) r[0])
                            .name((String) r[1])
                            .materialType(materialType)
                            .units(((Number) r[3]).longValue())
                            .sales(BigDecimal.valueOf(((Number) r[4]).longValue()))
                            .eco("천연 단일 섬유".equals(materialType))
                            .build();
                })
                .toList();

        return VendorAnalyticsDto.Res.builder()
                .fromDate(from.format(DATE_FMT))
                .toDate(to.format(DATE_FMT))
                .period(period)
                .kpi(kpi)
                .vendors(vendors)
                .circularMaterials(materials)
                .build();
    }

    /** circular_material_price_policy.material_group → FE 표시 라벨 변환. 팀원 InventoryService/Recommend 라벨과 통일. */
    private static String mapMaterialType(String group) {
        if (group == null) return "기타";
        return switch (group) {
            case "NATURAL_SINGLE" -> "천연 단일 섬유";
            case "SYNTHETIC"      -> "합성 섬유";
            case "BLEND"          -> "혼방";
            default -> group;
        };
    }

    private static Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
