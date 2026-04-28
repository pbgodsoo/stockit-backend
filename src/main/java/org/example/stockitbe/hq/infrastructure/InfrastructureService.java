package org.example.stockitbe.hq.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.infrastructure.model.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InfrastructureService {

    private final StoreRepository storeRepository;
    private final WarehouseRepository warehouseRepository;

    @Transactional(readOnly = true)
    public List<InfrastructureDto.StoreRes> findStores(String keyword, String region, InfraStatus status) {
        String safeKeyword = keyword == null ? "" : keyword.trim();
        String safeRegion = region == null ? "" : region.trim();
        List<Store> stores;
        if (status != null && !safeRegion.isBlank()) {
            stores = storeRepository.findByRegionContainingIgnoreCaseAndStatusAndNameContainingIgnoreCaseOrderByIdDesc(safeRegion, status, safeKeyword);
        } else if (!safeRegion.isBlank()) {
            stores = storeRepository.findByRegionContainingIgnoreCaseAndNameContainingIgnoreCaseOrderByIdDesc(safeRegion, safeKeyword);
        } else if (status != null) {
            stores = storeRepository.findByStatusAndNameContainingIgnoreCaseOrderByIdDesc(status, safeKeyword);
        } else {
            stores = storeRepository.findByNameContainingIgnoreCaseOrderByIdDesc(safeKeyword);
        }
        return stores.stream().map(InfrastructureDto.StoreRes::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InfrastructureDto.WarehouseRes> findWarehouses(String keyword, String region, InfraStatus status) {
        String safeKeyword = keyword == null ? "" : keyword.trim();
        String safeRegion = region == null ? "" : region.trim();
        List<Warehouse> warehouses;
        if (status != null && !safeRegion.isBlank()) {
            warehouses = warehouseRepository.findByRegionContainingIgnoreCaseAndStatusAndNameContainingIgnoreCaseOrderByIdDesc(safeRegion, status, safeKeyword);
        } else if (!safeRegion.isBlank()) {
            warehouses = warehouseRepository.findByRegionContainingIgnoreCaseAndNameContainingIgnoreCaseOrderByIdDesc(safeRegion, safeKeyword);
        } else if (status != null) {
            warehouses = warehouseRepository.findByStatusAndNameContainingIgnoreCaseOrderByIdDesc(status, safeKeyword);
        } else {
            warehouses = warehouseRepository.findByNameContainingIgnoreCaseOrderByIdDesc(safeKeyword);
        }
        return warehouses.stream().map(w -> InfrastructureDto.WarehouseRes.from(w, storeRepository.countByWarehouseCode(w.getCode()))).toList();
    }

    @Transactional
    public InfrastructureDto.StoreRes createStore(InfrastructureDto.StoreUpsertReq req) {
        if (!warehouseRepository.existsByCode(req.getWarehouseCode().trim())) {
            throw BaseException.from(BaseResponseStatus.WAREHOUSE_NOT_FOUND);
        }
        if (storeRepository.existsByNameIgnoreCase(req.getName().trim())) {
            throw BaseException.from(BaseResponseStatus.DUPLICATE_STORE_NAME);
        }
        Store saved = saveStoreWithGeneratedCode(req);
        return InfrastructureDto.StoreRes.from(saved);
    }

    @Transactional
    public InfrastructureDto.StoreRes updateStore(String code, InfrastructureDto.StoreUpsertReq req) {
        Store store = storeRepository.findByCode(code).orElseThrow(() -> BaseException.from(BaseResponseStatus.STORE_NOT_FOUND));
        if (!warehouseRepository.existsByCode(req.getWarehouseCode().trim())) {
            throw BaseException.from(BaseResponseStatus.WAREHOUSE_NOT_FOUND);
        }
        if (storeRepository.existsByNameIgnoreCaseAndCodeNot(req.getName().trim(), code)) {
            throw BaseException.from(BaseResponseStatus.DUPLICATE_STORE_NAME);
        }
        store.update(req.getName().trim(), req.getRegion().trim(), req.getType(), req.getManagerName().trim(), req.getContact().trim(), req.getAddress().trim(), req.getWarehouseCode().trim(), req.getStatus());
        return InfrastructureDto.StoreRes.from(store);
    }

    @Transactional
    public InfrastructureDto.WarehouseRes createWarehouse(InfrastructureDto.WarehouseUpsertReq req) {
        if (warehouseRepository.existsByNameIgnoreCase(req.getName().trim())) {
            throw BaseException.from(BaseResponseStatus.DUPLICATE_WAREHOUSE_NAME);
        }
        Warehouse saved = saveWarehouseWithGeneratedCode(req);
        return InfrastructureDto.WarehouseRes.from(saved, 0);
    }

    @Transactional
    public InfrastructureDto.WarehouseRes updateWarehouse(String code, InfrastructureDto.WarehouseUpsertReq req) {
        Warehouse warehouse = warehouseRepository.findByCode(code).orElseThrow(() -> BaseException.from(BaseResponseStatus.WAREHOUSE_NOT_FOUND));
        if (warehouseRepository.existsByNameIgnoreCaseAndCodeNot(req.getName().trim(), code)) {
            throw BaseException.from(BaseResponseStatus.DUPLICATE_WAREHOUSE_NAME);
        }
        warehouse.update(req.getName().trim(), req.getRegion().trim(), req.getManagerName().trim(), req.getContact().trim(), req.getAddress().trim(), req.getCapacity().trim(), req.getStatus());
        return InfrastructureDto.WarehouseRes.from(warehouse, storeRepository.countByWarehouseCode(warehouse.getCode()));
    }

    private Store saveStoreWithGeneratedCode(InfrastructureDto.StoreUpsertReq req) {
        for (int i = 0; i < 2; i++) {
            String code = nextCode(storeRepository.findAllByOrderByIdDesc().stream().map(Store::getCode).toList(), "ST");
            try {
                return storeRepository.save(req.toEntity(code));
            } catch (DataIntegrityViolationException e) {
                if (i == 1) throw e;
            }
        }
        throw BaseException.from(BaseResponseStatus.FAIL);
    }

    private Warehouse saveWarehouseWithGeneratedCode(InfrastructureDto.WarehouseUpsertReq req) {
        for (int i = 0; i < 2; i++) {
            String code = nextCode(warehouseRepository.findAllByOrderByIdDesc().stream().map(Warehouse::getCode).toList(), "WH");
            try {
                return warehouseRepository.save(req.toEntity(code));
            } catch (DataIntegrityViolationException e) {
                if (i == 1) throw e;
            }
        }
        throw BaseException.from(BaseResponseStatus.FAIL);
    }

    private String nextCode(List<String> codes, String prefix) {
        long max = codes.stream()
                .filter(c -> c != null && c.startsWith(prefix + "-"))
                .mapToLong(c -> {
                    try {
                        return Long.parseLong(c.substring(prefix.length() + 1));
                    } catch (Exception e) {
                        return 0L;
                    }
                })
                .max()
                .orElse(0L);
        return String.format("%s-%04d", prefix, max + 1);
    }
}
