package org.example.stockitbe.hq.analytics.turnoveranalytics.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

public class TurnoverAnalyticsDto {

    @Getter
    @Builder
    public static class Res {
        private String fromDate;
        private String toDate;
        private TurnoverPeriod period;
        private TurnoverScope scope;
        private String locationCode;                  // 특정 위치 한정 (옵션, null=전체)
        private List<LocationStats> locationStats;    // 매장·창고별 회전율 (블록 2)
        private InventoryHealth inventoryHealth;      // 신호등 카운트 + 모달 SKU 리스트 (블록 5)
    }

    @Getter
    @Builder
    public static class LocationStats {
        private String code;             // infrastructure.code (예: "ST-SL-0001")
        private String name;             // infrastructure.name
        private String type;             // "매장" | "창고"  (Service 에서 LocationType→한글 매핑)
        private long avgInventory;       // 위치별 현재 재고 SUM (단순화)
        private long sales;              // 기간 내 판매 수량 SUM
        private BigDecimal turnover;     // 회전율 (소수 1자리)
        private BigDecimal daysOnHand;   // 365 / turnover (소수 1자리, turnover=0 이면 999.0 cap)
        private String status;           // "
    }

    @Getter
    @Builder
    public static class InventoryHealth {
        private long totalSku;             // inventory row 수
        private long healthy;              // turnover ≥ 4
        private long caution;              // 2 ≤ turnover < 4
        private long warning;              // 1 ≤ turnover < 2
        private long danger;               // turnover < 1
        private BigDecimal totalValue;     // 전체 재고 가치 (M원, 소수 1자리)
        private BigDecimal lockedValue;    // danger 단계 묶인 금액 (M원)
        private List<SkuItem> healthySkus; // 모달용 — Top 50 샘플 (회전율 내림차순)
        private List<SkuItem> cautionSkus;
        private List<SkuItem> warningSkus; // 회전율 오름차순 (악성에 가까운 순)
        private List<SkuItem> dangerSkus;
    }

    @Getter
    @Builder
    public static class SkuItem {
        private String skuCode;          // product_sku.code
        private String productName;      // product_master.name
        private String category;         // category 한글 (Service 매핑)
        private String location;         // 재고 위치 infrastructure.name
        private BigDecimal turnover;
        private long daysOnHand;
        private long units;              // inventory.quantity
        private BigDecimal value;
    }
}
