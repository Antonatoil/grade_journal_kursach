package com.example.grade_journal_back.teachergrading.dto;

import java.time.LocalDate;

public record TeacherLessonOptionDto(
        Integer scheduleId,
        LocalDate lessonDate,
        String timeSlot,
        String courseName,
        String groupCode,
        String topic
) {
}