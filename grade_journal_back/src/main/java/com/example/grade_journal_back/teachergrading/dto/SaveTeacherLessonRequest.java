package com.example.grade_journal_back.teachergrading.dto;

import java.util.List;

public record SaveTeacherLessonRequest(
        List<TeacherLessonStudentUpdateDto> students
) {
}