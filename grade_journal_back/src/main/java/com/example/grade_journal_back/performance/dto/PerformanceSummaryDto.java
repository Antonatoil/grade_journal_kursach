package com.example.grade_journal_back.performance.dto;

public record PerformanceSummaryDto(
    Integer studentId,
    String studentFullName,
    String groupCode,
    Integer courseId,
    String courseName,
    Double averageAllSubjects,
    Double averageSelectedSubject,
    Double predictedNextGrade
) {
}