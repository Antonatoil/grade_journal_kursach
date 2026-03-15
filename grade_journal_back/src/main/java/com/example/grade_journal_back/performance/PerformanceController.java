package com.example.grade_journal_back.performance;

import com.example.grade_journal_back.performance.dto.PerformanceCourseOptionDto;
import com.example.grade_journal_back.performance.dto.PerformanceMetaResponse;
import com.example.grade_journal_back.performance.dto.PerformanceResponseDto;
import com.example.grade_journal_back.performance.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
public class PerformanceController {

    private final PerformanceService performanceService;

    @GetMapping("/meta")
    public PerformanceMetaResponse getMeta(Principal principal) {
        return performanceService.getMeta(principal.getName());
    }

    @GetMapping("/student-courses")
    public List<PerformanceCourseOptionDto> getStudentCourses(
        @RequestParam Integer studentId,
        Principal principal
    ) {
        return performanceService.getCoursesForStudent(principal.getName(), studentId);
    }

    @GetMapping
    public PerformanceResponseDto getPerformance(
        @RequestParam Integer studentId,
        @RequestParam Integer courseId,
        Principal principal
    ) {
        return performanceService.getPerformance(principal.getName(), studentId, courseId);
    }
}