package org.example.stockitbe.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.jwt.JwtUtil;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.user.model.AuthUserDetails;
import org.example.stockitbe.user.model.UserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String ACCESS_TOKEN_COOKIE = "Atoken";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        AuthUserDetails userDetails = (AuthUserDetails) authentication.getPrincipal();

        String token = jwtUtil.createToken(userDetails.getEmployeeCode(), userDetails.getRole());

        // HttpOnly Cookie 발급
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE, token);
        cookie.setHttpOnly(true);            // JS 접근 차단 (XSS 방지)
        cookie.setSecure(false);             // 운영(HTTPS): true 로 변경
        cookie.setPath("/");
        cookie.setMaxAge((int) (expirationMs / 1000));   // 초 단위
        cookie.setAttribute("SameSite", "Lax");          // CSRF 일부 방지
        response.addCookie(cookie);

        // Body는 사용자 정보만 (token 제외)
        UserDto.LoginRes loginRes = UserDto.LoginRes.builder()
                .employeeCode(userDetails.getEmployeeCode())
                .name(userDetails.getName())
                .role(userDetails.getRole())
                .build();

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(BaseResponse.success(loginRes)));
    }
}
