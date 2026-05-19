package org.example.stockitbe.hq.account.repository;

import jakarta.persistence.LockModeType;
import org.example.stockitbe.hq.account.model.entity.EmployeeCodeSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeCodeSequenceRepository
        extends JpaRepository<EmployeeCodeSequence, String> {

    /** SELECT ... FOR UPDATE — 다른 트랜잭션이 같은 role_code row 를 접근 시 대기 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM EmployeeCodeSequence s WHERE s.roleCode = :roleCode")
    Optional<EmployeeCodeSequence> findByRoleCodeForUpdate(@Param("roleCode") String roleCode);
}