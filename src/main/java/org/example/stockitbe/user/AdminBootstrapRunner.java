package org.example.stockitbe.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.user.model.User;
import org.example.stockitbe.user.model.UserRole;
import org.example.stockitbe.user.model.UserStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 부팅 시 3개 권한 (본사/매장/창고) 관리자 계정 자동 생성/보강.
 * 기존 사용자 데이터는 건드리지 않고, 사원번호별로 없는 admin 만 추가.
 *
 * 비밀번호는 환경변수로 주입:
 *   ADMIN_BOOTSTRAP_PASSWORD=YourPass!2026  (.env 또는 시스템 env)
 * 미주입 시 부트스트랩 전체 skip.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AdminBootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.bootstrap.password:#{null}}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("[AdminBootstrap] ADMIN_BOOTSTRAP_PASSWORD 미설정 — 부트스트랩 skip");
            return;
        }

        String encoded = passwordEncoder.encode(adminPassword);
        LocalDateTime now = LocalDateTime.now();

        createIfMissing("HQ-A0001", "hq-admin@stockit.com", "본사 관리자",
                "01000000001", "HQ-CENTER", "본사 본부",
                UserRole.HQ, encoded, now);
        createIfMissing("ST-A0001", "store-admin@stockit.com", "매장 관리자",
                "01000000002", "ST-SL-0001", "강남 플래그십점",
                UserRole.STORE, encoded, now);
        createIfMissing("WH-A0001", "warehouse-admin@stockit.com", "창고 관리자",
                "01000000003", "WH-IC-0001", "인천 송도 국제물류센터",
                UserRole.WAREHOUSE, encoded, now);
    }

    private void createIfMissing(String employeeCode, String email, String name,
                                 String phone, String locationCode, String locationName,
                                 UserRole role, String encodedPassword, LocalDateTime now) {
        if (userRepository.findByEmployeeCode(employeeCode).isPresent()) {
            log.info("[AdminBootstrap] {} 이미 존재 — skip", employeeCode);
            return;
        }
        User user = User.builder()
                .employeeCode(employeeCode)
                .email(email)
                .name(name)
                .phoneNumber(phone)
                .locationCode(locationCode)
                .locationName(locationName)
                .password(encodedPassword)
                .role(role)
                .status(UserStatus.APPROVED)
                .appliedAt(now)
                .processedAt(now)
                .build();
        userRepository.save(user);
        log.info("[AdminBootstrap] {} ({}) 생성 완료", employeeCode, role);
    }
}
