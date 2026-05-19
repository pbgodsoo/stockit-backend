package org.example.stockitbe.hq.account;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.jwt.JwtRefreshRepository;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.account.model.AccountDto;
import org.example.stockitbe.hq.account.model.entity.EmployeeCodeSequence;
import org.example.stockitbe.hq.account.repository.EmployeeCodeSequenceRepository;
import org.example.stockitbe.user.UserRepository;
import org.example.stockitbe.user.model.entity.User;
import org.example.stockitbe.user.model.entity.UserRole;
import org.example.stockitbe.user.model.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class AccountService {
    private final EmployeeCodeSequenceRepository sequenceRepository;
    private final UserRepository userRepository;
    private final JwtRefreshRepository jwtRefreshRepository;

    @Transactional(readOnly = true)
    public Page<AccountDto.PendingRes> getPendingAccounts(String keyword, Pageable pageable) {
        Specification<User> spec = buildSpec(keyword, null, UserStatus.PENDING);
        return userRepository.findAll(spec, pageable).map(AccountDto.PendingRes::from);
    }


    @Transactional
    public AccountDto.ProcessRes approve(Long accountId) {
        User user = findUserOrThrow(accountId);
        validatePending(user);

        String employeeCode = generateEmployeeCode(user.getRole());
        user.approve(employeeCode);

        return AccountDto.ProcessRes.from(user);
    }

    @Transactional
    public AccountDto.ProcessRes reject(Long accountId) {
        User user = findUserOrThrow(accountId);
        validatePending(user);

        user.reject();

        return AccountDto.ProcessRes.from(user);
    }

    //  본사 관리자가 사용자 계정을 탈퇴 처리
    @Transactional
    public AccountDto.ProcessRes withdraw(Long accountId) {
        User user = findUserOrThrow(accountId);
        validateApproved(user);

        user.withdraw();
        // 탈퇴 사용자의 모든 Refresh Token 삭제 → 즉시 강제 로그아웃 효과
        jwtRefreshRepository.deleteAllByEmployeeCode(user.getEmployeeCode());

        return AccountDto.ProcessRes.from(user);
    }


    private User findUserOrThrow(Long accountId) {
        return userRepository.findById(accountId)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.USER_NOT_FOUND));
    }

    private void validatePending(User user) {
        if (user.getStatus() != UserStatus.PENDING) {
            throw BaseException.from(BaseResponseStatus.USER_NOT_PENDING);
        }
    }

    //  APPROVED 상태인 사용자만 탈퇴 처리 가능
    private void validateApproved(User user) {
        if (user.getStatus() != UserStatus.APPROVED) {
            if (user.getStatus() == UserStatus.WITHDRAWN) {
                throw BaseException.from(BaseResponseStatus.USER_ALREADY_WITHDRAWN);
            }
            throw BaseException.from(BaseResponseStatus.USER_NOT_APPROVED);
        }
    }

    private String generateEmployeeCode(UserRole role) {
        String prefix = role.getCodePrefix();
        // 비관적 락으로 시퀀스 row 잠금 → 다른 트랜잭션은 커밋까지 대기
        EmployeeCodeSequence seq = sequenceRepository
                .findByRoleCodeForUpdate(prefix)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.EMPLOYEE_CODE_SEQUENCE_NOT_FOUND));
        int nextNumber = seq.incrementAndGet();
        return String.format("%s%04d", prefix, nextNumber);
    }

    /**
     * 전체 회원 목록 조회 (PENDING / APPROVED / REJECTED 모두)
     */
    @Transactional(readOnly = true)
    public Page<AccountDto.PendingRes> getAllAccounts(
            String keyword, UserRole role, UserStatus status, Pageable pageable) {
        Specification<User> spec = buildSpec(keyword, role, status);
        return userRepository.findAll(spec, pageable).map(AccountDto.PendingRes::from);
    }

    /**
     * 동적 검색 조건 빌더
     *  - keyword: 이름 OR 이메일 OR 사원코드 LIKE 검색 (null/blank 시 무시)
     *  - role:    권한 필터 (null 시 전체)
     *  - status:  상태 필터 (null 시 전체)
     */
    private Specification<User> buildSpec(String keyword, UserRole role, UserStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 키워드 검색 (이름/이메일/사원코드 OR)
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("name"), pattern),
                        cb.like(root.get("email"), pattern),
                        cb.like(root.get("employeeCode"), pattern)
                ));
            }
            // 권한 필터
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            // 상태 필터
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}