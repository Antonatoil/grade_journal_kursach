package com.example.grade_journal_back.schedule.dto;

import java.time.LocalDate;

public record ScheduleEntryDto(
    Integer scheduleId,
    LocalDate lessonDate,
    String timeSlot,
    Integer groupId,
    String groupCode,
    String courseName,
    String teacherFullName,
    String room,
    String lessonType,
    String topic
) {
}