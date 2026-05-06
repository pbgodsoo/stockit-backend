package org.example.stockitbe.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.jwt.JwtRefresh;
import org.example.stockitbe.common.jwt.JwtRefreshRepository;
import org.example.stockitbe.common.jwt.JwtUtil;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.user.model.AuthUserDetails;
import org.example.stockitbe.user.model.UserDto;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String ACCESS_TOKEN_COOKIE = "Atoken";
    private static final String REFRESH_TOKEN_COOKIE = "Rtoken";
    /** Refresh Token 은 /api/user/refresh 호출 시에만 전송되도록 Path 제한 */
    private static final String REFRESH_TOKEN_PATH = "/api/user";

    private final JwtUtil jwtUtil;
    private final JwtRefreshRepository jwtRefreshRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        AuthUserDetails userDetails = (AuthUserDetails) authentication.getPrincipal();
        String employeeCode = userDetails.getEmployeeCode();

        // 1. Access / Refresh 토큰 발급
        String accessToken = jwtUtil.createAccessToken(employeeCode, userDetails.getRole());
        String refreshToken = jwtUtil.createRefreshToken(employeeCode);

        // 2. 기존 Refresh Token 삭제 후 새로 저장 (1 user = 1 active refresh)
        jwtRefreshRepository.deleteAllByEmployeeCode(employeeCode);
        jwtRefreshRepository.save(JwtRefresh.builder()
                .employeeCode(employeeCode)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtUtil.getRefreshExpirationMs() / 1000))
                .createdAt(LocalDateTime.now())
                .build());

        // 3. Access Token 쿠키 (모든 경로에서 전송)
        Cookie accessCookie = new Cookie(ACCESS_TOKEN_COOKIE, accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false);   // 운영(HTTPS): true
        accessCookie.setPath("/");
        accessCookie.setMaxAge((int) (jwtUtil.getAccessExpirationMs() / 1000));
        accessCookie.setAttribute("SameSite", "Lax");
        response.addCookie(accessCookie);

        // 4. Refresh Token 쿠키 (Path 제한 — refresh 호출 시에만 전송)
        Cookie refreshCookie = new Cookie(REFRESH_TOKEN_COOKIE, refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath(REFRESH_TOKEN_PATH);
        refreshCookie.setMaxAge((int) (jwtUtil.getRefreshExpirationMs() / 1000));
        refreshCookie.setAttribute("SameSite", "Lax");
        response.addCookie(refreshCookie);

        // 5. Body 응답 (사용자 정보)
        UserDto.LoginRes loginRes = UserDto.LoginRes.builder()
                .employeeCode(employeeCode)
                .name(userDetails.getName())
                .role(userDetails.getRole())
                .build();

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(BaseResponse.success(loginRes)));
    }
}
