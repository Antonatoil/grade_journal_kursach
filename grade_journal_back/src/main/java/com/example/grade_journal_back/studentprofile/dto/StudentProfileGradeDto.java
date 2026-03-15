package com.example.grade_journal_back.studentprofile.dto;

import java.time.LocalDate;

public record StudentProfileGradeDto(
    LocalDate gradedDate,
    Double gradeValue,
    String assessmentType,
    String assessmentTitle
) {
}