package com.example.grade_journal_back.auth.service;

import com.example.grade_journal_back.auth.JwtService;
import com.example.grade_journal_back.auth.dto.LoginResponse;
import com.example.grade_journal_back.common.exception.BadRequestException;
import com.example.grade_journal_back.user.entity.RefreshToken;
import com.example.grade_journal_back.user.entity.UserAccount;
import com.example.grade_journal_back.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${app.jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    @Transactional
    public String createRefreshToken(UserAccount userAccount) {
        RefreshToken refreshToken = RefreshToken.builder()
            .userAccount(userAccount)
            .tokenValue(generateTokenValue())
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS))
            .build();

        refreshTokenRepository.save(refreshToken);
        return refreshToken.getTokenValue();
    }

    @Transactional
    public void revokeAllActiveTokens(Integer userId) {
        refreshTokenRepository.findAllByUserAccountUserAccountIdAndRevokedAtIsNull(userId)
            .forEach(token -> token.setRevokedAt(Instant.now()));
    }

    @Transactional
    public LoginResponse refresh(String refreshTokenValue) {
        RefreshToken storedToken = refreshTokenRepository.findByTokenValue(refreshTokenValue)
            .orElseThrow(() -> new BadRequestException("Refresh token не найден"));

        if (storedToken.getRevokedAt() != null) {
            throw new BadRequestException("Refresh token уже отозван");
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            storedToken.setRevokedAt(Instant.now());
            throw new BadRequestException("Refresh token истек");
        }

        UserAccount user = storedToken.getUserAccount();

        if (!user.isActive() || !user.isApproved()) {
            throw new BadRequestException("Пользователь неактивен или не одобрен");
        }

        storedToken.setRevokedAt(Instant.now());

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = createRefreshToken(user);

        return new LoginResponse(
            user.getUserAccountId(),
            user.getUsername(),
            user.getFullName(),
            user.getRole().getRoleCode(),
            newAccessToken,
            newRefreshToken,
            "Bearer"
        );
    }

    @Transactional
    public void revoke(String refreshTokenValue) {
        refreshTokenRepository.findByTokenValue(refreshTokenValue)
            .ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    private String generateTokenValue() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}