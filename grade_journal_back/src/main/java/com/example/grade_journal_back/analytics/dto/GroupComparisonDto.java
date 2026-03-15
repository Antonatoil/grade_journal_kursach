package com.example.grade_journal_back.analytics.dto;

import java.math.BigDecimal;

public record GroupComparisonDto(
        Integer groupId,
        String groupCode,
        Integer courseNo,
        Integer studentCount,
        BigDecimal averageGrade
) {
}