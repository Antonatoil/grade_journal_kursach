package com.example.grade_journal_back.teachergrading.dto;

import java.math.BigDecimal;

public record TeacherLessonStudentUpdateDto(
        Integer studentId,
        Integer enrollmentId,
        Boolean present,
        BigDecimal gradeValue,
        String teacherComment
) {
}