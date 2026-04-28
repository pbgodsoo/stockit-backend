package org.example.stockitbe.vendor;

import org.example.stockitbe.vendor.model.VendorProduct;
import org.example.stockitbe.vendor.model.VendorProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorProductRepository extends JpaRepository<VendorProduct, Long> {

    Optional<VendorProduct> findByCode(String code);

    List<VendorProduct> findAllByVendorIdAndStatusNotOrderByIdDesc(Long vendorId, VendorProductStatus excluded);

    boolean existsByVendorIdAndProductCodeAndStatusNot(Long vendorId, String productCode, VendorProductStatus excluded);

    long countByVendorId(Long vendorId);
}
