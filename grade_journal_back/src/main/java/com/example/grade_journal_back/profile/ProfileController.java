package com.example.grade_journal_back.profile;

import com.example.grade_journal_back.common.exception.NotFoundException;
import com.example.grade_journal_back.user.entity.UserAccount;
import com.example.grade_journal_back.user.repository.UserAccountRepository;
import com.example.grade_journal_back.student.repository.StudentRepository;
import com.example.grade_journal_back.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final UserAccountRepository userAccountRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    @Transactional(readOnly = true)
    @GetMapping("/api/profile/me")
    public ProfileResponse me(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new NotFoundException("Пользователь не найден");
        }

        UserAccount user = userAccountRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        ProfileResponse.StudentInfo studentInfo = studentRepository.findByUserAccountUserAccountId(user.getUserAccountId())
            .map(student -> new ProfileResponse.StudentInfo(
                student.getStudentId(),
                student.getStudentCard(),
                student.getGroup().getGroupCode(),
                student.getGroup().getCourseNo(),
                student.getGroup().getFacultyName(),
                student.getGroup().getSpecializationName()
            ))
            .orElse(null);

        ProfileResponse.TeacherInfo teacherInfo = teacherRepository.findByUserAccountUserAccountId(user.getUserAccountId())
            .map(teacher -> new ProfileResponse.TeacherInfo(
                teacher.getTeacherId(),
                teacher.getDepartment().getDepartmentCode(),
                teacher.getDepartment().getDepartmentName(),
                teacher.getPosition(),
                teacher.getPhone()
            ))
            .orElse(null);

        return new ProfileResponse(
            user.getUserAccountId(),
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            user.getRole().getRoleCode(),
            user.isActive(),
            user.isApproved(),
            studentInfo,
            teacherInfo
        );
    }
}