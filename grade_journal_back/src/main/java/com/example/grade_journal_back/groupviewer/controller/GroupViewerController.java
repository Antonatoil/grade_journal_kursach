package com.example.grade_journal_back.groupviewer.controller;

import com.example.grade_journal_back.groupviewer.dto.GroupOptionResponse;
import com.example.grade_journal_back.groupviewer.dto.GroupStudentRowResponse;
import com.example.grade_journal_back.groupviewer.service.GroupViewerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/group-viewer")
public class GroupViewerController {

    private final GroupViewerService groupViewerService;

    public GroupViewerController(GroupViewerService groupViewerService) {
        this.groupViewerService = groupViewerService;
    }

    @GetMapping("/groups")
    @PreAuthorize("isAuthenticated()")
    public List<GroupOptionResponse> getGroups() {
        return groupViewerService.getGroups();
    }

    @GetMapping("/groups/{groupId}/students")
    @PreAuthorize("isAuthenticated()")
    public List<GroupStudentRowResponse> getStudents(@PathVariable Integer groupId) {
        return groupViewerService.getStudentsByGroup(groupId);
    }
}