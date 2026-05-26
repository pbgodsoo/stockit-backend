package org.example.stockitbe.user;

import org.example.stockitbe.user.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmployeeCode(String employeeCode);
    boolean existsByEmployeeCode(String employeeCode);
    boolean existsByEmail(String email);
    Optional<User> findTopByEmployeeCodeStartingWithOrderByEmployeeCodeDesc(String prefix);

    /**
     * 특정 prefix 의 사원코드 중 가장 큰 숫자 부분을 반환 (없으면 0).
     * employee_code_sequence 와 user 테이블 데이터가 어긋났을 때 보정/초기화용 안전망.
     * 예) prefix="hq" + user 에 hq0001, hq0002 → 2 반환.
     */
    @Query(value = """
        SELECT COALESCE(MAX(CAST(SUBSTRING(employee_code, 3) AS UNSIGNED)), 0)
        FROM user
        WHERE employee_code REGEXP CONCAT('^', :prefix, '[0-9]+$')
        """, nativeQuery = true)
    int findMaxEmployeeCodeNumber(@Param("prefix") String prefix);

}
