package com.example.grade_journal_back.report.dto;

public record ExportReportRequest(
        boolean students,
        boolean teachers,
        boolean courses,
        boolean schedule,
        boolean performance,
        boolean attendance
) {

    public boolean hasAnySelection() {
        return students || teachers || courses || schedule || performance || attendance;
    }
}