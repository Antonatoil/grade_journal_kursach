package com.example.grade_journal_back.admin.dto;

import java.time.Instant;

public record PendingRegistrationDto(
    Integer requestId,
    String desiredRole,
    String username,
    String fullName,
    String email,
    String status,
    Instant createdAt
) {
}