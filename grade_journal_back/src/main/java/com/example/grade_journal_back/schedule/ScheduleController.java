package com.example.grade_journal_back.schedule;

import com.example.grade_journal_back.schedule.dto.CreateScheduleRequest;
import com.example.grade_journal_back.schedule.dto.ScheduleEntryDto;
import com.example.grade_journal_back.schedule.dto.ScheduleMetaResponse;
import com.example.grade_journal_back.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/meta")
    public ScheduleMetaResponse getMeta() {
        log.info("Schedule metadata requested");

        ScheduleMetaResponse response = scheduleService.getMeta();

        log.info("Schedule metadata returned successfully");
        return response;
    }

    @GetMapping("/groups/{groupId}")
    public List<ScheduleEntryDto> getGroupSchedule(@PathVariable Integer groupId) {
        log.info("Group schedule requested for groupId={}", groupId);

        List<ScheduleEntryDto> result = scheduleService.getScheduleByGroup(groupId);

        log.info("Group schedule returned for groupId={}, entries={}", groupId, result.size());
        return result;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> createSchedule(@Valid @RequestBody CreateScheduleRequest request) {
        log.info(
                "Schedule creation requested for groupId={}, teacherId={}, courseId={}, lessonDate={}, timeSlot='{}'",
                request.groupId(),
                request.teacherId(),
                request.courseId(),
                request.lessonDate(),
                request.timeSlot()
        );

        String message = scheduleService.createSchedule(request);

        log.info(
                "Schedule created successfully for groupId={}, teacherId={}, courseId={}, lessonDate={}, timeSlot='{}'",
                request.groupId(),
                request.teacherId(),
                request.courseId(),
                request.lessonDate(),
                request.timeSlot()
        );

        return Map.of("message", message);
    }
}