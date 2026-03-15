package com.example.grade_journal_back.performance.dto;

import java.util.List;

public record PerformanceResponseDto(
    PerformanceSummaryDto summary,
    List<PerformanceGradePointDto> grades,
    List<PerformanceGradePointDto> chartPoints
) {
}