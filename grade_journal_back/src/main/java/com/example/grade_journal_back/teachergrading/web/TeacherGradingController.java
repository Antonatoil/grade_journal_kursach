package com.example.grade_journal_back.teachergrading.web;

import com.example.grade_journal_back.teachergrading.dto.SaveTeacherLessonRequest;
import com.example.grade_journal_back.teachergrading.dto.TeacherGroupOptionDto;
import com.example.grade_journal_back.teachergrading.dto.TeacherLessonOptionDto;
import com.example.grade_journal_back.teachergrading.dto.TeacherLessonStudentDto;
import com.example.grade_journal_back.teachergrading.service.TeacherGradingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/teacher-grading")
public class TeacherGradingController {

    private final TeacherGradingService teacherGradingService;

    public TeacherGradingController(TeacherGradingService teacherGradingService) {
        this.teacherGradingService = teacherGradingService;
    }

    @GetMapping("/groups")
    public List<TeacherGroupOptionDto> getGroups() {
        log.info("Teacher groups requested");

        List<TeacherGroupOptionDto> result = teacherGradingService.getMyGroups();

        log.info("Teacher groups returned successfully: count={}", result.size());
        return result;
    }

    @GetMapping("/lessons")
    public List<TeacherLessonOptionDto> getLessons(@RequestParam Integer groupId) {
        log.info("Teacher lessons requested for groupId={}", groupId);

        List<TeacherLessonOptionDto> result = teacherGradingService.getMyLessons(groupId);

        log.info("Teacher lessons returned successfully for groupId={}, count={}", groupId, result.size());
        return result;
    }

    @GetMapping("/lesson-students")
    public List<TeacherLessonStudentDto> getLessonStudents(@RequestParam Integer scheduleId) {
        log.info("Teacher lesson students requested for scheduleId={}", scheduleId);

        List<TeacherLessonStudentDto> result = teacherGradingService.getLessonStudents(scheduleId);

        log.info(
                "Teacher lesson students returned successfully for scheduleId={}, count={}",
                scheduleId,
                result.size()
        );
        return result;
    }

    @org.springframework.web.bind.annotation.PostMapping("/lesson-students/{scheduleId}/save")
    public void saveLesson(
            @PathVariable Integer scheduleId,
            @RequestBody SaveTeacherLessonRequest request
    ) {
        int studentsCount = request == null || request.students() == null ? 0 : request.students().size();

        log.info(
                "Teacher lesson save requested for scheduleId={}, studentsCount={}",
                scheduleId,
                studentsCount
        );

        teacherGradingService.saveLesson(scheduleId, request);

        log.info("Teacher lesson saved successfully for scheduleId={}", scheduleId);
    }
}