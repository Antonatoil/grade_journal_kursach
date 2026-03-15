package com.example.grade_journal_back.performance.dto;

public record PerformanceCourseOptionDto(
    Integer courseId,
    String courseCode,
    String courseName,
    Integer studyYear
) {
}