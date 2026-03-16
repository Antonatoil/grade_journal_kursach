package com.example.grade_journal_back.integration.oauth;

public record SocialLoginResult(
        String status,
        Integer userAccountId,
        String username,
        String roleCode,
        String email,
        String fullName,
        String message
) {
}