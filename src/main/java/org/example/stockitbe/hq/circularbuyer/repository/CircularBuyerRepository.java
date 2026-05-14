package org.example.stockitbe.hq.circularbuyer.repository;

import jakarta.persistence.LockModeType;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CircularBuyerRepository
        extends JpaRepository<CircularBuyer, Long>, JpaSpecificationExecutor<CircularBuyer> {

    Optional<CircularBuyer> findByCode(String code);

    /**
     * 자동 코드 부여 시 동시성 제어 — 마지막(max code) row 에 PESSIMISTIC_WRITE 락.
     * 두 트랜잭션이 동시에 호출하면 한 쪽은 락 대기 → 깨어나면 새 max 다시 조회 → 충돌 회피.
     * Pageable 로 top 1 만 조회. 빈 테이블이면 빈 리스트 (시드 30건이 있어 정상 운영 시 항상 1건 이상).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from CircularBuyer b order by b.code desc")
    List<CircularBuyer> findAllOrderByCodeDescForUpdate(Pageable pageable);
}
