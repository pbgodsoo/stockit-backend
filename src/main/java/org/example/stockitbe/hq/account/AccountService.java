package org.example.stockitbe.hq.account;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.account.model.AccountDto;
import org.example.stockitbe.user.UserRepository;
import org.example.stockitbe.user.model.User;
import org.example.stockitbe.user.model.UserRole;
import org.example.stockitbe.user.model.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AccountDto.PendingRes> getPendingAccounts() {
        return userRepository.findByStatusOrderByAppliedAtAsc(UserStatus.PENDING).stream()
                .map(AccountDto.PendingRes::from)
                .toList();
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

    private User findUserOrThrow(Long accountId) {
        return userRepository.findById(accountId)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.USER_NOT_FOUND));
    }

    private void validatePending(User user) {
        if (user.getStatus() != UserStatus.PENDING) {
            throw BaseException.from(BaseResponseStatus.USER_NOT_PENDING);
        }
    }

    private String generateEmployeeCode(UserRole role) {
        String prefix = role.getCodePrefix();
        int nextNumber = userRepository
                .findTopByEmployeeCodeStartingWithOrderByEmployeeCodeDesc(prefix)
                .map(user -> Integer.parseInt(user.getEmployeeCode().substring(2)) + 1)
                .orElse(1);
        return String.format("%s%04d", prefix, nextNumber);
    }

    /**
     * 전체 회원 목록 조회 (PENDING / APPROVED / REJECTED 모두)
     */
    @Transactional(readOnly = true)
    public List<AccountDto.PendingRes> getAllAccounts() {
        return userRepository.findAllByOrderByAppliedAtDesc().stream()
                .map(AccountDto.PendingRes::from)
                .toList();
    }
}