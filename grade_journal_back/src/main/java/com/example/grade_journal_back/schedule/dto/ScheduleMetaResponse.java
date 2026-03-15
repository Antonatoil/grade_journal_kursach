package com.example.grade_journal_back.schedule.dto;

import java.util.List;

public record ScheduleMetaResponse(
    List<ScheduleGroupOptionDto> groups,
    List<ScheduleTeacherOptionDto> teachers,
    List<ScheduleCourseOptionDto> courses,
    List<String> timeSlots,
    List<String> lessonTypes
) {
}