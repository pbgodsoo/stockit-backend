package org.example.stockitbe.hq.circularbuyer.sync.repository;

import org.example.stockitbe.hq.circularbuyer.sync.model.EsSyncOutbox;
import org.example.stockitbe.hq.circularbuyer.sync.model.EsSyncOutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface EsSyncOutboxRepository extends JpaRepository<EsSyncOutbox, Long> {

    List<EsSyncOutbox> findByStatusAndNextRetryAtLessThanEqualOrderByIdAsc(
            EsSyncOutboxStatus status,
            Instant now,
            Pageable pageable
    );
}
