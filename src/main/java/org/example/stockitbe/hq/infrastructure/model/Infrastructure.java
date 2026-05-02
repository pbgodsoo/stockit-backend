package org.example.stockitbe.hq.infrastructure.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

@Entity
@Table(name = "infrastructure", uniqueConstraints = {
        @UniqueConstraint(name = "uk_infrastructure_code", columnNames = "code")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Infrastructure extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 16)
    private LocationType locationType;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "region", nullable = false, length = 32)
    private String region;

    @Column(name = "manager_name", nullable = false, length = 64)
    private String managerName;

    @Column(name = "contact", nullable = false, length = 32)
    private String contact;

    @Column(name = "address", nullable = false, length = 256)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private InfraStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "store_type", length = 16)
    private StoreType storeType;

    @Column(name = "mapped_warehouse_code", length = 32)
    private String mappedWarehouseCode;

    @Column(name = "capacity", length = 64)
    private String capacity;

    @Builder
    private Infrastructure(String code, LocationType locationType, String name, String region, String managerName,
                           String contact, String address, InfraStatus status,
                           StoreType storeType, String mappedWarehouseCode, String capacity) {
        this.code = code;
        this.locationType = locationType;
        this.name = name;
        this.region = region;
        this.managerName = managerName;
        this.contact = contact;
        this.address = address;
        this.status = status == null ? InfraStatus.ACTIVE : status;
        this.storeType = storeType;
        this.mappedWarehouseCode = mappedWarehouseCode;
        this.capacity = capacity;
    }

    public void update(String name, String region, String managerName, String contact, String address,
                       InfraStatus status, StoreType storeType, String mappedWarehouseCode, String capacity) {
        this.name = name;
        this.region = region;
        this.managerName = managerName;
        this.contact = contact;
        this.address = address;
        this.status = status;
        this.storeType = storeType;
        this.mappedWarehouseCode = mappedWarehouseCode;
        this.capacity = capacity;
    }
}
