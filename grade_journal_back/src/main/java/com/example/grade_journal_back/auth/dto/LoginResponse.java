package com.example.grade_journal_back.auth.dto;

public record LoginResponse(
    Integer userId,
    String username,
    String fullName,
    String role,
    String accessToken,
    String refreshToken,
    String tokenType
) {
}