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

        // HTTP Basic 인증 비활성화
        http.httpBasic((basic) -> basic.disable());

        // 세션 정책 STATELESS (JWT 사용)
        http.sessionManagement((session) ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 요청 경로별 인가 설정
        http.authorizeHttpRequests((auth) -> auth
                // ERROR/ASYNC 디스패처는 허용
                // (필터 체인에서 예외가 발생해도 /error 진입 시 재차 인증 실패가 나지 않도록 허용)
                .dispatcherTypeMatchers(
                        jakarta.servlet.DispatcherType.ASYNC,
                        jakarta.servlet.DispatcherType.ERROR
                ).permitAll()
                .requestMatchers("/api/user/signup", "/api/user/login", "/api/user/logout", "/api/user/refresh", "/api/public/**").permitAll() // 인증 없이 허용
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll() // k8s probe / 모니터링
                // Spring 기본 에러 엔드포인트 허용
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/hq/**").hasRole("HQ")
                .requestMatchers("/api/store/**").hasRole("STORE")
                .requestMatchers("/api/warehouse/**").hasRole("WAREHOUSE")
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
                // 인증은 되었으나 권한(ROLE)이 없는 경우 403 반환
                .accessDeniedHandler((req, res, accessDeniedException) -> {
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.setContentType("application/json;charset=UTF-8");
                    res.setCharacterEncoding("UTF-8");
                    res.getWriter().write(
                            "{\"success\":false,\"code\":3008,\"message\":\"권한이 없는 요청입니다.\"}"
                    );
                })
        );

        // LoginFilter 등록 (로그인 처리)
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
