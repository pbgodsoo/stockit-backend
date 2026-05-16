package org.example.stockitbe.hq.circularbuyer.repository;

import jakarta.persistence.LockModeType;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CircularBuyerRepository
        extends JpaRepository<CircularBuyer, Long>, JpaSpecificationExecutor<CircularBuyer> {

    Optional<CircularBuyer> findByCode(String code);

    /**
     * 목록 페이지 조회 — embedding 컬럼을 SELECT에서 제외해 행당 ~23KB JSON 역직렬화 생략.
     * kw: null 이면 keyword 조건 없음, mf: null 이면 materialFit 조건 없음, pt: null 이면 partnerType 조건 없음.
     */
    @Query(value = "SELECT b.code as code, b.companyName as companyName, b.industryGroup as industryGroup, " +
                   "b.factoryProduct as factoryProduct, b.description as description, " +
                   "b.primaryMaterialFit as primaryMaterialFit, b.managerName as managerName, " +
                   "b.phone as phone, b.address as address, b.partnerType as partnerType " +
                   "FROM CircularBuyer b " +
                   "WHERE (:kw IS NULL OR LOWER(b.companyName) LIKE :kw " +
                   "       OR LOWER(b.code) LIKE :kw OR LOWER(b.managerName) LIKE :kw) " +
                   "AND (:mf IS NULL OR b.primaryMaterialFit = :mf) " +
                   "AND (:pt IS NULL OR b.partnerType = :pt)",
           countQuery = "SELECT COUNT(b) FROM CircularBuyer b " +
                        "WHERE (:kw IS NULL OR LOWER(b.companyName) LIKE :kw " +
                        "       OR LOWER(b.code) LIKE :kw OR LOWER(b.managerName) LIKE :kw) " +
                        "AND (:mf IS NULL OR b.primaryMaterialFit = :mf) " +
                        "AND (:pt IS NULL OR b.partnerType = :pt)")
    Page<CircularBuyerListView> findPageWithoutEmbedding(
            @Param("kw") String kwLike,
            @Param("mf") String materialFit,
            @Param("pt") String partnerType,
            Pageable pageable);

    /** primaryMaterialFit 별 건수 — 통계 카드용 단일 쿼리. */
    @Query("SELECT b.primaryMaterialFit as materialFit, COUNT(b) as count FROM CircularBuyer b GROUP BY b.primaryMaterialFit")
    List<CircularBuyerMaterialFitCount> countGroupByMaterialFit();

    /**
     * 자동 코드 부여 시 동시성 제어 — 마지막(max code) row 에 PESSIMISTIC_WRITE 락.
     * 두 트랜잭션이 동시에 호출하면 한 쪽은 락 대기 → 깨어나면 새 max 다시 조회 → 충돌 회피.
     * Pageable 로 top 1 만 조회. 빈 테이블이면 빈 리스트 (시드 30건이 있어 정상 운영 시 항상 1건 이상).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from CircularBuyer b order by b.code desc")
    List<CircularBuyer> findAllOrderByCodeDescForUpdate(Pageable pageable);
}
