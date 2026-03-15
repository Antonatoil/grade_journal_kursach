package com.example.grade_journal_back.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUserUpsertRequest(
    @NotBlank(message = "Логин обязателен")
    @Size(max = 50, message = "Логин должен быть не длиннее 50 символов")
    String username,

    @Size(max = 100, message = "Пароль должен быть не длиннее 100 символов")
    String password,

    @NotBlank(message = "ФИО обязательно")
    @Size(max = 150, message = "ФИО должно быть не длиннее 150 символов")
    String fullName,

    @Size(max = 150, message = "Email должен быть не длиннее 150 символов")
    String email,

    @NotBlank(message = "Роль обязательна")
    String role,

    @NotNull(message = "Флаг активности обязателен")
    Boolean active,

    @NotNull(message = "Флаг одобрения обязателен")
    Boolean approved,

    Integer groupId,
    String studentCard,
    Integer departmentId,
    String position,
    String phone
) {
}