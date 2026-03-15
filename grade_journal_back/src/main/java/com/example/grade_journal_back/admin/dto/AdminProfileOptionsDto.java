package com.example.grade_journal_back.admin.dto;

import java.util.List;

public record AdminProfileOptionsDto(
    List<GroupOptionDto> groups,
    List<DepartmentOptionDto> departments
) {
    public record GroupOptionDto(
        Integer groupId,
        String groupCode,
        Short courseNo,
        Short admissionYear,
        String facultyName,
        String specializationName
    ) {
    }

    public record DepartmentOptionDto(
        Integer departmentId,
        String departmentCode,
        String departmentName
    ) {
    }
}