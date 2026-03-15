package com.example.grade_journal_back.studentprofile.dto;

import java.util.List;

public record StudentProfileCourseDto(
    Integer courseId,
    String courseCode,
    String courseName,
    Double averageGrade,
    Double predictedGrade,
    Double attendanceRate,
    Integer missedHours,
    String riskLevel,
    List<String> recommendations,
    List<StudentProfileGradeDto> grades
) {
}