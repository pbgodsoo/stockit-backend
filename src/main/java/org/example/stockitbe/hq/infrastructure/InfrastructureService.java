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

    private final InfrastructureRepository infrastructureRepository;

    @Transactional(readOnly = true)
    public List<InfrastructureDto.Res> findInfrastructures(LocationType type, String keyword, String region, InfraStatus status) {
        String safeKeyword = keyword == null ? "" : keyword.trim();
        String safeRegion = region == null ? "" : region.trim();

        List<Infrastructure> rows;
        if (type != null) {
            if (status != null && !safeRegion.isBlank()) {
                rows = infrastructureRepository.findByLocationTypeAndRegionContainingIgnoreCaseAndStatusAndNameContainingIgnoreCaseOrderByIdDesc(type, safeRegion, status, safeKeyword);
            } else if (!safeRegion.isBlank()) {
                rows = infrastructureRepository.findByLocationTypeAndRegionContainingIgnoreCaseAndNameContainingIgnoreCaseOrderByIdDesc(type, safeRegion, safeKeyword);
            } else if (status != null) {
                rows = infrastructureRepository.findByLocationTypeAndStatusAndNameContainingIgnoreCaseOrderByIdDesc(type, status, safeKeyword);
            } else {
                rows = infrastructureRepository.findByLocationTypeAndNameContainingIgnoreCaseOrderByIdDesc(type, safeKeyword);
            }
        } else {
            if (status != null && !safeRegion.isBlank()) {
                rows = infrastructureRepository.findByRegionContainingIgnoreCaseAndStatusAndNameContainingIgnoreCaseOrderByIdDesc(safeRegion, status, safeKeyword);
            } else if (!safeRegion.isBlank()) {
                rows = infrastructureRepository.findByRegionContainingIgnoreCaseAndNameContainingIgnoreCaseOrderByIdDesc(safeRegion, safeKeyword);
            } else if (status != null) {
                rows = infrastructureRepository.findByStatusAndNameContainingIgnoreCaseOrderByIdDesc(status, safeKeyword);
            } else {
                rows = infrastructureRepository.findByNameContainingIgnoreCaseOrderByIdDesc(safeKeyword);
            }
        }

        return rows.stream().map(this::toRes).toList();
    }

    @Transactional(readOnly = true)
    public InfrastructureDto.Res findInfrastructureByCode(String code) {
        Infrastructure infra = infrastructureRepository.findByCode(code)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.NOT_FOUND_DATA));
        return toRes(infra);
    }

    @Transactional
    public InfrastructureDto.Res createInfrastructure(InfrastructureDto.UpsertReq req) {
        NormalizedInfra normalized = normalizeAndValidate(req, null);
        if (infrastructureRepository.existsByLocationTypeAndNameIgnoreCase(req.getLocationType(), req.getName().trim())) {
            throw BaseException.from(duplicateNameStatus(req.getLocationType()));
        }
        Infrastructure saved = saveWithGeneratedCode(req, normalized);
        return toRes(saved);
    }

    @Transactional
    public InfrastructureDto.Res updateInfrastructure(String code, InfrastructureDto.UpsertReq req) {
        Infrastructure infra = infrastructureRepository.findByCode(code)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.NOT_FOUND_DATA));

        if (infra.getLocationType() != req.getLocationType()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        NormalizedInfra normalized = normalizeAndValidate(req, code);
        if (infrastructureRepository.existsByLocationTypeAndNameIgnoreCaseAndCodeNot(req.getLocationType(), req.getName().trim(), code)) {
            throw BaseException.from(duplicateNameStatus(req.getLocationType()));
        }

        infra.update(
                req.getName().trim(),
                req.getRegion().trim(),
                req.getManagerName().trim(),
                req.getContact().trim(),
                req.getAddress().trim(),
                req.getStatus(),
                normalized.storeType(),
                normalized.mappedWarehouseCode(),
                normalized.capacity()
        );
        return toRes(infra);
    }

    private Infrastructure saveWithGeneratedCode(InfrastructureDto.UpsertReq req, NormalizedInfra normalized) {
        String prefix = req.getLocationType() == LocationType.STORE ? "ST" : "WH";
        for (int i = 0; i < 2; i++) {
            String code = nextCode(infrastructureRepository.findAllByOrderByIdDesc().stream().map(Infrastructure::getCode).toList(), prefix);
            try {
                return infrastructureRepository.save(req.toEntity(code, normalized.storeType(), normalized.mappedWarehouseCode(), normalized.capacity()));
            } catch (DataIntegrityViolationException e) {
                if (i == 1) throw e;
            }
        }
        throw BaseException.from(BaseResponseStatus.FAIL);
    }

    private NormalizedInfra normalizeAndValidate(InfrastructureDto.UpsertReq req, String selfCodeForUpdate) {
        if (req.getLocationType() == LocationType.STORE) {
            if (req.getStoreType() == null || req.getMappedWarehouseCode() == null || req.getMappedWarehouseCode().isBlank()) {
                throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
            }
            String mappedCode = req.getMappedWarehouseCode().trim();
            boolean exists = infrastructureRepository.existsByCodeAndLocationType(mappedCode, LocationType.WAREHOUSE);
            if (!exists || (selfCodeForUpdate != null && selfCodeForUpdate.equals(mappedCode))) {
                throw BaseException.from(BaseResponseStatus.WAREHOUSE_NOT_FOUND);
            }
            return new NormalizedInfra(req.getStoreType(), mappedCode, null);
        }

        if (req.getCapacity() == null || req.getCapacity().isBlank()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
        return new NormalizedInfra(null, null, req.getCapacity().trim());
    }

    private InfrastructureDto.Res toRes(Infrastructure infra) {
        Long mappedStoreCount = null;
        if (infra.getLocationType() == LocationType.WAREHOUSE) {
            mappedStoreCount = infrastructureRepository.countByMappedWarehouseCode(infra.getCode());
        }
        return InfrastructureDto.Res.from(infra, mappedStoreCount);
    }

    private BaseResponseStatus duplicateNameStatus(LocationType type) {
        return type == LocationType.STORE
                ? BaseResponseStatus.DUPLICATE_STORE_NAME
                : BaseResponseStatus.DUPLICATE_WAREHOUSE_NAME;
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

    private record NormalizedInfra(StoreType storeType, String mappedWarehouseCode, String capacity) {
    }
}
