package org.example.stockitbe.user;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.jwt.JwtRefreshRepository;
import org.example.stockitbe.common.model.BaseResponseStatus;
// Phase 2 알림 트리거 — 회원가입 시 본사에 PENDING 알림 발행
import org.example.stockitbe.notification.event.NotificationEvent;
import org.example.stockitbe.notification.model.entity.NotificationSeverity;
import org.example.stockitbe.notification.model.entity.NotificationType;
import org.example.stockitbe.user.model.entity.AuthUserDetails;
import org.example.stockitbe.user.model.entity.User;
import org.example.stockitbe.user.model.entity.UserRole;
import org.example.stockitbe.user.model.dto.UserDto;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;



@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    // 정규식 Pattern 1회 컴파일 후 재사용 — 매 매칭마다 컴파일 비용 회피.
    // String.matches() 는 내부적으로 매 호출 시 Pattern.compile() 수행 → 회원가입 폭주 시 누적 부담.
    // Pattern 인스턴스는 thread-safe 이므로 static final 로 안전하게 공유.

    // 전화번호: 하이픈 없이 010 + 숫자 8자리 (총 11자리)
    private static final Pattern PHONE_PATTERN = Pattern.compile("^010\\d{8}$");
    // 비밀번호 정책: 대소문자, 숫자, 특수문자(!@#$%^&*) 포함 8자 이상
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtRefreshRepository jwtRefreshRepository;
    // Phase 2 — 도메인 이벤트 발행자 (Spring 기본 제공). signup() 끝에서 NotificationEvent 발행에 사용
    private final ApplicationEventPublisher eventPublisher;

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

        if (dto.getPassword() == null || !PASSWORD_PATTERN.matcher(dto.getPassword()).matches()) {
            throw BaseException.from(BaseResponseStatus.SIGNUP_INVALID_PASSWORD);
        }

        if (dto.getPhoneNumber() == null || !PHONE_PATTERN.matcher(dto.getPhoneNumber()).matches()) {
            throw BaseException.from(BaseResponseStatus.SIGNUP_INVALID_PHONE);
        }

        // 2. 비밀번호 인코딩
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        // 3. User 저장
        User saved = userRepository.save(dto.toEntity(encodedPassword));



        // 4. 본사(HQ) 전체에 회원가입 PENDING 알림 발행
        //    AFTER_COMMIT 으로 처리되므로, 위 user 저장이 롤백되면 알림도 발생하지 않음 (정합성 보장)
        eventPublisher.publishEvent(NotificationEvent.builder()
                .type(NotificationType.USER_SIGNUP_PENDING)
                .severity(NotificationSeverity.INFO)
                .title("신규 회원가입 신청")
                .message(saved.getName() + "(" + saved.getEmail() + ") 회원가입 승인 대기 중입니다.")
                .targetRole(UserRole.HQ)                   // 본사 전체 수신
                .refType("USER")                            // 원천 도메인 추적용
                .refId(String.valueOf(saved.getId()))
                .build());



        // 5. 응답 반환
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
     * - User 의 @Version 으로 동시 UPDATE 충돌 자동 감지 → OptimisticLockingFailureException 발생 시
     *   USER_CONCURRENT_MODIFICATION 으로 변환하여 친화 메시지 전달.
     */
    @Transactional
    public UserDto.MypageRes updatePhone(String employeeCode, String phoneNumber) {
        try {
            User user = userRepository.findByEmployeeCode(employeeCode)
                    .orElseThrow(() -> BaseException.from(BaseResponseStatus.USER_NOT_FOUND));

            if (phoneNumber == null || !PHONE_PATTERN.matcher(phoneNumber).matches()) {
                throw BaseException.from(BaseResponseStatus.SIGNUP_INVALID_PHONE);
            }

            user.updatePhone(phoneNumber);
            return UserDto.MypageRes.from(user);
        } catch (OptimisticLockingFailureException ex) {
            // 두 탭/디바이스 동시 UPDATE 충돌 — 본인 정보 수정의 lost update 방지.
            throw BaseException.from(BaseResponseStatus.USER_CONCURRENT_MODIFICATION);
        }
    }

    /**
     * 비밀번호 변경.
     * - 현재 비밀번호 검증 → 새 비밀번호 정책 검증 → 동일 비번 차단 → 인코딩 저장
     * - (보안) 본인 포함 모든 Refresh Token 삭제 → 모든 디바이스 강제 로그아웃
     * - User 의 @Version 으로 동시 UPDATE 충돌 자동 감지.
     */
    @Transactional
    public void updatePassword(String employeeCode, String currentPassword, String newPassword) {
        try {
            User user = userRepository.findByEmployeeCode(employeeCode)
                    .orElseThrow(() -> BaseException.from(BaseResponseStatus.USER_NOT_FOUND));

            // 1) 현재 비밀번호 검증
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw BaseException.from(BaseResponseStatus.PASSWORD_WRONG);
            }
            // 2) 새 비밀번호 정책 검증 (대소문자/숫자/특수문자 포함 8자 이상)
            if (newPassword == null || !PASSWORD_PATTERN.matcher(newPassword).matches()) {
                throw BaseException.from(BaseResponseStatus.SIGNUP_INVALID_PASSWORD);
            }
            // 3) 동일 비밀번호 차단
            if (passwordEncoder.matches(newPassword, user.getPassword())) {
                throw BaseException.from(BaseResponseStatus.USER_PASSWORD_SAME);
            }

            user.updatePassword(passwordEncoder.encode(newPassword));

            // (보안) 모든 디바이스 강제 로그아웃 — 본인 디바이스도 포함
            jwtRefreshRepository.deleteAllByEmployeeCode(employeeCode);
        } catch (OptimisticLockingFailureException ex) {
            throw BaseException.from(BaseResponseStatus.USER_CONCURRENT_MODIFICATION);
        }
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
