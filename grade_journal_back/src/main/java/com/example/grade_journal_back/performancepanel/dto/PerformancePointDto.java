package com.example.grade_journal_back.performancepanel.dto;

public record PerformancePointDto(
        Integer orderNo,
        Integer value,
        String gradeType,
        String gradedAt
) {
}