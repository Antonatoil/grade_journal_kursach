package com.example.grade_journal_back.analytics.dto;

public record TeacherOptionDto(
        Integer teacherId,
        String fullName,
        String departmentName,
        String position
) {
}