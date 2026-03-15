package com.example.grade_journal_back.analytics.dto;

import java.math.BigDecimal;

public record StudentComparisonDto(
        Integer studentId,
        String fullName,
        String groupCode,
        String studentCard,
        BigDecimal averageGrade
) {
}