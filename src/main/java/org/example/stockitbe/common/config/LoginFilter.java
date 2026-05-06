package org.example.stockitbe.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.user.model.UserDto;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;


import java.io.IOException;

public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public LoginFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
        setRequiresAuthenticationRequestMatcher(
                PathPatternRequestMatcher.withDefaults()
                        .matcher(HttpMethod.POST, "/api/user/login")
        );
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        try {
            UserDto.LoginReq loginReq = objectMapper.readValue(
                    request.getInputStream(), UserDto.LoginReq.class);

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            loginReq.getEmployeeCode(), loginReq.getPassword());

            return getAuthenticationManager().authenticate(authToken);
        } catch (IOException e) {
            throw new AuthenticationServiceException(
                    BaseResponseStatus.LOGIN_FAILED.getMessage(), e);
        }
    }
}
