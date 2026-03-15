package com.example.grade_journal_back.teachergrading.web;

import com.example.grade_journal_back.teachergrading.dto.SaveTeacherLessonRequest;
import com.example.grade_journal_back.teachergrading.dto.TeacherGroupOptionDto;
import com.example.grade_journal_back.teachergrading.dto.TeacherLessonOptionDto;
import com.example.grade_journal_back.teachergrading.dto.TeacherLessonStudentDto;
import com.example.grade_journal_back.teachergrading.service.TeacherGradingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teacher-grading")
public class TeacherGradingController {

    private final TeacherGradingService teacherGradingService;

    public TeacherGradingController(TeacherGradingService teacherGradingService) {
        this.teacherGradingService = teacherGradingService;
    }

    @GetMapping("/groups")
    public List<TeacherGroupOptionDto> getGroups() {
        return teacherGradingService.getMyGroups();
    }

    @GetMapping("/lessons")
    public List<TeacherLessonOptionDto> getLessons(@RequestParam Integer groupId) {
        return teacherGradingService.getMyLessons(groupId);
    }

    @GetMapping("/lesson-students")
    public List<TeacherLessonStudentDto> getLessonStudents(@RequestParam Integer scheduleId) {
        return teacherGradingService.getLessonStudents(scheduleId);
    }

    @org.springframework.web.bind.annotation.PostMapping("/lesson-students/{scheduleId}/save")
    public void saveLesson(
            @PathVariable Integer scheduleId,
            @RequestBody SaveTeacherLessonRequest request
    ) {
        teacherGradingService.saveLesson(scheduleId, request);
    }
}