package org.example.stockitbe.common.jwt;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_token",
        indexes = {
        @Index(name = "id_refresh_employee_code",
                columnList = "employeeCode")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtRefresh {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 토큰을 발급받은 사용자의 사원코드
    @Column(nullable = false)
    private String employeeCode;

    //  Refresh 토큰 값
    @Column(nullable = false, unique = true)
    private String token;

    //  만료 시간
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    //  발급 시각
    @Column(nullable = false)
    private LocalDateTime createdAt;

    //  만료 여부 (현재 시각 기준)
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
