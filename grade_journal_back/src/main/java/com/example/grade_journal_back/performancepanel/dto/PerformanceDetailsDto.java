package com.example.grade_journal_back.performancepanel.dto;

import java.util.List;

public record PerformanceDetailsDto(
        String studentName,
        String groupCode,
        String courseName,
        Double averageAllCourses,
        Double averageSelectedCourse,
        Integer predictedFinalGrade,
        String recommendationSummary,
        List<String> recommendations,
        List<PerformancePointDto> points,
        Integer gradeCount,
        Double attendanceRate,
        Integer missedHours,
        String lastThreeGrades,
        String trend,
        String modelVersion
) {
}