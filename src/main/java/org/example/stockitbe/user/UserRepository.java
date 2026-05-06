package org.example.stockitbe.user;

import org.example.stockitbe.user.model.User;
import org.example.stockitbe.user.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmployeeCode(String employeeCode);
    boolean existsByEmployeeCode(String employeeCode);
    boolean existsByEmail(String email);
    Optional<User> findTopByEmployeeCodeStartingWithOrderByEmployeeCodeDesc(String prefix);

    // 상태별 사용자 목록 조회 (신청일시 오름차순)
    List<User> findByStatusOrderByAppliedAtAsc(UserStatus status);

    // 전체 회원 목록
    List<User> findAllByOrderByAppliedAtDesc();

}
