package org.example.stockitbe.hq.esg.quota;

import org.example.stockitbe.hq.esg.quota.model.EmissionQuota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmissionQuotaRepository extends JpaRepository<EmissionQuota, Long> {
    Optional<EmissionQuota> findByFiscalYear(Integer fiscalYear);
}
