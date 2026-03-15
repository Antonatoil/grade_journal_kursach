package com.example.grade_journal_back.schedule.dto;

public record ScheduleCourseOptionDto(
    Integer courseId,
    String courseCode,
    String courseName,
    Integer studyYear,
    String controlForm
) {
}