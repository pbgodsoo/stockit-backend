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
        // CSRF ??쑵??源딆넅 (REST API + JWT 疫꿸퀡而?
        http.csrf((csrf) -> csrf.disable());

        // Form 嚥≪뮄?????쑵??源딆넅
        http.formLogin((form) -> form.disable());

        //Basic ?紐꾩쵄 ??쑵??源딆넅
        http.httpBasic((basic) -> basic.disable());

        // ?紐꾨?STATELESS (JWT ????
        http.sessionManagement((session) ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 亦낅슦釉녘퉪??臾롫젏 ??뽯선
        http.authorizeHttpRequests((auth) -> auth
                // ERROR ?遺용뮞??ν뒄???뚢뫂?껅에?살쑎 ??됱뇚 筌ｌ꼶???⑥눘??癒?퐣 ??彛??뉖릭沃샕嚥??紐꾩쵄 筌ｋ똾寃?癒?퐣 ??뽰뇚??뺣뼄.
                // (??됱뇚 ?癒?뵥??401嚥???肉???쇱젫 ??쑴已??됰뮞 ?癒?쑎揶쎛 揶쎛??????얜챷??獄쎻뫗?)
                .dispatcherTypeMatchers(
                        jakarta.servlet.DispatcherType.ASYNC,
                        jakarta.servlet.DispatcherType.ERROR
                ).permitAll()
                .requestMatchers("/api/user/signup", "/api/user/login", "/api/user/logout", "/api/user/refresh", "/api/public/**").permitAll()        // ???뜚揶쎛??嚥≪뮄???                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll() // k8s probe/筌롫??껆뵳???륁춿 ??됱뒠
                // Spring 疫꿸퀡???癒?쑎 ?遺얜굡????紐껊뮉 ??? ??됱뇚 ???쐭筌?野껋럥以???嚥??紐꾩쵄 ??됱뇚????筌왖 ??꾩쓺 ??됱뒠??뺣뼄.
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/hq/**").hasRole("HQ")                  // 癰귣챷沅?                .requestMatchers("/api/store/**").hasRole("STORE")            // 筌띲끉??                .requestMatchers("/api/warehouse/**").hasRole("WAREHOUSE")    // 筌≪럡??                .anyRequest().authenticated()
        );

        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, authEx) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json;charset=UTF-8");
                    res.setCharacterEncoding("UTF-8");
                    res.getWriter().write(
                            "{\"success\":false,\"code\":3002,\"message\":\"?紐꾩쵄???袁⑹뒄??몃빍??\"}"
                    );
                })
                // ?紐꾩쵄?? ??뤿?筌왖筌?亦낅슦釉?ROLE) ?봔鈺곌퉮??野껋럩??몴?403??곗쨮 ?브쑬????癒?뵥 ???툢??揶쎛?館釉?袁⑥쨯 ??뺣뼄.
                .accessDeniedHandler((req, res, accessDeniedException) -> {
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.setContentType("application/json;charset=UTF-8");
                    res.setCharacterEncoding("UTF-8");
                    res.getWriter().write(
                            "{\"success\":false,\"code\":3008,\"message\":\"亦낅슦釉????곷뮸??덈뼄.\"}"
                    );
                })
        );

        // LoginFilter ?源낆쨯 (筌욊낯???紐꾨뮞??곷뮞??
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
