package org.example.stockitbe.hq.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.infrastructure.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InfrastructureMappingService {

    private final InfrastructureRepository infrastructureRepository;
    private final StoreWarehouseMapRepository storeWarehouseMapRepository;

    @Transactional(readOnly = true)
    public List<InfrastructureMappingDto.StoreMappingItem> getStoreMappings(String keyword, String region, InfraStatus status) {
        String safeKeyword = keyword == null ? "" : keyword.trim();
        String safeRegion = region == null ? "" : region.trim();

        List<Infrastructure> stores;
        if (status != null && !safeRegion.isBlank()) {
            stores = infrastructureRepository.findByLocationTypeAndRegionContainingIgnoreCaseAndStatusAndNameContainingIgnoreCaseOrderByIdDesc(
                    LocationType.STORE, safeRegion, status, safeKeyword);
        } else if (!safeRegion.isBlank()) {
            stores = infrastructureRepository.findByLocationTypeAndRegionContainingIgnoreCaseAndNameContainingIgnoreCaseOrderByIdDesc(
                    LocationType.STORE, safeRegion, safeKeyword);
        } else if (status != null) {
            stores = infrastructureRepository.findByLocationTypeAndStatusAndNameContainingIgnoreCaseOrderByIdDesc(
                    LocationType.STORE, status, safeKeyword);
        } else {
            stores = infrastructureRepository.findByLocationTypeAndNameContainingIgnoreCaseOrderByIdDesc(LocationType.STORE, safeKeyword);
        }

        Map<Long, List<StoreWarehouseMap>> mapsByStoreId = storeWarehouseMapRepository.findByStoreIn(stores)
                .stream()
                .collect(Collectors.groupingBy(map -> map.getStore().getId()));

        return stores.stream().map(store -> {
            StoreWarehouseMap primary = null;
            StoreWarehouseMap backup = null;
            for (StoreWarehouseMap map : mapsByStoreId.getOrDefault(store.getId(), List.of())) {
                if (map.getRole() == StoreWarehouseRole.PRIMARY) primary = map;
                if (map.getRole() == StoreWarehouseRole.BACKUP) backup = map;
            }
            return InfrastructureMappingDto.StoreMappingItem.builder()
                    .storeCode(store.getCode())
                    .storeName(store.getName())
                    .region(store.getRegion())
                    .status(store.getStatus())
                    .primaryWarehouseCode(primary == null ? null : primary.getWarehouse().getCode())
                    .primaryWarehouseName(primary == null ? null : primary.getWarehouse().getName())
                    .backupWarehouseCode(backup == null ? null : backup.getWarehouse().getCode())
                    .backupWarehouseName(backup == null ? null : backup.getWarehouse().getName())
                    .updatedAt(store.getUpdatedAt())
                    .build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public InfrastructureMappingDto.OptionsRes getOptions() {
        List<InfrastructureMappingDto.OptionItem> stores = infrastructureRepository.findByLocationTypeOrderByIdDesc(LocationType.STORE)
                .stream()
                .map(store -> InfrastructureMappingDto.OptionItem.builder().code(store.getCode()).name(store.getName()).build())
                .toList();
        List<InfrastructureMappingDto.OptionItem> warehouses = infrastructureRepository.findByLocationTypeOrderByIdDesc(LocationType.WAREHOUSE)
                .stream()
                .map(warehouse -> InfrastructureMappingDto.OptionItem.builder().code(warehouse.getCode()).name(warehouse.getName()).build())
                .toList();

        return InfrastructureMappingDto.OptionsRes.builder()
                .stores(stores)
                .warehouses(warehouses)
                .build();
    }

    @Transactional
    public InfrastructureMappingDto.StoreMappingItem saveStoreMappings(String storeCode, InfrastructureMappingDto.UpsertReq req) {
        String primaryCode = req.getPrimaryWarehouseCode().trim();
        String backupCode = req.getBackupWarehouseCode() == null ? null : req.getBackupWarehouseCode().trim();
        if (primaryCode.isBlank()) {
            throw BaseException.from(BaseResponseStatus.STORE_WAREHOUSE_MAPPING_PRIMARY_REQUIRED);
        }
        if (backupCode != null && !backupCode.isBlank() && primaryCode.equals(backupCode)) {
            throw BaseException.from(BaseResponseStatus.STORE_WAREHOUSE_MAPPING_PRIMARY_BACKUP_SAME);
        }

        Infrastructure store = infrastructureRepository.findByCode(storeCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.STORE_NOT_FOUND));
        if (store.getLocationType() != LocationType.STORE) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        Infrastructure primaryWarehouse = lookupWarehouse(primaryCode);
        Infrastructure backupWarehouse = null;
        if (backupCode != null && !backupCode.isBlank()) {
            backupWarehouse = lookupWarehouse(backupCode);
        }

        upsertRole(store, StoreWarehouseRole.PRIMARY, primaryWarehouse);
        if (backupWarehouse != null) {
            upsertRole(store, StoreWarehouseRole.BACKUP, backupWarehouse);
        } else {
            deleteRole(store, StoreWarehouseRole.BACKUP);
        }

        return toStoreMappingItem(store);
    }

    private void upsertRole(Infrastructure store, StoreWarehouseRole role, Infrastructure warehouse) {
        StoreWarehouseMap byRole = storeWarehouseMapRepository.findByStoreAndRole(store, role).orElse(null);
        if (byRole != null) {
            StoreWarehouseMap duplicate = storeWarehouseMapRepository.findByStoreAndWarehouse(store, warehouse).orElse(null);
            if (duplicate != null && !duplicate.getId().equals(byRole.getId())) {
                throw BaseException.from(BaseResponseStatus.STORE_WAREHOUSE_MAPPING_DUPLICATE_WAREHOUSE);
            }
            byRole.changeWarehouse(warehouse);
            return;
        }
        if (storeWarehouseMapRepository.findByStoreAndWarehouse(store, warehouse).isPresent()) {
            throw BaseException.from(BaseResponseStatus.STORE_WAREHOUSE_MAPPING_DUPLICATE_WAREHOUSE);
        }
        storeWarehouseMapRepository.save(new StoreWarehouseMap(store, warehouse, role));
    }

    private void deleteRole(Infrastructure store, StoreWarehouseRole role) {
        storeWarehouseMapRepository.findByStoreAndRole(store, role)
                .ifPresent(storeWarehouseMapRepository::delete);
    }

    private Infrastructure lookupWarehouse(String warehouseCode) {
        Infrastructure warehouse = infrastructureRepository.findByCode(warehouseCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.WAREHOUSE_NOT_FOUND));
        if (warehouse.getLocationType() != LocationType.WAREHOUSE) {
            throw BaseException.from(BaseResponseStatus.WAREHOUSE_NOT_FOUND);
        }
        return warehouse;
    }

    private InfrastructureMappingDto.StoreMappingItem toStoreMappingItem(Infrastructure store) {
        List<StoreWarehouseMap> maps = storeWarehouseMapRepository.findByStoreIn(List.of(store));
        StoreWarehouseMap primary = null;
        StoreWarehouseMap backup = null;
        for (StoreWarehouseMap map : maps) {
            if (map.getRole() == StoreWarehouseRole.PRIMARY) primary = map;
            if (map.getRole() == StoreWarehouseRole.BACKUP) backup = map;
        }
        return InfrastructureMappingDto.StoreMappingItem.builder()
                .storeCode(store.getCode())
                .storeName(store.getName())
                .region(store.getRegion())
                .status(store.getStatus())
                .primaryWarehouseCode(primary == null ? null : primary.getWarehouse().getCode())
                .primaryWarehouseName(primary == null ? null : primary.getWarehouse().getName())
                .backupWarehouseCode(backup == null ? null : backup.getWarehouse().getCode())
                .backupWarehouseName(backup == null ? null : backup.getWarehouse().getName())
                .updatedAt(store.getUpdatedAt())
                .build();
    }
}
