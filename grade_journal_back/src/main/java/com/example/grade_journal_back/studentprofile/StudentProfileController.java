package com.example.grade_journal_back.studentprofile;

import com.example.grade_journal_back.studentprofile.dto.StudentProfileOptionDto;
import com.example.grade_journal_back.studentprofile.dto.StudentProfileResponseDto;
import com.example.grade_journal_back.studentprofile.service.StudentProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/student-profiles")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    @GetMapping("/options")
    public List<StudentProfileOptionDto> getOptions(
        @RequestParam(required = false, defaultValue = "") String query,
        Principal principal
    ) {
        return studentProfileService.getOptions(principal.getName(), query);
    }

    @GetMapping("/{studentId}")
    public StudentProfileResponseDto getProfile(
        @PathVariable Integer studentId,
        Principal principal
    ) {
        return studentProfileService.getProfile(principal.getName(), studentId);
    }
}