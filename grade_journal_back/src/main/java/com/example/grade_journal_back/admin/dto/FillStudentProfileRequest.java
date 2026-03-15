package com.example.grade_journal_back.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FillStudentProfileRequest(
    @NotNull(message = "Необходимо выбрать группу")
    Integer groupId,

    @NotBlank(message = "Студенческий билет обязателен")
    @Size(max = 20, message = "Студенческий билет должен быть не длиннее 20 символов")
    String studentCard
) {
}