package com.example.grade_journal_back.integration.oauth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

@Service
public class SocialLoginService {

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Random random = new Random();

    public SocialLoginService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SocialLoginResult processGithubLogin(String providerUserId, String email, String fullName, String requestedRole) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        String safeRole = "teacher".equalsIgnoreCase(requestedRole) ? "teacher" : "student";

        Optional<UserRow> linkedUser = findByOAuth("github", providerUserId);
        if (linkedUser.isPresent()) {
            return buildResultFromUser(linkedUser.get(), normalizedEmail, fullName);
        }

        Optional<UserRow> emailUser = findByEmail(normalizedEmail);
        if (emailUser.isPresent()) {
            UserRow user = emailUser.get();
            linkOAuth(user.userAccountId(), providerUserId, normalizedEmail);
            return buildResultFromUser(user, normalizedEmail, fullName);
        }

        if (hasPendingRequest(normalizedEmail)) {
            return new SocialLoginResult(
                    "pending",
                    null,
                    null,
                    safeRole,
                    normalizedEmail,
                    fullName,
                    "Заявка уже создана и ожидает решения администратора."
            );
        }

        String username = generateUniqueUsername(normalizedEmail, fullName);
        String passwordHash = passwordEncoder.encode("oauth2-github-" + providerUserId);

        jdbcTemplate.update(
                """
                insert into registration_request
                    (desired_role, username, password_hash, full_name, email, status, created_at)
                values (?, ?, ?, ?, ?, 'pending', now())
                """,
                safeRole,
                username,
                passwordHash,
                fullName,
                normalizedEmail
        );

        return new SocialLoginResult(
                "pending",
                null,
                username,
                safeRole,
                normalizedEmail,
                fullName,
                "Заявка через GitHub создана и отправлена администратору."
        );
    }

    private SocialLoginResult buildResultFromUser(UserRow user, String email, String fullName) {
        if (!user.isApproved()) {
            return new SocialLoginResult(
                    "pending",
                    user.userAccountId(),
                    user.username(),
                    user.roleCode(),
                    email,
                    fullName,
                    "Аккаунт найден, но еще не одобрен администратором."
            );
        }

        if (!user.isActive()) {
            return new SocialLoginResult(
                    "inactive",
                    user.userAccountId(),
                    user.username(),
                    user.roleCode(),
                    email,
                    fullName,
                    "Аккаунт найден, но он отключен."
            );
        }

        return new SocialLoginResult(
                "approved",
                user.userAccountId(),
                user.username(),
                user.roleCode(),
                email,
                fullName,
                "Вход через GitHub выполнен успешно."
        );
    }

    private Optional<UserRow> findByOAuth(String provider, String providerUserId) {
        List<UserRow> rows = jdbcTemplate.query(
                """
                select ua.user_account_id,
                       ua.username,
                       r.role_code,
                       ua.is_approved,
                       ua.is_active
                from oauth_account oa
                join user_account ua on ua.user_account_id = oa.user_account_id
                join role r on r.role_id = ua.role_id
                where oa.provider = ?
                  and oa.provider_user_id = ?
                """,
                (rs, rowNum) -> new UserRow(
                        rs.getInt("user_account_id"),
                        rs.getString("username"),
                        rs.getString("role_code"),
                        rs.getBoolean("is_approved"),
                        rs.getBoolean("is_active")
                ),
                provider,
                providerUserId
        );

        return rows.stream().findFirst();
    }

    private Optional<UserRow> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        List<UserRow> rows = jdbcTemplate.query(
                """
                select ua.user_account_id,
                       ua.username,
                       r.role_code,
                       ua.is_approved,
                       ua.is_active
                from user_account ua
                join role r on r.role_id = ua.role_id
                where lower(ua.email) = lower(?)
                """,
                (rs, rowNum) -> new UserRow(
                        rs.getInt("user_account_id"),
                        rs.getString("username"),
                        rs.getString("role_code"),
                        rs.getBoolean("is_approved"),
                        rs.getBoolean("is_active")
                ),
                email
        );

        return rows.stream().findFirst();
    }

    private void linkOAuth(Integer userAccountId, String providerUserId, String email) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from oauth_account
                where provider = 'github'
                  and provider_user_id = ?
                """,
                Integer.class,
                providerUserId
        );

        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update(
                """
                insert into oauth_account (user_account_id, provider, provider_user_id, provider_email, created_at)
                values (?, 'github', ?, ?, now())
                on conflict do nothing
                """,
                userAccountId,
                providerUserId,
                email
        );
    }

    private boolean hasPendingRequest(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from registration_request
                where lower(email) = lower(?)
                  and status = 'pending'
                """,
                Integer.class,
                email
        );

        return count != null && count > 0;
    }

    private String generateUniqueUsername(String email, String fullName) {
        String base = Optional.ofNullable(email)
                .filter(it -> !it.isBlank() && it.contains("@"))
                .map(it -> it.substring(0, it.indexOf("@")))
                .orElseGet(() -> Optional.ofNullable(fullName)
                        .filter(it -> !it.isBlank())
                        .map(this::slugify)
                        .orElse("github_user"));

        base = slugify(base);
        if (base.isBlank()) {
            base = "github_user";
        }

        String candidate = base;
        while (usernameExists(candidate)) {
            candidate = base + "_" + (1000 + random.nextInt(9000));
        }

        return candidate;
    }

    private boolean usernameExists(String username) {
        Integer userCount = jdbcTemplate.queryForObject(
                "select count(*) from user_account where username = ?",
                Integer.class,
                username
        );

        Integer requestCount = jdbcTemplate.queryForObject(
                "select count(*) from registration_request where username = ?",
                Integer.class,
                username
        );

        return (userCount != null && userCount > 0) || (requestCount != null && requestCount > 0);
    }

    private String slugify(String value) {
        return Objects.requireNonNullElse(value, "")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private record UserRow(
            Integer userAccountId,
            String username,
            String roleCode,
            boolean isApproved,
            boolean isActive
    ) {
    }
}