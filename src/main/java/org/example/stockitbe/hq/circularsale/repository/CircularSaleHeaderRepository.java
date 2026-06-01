package org.example.stockitbe.hq.circularsale.repository;

import org.example.stockitbe.hq.circularsale.model.entity.CircularSaleHeader;
import org.example.stockitbe.hq.circularsale.model.CircularSaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.Optional;

public interface CircularSaleHeaderRepository extends JpaRepository<CircularSaleHeader, Long> {
    Optional<CircularSaleHeader> findBySaleNo(String saleNo);

    @Query("""
            select h
            from CircularSaleHeader h
            where (:fromDate is null or h.soldAt >= :fromDate)
              and (:toDateExclusive is null or h.soldAt < :toDateExclusive)
              and (:buyerId is null or h.buyerId = :buyerId)
              and (:materialType is null or h.materialType = :materialType)
              and (:keyword is null or lower(h.saleNo) like concat('%', lower(:keyword), '%')
                   or exists (
                       select 1 from CircularSaleItem i
                       where i.saleHeaderId = h.id
                         and (lower(i.productName) like concat('%', lower(:keyword), '%')
                              or lower(i.skuCode) like concat('%', lower(:keyword), '%'))
                   ))
              and (:saleType is null or h.saleType = :saleType)
            """)
    Page<CircularSaleHeader> search(
            @Param("fromDate") Date fromDate,
            @Param("toDateExclusive") Date toDateExclusive,
            @Param("buyerId") Long buyerId,
            @Param("materialType") String materialType,
            @Param("keyword") String keyword,
            @Param("saleType") String saleType,
            Pageable pageable
    );

    Optional<CircularSaleHeader> findByOutboundHeaderId(Long outboundHeaderId);

    @Query("select count(h) from CircularSaleHeader h where h.status = :status")
    long countByStatus(@Param("status") CircularSaleStatus status);
}
