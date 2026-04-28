package org.example.stockitbe.hq.infrastructure.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

@Entity
@Table(name = "warehouse", uniqueConstraints = {
        @UniqueConstraint(name = "uk_warehouse_code", columnNames = "code")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Warehouse extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

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

    @Column(name = "capacity", nullable = false, length = 64)
    private String capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private InfraStatus status;

    @Builder
    private Warehouse(String code, String name, String region, String managerName,
                      String contact, String address, String capacity, InfraStatus status) {
        this.code = code;
        this.name = name;
        this.region = region;
        this.managerName = managerName;
        this.contact = contact;
        this.address = address;
        this.capacity = capacity;
        this.status = status == null ? InfraStatus.ACTIVE : status;
    }

    public void update(String name, String region, String managerName,
                       String contact, String address, String capacity, InfraStatus status) {
        this.name = name;
        this.region = region;
        this.managerName = managerName;
        this.contact = contact;
        this.address = address;
        this.capacity = capacity;
        this.status = status;
    }
}
