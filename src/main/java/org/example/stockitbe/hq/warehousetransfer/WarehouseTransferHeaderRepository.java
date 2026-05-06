package org.example.stockitbe.hq.warehousetransfer;

import org.example.stockitbe.hq.warehousetransfer.model.WarehouseTransferHeader;
import org.example.stockitbe.hq.warehousetransfer.model.WarehouseTransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface WarehouseTransferHeaderRepository extends JpaRepository<WarehouseTransferHeader, Long> {
    Optional<WarehouseTransferHeader> findByTransferNo(String transferNo);
    Optional<WarehouseTransferHeader> findTopByTransferNoStartingWithOrderByTransferNoDesc(String transferNoPrefix);
    List<WarehouseTransferHeader> findByRequestedAtBetweenOrderByRequestedAtDescIdDesc(Date from, Date to);
    List<WarehouseTransferHeader> findByStatusAndRequestedAtBetweenOrderByRequestedAtDescIdDesc(
            WarehouseTransferStatus status, Date from, Date to
    );
}
