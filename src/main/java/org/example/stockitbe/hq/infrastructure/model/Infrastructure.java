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
}, indexes = {
        @Index(name = "idx_infrastructure_type_status_region_id", columnList = "location_type,status,region,id"),
        @Index(name = "idx_infrastructure_type_region_id", columnList = "location_type,region,id")
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

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private InfraStatus status;

    @Builder
    private Infrastructure(String code, LocationType locationType, String name, String region, String managerName,
                           String contact, String address, Double latitude, Double longitude, InfraStatus status) {
        this.code = code;
        this.locationType = locationType;
        this.name = name;
        this.region = region;
        this.managerName = managerName;
        this.contact = contact;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status == null ? InfraStatus.ACTIVE : status;
    }

    public void update(String name, String region, String managerName, String contact, String address,
                       InfraStatus status) {
        this.name = name;
        this.region = region;
        this.managerName = managerName;
        this.contact = contact;
        this.address = address;
        this.status = status;
    }

    public void updateCoordinates(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
