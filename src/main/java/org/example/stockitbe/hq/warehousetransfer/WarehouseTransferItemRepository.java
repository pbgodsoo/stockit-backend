package org.example.stockitbe.hq.warehousetransfer;

import org.example.stockitbe.hq.warehousetransfer.model.WarehouseTransferItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface WarehouseTransferItemRepository extends JpaRepository<WarehouseTransferItem, Long> {
    List<WarehouseTransferItem> findAllByHeader_IdOrderByIdAsc(Long headerId);
    List<WarehouseTransferItem> findAllByHeader_IdIn(Collection<Long> headerIds);
}
