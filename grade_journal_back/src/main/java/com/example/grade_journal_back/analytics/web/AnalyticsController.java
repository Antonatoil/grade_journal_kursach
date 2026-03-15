package com.example.grade_journal_back.analytics.web;

import com.example.grade_journal_back.analytics.dto.GroupComparisonDto;
import com.example.grade_journal_back.analytics.dto.RiskGroupStudentDto;
import com.example.grade_journal_back.analytics.dto.StudentComparisonDto;
import com.example.grade_journal_back.analytics.dto.TeacherLessonDto;
import com.example.grade_journal_back.analytics.dto.TeacherOptionDto;
import com.example.grade_journal_back.analytics.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/teachers")
    public List<TeacherOptionDto> getTeachers() {
        return analyticsService.getTeacherOptions();
    }

    @GetMapping("/teacher-schedule")
    public List<TeacherLessonDto> getTeacherSchedule(@RequestParam Integer teacherId) {
        return analyticsService.getTeacherSchedule(teacherId);
    }

    @GetMapping("/group-comparison")
    public List<GroupComparisonDto> getGroupComparison() {
        return analyticsService.getGroupComparison();
    }

    @GetMapping("/student-comparison")
    public List<StudentComparisonDto> getStudentComparison(
            @RequestParam(defaultValue = "desc") String sort
    ) {
        return analyticsService.getStudentComparison(sort);
    }

    @GetMapping("/risk-groups")
    public List<RiskGroupStudentDto> getRiskGroups() {
        return analyticsService.getRiskGroups();
    }
}