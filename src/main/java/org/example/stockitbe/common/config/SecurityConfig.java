package org.example.stockitbe.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {
    private final AuthenticationConfiguration configuration;
    private final LoginFilter loginFilter;

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        //  CSRF 비활성화 (REST API + JWT 기반)
        http.csrf((csrf) -> csrf.disable());

        //  Form 로그인/Basic 인증 비활성화
        http.formLogin((form) -> form.disable());
        http.httpBasic((basic) -> basic.disable());


        //  세션 STATELESS (JWT 사용 시)
        http.sessionManagement((session) ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));


        http.authorizeHttpRequests(
                (auth) -> auth
                        .requestMatchers("/api/user/**","/login").permitAll() // 모든 사용자 허용 페이지
                        .requestMatchers("/api/hq/**").hasRole("HQ")                // 본사 관리자
                        .requestMatchers("/api/store/**").hasRole("STORE")          //  매장 관리자
                        .requestMatchers("/api/warehouse/**").hasRole("WAREHOUSE")  //  창고 관리자
                        .anyRequest().authenticated()         // 위에서 명시하지 않은 나머지 모든 주소들은 로그인을 해야 접근할 수 이싿.
        );


        http.addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
