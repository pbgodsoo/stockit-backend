package org.example.stockitbe.hq.vendor;

import org.example.stockitbe.hq.vendor.model.VendorProduct;
import org.example.stockitbe.hq.vendor.model.VendorProductStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorProductRepository extends JpaRepository<VendorProduct, Long> {

    @EntityGraph(attributePaths = "vendor")
    Optional<VendorProduct> findByCode(String code);

    @EntityGraph(attributePaths = "vendor")
    List<VendorProduct> findAllByVendorIdAndStatusNotOrderByIdDesc(Long vendorId, VendorProductStatus excluded);

    @EntityGraph(attributePaths = "vendor")
    List<VendorProduct> findAllByStatusOrderByIdDesc(VendorProductStatus status);

    @EntityGraph(attributePaths = "vendor")
    List<VendorProduct> findAllByStatusNotOrderByIdDesc(VendorProductStatus excluded);

    boolean existsByVendorIdAndProductCodeAndStatusNot(Long vendorId, String productCode, VendorProductStatus excluded);

    long countByVendorId(Long vendorId);
}
