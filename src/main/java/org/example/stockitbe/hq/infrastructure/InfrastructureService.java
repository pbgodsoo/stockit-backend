package org.example.stockitbe.hq.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.infrastructure.model.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class InfrastructureService {

    private static final Map<String, String> REGION_CODE_MAP = buildRegionCodeMap();
    private static final Pattern INFRA_CODE_PATTERN = Pattern.compile("^(ST|WH)-([A-Z]{2})-(\\d{4})$");

    private final InfrastructureRepository infrastructureRepository;
    private final StoreWarehouseMapRepository storeWarehouseMapRepository;

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

        Map<Long, Long> mappedStoreCountByWarehouseId = mappedStoreCountByWarehouseId(rows);
        return rows.stream().map(row -> toRes(row, mappedStoreCountByWarehouseId)).toList();
    }

    @Transactional(readOnly = true)
    public InfrastructureDto.Res findInfrastructureByCode(String code) {
        Infrastructure infra = infrastructureRepository.findByCode(code)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.NOT_FOUND_DATA));
        Map<Long, Long> mappedStoreCountByWarehouseId = mappedStoreCountByWarehouseId(List.of(infra));
        return toRes(infra, mappedStoreCountByWarehouseId);
    }

    @Transactional
    public InfrastructureDto.Res createInfrastructure(InfrastructureDto.UpsertReq req) {
        normalizeAndValidate(req);
        if (infrastructureRepository.existsByLocationTypeAndNameIgnoreCase(req.getLocationType(), req.getName().trim())) {
            throw BaseException.from(duplicateNameStatus(req.getLocationType()));
        }
        Infrastructure saved = saveWithGeneratedCode(req);
        return toRes(saved, Map.of());
    }

    @Transactional
    public InfrastructureDto.Res updateInfrastructure(String code, InfrastructureDto.UpsertReq req) {
        Infrastructure infra = infrastructureRepository.findByCode(code)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.NOT_FOUND_DATA));

        if (infra.getLocationType() != req.getLocationType()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        normalizeAndValidate(req);
        if (infrastructureRepository.existsByLocationTypeAndNameIgnoreCaseAndCodeNot(req.getLocationType(), req.getName().trim(), code)) {
            throw BaseException.from(duplicateNameStatus(req.getLocationType()));
        }

        infra.update(
                req.getName().trim(),
                req.getRegion().trim(),
                req.getManagerName().trim(),
                req.getContact().trim(),
                req.getAddress().trim(),
                req.getStatus()
        );
        Map<Long, Long> mappedStoreCountByWarehouseId = mappedStoreCountByWarehouseId(List.of(infra));
        return toRes(infra, mappedStoreCountByWarehouseId);
    }

    private Infrastructure saveWithGeneratedCode(InfrastructureDto.UpsertReq req) {
        String typePrefix = req.getLocationType() == LocationType.STORE ? "ST" : "WH";
        String regionCode = resolveRegionCode(req.getRegion());
        for (int i = 0; i < 2; i++) {
            String code = nextCode(typePrefix, regionCode);
            try {
                return infrastructureRepository.save(req.toEntity(code));
            } catch (DataIntegrityViolationException e) {
                if (i == 1) throw e;
            }
        }
        throw BaseException.from(BaseResponseStatus.FAIL);
    }

    private void normalizeAndValidate(InfrastructureDto.UpsertReq req) {
        resolveRegionCode(req.getRegion());
    }

    private InfrastructureDto.Res toRes(Infrastructure infra, Map<Long, Long> mappedStoreCountByWarehouseId) {
        Long mappedStoreCount = null;
        if (infra.getLocationType() == LocationType.WAREHOUSE) {
            mappedStoreCount = mappedStoreCountByWarehouseId.getOrDefault(infra.getId(), 0L);
        }
        return InfrastructureDto.Res.from(infra, mappedStoreCount);
    }

    private Map<Long, Long> mappedStoreCountByWarehouseId(List<Infrastructure> rows) {
        List<Infrastructure> warehouses = rows.stream()
                .filter(it -> it.getLocationType() == LocationType.WAREHOUSE)
                .toList();
        if (warehouses.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> mappedStoreCountByWarehouseId = new HashMap<>();
        List<StoreWarehouseMap> maps = storeWarehouseMapRepository.findByWarehouseIn(warehouses);
        for (StoreWarehouseMap map : maps) {
            Long warehouseId = map.getWarehouse().getId();
            mappedStoreCountByWarehouseId.put(warehouseId, mappedStoreCountByWarehouseId.getOrDefault(warehouseId, 0L) + 1L);
        }
        return mappedStoreCountByWarehouseId;
    }

    private BaseResponseStatus duplicateNameStatus(LocationType type) {
        return type == LocationType.STORE
                ? BaseResponseStatus.DUPLICATE_STORE_NAME
                : BaseResponseStatus.DUPLICATE_WAREHOUSE_NAME;
    }

    private String nextCode(String typePrefix, String regionCode) {
        long max = infrastructureRepository.findAllByOrderByIdDesc().stream()
                .map(Infrastructure::getCode)
                .filter(c -> c != null && c.startsWith(typePrefix + "-" + regionCode + "-"))
                .map(INFRA_CODE_PATTERN::matcher)
                .filter(Matcher::matches)
                .filter(m -> m.group(1).equals(typePrefix) && m.group(2).equals(regionCode))
                .mapToLong(m -> Long.parseLong(m.group(3)))
                .max()
                .orElse(0L);
        return String.format("%s-%s-%04d", typePrefix, regionCode, max + 1);
    }

    private String resolveRegionCode(String regionRaw) {
        String region = regionRaw == null ? "" : regionRaw.trim();
        if (region.isBlank()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        String upper = region.toUpperCase(Locale.ROOT);
        String codeFromCode = REGION_CODE_MAP.get(upper);
        if (codeFromCode != null) {
            return codeFromCode;
        }

        String codeFromName = REGION_CODE_MAP.get(region);
        if (codeFromName != null) {
            return codeFromName;
        }

        throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
    }

    private static Map<String, String> buildRegionCodeMap() {
        Map<String, String> map = new HashMap<>();
        map.put("SL", "SL");
        map.put("GG", "GG");
        map.put("IC", "IC");
        map.put("BS", "BS");
        map.put("DJ", "DJ");
        map.put("GJ", "GJ");
        map.put("GW", "GW");
        map.put("JJ", "JJ");
        map.put("CN", "CN");
        map.put("YN", "YN");
        map.put("HN", "HN");

        map.put("서울", "SL");
        map.put("경기", "GG");
        map.put("인천", "IC");
        map.put("부산", "BS");
        map.put("대전", "DJ");
        map.put("광주", "GJ");
        map.put("강원", "GW");
        map.put("제주", "JJ");
        map.put("충청", "CN");
        map.put("영남", "YN");
        map.put("호남", "HN");
        return Map.copyOf(map);
    }

    public List<InfrastructureDto.PublicRes> findActiveForSignup(LocationType type, String region) {
        List<Infrastructure> rows = (type != null)
                ? infrastructureRepository.findAllByLocationTypeAndStatusOrderByCodeAsc(type, InfraStatus.ACTIVE)
                : infrastructureRepository.findAllByOrderByIdDesc();   // 또는 새 메서드 추가
        return rows.stream()
                .filter(i -> InfraStatus.ACTIVE.equals(i.getStatus()))   // type null 분기 시 추가 필터
                .filter(i -> region == null || region.isBlank() || region.equals(i.getRegion()))
                .map(i -> InfrastructureDto.PublicRes.builder()
                        .code(i.getCode())
                        .locationType(i.getLocationType())
                        .name(i.getName())
                        .region(i.getRegion())
                        .build())
                .toList();
    }


}
