package com.example.grade_journal_back.schedule.dto;

public record ScheduleTeacherOptionDto(
    Integer teacherId,
    String fullName,
    String departmentName,
    String position
) {
}