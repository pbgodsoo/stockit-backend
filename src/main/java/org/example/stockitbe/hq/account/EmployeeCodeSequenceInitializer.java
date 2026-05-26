package org.example.stockitbe.hq.account;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.hq.account.model.entity.EmployeeCodeSequence;
import org.example.stockitbe.hq.account.repository.EmployeeCodeSequenceRepository;
import org.example.stockitbe.user.UserRepository;
import org.example.stockitbe.user.model.entity.UserRole;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사원코드 시퀀스 시드 자동 초기화.
 * BE 부팅 시 1회 실행, 멱등 처리(이미 있으면 skip).
 * 신규 환경 배포·신입 팀원 git pull 모두 자동 대응.
 *
 * 초기값 산정:
 *   user 테이블에 이미 같은 prefix 의 사번이 있으면 그 MAX 로 초기화.
 *   → 부트스트랩 admin (예: hq0001) 이나 시드 데이터가 먼저 들어와 있어도
 *     첫 승인이 Duplicate entry 로 실패하는 사고를 사전 차단.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeCodeSequenceInitializer implements ApplicationRunner {

    private final EmployeeCodeSequenceRepository sequenceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (UserRole role : UserRole.values()) {
            String prefix = role.getCodePrefix();
            if (sequenceRepository.existsById(prefix)) {
                continue;
            }
            // user 테이블의 실제 MAX 로 초기화 — 0 으로 하드코딩하던 기존 버그 수정.
            int initialNumber = userRepository.findMaxEmployeeCodeNumber(prefix);
            sequenceRepository.save(EmployeeCodeSequence.of(prefix, initialNumber));
            log.info("[Seed] employee_code_sequence 초기화: role={}, last_number={}", prefix, initialNumber);
        }
    }
}
