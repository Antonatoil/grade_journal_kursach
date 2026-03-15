package com.example.grade_journal_back.studentprofile.dto;

import java.util.List;

public record StudentProfileResponseDto(
    Integer userId,
    String username,
    String fullName,
    String email,
    boolean active,
    boolean approved,
    Integer studentId,
    String studentCard,
    String groupCode,
    Integer courseNo,
    String facultyName,
    String specializationName,
    List<StudentProfileCourseDto> subjects
) {
}