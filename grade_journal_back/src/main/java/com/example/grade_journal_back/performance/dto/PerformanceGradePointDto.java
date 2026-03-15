package com.example.grade_journal_back.performance.dto;

import java.time.LocalDate;

public record PerformanceGradePointDto(
    Integer gradeId,
    LocalDate gradedDate,
    Double gradeValue,
    String assessmentType,
    String assessmentTitle,
    Integer sequenceNo,
    boolean predicted
) {
}