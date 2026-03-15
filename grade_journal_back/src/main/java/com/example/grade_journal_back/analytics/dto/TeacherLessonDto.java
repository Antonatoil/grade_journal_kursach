package com.example.grade_journal_back.analytics.dto;

import java.time.LocalDate;

public record TeacherLessonDto(
        LocalDate lessonDate,
        String timeSlot,
        String courseName,
        String groupCode,
        String room,
        String lessonType,
        String topic
) {
}