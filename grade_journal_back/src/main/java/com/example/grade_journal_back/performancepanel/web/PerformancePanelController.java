package com.example.grade_journal_back.performancepanel.web;

import com.example.grade_journal_back.performancepanel.dto.PerformanceCourseOptionDto;
import com.example.grade_journal_back.performancepanel.dto.PerformanceDetailsDto;
import com.example.grade_journal_back.performancepanel.dto.PerformanceStudentOptionDto;
import com.example.grade_journal_back.performancepanel.service.PerformancePanelService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/performance-panel")
public class PerformancePanelController {

    private final PerformancePanelService performancePanelService;

    public PerformancePanelController(PerformancePanelService performancePanelService) {
        this.performancePanelService = performancePanelService;
    }

    @GetMapping("/students")
    public List<PerformanceStudentOptionDto> getStudents() {
        return performancePanelService.getStudents();
    }

    @GetMapping("/courses")
    public List<PerformanceCourseOptionDto> getCourses(@RequestParam Integer studentId) {
        return performancePanelService.getCourses(studentId);
    }

    @GetMapping("/details")
    public PerformanceDetailsDto getDetails(
            @RequestParam Integer studentId,
            @RequestParam Integer courseId
    ) {
        return performancePanelService.getDetails(studentId, courseId);
    }
}