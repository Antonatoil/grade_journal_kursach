package com.example.grade_journal_back.integration.oauth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth/oauth2")
public class OAuth2StartController {

    @GetMapping("/github/start")
    public void startGithubLogin(
            @RequestParam(defaultValue = "student") String role,
            HttpServletResponse response
    ) throws IOException {
        String safeRole = "teacher".equalsIgnoreCase(role) ? "teacher" : "student";

        Cookie roleCookie = new Cookie("oauth2_requested_role", safeRole);
        roleCookie.setHttpOnly(true);
        roleCookie.setPath("/");
        roleCookie.setMaxAge(300);
        response.addCookie(roleCookie);

        response.sendRedirect("/oauth2/authorization/github");
    }
}