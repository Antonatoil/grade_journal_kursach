package com.example.grade_journal_back.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FillTeacherProfileRequest(
    @NotNull(message = "Необходимо выбрать кафедру")
    Integer departmentId,

    @NotBlank(message = "Должность обязательна")
    @Size(max = 100, message = "Должность должна быть не длиннее 100 символов")
    String position,

    @Size(max = 30, message = "Телефон должен быть не длиннее 30 символов")
    String phone
) {
}