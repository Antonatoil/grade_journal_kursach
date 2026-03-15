package com.example.grade_journal_back.common.api;

import java.time.Instant;
import java.util.List;

public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message,
    List<String> details
) {
}