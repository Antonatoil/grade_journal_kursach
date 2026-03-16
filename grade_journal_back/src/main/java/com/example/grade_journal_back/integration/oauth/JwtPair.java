package com.example.grade_journal_back.integration.oauth;

public record JwtPair(
        String accessToken,
        String refreshToken
) {
}