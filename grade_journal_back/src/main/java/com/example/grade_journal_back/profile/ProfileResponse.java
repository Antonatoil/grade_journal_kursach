package com.example.grade_journal_back.profile;

public record ProfileResponse(
    Integer userId,
    String username,
    String fullName,
    String email,
    String role,
    boolean active,
    boolean approved,
    StudentInfo student,
    TeacherInfo teacher
) {
    public record StudentInfo(
        Integer studentId,
        String studentCard,
        String groupCode,
        Short courseNo,
        String facultyName,
        String specializationName
    ) {
    }

    public record TeacherInfo(
        Integer teacherId,
        String departmentCode,
        String departmentName,
        String position,
        String phone
    ) {
    }
}