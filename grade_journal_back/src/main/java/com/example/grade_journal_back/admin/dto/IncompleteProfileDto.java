package com.example.grade_journal_back.admin.dto;

import java.time.Instant;

public record IncompleteProfileDto(
    Integer userId,
    String username,
    String fullName,
    String email,
    String role,
    boolean approved,
    Instant createdAt
) {
}