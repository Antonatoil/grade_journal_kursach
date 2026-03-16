package com.example.grade_journal_back.integration.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class RefreshTokenJdbcStore {

    private final JdbcTemplate jdbcTemplate;
    private final int refreshTokenDays;

    public RefreshTokenJdbcStore(
            JdbcTemplate jdbcTemplate,
            @Value("${app.jwt.refresh-expiration-days:${jwt.refresh-expiration-days:30}}") int refreshTokenDays
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.refreshTokenDays = refreshTokenDays;
    }

    public String create(Integer userAccountId) {
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");

        jdbcTemplate.update(
                """
                update refresh_token
                set revoked_at = now()
                where user_account_id = ?
                  and revoked_at is null
                """,
                userAccountId
        );

        jdbcTemplate.update(
                """
                insert into refresh_token (user_account_id, token_value, expires_at, created_at, revoked_at)
                values (?, ?, ?, now(), null)
                """,
                userAccountId,
                token,
                OffsetDateTime.now().plusDays(refreshTokenDays)
        );

        return token;
    }
}