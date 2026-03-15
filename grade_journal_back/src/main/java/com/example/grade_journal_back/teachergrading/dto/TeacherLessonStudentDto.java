package com.example.grade_journal_back.teachergrading.dto;

import java.math.BigDecimal;

public record TeacherLessonStudentDto(
        Integer studentId,
        Integer enrollmentId,
        String fullName,
        String studentCard,
        Boolean present,
        Integer missedHours,
        BigDecimal gradeValue,
        String teacherComment
) {
}