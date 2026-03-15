package com.example.grade_journal_back.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
    @NotBlank(message = "Логин обязателен")
    @Size(min = 3, max = 50, message = "Логин должен быть длиной от 3 до 50 символов")
    String username,

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, max = 100, message = "Пароль должен быть длиной от 6 до 100 символов")
    String password,

    @NotBlank(message = "ФИО обязательно")
    @Size(min = 5, max = 150, message = "ФИО должно быть длиной от 5 до 150 символов")
    String fullName,

    @Email(message = "Некорректный email")
    String email,

    @NotBlank(message = "Роль обязательна")
    @Pattern(regexp = "teacher|student", message = "Допустимые роли: teacher или student")
    String desiredRole
) {
}