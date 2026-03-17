package com.example.grade_journal_back.auth.service;

import com.example.grade_journal_back.auth.JwtService;
import com.example.grade_journal_back.auth.dto.LoginResponse;
import com.example.grade_journal_back.common.exception.BadRequestException;
import com.example.grade_journal_back.user.entity.RefreshToken;
import com.example.grade_journal_back.user.entity.UserAccount;
import com.example.grade_journal_back.user.repository.RefreshTokenRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${app.jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    @Transactional
    public String createRefreshToken(UserAccount userAccount) {
        log.info(
                "Creating refresh token for userId={}, username='{}'",
                userAccount.getUserAccountId(),
                userAccount.getUsername()
        );

        RefreshToken refreshToken = RefreshToken.builder()
                .userAccount(userAccount)
                .tokenValue(generateTokenValue())
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS))
                .build();

        refreshTokenRepository.save(refreshToken);

        log.info(
                "Refresh token created successfully for userId={}, expiresAt={}",
                userAccount.getUserAccountId(),
                refreshToken.getExpiresAt()
        );

        return refreshToken.getTokenValue();
    }

    @Transactional
    public void revokeAllActiveTokens(Integer userId) {
        log.info("Revoking all active refresh tokens for userId={}", userId);

        refreshTokenRepository.findAllByUserAccountUserAccountIdAndRevokedAtIsNull(userId)
                .forEach(token -> token.setRevokedAt(Instant.now()));

        log.info("All active refresh tokens revoked for userId={}", userId);
    }

    @Transactional
    public LoginResponse refresh(String refreshTokenValue) {
        log.info("Processing refresh token request");

        RefreshToken storedToken = refreshTokenRepository.findByTokenValue(refreshTokenValue)
                .orElseThrow(() -> {
                    log.warn("Refresh token request failed: token not found");
                    return new BadRequestException("Refresh token не найден");
                });

        if (storedToken.getRevokedAt() != null) {
            log.warn(
                    "Refresh token request failed: token already revoked for userId={}",
                    storedToken.getUserAccount().getUserAccountId()
            );
            throw new BadRequestException("Refresh token уже отозван");
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            log.warn(
                    "Refresh token request failed: token expired for userId={}, expiresAt={}",
                    storedToken.getUserAccount().getUserAccountId(),
                    storedToken.getExpiresAt()
            );
            storedToken.setRevokedAt(Instant.now());
            throw new BadRequestException("Refresh token истек");
        }

        UserAccount user = storedToken.getUserAccount();

        if (!user.isActive() || !user.isApproved()) {
            log.warn(
                    "Refresh token request failed: userId={} is inactive or not approved",
                    user.getUserAccountId()
            );
            throw new BadRequestException("Пользователь неактивен или не одобрен");
        }

        storedToken.setRevokedAt(Instant.now());

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = createRefreshToken(user);

        log.info(
                "Refresh token request processed successfully for userId={}, username='{}'",
                user.getUserAccountId(),
                user.getUsername()
        );

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
        log.info("Processing refresh token revoke request");

        refreshTokenRepository.findByTokenValue(refreshTokenValue)
                .ifPresent(token -> {
                    token.setRevokedAt(Instant.now());
                    log.info(
                            "Refresh token revoked successfully for userId={}",
                            token.getUserAccount().getUserAccountId()
                    );
                });
    }

    private String generateTokenValue() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}