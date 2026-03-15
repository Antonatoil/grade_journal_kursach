package com.example.grade_journal_back.admin.dto;

public record AdminActionResponse(
    Integer requestId,
    String status,
    String message
) {
}