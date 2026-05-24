package org.example.stockitbe.user;

import io.jsonwebtoken.Claims;
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




    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<UserDto.SignupRes>> signup(
            @RequestBody UserDto.SignupReq req) {
        UserDto.SignupRes result = userService.signup(req);
        return ResponseEntity.ok(BaseResponse.success(result));

    }


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

    @GetMapping("/mypage")
    public ResponseEntity<BaseResponse<UserDto.MypageRes>> getMypage(
            @AuthenticationPrincipal AuthUserDetails userDetails) {
        return ResponseEntity.ok(BaseResponse.success(
                userService.getMypage(userDetails.getEmployeeCode())
        ));
    }

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



