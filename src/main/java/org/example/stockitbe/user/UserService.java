package org.example.stockitbe.user;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.jwt.JwtRefreshRepository;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.user.model.AuthUserDetails;
import org.example.stockitbe.user.model.User;
import org.example.stockitbe.user.model.UserDto;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    // 전화번호: 하이픈 없이 010 + 숫자 8자리 (총 11자리)
    private static final String PHONE_REGEX = "^010\\d{8}$";
    // 비밀번호 정책: 대소문자, 숫자, 특수문자(!@#$%^&*) 포함 8자 이상
    private static final String PASSWORD_REGEX =
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtRefreshRepository jwtRefreshRepository;

    /**
     * 회원가입 신청
     * - 이메일 중복 검사 후 PENDING 상태로 저장
     * - 사원코드는 본사 관리자 승인 시점에 부여됨
     */
    @Transactional
    public UserDto.SignupRes signup(UserDto.SignupReq dto) {
        // 1. 이메일 중복 검사
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw BaseException.from(BaseResponseStatus.SIGNUP_DUPLICATE_EMAIL);
        }

        // 2. 비밀번호 인코딩
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        // 3. User 저장
        User saved = userRepository.save(dto.toEntity(encodedPassword));

        // 4. 응답 반환
        return UserDto.SignupRes.from(saved);
    }

    /**
     * 마이페이지 정보 조회 (현재 로그인 사용자 본인)
     */
    @Transactional(readOnly = true)
    public UserDto.MypageRes getMypage(String employeeCode) {
        User user = userRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.USER_NOT_FOUND));
        return UserDto.MypageRes.from(user);
    }

    /**
     * 전화번호 수정.
     * - FE 에서 하이픈 없이 11자리 숫자 전송 (예: "01012345678")
     */
    @Transactional
    public UserDto.MypageRes updatePhone(String employeeCode, String phoneNumber) {
        User user = userRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.USER_NOT_FOUND));

        if (phoneNumber == null || !phoneNumber.matches(PHONE_REGEX)) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        user.updatePhone(phoneNumber);
        return UserDto.MypageRes.from(user);
    }

    /**
     * 비밀번호 변경.
     * - 현재 비밀번호 검증 → 새 비밀번호 정책 검증 → 동일 비번 차단 → 인코딩 저장
     * - (보안) 본인 포함 모든 Refresh Token 삭제 → 모든 디바이스 강제 로그아웃
     */
    @Transactional
    public void updatePassword(String employeeCode, String currentPassword, String newPassword) {
        User user = userRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.USER_NOT_FOUND));

        // 1) 현재 비밀번호 검증
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw BaseException.from(BaseResponseStatus.PASSWORD_WRONG);
        }
        // 2) 새 비밀번호 정책 검증 (대소문자/숫자/특수문자 포함 8자 이상)
        if (newPassword == null || !newPassword.matches(PASSWORD_REGEX)) {
            throw BaseException.from(BaseResponseStatus.SIGNUP_INVALID_PASSWORD);
        }
        // 3) 동일 비밀번호 차단
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw BaseException.from(BaseResponseStatus.USER_PASSWORD_SAME);
        }

        user.updatePassword(passwordEncoder.encode(newPassword));

        // (보안) 모든 디바이스 강제 로그아웃 — 본인 디바이스도 포함
        jwtRefreshRepository.deleteAllByEmployeeCode(employeeCode);
    }

    /**
     * Spring Security 가 로그인 시 호출 — employeeCode 로 사용자 조회.
     */
    @Override
    public UserDetails loadUserByUsername(String employeeCode) throws UsernameNotFoundException {
        User user = userRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.USER_NOT_FOUND));
        return AuthUserDetails.from(user);
    }
}
