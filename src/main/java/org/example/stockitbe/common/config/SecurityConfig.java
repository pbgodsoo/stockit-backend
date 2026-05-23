package org.example.stockitbe.common.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.jwt.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

    private final AuthenticationConfiguration configuration;
    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;
    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        // CSRF 비활성화 (REST API + JWT 기반)
        http.csrf((csrf) -> csrf.disable());

        // Form 로그인 비활성화
        http.formLogin((form) -> form.disable());

        // Basic 인증 비활성화
        http.httpBasic((basic) -> basic.disable());

        // 세션을 STATELESS (JWT 사용)
        http.sessionManagement((session) ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 인가 규칙 정의
        http.authorizeHttpRequests((auth) -> auth
                // ERROR 디스패치는 항상 통과 — 내부 에러 페이지로의 재진입 시 인증 검사 제외.
                // (에러 핸들러가 401/403 응답을 직접 쓰도록 위에서 처리)
                .dispatcherTypeMatchers(
                        jakarta.servlet.DispatcherType.ASYNC,
                        jakarta.servlet.DispatcherType.ERROR
                ).permitAll()
                .requestMatchers("/api/user/signup", "/api/user/login", "/api/user/logout", "/api/user/refresh", "/api/public/**").permitAll()
                .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll() // k8s probe / 모니터링용 엔드포인트
                // Spring 기본 에러 디스패치 경로 — 인증 없이 도달 가능해야 핸들러가 정상 동작
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/hq/**").hasRole("HQ")                  // 본사
                .requestMatchers("/api/store/**").hasRole("STORE")            // 매장
                .requestMatchers("/api/warehouse/**").hasRole("WAREHOUSE")    // 물류창고
                .anyRequest().authenticated()
        );

        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, authEx) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json;charset=UTF-8");
                    res.setCharacterEncoding("UTF-8");
                    res.getWriter().write(
                            "{\"success\":false,\"code\":3002,\"message\":\"인증이 필요하거나 만료되었습니다.\"}"
                    );
                })
                // 인증은 되었으나 권한(ROLE) 불일치로 거부된 경우(403) — 메시지를 별도로 내려 구분
                .accessDeniedHandler((req, res, accessDeniedException) -> {
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.setContentType("application/json;charset=UTF-8");
                    res.setCharacterEncoding("UTF-8");
                    res.getWriter().write(
                            "{\"success\":false,\"code\":3008,\"message\":\"권한이 없는 요청입니다.\"}"
                    );
                })
        );

        // LoginFilter 등록 (커스텀 로그인 처리)
        AuthenticationManager authManager = configuration.getAuthenticationManager();
        LoginFilter loginFilter = new LoginFilter(authManager);
        loginFilter.setAuthenticationManager(authManager);
        loginFilter.setAuthenticationSuccessHandler(loginSuccessHandler);
        loginFilter.setAuthenticationFailureHandler(loginFailureHandler);

        http.addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
