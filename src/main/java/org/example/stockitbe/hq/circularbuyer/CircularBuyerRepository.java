package org.example.stockitbe.hq.circularbuyer;

import org.example.stockitbe.hq.circularbuyer.model.CircularBuyer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CircularBuyerRepository
        extends JpaRepository<CircularBuyer, Long>, JpaSpecificationExecutor<CircularBuyer> {

    Optional<CircularBuyer> findByCode(String code);

    boolean existsByCode(String code);
}
