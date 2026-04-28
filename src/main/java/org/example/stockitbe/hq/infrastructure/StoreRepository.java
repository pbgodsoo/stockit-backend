package org.example.stockitbe.hq.infrastructure;

import org.example.stockitbe.hq.infrastructure.model.Store;
import org.example.stockitbe.hq.infrastructure.model.InfraStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findByCode(String code);
    List<Store> findAllByOrderByIdDesc();
    long countByWarehouseCode(String warehouseCode);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndCodeNot(String name, String code);
    List<Store> findByRegionContainingIgnoreCaseAndStatusAndNameContainingIgnoreCaseOrderByIdDesc(String region, InfraStatus status, String keyword);
    List<Store> findByRegionContainingIgnoreCaseAndNameContainingIgnoreCaseOrderByIdDesc(String region, String keyword);
    List<Store> findByStatusAndNameContainingIgnoreCaseOrderByIdDesc(InfraStatus status, String keyword);
    List<Store> findByNameContainingIgnoreCaseOrderByIdDesc(String keyword);
}
