package org.example.stockitbe.hq.infrastructure;

import org.example.stockitbe.hq.infrastructure.model.Warehouse;
import org.example.stockitbe.hq.infrastructure.model.InfraStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    Optional<Warehouse> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndCodeNot(String name, String code);
    List<Warehouse> findByRegionContainingIgnoreCaseAndStatusAndNameContainingIgnoreCaseOrderByIdDesc(String region, InfraStatus status, String keyword);
    List<Warehouse> findByRegionContainingIgnoreCaseAndNameContainingIgnoreCaseOrderByIdDesc(String region, String keyword);
    List<Warehouse> findByStatusAndNameContainingIgnoreCaseOrderByIdDesc(InfraStatus status, String keyword);
    List<Warehouse> findByNameContainingIgnoreCaseOrderByIdDesc(String keyword);
    List<Warehouse> findAllByOrderByIdDesc();
}
