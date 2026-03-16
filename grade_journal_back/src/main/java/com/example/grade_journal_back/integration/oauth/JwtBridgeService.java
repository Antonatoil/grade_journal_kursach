package com.example.grade_journal_back.integration.oauth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class JwtBridgeService {

    private final String secret;
    private final long accessMinutes;

    public JwtBridgeService(
            @Value("${app.jwt.secret:${jwt.secret:change-this-secret-to-at-least-32-characters}}") String secret,
            @Value("${app.jwt.access-expiration-minutes:${jwt.access-expiration-minutes:30}}") long accessMinutes
    ) {
        this.secret = secret;
        this.accessMinutes = accessMinutes;
    }

    public String createAccessToken(String username, String roleCode) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(username)
                .claim("role", roleCode)
                .claim("roles", List.of(roleCode))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessMinutes * 60)))
                .signWith(secretKey())
                .compact();
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}