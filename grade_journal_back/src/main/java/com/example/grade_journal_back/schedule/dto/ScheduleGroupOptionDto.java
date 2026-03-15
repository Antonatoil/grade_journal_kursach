package com.example.grade_journal_back.schedule.dto;

public record ScheduleGroupOptionDto(
    Integer groupId,
    String groupCode,
    Integer courseNo,
    Integer admissionYear,
    String facultyName,
    String specializationName
) {
}