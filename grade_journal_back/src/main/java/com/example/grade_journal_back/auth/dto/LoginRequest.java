package com.example.grade_journal_back.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Логин обязателен")
    String username,

    @NotBlank(message = "Пароль обязателен")
    String password
) {
}