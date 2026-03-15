package com.example.grade_journal_back.admin.dto;

import java.time.Instant;

public record AdminUserRowDto(
    Integer userId,
    String username,
    String fullName,
    String email,
    String role,
    boolean active,
    boolean approved,
    boolean profileCompleted,
    Instant createdAt,
    Integer studentId,
    String studentCard,
    Integer groupId,
    String groupCode,
    Short courseNo,
    String facultyName,
    String specializationName,
    Integer teacherId,
    Integer departmentId,
    String departmentCode,
    String departmentName,
    String position,
    String phone
) {
}