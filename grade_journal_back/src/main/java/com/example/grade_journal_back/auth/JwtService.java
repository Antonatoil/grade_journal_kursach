package com.example.grade_journal_back.auth;

import com.example.grade_journal_back.user.entity.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-expiration-minutes}")
    private long accessExpirationMinutes;

    public String generateAccessToken(UserAccount userAccount) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", userAccount.getRole().getRoleCode());
        claims.put("fullName", userAccount.getFullName());

        Instant now = Instant.now();

        return Jwts.builder()
            .claims(claims)
            .subject(userAccount.getUsername())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(accessExpirationMinutes, ChronoUnit.MINUTES)))
            .signWith(getSigningKey())
            .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserAccount userAccount) {
        String username = extractUsername(token);
        return username.equals(userAccount.getUsername()) && !isTokenExpired(token);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}