package org.example.stockitbe.hq.inventory.model;

import java.util.EnumSet;
import java.util.Set;

public final class InventoryStatusPolicy {

    // 조회 API에서 노출 가능한 재고 상태 목록
    public static final Set<InventoryStatus> QUERY_ALLOWED_STATUSES =
            EnumSet.of(InventoryStatus.NORMAL, InventoryStatus.CIRCULAR_CANDIDATE);

    private InventoryStatusPolicy() {
    }
}
