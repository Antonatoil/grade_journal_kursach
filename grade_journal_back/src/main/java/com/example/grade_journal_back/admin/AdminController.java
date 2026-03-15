package com.example.grade_journal_back.admin;

import com.example.grade_journal_back.admin.dto.*;
import com.example.grade_journal_back.admin.service.AdminProfileSetupService;
import com.example.grade_journal_back.admin.service.AdminUserManagementService;
import com.example.grade_journal_back.registration.service.RegistrationApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final RegistrationApprovalService registrationApprovalService;
    private final AdminProfileSetupService adminProfileSetupService;
    private final AdminUserManagementService adminUserManagementService;

    @GetMapping("/registration-requests")
    public List<PendingRegistrationDto> getPendingRequests() {
        return registrationApprovalService.getPendingRequests();
    }

    @PostMapping("/registration-requests/{requestId}/approve")
    public AdminActionResponse approve(@PathVariable Integer requestId, Principal principal) {
        return registrationApprovalService.approve(requestId, principal.getName());
    }

    @PostMapping("/registration-requests/{requestId}/reject")
    public AdminActionResponse reject(@PathVariable Integer requestId, Principal principal) {
        return registrationApprovalService.reject(requestId, principal.getName());
    }

    @GetMapping("/incomplete-profiles")
    public List<IncompleteProfileDto> getIncompleteProfiles() {
        return adminProfileSetupService.getIncompleteProfiles();
    }

    @GetMapping("/profile-options")
    public AdminProfileOptionsDto getProfileOptions() {
        return adminProfileSetupService.getOptions();
    }

    @PostMapping("/users/{userId}/fill-student-profile")
    public Map<String, String> fillStudentProfile(
        @PathVariable Integer userId,
        @Valid @RequestBody FillStudentProfileRequest request
    ) {
        return Map.of("message", adminProfileSetupService.fillStudentProfile(userId, request));
    }

    @PostMapping("/users/{userId}/fill-teacher-profile")
    public Map<String, String> fillTeacherProfile(
        @PathVariable Integer userId,
        @Valid @RequestBody FillTeacherProfileRequest request
    ) {
        return Map.of("message", adminProfileSetupService.fillTeacherProfile(userId, request));
    }

    @GetMapping("/users")
    public List<AdminUserRowDto> getUsers(@RequestParam(required = false) String query) {
        return adminUserManagementService.getUsers(query);
    }

    @PostMapping("/users")
    public Map<String, String> createUser(@Valid @RequestBody AdminUserUpsertRequest request) {
        return Map.of("message", adminUserManagementService.createUser(request));
    }

    @PutMapping("/users/{userId}")
    public Map<String, String> updateUser(
        @PathVariable Integer userId,
        @Valid @RequestBody AdminUserUpsertRequest request
    ) {
        return Map.of("message", adminUserManagementService.updateUser(userId, request));
    }
}