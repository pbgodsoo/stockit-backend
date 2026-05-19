package org.example.stockitbe.common.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Refresh Token DB 작업 전용 서비스.
 *
 * LoginSuccessHandler 에서 메서드 전체에 @Transactional 을 걸지 않도록
 * DB 작업(삭제 + 저장)만 분리하여 최소 트랜잭션 범위로 호출한다.
 *
 * 효과:
 *  - HikariCP 커넥션 점유 시간 단축 (응답 쓰기 도중에는 커넥션 미점유)
 *  - 응답 쓰기 실패 시에도 RT 는 커밋되어 클라가 RT 재사용 가능
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final JwtRefreshRepository jwtRefreshRepository;

    /**
     * 동일 사용자의 기존 RT 모두 삭제 후 새 RT 1건 저장 (1 user = 1 active refresh).
     * 트랜잭션 범위 = 이 메서드 안쪽 (수 ms).
     */
    @Transactional
    public void replaceRefreshToken(String employeeCode, String token, long refreshExpirationMs) {
        jwtRefreshRepository.deleteAllByEmployeeCode(employeeCode);
        LocalDateTime now = LocalDateTime.now();
        jwtRefreshRepository.save(JwtRefresh.builder()
                .employeeCode(employeeCode)
                .token(token)
                .expiresAt(now.plusSeconds(refreshExpirationMs / 1000))
                .createdAt(now)
                .build());
    }
}
