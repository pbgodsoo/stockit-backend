package org.example.stockitbe.hq.product;

import org.example.stockitbe.hq.product.model.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {
    Optional<ProductSku> findBySkuCode(String skuCode);
    List<ProductSku> findByProductCodeOrderByIdDesc(String productCode);
    long countByProductCode(String productCode);
    List<ProductSku> findAllByOrderByIdDesc();
    boolean existsByProductCodeAndColorAndSize(String productCode, String color, String size);
    long deleteByProductCode(String productCode);
    List<ProductSku> findAllByProductCodeInOrderByIdAsc(Collection<String> productCodes);
}
