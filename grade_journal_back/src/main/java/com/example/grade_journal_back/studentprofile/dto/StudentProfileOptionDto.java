package com.example.grade_journal_back.studentprofile.dto;

public record StudentProfileOptionDto(
    Integer studentId,
    String fullName,
    String groupCode,
    Integer courseNo
) {
}