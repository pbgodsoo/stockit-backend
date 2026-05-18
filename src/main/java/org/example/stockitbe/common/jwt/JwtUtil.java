package org.example.stockitbe.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.stockitbe.user.model.entity.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey secretKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-ms}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    //  Access Token 발급 — employeeCode + role + type=access 클레임
    public String createAccessToken(String employeeCode, UserRole role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(employeeCode)
                .claim("role", role.name())
                .claim("type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessExpirationMs))
                .signWith(secretKey)
                .compact();
    }

    //  Refresh Token 발급 — employeeCode + type=refresh 클레임 (role 없음)
    public String createRefreshToken(String employeeCode) {
        Date now = new Date();
        return Jwts.builder()
                .subject(employeeCode)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpirationMs))
                .signWith(secretKey)
                .compact();
    }


    //  JWT 클레임 파싱
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    //  토큰 유효성 검증
    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public long getAccessExpirationMs() { return accessExpirationMs; }
    public long getRefreshExpirationMs() { return refreshExpirationMs; }
}


