package com.example.grade_journal_back.performance.dto;

import java.util.List;

public record PerformanceMetaResponse(
    List<PerformanceStudentOptionDto> students,
    List<PerformanceCourseOptionDto> courses
) {
}