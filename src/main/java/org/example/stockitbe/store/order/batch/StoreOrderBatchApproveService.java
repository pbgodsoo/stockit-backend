package org.example.stockitbe.store.order.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.store.order.batch.model.dto.StoreOrderBatchDto;
import org.example.stockitbe.store.order.model.enums.StoreOrderStatus;
import org.example.stockitbe.store.order.repository.StoreOrderHeaderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreOrderBatchApproveService {

    private final StoreOrderHeaderRepository headerRepository;
    private final InfrastructureRepository infrastructureRepository;

    // 승인 대기 발주건이 있는 매장 조회.
    // 두 단계로 분리하는 이유: countPendingByStore()가 storeId와 건수만 반환하므로
    // 매장명·지역 등 상세 정보는 storeId 목록으로 Infrastructure를 IN 쿼리 한 번에 조회해 N+1을 방지한다.
    @Transactional(readOnly = true)
    public List<StoreOrderBatchDto.PendingStoreRes> listPendingStores() {
        List<StoreOrderHeaderRepository.PendingStoreProjection> rows =
                headerRepository.countPendingByStore(StoreOrderStatus.REQUESTED);
        if (rows.isEmpty()) return List.of();

        List<Long> storeIds = rows.stream().map(StoreOrderHeaderRepository.PendingStoreProjection::getStoreId).toList();
        Map<Long, Infrastructure> infraById = new HashMap<>();
        for (Infrastructure infra : infrastructureRepository.findAllById(storeIds)) {
            infraById.put(infra.getId(), infra);
        }

        List<StoreOrderBatchDto.PendingStoreRes> result = new ArrayList<>();
        for (StoreOrderHeaderRepository.PendingStoreProjection row : rows) {
            Infrastructure store = infraById.get(row.getStoreId());
            if (store == null) continue;
            result.add(StoreOrderBatchDto.PendingStoreRes.builder()
                    .storeCode(store.getCode())
                    .storeName(store.getName())
                    .region(store.getRegion())
                    .requestedCount(row.getRequestedCount() == null ? 0 : row.getRequestedCount().intValue())
                    .build());
        }

        result.sort(Comparator.comparing(StoreOrderBatchDto.PendingStoreRes::getRequestedCount).reversed()
                .thenComparing(StoreOrderBatchDto.PendingStoreRes::getStoreName));
        return result;
    }
}
