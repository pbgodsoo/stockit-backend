package org.example.stockitbe.hq.warehousetransfer;

import org.example.stockitbe.hq.warehousetransfer.model.WarehouseTransferItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseTransferItemRepository extends JpaRepository<WarehouseTransferItem, Long> {
}
