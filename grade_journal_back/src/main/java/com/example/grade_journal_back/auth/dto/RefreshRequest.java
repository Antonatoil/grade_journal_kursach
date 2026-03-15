package com.example.grade_journal_back.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
    @NotBlank(message = "Refresh token обязателен")
    String refreshToken
) {
}