package com.example.grade_journal_back.schedule;

import com.example.grade_journal_back.schedule.dto.CreateScheduleRequest;
import com.example.grade_journal_back.schedule.dto.ScheduleEntryDto;
import com.example.grade_journal_back.schedule.dto.ScheduleMetaResponse;
import com.example.grade_journal_back.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/meta")
    public ScheduleMetaResponse getMeta() {
        return scheduleService.getMeta();
    }

    @GetMapping("/groups/{groupId}")
    public List<ScheduleEntryDto> getGroupSchedule(@PathVariable Integer groupId) {
        return scheduleService.getScheduleByGroup(groupId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> createSchedule(@Valid @RequestBody CreateScheduleRequest request) {
        return Map.of("message", scheduleService.createSchedule(request));
    }
}