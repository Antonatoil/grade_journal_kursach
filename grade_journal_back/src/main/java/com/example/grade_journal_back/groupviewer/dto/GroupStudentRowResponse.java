package com.example.grade_journal_back.groupviewer.dto;

public record GroupStudentRowResponse(
        Integer studentId,
        String username,
        String fullName,
        String email,
        String studentCard,
        String groupCode,
        Integer courseNo,
        Boolean active,
        Boolean approved
) {
}