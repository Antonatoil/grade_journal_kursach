package com.example.grade_journal_back.integration.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

@Component
public class GithubOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final SocialLoginService socialLoginService;
    private final JwtBridgeService jwtBridgeService;
    private final RefreshTokenJdbcStore refreshTokenJdbcStore;
    private final String frontendBaseUrl;

    public GithubOAuth2SuccessHandler(
            SocialLoginService socialLoginService,
            JwtBridgeService jwtBridgeService,
            RefreshTokenJdbcStore refreshTokenJdbcStore,
            @Value("${app.oauth2.frontend-base-url:http://localhost:5173}") String frontendBaseUrl
    ) {
        this.socialLoginService = socialLoginService;
        this.jwtBridgeService = jwtBridgeService;
        this.refreshTokenJdbcStore = refreshTokenJdbcStore;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User user = (OAuth2User) authentication.getPrincipal();

        String requestedRole = resolveRequestedRole(request);
        String githubId = valueAsString(user.getAttributes().get("id"));
        String email = valueAsString(user.getAttributes().get("email"));
        String fullName = firstNonBlank(
                valueAsString(user.getAttributes().get("name")),
                valueAsString(user.getAttributes().get("login")),
                "GitHub User"
        );

        SocialLoginResult result = socialLoginService.processGithubLogin(
                githubId,
                email,
                fullName,
                requestedRole
        );

        clearRequestedRoleCookie(response);

        String redirectUrl = buildRedirectUrl(result);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String resolveRequestedRole(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return "student";
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> "oauth2_requested_role".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .filter(value -> "teacher".equalsIgnoreCase(value) || "student".equalsIgnoreCase(value))
                .orElse("student");
    }

    private void clearRequestedRoleCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("oauth2_requested_role", "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String buildRedirectUrl(SocialLoginResult result) {
        String base = frontendBaseUrl.endsWith("/") ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1) : frontendBaseUrl;

        if ("approved".equals(result.status())) {
            String accessToken = jwtBridgeService.createAccessToken(result.username(), result.roleCode());
            String refreshToken = refreshTokenJdbcStore.create(result.userAccountId());

            return base + "/?oauth2_status=approved"
                    + "&oauth2_access=" + encode(accessToken)
                    + "&oauth2_refresh=" + encode(refreshToken)
                    + "&oauth2_role=" + encode(result.roleCode())
                    + "&oauth2_message=" + encode(result.message());
        }

        return base + "/?oauth2_status=" + encode(result.status())
                + "&oauth2_email=" + encode(result.email() == null ? "" : result.email())
                + "&oauth2_message=" + encode(result.message());
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String valueAsString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String a, String b, String fallback) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return fallback;
    }
}