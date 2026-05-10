package org.example.stockitbe.store.sale;

import org.example.stockitbe.store.sale.model.StoreSaleStatus;
import org.example.stockitbe.store.sale.model.entity.StoreSaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface StoreSaleItemRepository extends JpaRepository<StoreSaleItem, Long> {
    List<StoreSaleItem> findAllBySaleHeaderIdOrderByIdAsc(Long saleHeaderId);
    List<StoreSaleItem> findAllBySaleHeaderIdIn(Collection<Long> saleHeaderIds);

    // 카테고리별 합계 (조인으로 기간 + 매장 + 메인카테고리 필터)
    @Query("SELECT i.mainCategory, " +
            "       COUNT(DISTINCT i.productCode), " +
            "       COALESCE(SUM(i.lineAmount), 0), " +
            "       COALESCE(SUM(i.quantity), 0) " +
            "FROM StoreSaleItem i, StoreSaleHeader h " +
            "WHERE i.saleHeaderId = h.id AND h.status = :status " +
            "  AND h.soldAt >= :from AND h.soldAt < :to " +
            "  AND (:storeId IS NULL OR h.storeId = :storeId) " +
            "GROUP BY i.mainCategory")
    List<Object[]> aggregateByMainCategory(@Param("status") StoreSaleStatus status,
                                           @Param("from") Date from, @Param("to") Date to,
                                           @Param("storeId") Long storeId);

    @Query("SELECT i.mainCategory, i.subCategory, " +
            "       COALESCE(SUM(i.quantity), 0), COALESCE(SUM(i.lineAmount), 0) " +
            "FROM StoreSaleItem i, StoreSaleHeader h " +
            "WHERE i.saleHeaderId = h.id AND h.status = :status " +
            "  AND h.soldAt >= :from AND h.soldAt < :to " +
            "  AND (:storeId IS NULL OR h.storeId = :storeId) " +
            "  AND (:mainCategory IS NULL OR i.mainCategory = :mainCategory) " +
            "GROUP BY i.mainCategory, i.subCategory " +
            "ORDER BY SUM(i.lineAmount) DESC")
    List<Object[]> aggregateBySubCategory(@Param("status") StoreSaleStatus status,
                                          @Param("from") Date from, @Param("to") Date to,
                                          @Param("storeId") Long storeId,
                                          @Param("mainCategory") String mainCategory);

    @Query("SELECT i.productCode, MAX(i.productName), MAX(i.mainCategory), MAX(i.subCategory), " +
            "       COALESCE(SUM(i.quantity), 0), COALESCE(SUM(i.lineAmount), 0) " +
            "FROM StoreSaleItem i, StoreSaleHeader h " +
            "WHERE i.saleHeaderId = h.id AND h.status = :status " +
            "  AND h.soldAt >= :from AND h.soldAt < :to " +
            "  AND (:storeId IS NULL OR h.storeId = :storeId) " +
            "  AND (:mainCategory IS NULL OR i.mainCategory = :mainCategory) " +
            "GROUP BY i.productCode " +
            "ORDER BY SUM(i.lineAmount) DESC")
    List<Object[]> aggregateByProduct(@Param("status") StoreSaleStatus status,
                                      @Param("from") Date from, @Param("to") Date to,
                                      @Param("storeId") Long storeId,
                                      @Param("mainCategory") String mainCategory);

}

