package com.example.grade_journal_back.performance.dto;

public record PerformanceStudentOptionDto(
    Integer studentId,
    String fullName,
    String groupCode,
    Integer courseNo
) {
}