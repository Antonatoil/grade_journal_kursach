package com.example.grade_journal_back.groupviewer.dto;

public record GroupOptionResponse(
        Integer groupId,
        String groupCode,
        Integer courseNo,
        String facultyName,
        String specializationName
) {
}