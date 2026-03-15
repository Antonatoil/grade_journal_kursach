package com.example.grade_journal_back.user;

import com.example.grade_journal_back.admin.dto.AdminUserRowDto;
import com.example.grade_journal_back.admin.service.AdminUserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
public class UserDirectoryController {

    private final AdminUserManagementService adminUserManagementService;

    @GetMapping("/directory")
    public List<AdminUserRowDto> getDirectory() {
        return adminUserManagementService.getUsers(null);
    }
}