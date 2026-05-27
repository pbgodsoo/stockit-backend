package org.example.stockitbe.user;

import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.jwt.JwtRefresh;
import org.example.stockitbe.common.jwt.JwtRefreshRepository;
import org.example.stockitbe.common.jwt.JwtUtil;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.user.model.entity.AuthUserDetails;
import org.example.stockitbe.user.model.entity.User;
import org.example.stockitbe.user.model.dto.UserDto;
import org.example.stockitbe.user.model.entity.UserStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회원 - 인증/마이페이지", description = "회원가입 · Access Token 갱신 · 로그아웃 · 마이페이지 조회/수정 API (USER-001~004, 008)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {
    private static final String ACCESS_TOKEN_COOKIE = "Atoken";
    private static final String REFRESH_TOKEN_COOKIE = "Rtoken";
    private static final String REFRESH_TOKEN_PATH = "/api/user";


    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final JwtRefreshRepository jwtRefreshRepository;
    private final UserRepository userRepository;



    @Operation(summary = "로그인",
            description = "사원코드와 비밀번호로 로그인합니다. 성공 시 HttpOnly 쿠키로 Atoken(Access)/Rtoken(Refresh) 토큰이 발급됩니다. " +
                    "실제 처리는 Spring Security의 LoginFilter가 담당하므로 이 메서드는 호출되지 않으며 Swagger 문서 노출 전용입니다.")
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<UserDto.LoginRes>> login(@RequestBody UserDto.LoginReq req) {
        // LoginFilter (UsernamePasswordAuthenticationFilter 상속) 가 /api/user/login POST 요청을 가로챕니다.
        // 이 메서드 본문은 실행되지 않으며 Swagger UI 노출 + OpenAPI 스키마 생성 용도입니다.
        throw new UnsupportedOperationException("Handled by LoginFilter");
    }


    @Operation(summary = "회원가입 신청",
            description = "신규 회원이 가입을 신청합니다. 본사 관리자 승인 후 사원코드가 자동 발급됩니다.")
    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<UserDto.SignupRes>> signup(
            @RequestBody UserDto.SignupReq req) {
        UserDto.SignupRes result = userService.signup(req);
        return ResponseEntity.ok(BaseResponse.success(result));

    }

    @Operation(summary = "Access Token 자동 갱신",
            description = "Refresh Token 쿠키로 새 Access Token을 발급받습니다.")
    @PostMapping("/refresh")
    @Transactional
    public ResponseEntity<BaseResponse<Void>> refresh(HttpServletRequest request,
                                                      HttpServletResponse response) {
        String refreshTokenValue = extractCookie(request, REFRESH_TOKEN_COOKIE);
        if (refreshTokenValue == null || !jwtUtil.validate(refreshTokenValue)) {
            throw BaseException.from(BaseResponseStatus.JWT_INVALID);
        }

        // DB에서 토큰 존재 확인 (무효화된 토큰 차단)
        JwtRefresh stored = jwtRefreshRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.JWT_INVALID));

        if (stored.isExpired()) {
            jwtRefreshRepository.delete(stored);
            throw BaseException.from(BaseResponseStatus.JWT_EXPIRED);
        }

        // 사용자 조회 + 정지 여부 체크
        Claims claims = jwtUtil.parseClaims(refreshTokenValue);
        String employeeCode = claims.getSubject();
        User user = userRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.APPROVED) {
            // 정지/거절된 사용자는 모든 토큰 무효화
            jwtRefreshRepository.deleteAllByEmployeeCode(employeeCode);
            throw BaseException.from(BaseResponseStatus.JWT_REFRESH_NOT_APPROVED);
        }

        // 새 Access Token 발급
        String newAccessToken = jwtUtil.createAccessToken(employeeCode, user.getRole());
        Cookie accessCookie = new Cookie(ACCESS_TOKEN_COOKIE, newAccessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false);
        accessCookie.setPath("/");
        accessCookie.setMaxAge((int) (jwtUtil.getAccessExpirationMs() / 1000));
        accessCookie.setAttribute("SameSite", "Lax");
        response.addCookie(accessCookie);

        return ResponseEntity.ok(BaseResponse.success(null));
    }



    @Operation(summary = "로그아웃",
            description = "Access/Refresh Token 쿠키를 삭제하고 DB의 Refresh Token을 무효화합니다.")
    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<BaseResponse<Void>> logout(HttpServletRequest request,
                                                     HttpServletResponse response) {
        String refreshTokenValue = extractCookie(request, REFRESH_TOKEN_COOKIE);
        if (refreshTokenValue != null) {
            try {
                Claims claims = jwtUtil.parseClaims(refreshTokenValue);
                String employeeCode = claims.getSubject();
                jwtRefreshRepository.deleteAllByEmployeeCode(employeeCode);
            } catch (Exception ignored) {
                // 토큰 파싱 실패해도 쿠키 삭제는 진행
            }
        }

        // Access Token 쿠키 만료
        clearCookie(response, ACCESS_TOKEN_COOKIE, "/");
        // Refresh Token 쿠키 만료
        clearCookie(response, REFRESH_TOKEN_COOKIE, REFRESH_TOKEN_PATH);

        return ResponseEntity.ok(BaseResponse.success(null));
    }

    @Operation(summary = "마이페이지 본인 정보 조회",
            description = "현재 로그인한 사용자의 본인 정보를 반환합니다.")
    @GetMapping("/mypage")
    public ResponseEntity<BaseResponse<UserDto.MypageRes>> getMypage(
            @AuthenticationPrincipal AuthUserDetails userDetails) {
        return ResponseEntity.ok(BaseResponse.success(
                userService.getMypage(userDetails.getEmployeeCode())
        ));
    }

    @Operation(summary = "비밀번호 변경",
            description = "기존 비밀번호 검증 후 새 비밀번호로 변경합니다. 변경 시 모든 디바이스가 강제 로그아웃됩니다.")
    @PatchMapping("/mypage/password")
    public ResponseEntity<BaseResponse<Void>> updatePassword(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @RequestBody UserDto.UpdatePasswordReq req) {
        userService.updatePassword(
                userDetails.getEmployeeCode(),
                req.getCurrentPassword(),
                req.getNewPassword()
        );
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    @Operation(summary = "전화번호 수정",
            description = "본인 전화번호를 수정합니다. 하이픈 없이 010 + 8자리 숫자 형식.")
    @PatchMapping("/mypage/phone")
    public ResponseEntity<BaseResponse<UserDto.MypageRes>> updatePhone(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @RequestBody UserDto.UpdatePhoneReq req) {
        UserDto.MypageRes result = userService.updatePhone(
                userDetails.getEmployeeCode(),
                req.getPhoneNumber()
        );
        return ResponseEntity.ok(BaseResponse.success(result));
    }



    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private void clearCookie(HttpServletResponse response, String name, String path) {
        Cookie cookie = new Cookie(name, null);
        cookie.setMaxAge(0);
        cookie.setPath(path);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}



