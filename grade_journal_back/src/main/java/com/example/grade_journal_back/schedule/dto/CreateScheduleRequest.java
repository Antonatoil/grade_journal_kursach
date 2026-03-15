package com.example.grade_journal_back.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateScheduleRequest(
    @NotNull(message = "Необходимо выбрать группу")
    Integer groupId,

    @NotNull(message = "Необходимо выбрать преподавателя")
    Integer teacherId,

    @NotNull(message = "Необходимо выбрать дисциплину")
    Integer courseId,

    @NotNull(message = "Необходимо выбрать дату")
    LocalDate lessonDate,

    @NotBlank(message = "Необходимо выбрать время")
    String timeSlot,

    @NotBlank(message = "Необходимо выбрать тип занятия")
    String lessonType,

    String room,
    String topic
) {
}