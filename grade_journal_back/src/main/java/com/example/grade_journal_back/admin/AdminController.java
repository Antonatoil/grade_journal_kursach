package com.example.grade_journal_back.admin;

import com.example.grade_journal_back.admin.dto.AdminActionResponse;
import com.example.grade_journal_back.admin.dto.AdminProfileOptionsDto;
import com.example.grade_journal_back.admin.dto.AdminUserRowDto;
import com.example.grade_journal_back.admin.dto.AdminUserUpsertRequest;
import com.example.grade_journal_back.admin.dto.FillStudentProfileRequest;
import com.example.grade_journal_back.admin.dto.FillTeacherProfileRequest;
import com.example.grade_journal_back.admin.dto.IncompleteProfileDto;
import com.example.grade_journal_back.admin.dto.PendingRegistrationDto;
import com.example.grade_journal_back.admin.service.AdminProfileSetupService;
import com.example.grade_journal_back.admin.service.AdminUserManagementService;
import com.example.grade_journal_back.registration.service.RegistrationApprovalService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
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
        log.info("Admin requested pending registration requests");
        List<PendingRegistrationDto> result = registrationApprovalService.getPendingRequests();
        log.info("Pending registration requests returned: count={}", result.size());
        return result;
    }

    @PostMapping("/registration-requests/{requestId}/approve")
    public AdminActionResponse approve(@PathVariable Integer requestId, Principal principal) {
        log.info(
                "Admin '{}' requested approval for registration requestId={}",
                principal.getName(),
                requestId
        );

        AdminActionResponse response = registrationApprovalService.approve(requestId, principal.getName());

        log.info(
                "Registration requestId={} approved by admin '{}'",
                requestId,
                principal.getName()
        );

        return response;
    }

    @PostMapping("/registration-requests/{requestId}/reject")
    public AdminActionResponse reject(@PathVariable Integer requestId, Principal principal) {
        log.info(
                "Admin '{}' requested rejection for registration requestId={}",
                principal.getName(),
                requestId
        );

        AdminActionResponse response = registrationApprovalService.reject(requestId, principal.getName());

        log.info(
                "Registration requestId={} rejected by admin '{}'",
                requestId,
                principal.getName()
        );

        return response;
    }

    @GetMapping("/incomplete-profiles")
    public List<IncompleteProfileDto> getIncompleteProfiles() {
        log.info("Admin requested incomplete profiles list");
        List<IncompleteProfileDto> result = adminProfileSetupService.getIncompleteProfiles();
        log.info("Incomplete profiles returned: count={}", result.size());
        return result;
    }

    @GetMapping("/profile-options")
    public AdminProfileOptionsDto getProfileOptions() {
        log.info("Admin requested profile options");
        AdminProfileOptionsDto response = adminProfileSetupService.getOptions();
        log.info("Profile options returned successfully");
        return response;
    }

    @PostMapping("/users/{userId}/fill-student-profile")
    public Map<String, String> fillStudentProfile(
            @PathVariable Integer userId,
            @Valid @RequestBody FillStudentProfileRequest request
    ) {
        log.info("Admin requested student profile fill for userId={}", userId);
        String message = adminProfileSetupService.fillStudentProfile(userId, request);
        log.info("Student profile filled successfully for userId={}", userId);
        return Map.of("message", message);
    }

    @PostMapping("/users/{userId}/fill-teacher-profile")
    public Map<String, String> fillTeacherProfile(
            @PathVariable Integer userId,
            @Valid @RequestBody FillTeacherProfileRequest request
    ) {
        log.info("Admin requested teacher profile fill for userId={}", userId);
        String message = adminProfileSetupService.fillTeacherProfile(userId, request);
        log.info("Teacher profile filled successfully for userId={}", userId);
        return Map.of("message", message);
    }

    @GetMapping("/users")
    public List<AdminUserRowDto> getUsers(@RequestParam(required = false) String query) {
        log.info("Admin requested users list with query='{}'", query);
        List<AdminUserRowDto> result = adminUserManagementService.getUsers(query);
        log.info("Users list returned: count={}", result.size());
        return result;
    }

    @PostMapping("/users")
    public Map<String, String> createUser(@Valid @RequestBody AdminUserUpsertRequest request) {
        log.info(
                "Admin requested user creation for username='{}', role='{}'",
                request.username(),
                request.role()
        );

        String message = adminUserManagementService.createUser(request);

        log.info(
                "User created successfully for username='{}', role='{}'",
                request.username(),
                request.role()
        );

        return Map.of("message", message);
    }

    @PutMapping("/users/{userId}")
    public Map<String, String> updateUser(
            @PathVariable Integer userId,
            @Valid @RequestBody AdminUserUpsertRequest request
    ) {
        log.info(
                "Admin requested user update for userId={}, username='{}', role='{}'",
                userId,
                request.username(),
                request.role()
        );

        String message = adminUserManagementService.updateUser(userId, request);

        log.info("User updated successfully for userId={}", userId);

        return Map.of("message", message);
    }
}