package org.example.stockitbe.user;

import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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


@Tag(name = "사용자 인증", description = "로그인·회원가입·토큰 갱신·마이페이지 API")
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


    @Operation(
            summary = "로그인",
            description = """
                    사번(employeeCode)과 비밀번호로 로그인한다.

                    **테스트 계정 (비밀번호 공통: `Stockit!2026`)**

                    | 권한 | 사번 |
                    |------|------|
                    | 본사 (HQ) | `hq0001` |
                    | 매장 (STORE) | `st0001` |
                    | 창고 (WAREHOUSE) | `wh0001` |

                    **Swagger UI 테스트 방법:**
                    1. 아래 Request body에 employeeCode / password 입력 후 Execute
                    2. 응답 body의 `result.accessToken` 값을 복사
                    3. 화면 상단 **Authorize 🔒** 버튼 클릭 → 복사한 값 붙여넣기 → Authorize
                    4. 이후 모든 API 요청에 `Authorization: Bearer <token>` 헤더가 자동 첨부됨

                    > 브라우저/FE는 HTTP-only 쿠키(Atoken)를 사용합니다. accessToken 필드는 Swagger 전용입니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공 — result.accessToken 복사 후 Authorize에 입력"),
            @ApiResponse(responseCode = "401", description = "사번 또는 비밀번호 불일치")
    })
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<UserDto.LoginRes>> login(@RequestBody UserDto.LoginReq req) {
        // LoginFilter(Spring Security)가 이 요청을 먼저 인터셉트하여 처리한다.
        // 이 메서드 본체는 실행되지 않는다.
        throw new UnsupportedOperationException("Handled by LoginFilter");
    }


    @Operation(summary = "회원가입 신청", description = "신규 사용자 가입을 신청한다. 관리자 승인 후 로그인 가능.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "신청 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<UserDto.SignupRes>> signup(
            @RequestBody UserDto.SignupReq req) {
        UserDto.SignupRes result = userService.signup(req);
        return ResponseEntity.ok(BaseResponse.success(result));
    }


    @Operation(summary = "로그아웃", description = "Refresh Token을 무효화하고 Access/Refresh 쿠키를 삭제한다.")
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
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
            }
        }

        clearCookie(response, ACCESS_TOKEN_COOKIE, "/");
        clearCookie(response, REFRESH_TOKEN_COOKIE, REFRESH_TOKEN_PATH);

        return ResponseEntity.ok(BaseResponse.success(null));
    }


    @Operation(summary = "Access Token 갱신", description = "Refresh Token 쿠키(Rtoken)를 이용해 새 Access Token을 발급한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "갱신 성공"),
            @ApiResponse(responseCode = "401", description = "Refresh Token 만료 또는 무효")
    })
    @PostMapping("/refresh")
    @Transactional
    public ResponseEntity<BaseResponse<Void>> refresh(HttpServletRequest request,
                                                      HttpServletResponse response) {
        String refreshTokenValue = extractCookie(request, REFRESH_TOKEN_COOKIE);
        if (refreshTokenValue == null || !jwtUtil.validate(refreshTokenValue)) {
            throw BaseException.from(BaseResponseStatus.JWT_INVALID);
        }

        JwtRefresh stored = jwtRefreshRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.JWT_INVALID));

        if (stored.isExpired()) {
            jwtRefreshRepository.delete(stored);
            throw BaseException.from(BaseResponseStatus.JWT_EXPIRED);
        }

        Claims claims = jwtUtil.parseClaims(refreshTokenValue);
        String employeeCode = claims.getSubject();
        User user = userRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.APPROVED) {
            jwtRefreshRepository.deleteAllByEmployeeCode(employeeCode);
            throw BaseException.from(BaseResponseStatus.JWT_REFRESH_NOT_APPROVED);
        }

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

    @Operation(summary = "마이페이지 조회", description = "로그인 사용자의 프로필 정보를 조회한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/mypage")
    public ResponseEntity<BaseResponse<UserDto.MypageRes>> getMypage(
            @AuthenticationPrincipal AuthUserDetails userDetails) {
        return ResponseEntity.ok(BaseResponse.success(
                userService.getMypage(userDetails.getEmployeeCode())
        ));
    }

    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호 확인 후 새 비밀번호로 변경한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "현재 비밀번호 불일치")
    })
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

    @Operation(summary = "전화번호 변경", description = "로그인 사용자의 전화번호를 변경한다.")
    @ApiResponse(responseCode = "200", description = "변경 성공")
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
