package org.example.stockitbe.store.sale.repository;

import org.example.stockitbe.store.sale.model.StoreSaleStatus;
import org.example.stockitbe.store.sale.model.entity.StoreSaleHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface StoreSaleHeaderRepository extends JpaRepository<StoreSaleHeader, Long> {
    Optional<StoreSaleHeader> findBySaleNo(String saleNo);
    List<StoreSaleHeader> findAllByOrderBySoldAtDescIdDesc();

    // ── 분석용 KPI 집계 ──
    @Query("SELECT COALESCE(SUM(h.totalAmount), 0) FROM StoreSaleHeader h " +
            "WHERE h.status = :status AND h.soldAt >= :from AND h.soldAt < :to " +
            "  AND (:storeId IS NULL OR h.storeId = :storeId)")
    Long sumTotalAmount(@Param("status") StoreSaleStatus status,
                        @Param("from") Date from, @Param("to") Date to,
                        @Param("storeId") Long storeId);

    @Query("SELECT COALESCE(SUM(h.totalQuantity), 0) FROM StoreSaleHeader h " +
            "WHERE h.status = :status AND h.soldAt >= :from AND h.soldAt < :to " +
            "  AND (:storeId IS NULL OR h.storeId = :storeId)")
    Long sumTotalQuantity(@Param("status") StoreSaleStatus status,
                          @Param("from") Date from, @Param("to") Date to,
                          @Param("storeId") Long storeId);

    @Query("SELECT COUNT(DISTINCT h.storeId) FROM StoreSaleHeader h " +
            "WHERE h.status = :status AND h.soldAt >= :from AND h.soldAt < :to")
    Long countActiveStores(@Param("status") StoreSaleStatus status,
                           @Param("from") Date from, @Param("to") Date to);

    // 일자별 매출
    @Query(value = "SELECT DATE(h.sold_at) AS day, " +
            "       COALESCE(SUM(h.total_amount), 0) AS revenue " +
            "FROM store_sale_header h " +
            "WHERE h.status = :status " +
            "  AND h.sold_at >= :from AND h.sold_at < :to " +
            "  AND (:storeId IS NULL OR h.store_id = :storeId) " +
            "GROUP BY DATE(h.sold_at) " +
            "ORDER BY DATE(h.sold_at) ASC",
            nativeQuery = true)
    List<Object[]> dailyRevenue(@Param("status") String status,
                                @Param("from") Date from, @Param("to") Date to,
                                @Param("storeId") Long storeId);
}

