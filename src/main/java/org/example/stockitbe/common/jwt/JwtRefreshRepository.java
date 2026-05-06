package org.example.stockitbe.common.jwt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface JwtRefreshRepository extends JpaRepository<JwtRefresh, Long> {

    Optional<JwtRefresh> findByToken(String token);

    //  사용자의 모든 Refresh Token 삭제 (로그아웃, 강제 로그아웃, 정지)
    void deleteAllByEmployeeCode(String employeeCode);

    //  만료된 토큰 일괄 정리 (배치용, 선택)
    void deleteAllByExpiresAtBefore(LocalDateTime now);
}
