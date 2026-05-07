package org.example.stockitbe.hq.inventory.model;

import java.util.EnumSet;
import java.util.Set;

public final class InventoryStatusPolicy {

    public static final Set<InventoryStatus> QUERY_ALLOWED_STATUSES =
            EnumSet.of(InventoryStatus.NORMAL, InventoryStatus.CIRCULAR_CANDIDATE);

    private InventoryStatusPolicy() {
    }
}
