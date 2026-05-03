package org.example.stockitbe.common.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;


//@Component
public class LoginFilter extends UsernamePasswordAuthenticationFilter {
    private final AuthenticationManager authenticationManager;

    public LoginFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
        this.authenticationManager = authenticationManager;
    }





    // 1번
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        System.out.println("내 필터가 실행되는데요??");

        String email = "test01@test.com";
        String password = "qwer1234";
        // 2번
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(email, password, null);

        // 3번
        authenticationManager.authenticate(token);

        return super.attemptAuthentication(request, response);
    }

}
