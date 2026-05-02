package org.example.stockitbe.user;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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



    // 5번
    @Override
    public UserDetails loadUserByUsername(String employeeCode) throws UsernameNotFoundException{
        // 6번
        User user = userRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.USER_NOT_FOUND));

        // 7번
        return AuthUserDetails.from(user);
    }
}
