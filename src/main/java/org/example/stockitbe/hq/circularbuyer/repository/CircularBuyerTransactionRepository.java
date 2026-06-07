package org.example.stockitbe.hq.circularbuyer.repository;

import org.example.stockitbe.hq.circularbuyer.model.CircularBuyerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public interface CircularBuyerTransactionRepository extends JpaRepository<CircularBuyerTransaction, Long> {

    long countByTransactedAtBetween(Date from, Date to);

    /** KPI: 기간 내 총 매출, 활성 거래처 수, 거래된 소재 종류 수. (SALE 거래만 집계) */
    @Query(value = """
        SELECT
            COALESCE(SUM(t.total_amount), 0)     AS totalSales,
            COUNT(DISTINCT t.buyer_id)           AS activeVendors,
            COUNT(DISTINCT t.material_code)      AS activeMaterials
        FROM circular_buyer_transaction t
        WHERE t.transacted_at >= :from
          AND t.transacted_at <  :to
          AND t.sale_type = 'SALE'
        """, nativeQuery = true)
    List<Object[]> kpiAggregate(@Param("from") Date from, @Param("to") Date to);

    /** TOP 거래처 (매출 1위). (SALE 거래만 집계) */
    @Query(value = """
        SELECT b.company_name AS name, SUM(t.total_amount) AS amount
        FROM circular_buyer_transaction t
        JOIN circular_buyer b ON b.id = t.buyer_id
        WHERE t.transacted_at >= :from AND t.transacted_at < :to
          AND t.sale_type = 'SALE'
        GROUP BY b.company_name
        ORDER BY amount DESC
        LIMIT 1
        """, nativeQuery = true)
    List<Object[]> topVendor(@Param("from") Date from, @Param("to") Date to);

    /** TOP 소재 (판매량 1kg 1위, 한글명). 정렬 기준을 매출 → 판매 무게 로 변경. (SALE 거래만 집계) */
    @Query(value = """
        SELECT p.material_name_ko AS name, SUM(t.weight_kg) AS weight
        FROM circular_buyer_transaction t
        JOIN circular_material_price_policy p ON p.material_code = t.material_code
        WHERE t.transacted_at >= :from AND t.transacted_at < :to
          AND t.sale_type = 'SALE'
        GROUP BY p.material_name_ko
        ORDER BY weight DESC
        LIMIT 1
        """, nativeQuery = true)
    List<Object[]> topMaterial(@Param("from") Date from, @Param("to") Date to);

    /**
     * 거래처별 집계. (SALE 거래만 집계)
     * 각 거래처가 가장 많이 거래한 소재명 + 단가 + 총 weight + 총 매출.
     * 결과 컬럼 순서: company_name, material_name_ko, price_per_kg, total_weight, total_amount
     */
    @Query(value = """
        SELECT
            b.company_name,
            (SELECT p2.material_name_ko
               FROM circular_buyer_transaction t2
               JOIN circular_material_price_policy p2 ON p2.material_code = t2.material_code
              WHERE t2.buyer_id = b.id
                AND t2.transacted_at >= :from AND t2.transacted_at < :to
                AND t2.sale_type = 'SALE'
              GROUP BY p2.material_name_ko
              ORDER BY SUM(t2.total_amount) DESC
              LIMIT 1)                         AS material_name_ko,
            (SELECT p3.price_per_kg
               FROM circular_buyer_transaction t3
               JOIN circular_material_price_policy p3 ON p3.material_code = t3.material_code
              WHERE t3.buyer_id = b.id
                AND t3.transacted_at >= :from AND t3.transacted_at < :to
                AND t3.sale_type = 'SALE'
              GROUP BY p3.material_code, p3.price_per_kg
              ORDER BY SUM(t3.total_amount) DESC
              LIMIT 1)                         AS unit_price,
            SUM(t.weight_kg)                   AS total_weight,
            SUM(t.total_amount)                AS total_amount
        FROM circular_buyer_transaction t
        JOIN circular_buyer b ON b.id = t.buyer_id
        WHERE t.transacted_at >= :from AND t.transacted_at < :to
          AND t.sale_type = 'SALE'
        GROUP BY b.id, b.company_name
        ORDER BY total_amount DESC
        """, nativeQuery = true)
    List<Object[]> aggregateByVendor(@Param("from") Date from, @Param("to") Date to);

    /**
     * 소재별 집계 (순환재고 상세). (SALE 거래만 집계)
     * 결과: material_code, material_name_ko, material_group, total_weight, total_amount
     * 정렬 기준을 매출 → 판매 무게(kg) 로 변경 — 소재 매출 순위가 kg 기준으로 노출.
     */
    @Query(value = """
        SELECT
            p.material_code,
            p.material_name_ko,
            p.material_group,
            SUM(t.weight_kg)     AS total_weight,
            SUM(t.total_amount)  AS total_amount
        FROM circular_buyer_transaction t
        JOIN circular_material_price_policy p ON p.material_code = t.material_code
        WHERE t.transacted_at >= :from AND t.transacted_at < :to
          AND t.sale_type = 'SALE'
        GROUP BY p.material_code, p.material_name_ko, p.material_group
        ORDER BY total_weight DESC
        """, nativeQuery = true)
    List<Object[]> aggregateByMaterial(@Param("from") Date from, @Param("to") Date to);

    /**
     * 지정 연도의 월별 수익/거래 건수 집계. (SALE 거래만 집계)
     * 결과 컬럼: month(1~12), revenue(SUM total_amount), cnt(COUNT)
     *
     * 성능 개선 (2026-05-23): WHERE 절에서 YEAR(transacted_at) 함수 제거 → 범위 조건으로 변경.
     *   - Before: WHERE YEAR(transacted_at) = ?  → 컬럼에 함수 적용으로 인덱스 사용 불가 (풀 스캔)
     *   - After : WHERE transacted_at >= :from AND transacted_at < :to → transacted_at 인덱스 활용
     *   - GROUP BY MONTH(...) 는 결과 그룹핑용이라 인덱스 영향 없음 (유지)
     */
    @Query(value = """
        SELECT MONTH(t.transacted_at) AS m,
               COALESCE(SUM(t.total_amount), 0) AS revenue,
               COUNT(t.id) AS cnt
        FROM circular_buyer_transaction t
        WHERE t.transacted_at >= :from
          AND t.transacted_at <  :to
          AND t.sale_type = 'SALE'
        GROUP BY MONTH(t.transacted_at)
        ORDER BY MONTH(t.transacted_at)
        """, nativeQuery = true)
    List<Object[]> aggregateMonthlyRevenue(@Param("from") Date from, @Param("to") Date to);

    /**
     * 점수 페이지용 — 지정 기간의 거래 이벤트 + 거래처 첫 거래일(newBuyer 판정용) + partner_type + sale_type.
     * DONATION은 buyer_id=NULL 이므로 LEFT JOIN + COALESCE 로 처리.
     * 결과 컬럼:
     *   r[0] id
     *   r[1] transacted_at
     *   r[2] COALESCE(company_name, donee_name)  — SALE: 거래처명, DONATION: 기부처명
     *   r[3] material_code
     *   r[4] weight_kg
     *   r[5] first_tx_at  — SALE: 해당 buyer 최초 거래 시점, DONATION: 자기 시점 (newBuyer 보너스 미적용)
     *   r[6] COALESCE(partner_type, 'general')   — DONATION은 'general' 폴백
     *   r[7] sale_type                            — "SALE" | "DONATION"
     *
     * 성능 개선 (2026-05-23): WHERE YEAR(transacted_at) 함수 제거 → 범위 조건. 인덱스 활용.
     */
    @Query(value = """
        SELECT t.id,
               t.transacted_at,
               COALESCE(b.company_name, t.donee_name) AS display_name,
               t.material_code,
               t.weight_kg,
               CASE WHEN t.buyer_id IS NOT NULL
                    THEN (SELECT MIN(t2.transacted_at)
                            FROM circular_buyer_transaction t2
                           WHERE t2.buyer_id = t.buyer_id)
                    ELSE t.transacted_at
               END AS first_tx_at,
               COALESCE(b.partner_type, 'general') AS partner_type,
               t.sale_type
        FROM circular_buyer_transaction t
        LEFT JOIN circular_buyer b ON b.id = t.buyer_id
        WHERE t.transacted_at >= :from
          AND t.transacted_at <  :to
        ORDER BY t.id DESC
        """, nativeQuery = true)
    List<Object[]> findEventsForYear(@Param("from") Date from, @Param("to") Date to);

    /**
     * 판매 헤더 1건의 ESG 점수 계산용 — transacted_at 정확 매칭 + 거래처(SALE) 또는 기부처(DONATION).
     * findEventsForYear 와 동일한 컬럼 구조 반환 (ScoreEventsService.buildGroupEventDto 재사용).
     * SALE:     buyer_id = :buyerId
     * DONATION: buyer_id IS NULL AND donee_name = :doneeName
     * NULL 비교는 SQL 표준 동작(NULL = x → false)으로 자연 분기됨.
     */
    @Query(value = """
        SELECT t.id,
               t.transacted_at,
               COALESCE(b.company_name, t.donee_name) AS display_name,
               t.material_code,
               t.weight_kg,
               CASE WHEN t.buyer_id IS NOT NULL
                    THEN (SELECT MIN(t2.transacted_at)
                            FROM circular_buyer_transaction t2
                           WHERE t2.buyer_id = t.buyer_id)
                    ELSE t.transacted_at
               END AS first_tx_at,
               COALESCE(b.partner_type, 'general') AS partner_type,
               t.sale_type
        FROM circular_buyer_transaction t
        LEFT JOIN circular_buyer b ON b.id = t.buyer_id
        WHERE t.transacted_at = :transactedAt
          AND (
                (t.sale_type = 'SALE'     AND t.buyer_id  = :buyerId)
             OR (t.sale_type = 'DONATION' AND t.buyer_id IS NULL AND t.donee_name = :doneeName)
          )
        ORDER BY t.id DESC
        """, nativeQuery = true)
    List<Object[]> findEventsForSaleHeader(
            @Param("transactedAt") LocalDateTime transactedAt,
            @Param("buyerId") Long buyerId,
            @Param("doneeName") String doneeName);
}
