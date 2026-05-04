package org.example.stockitbe.common.config;

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

        //Basic 인증 비활성화
        http.httpBasic((basic) -> basic.disable());

        // 세션 STATELESS (JWT 사용)
        http.sessionManagement((session) ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 권한별 접근 제어
        http.authorizeHttpRequests((auth) -> auth
                .requestMatchers("/api/user/signup", "/api/user/login", "/api/user/logout").permitAll()        // 회원가입/로그인
                .requestMatchers("/api/hq/**").hasRole("HQ")                  // 본사
                .requestMatchers("/api/store/**").hasRole("STORE")            // 매장
                .requestMatchers("/api/warehouse/**").hasRole("WAREHOUSE")    // 창고
                .anyRequest().authenticated()
        );

        // LoginFilter 등록 (직접 인스턴스화)
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
