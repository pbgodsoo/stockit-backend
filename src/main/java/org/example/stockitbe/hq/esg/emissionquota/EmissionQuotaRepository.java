package org.example.stockitbe.hq.esg.emissionquota;

import org.example.stockitbe.hq.esg.emissionquota.model.EmissionQuota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmissionQuotaRepository extends JpaRepository<EmissionQuota, Long> {
    Optional<EmissionQuota> findByFiscalYear(Integer fiscalYear);
}
