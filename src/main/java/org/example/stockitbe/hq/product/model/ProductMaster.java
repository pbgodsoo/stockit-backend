package org.example.stockitbe.hq.product.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

@Entity
@Table(name = "product_master", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_master_code", columnNames = "code")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMaster extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "category_code", nullable = false, length = 32)
    private String categoryCode;

    @Column(name = "base_price", nullable = false)
    private Long basePrice;

    @Column(name = "lead_time_days", nullable = false)
    private Integer leadTimeDays;

    @Column(name = "main_vendor_code", nullable = false, length = 32)
    private String mainVendorCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ProductStatus status;

    @Builder
    private ProductMaster(String code, String name, String categoryCode, Long basePrice, Integer leadTimeDays,
                          String mainVendorCode, ProductStatus status) {
        this.code = code;
        this.name = name;
        this.categoryCode = categoryCode;
        this.basePrice = basePrice;
        this.leadTimeDays = leadTimeDays;
        this.mainVendorCode = mainVendorCode;
        this.status = status == null ? ProductStatus.ACTIVE : status;
    }

    public void update(String name, String categoryCode, Long basePrice, Integer leadTimeDays, String mainVendorCode, ProductStatus status) {
        this.name = name;
        this.categoryCode = categoryCode;
        this.basePrice = basePrice;
        this.leadTimeDays = leadTimeDays;
        this.mainVendorCode = mainVendorCode;
        this.status = status;
    }
}
