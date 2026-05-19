package org.example.stockitbe.hq.account;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.hq.account.model.entity.EmployeeCodeSequence;
import org.example.stockitbe.hq.account.repository.EmployeeCodeSequenceRepository;
import org.example.stockitbe.user.model.entity.UserRole;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사원코드 시퀀스 시드 자동 초기화.
 * BE 부팅 시 1회 실행, 멱등 처리(이미 있으면 skip).
 * 신규 환경 배포·신입 팀원 git pull 모두 자동 대응.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeCodeSequenceInitializer implements ApplicationRunner {

    private final EmployeeCodeSequenceRepository sequenceRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (UserRole role : UserRole.values()) {
            String prefix = role.getCodePrefix();
            if (sequenceRepository.existsById(prefix)) {
                continue;
            }
            sequenceRepository.save(EmployeeCodeSequence.of(prefix, 0));
            log.info("[Seed] employee_code_sequence 초기화: role={}, last_number=0", prefix);
        }
    }
}
