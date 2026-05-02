package org.example.stockitbe.hq.infrastructure;

import org.example.stockitbe.hq.infrastructure.model.InfraStatus;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InfrastructureRepository extends JpaRepository<Infrastructure, Long> {
    Optional<Infrastructure> findByCode(String code);
    boolean existsByLocationTypeAndNameIgnoreCase(LocationType locationType, String name);
    boolean existsByLocationTypeAndNameIgnoreCaseAndCodeNot(LocationType locationType, String name, String code);
    boolean existsByCodeAndLocationType(String code, LocationType locationType);
    long countByMappedWarehouseCode(String mappedWarehouseCode);
    List<Infrastructure> findAllByOrderByIdDesc();

    List<Infrastructure> findByLocationTypeAndRegionContainingIgnoreCaseAndStatusAndNameContainingIgnoreCaseOrderByIdDesc(
            LocationType locationType, String region, InfraStatus status, String keyword);
    List<Infrastructure> findByLocationTypeAndRegionContainingIgnoreCaseAndNameContainingIgnoreCaseOrderByIdDesc(
            LocationType locationType, String region, String keyword);
    List<Infrastructure> findByLocationTypeAndStatusAndNameContainingIgnoreCaseOrderByIdDesc(
            LocationType locationType, InfraStatus status, String keyword);
    List<Infrastructure> findByLocationTypeAndNameContainingIgnoreCaseOrderByIdDesc(LocationType locationType, String keyword);

    List<Infrastructure> findByRegionContainingIgnoreCaseAndStatusAndNameContainingIgnoreCaseOrderByIdDesc(String region, InfraStatus status, String keyword);
    List<Infrastructure> findByRegionContainingIgnoreCaseAndNameContainingIgnoreCaseOrderByIdDesc(String region, String keyword);
    List<Infrastructure> findByStatusAndNameContainingIgnoreCaseOrderByIdDesc(InfraStatus status, String keyword);
    List<Infrastructure> findByNameContainingIgnoreCaseOrderByIdDesc(String keyword);
}
