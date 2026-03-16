package com.example.grade_journal_back.admin.service;

import com.example.grade_journal_back.admin.dto.AdminProfileOptionsDto;
import com.example.grade_journal_back.admin.dto.FillStudentProfileRequest;
import com.example.grade_journal_back.admin.dto.FillTeacherProfileRequest;
import com.example.grade_journal_back.admin.dto.IncompleteProfileDto;
import com.example.grade_journal_back.common.exception.BadRequestException;
import com.example.grade_journal_back.common.exception.NotFoundException;
import com.example.grade_journal_back.student.entity.Student;
import com.example.grade_journal_back.student.entity.StudyGroup;
import com.example.grade_journal_back.student.repository.StudentRepository;
import com.example.grade_journal_back.student.repository.StudyGroupRepository;
import com.example.grade_journal_back.teacher.entity.Department;
import com.example.grade_journal_back.teacher.entity.Teacher;
import com.example.grade_journal_back.teacher.repository.DepartmentRepository;
import com.example.grade_journal_back.teacher.repository.TeacherRepository;
import com.example.grade_journal_back.user.entity.UserAccount;
import com.example.grade_journal_back.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;

import org.springframework.cache.annotation.Cacheable;

@Service
@RequiredArgsConstructor
public class AdminProfileSetupService {

    private final UserAccountRepository userAccountRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public List<IncompleteProfileDto> getIncompleteProfiles() {
        return userAccountRepository.findAllByApprovedTrueAndRoleRoleCodeInOrderByCreatedAtAsc(List.of("student", "teacher"))
            .stream()
            .filter(this::isIncomplete)
            .map(user -> new IncompleteProfileDto(
                user.getUserAccountId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getRoleCode(),
                user.isApproved(),
                user.getCreatedAt()
            ))
            .toList();
    }

    @Cacheable("adminProfileOptions")
    @Transactional(readOnly = true)
    public AdminProfileOptionsDto getOptions() {
        List<AdminProfileOptionsDto.GroupOptionDto> groups = studyGroupRepository.findAll()
            .stream()
            .map(group -> new AdminProfileOptionsDto.GroupOptionDto(
                group.getGroupId(),
                group.getGroupCode(),
                group.getCourseNo(),
                group.getAdmissionYear(),
                group.getFacultyName(),
                group.getSpecializationName()
            ))
            .toList();

        List<AdminProfileOptionsDto.DepartmentOptionDto> departments = departmentRepository.findAll()
            .stream()
            .map(department -> new AdminProfileOptionsDto.DepartmentOptionDto(
                department.getDepartmentId(),
                department.getDepartmentCode(),
                department.getDepartmentName()
            ))
            .toList();

        return new AdminProfileOptionsDto(groups, departments);
    }

    @CacheEvict(value = "adminProfileOptions", allEntries = true)
    @Transactional
    public String fillStudentProfile(Integer userId, FillStudentProfileRequest request) {
        UserAccount user = userAccountRepository.findByUserAccountId(userId)
            .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        validateRole(user, "student");

        if (studentRepository.existsByUserAccountUserAccountId(userId)) {
            throw new BadRequestException("Профиль студента уже заполнен");
        }

        StudyGroup group = studyGroupRepository.findByGroupId(request.groupId())
            .orElseThrow(() -> new NotFoundException("Учебная группа не найдена"));

        String studentCard = request.studentCard().trim();

        if (studentRepository.existsByStudentCard(studentCard)) {
            throw new BadRequestException("Студенческий билет уже используется");
        }

        Student student = Student.builder()
            .userAccount(user)
            .group(group)
            .studentCard(studentCard)
            .build();

        studentRepository.save(student);
        return "Профиль студента успешно заполнен";
    }

    @CacheEvict(value = "adminProfileOptions", allEntries = true)
    @Transactional
    public String fillTeacherProfile(Integer userId, FillTeacherProfileRequest request) {
        UserAccount user = userAccountRepository.findByUserAccountId(userId)
            .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        validateRole(user, "teacher");

        if (teacherRepository.existsByUserAccountUserAccountId(userId)) {
            throw new BadRequestException("Профиль преподавателя уже заполнен");
        }

        Department department = departmentRepository.findByDepartmentId(request.departmentId())
            .orElseThrow(() -> new NotFoundException("Кафедра не найдена"));

        Teacher teacher = Teacher.builder()
            .userAccount(user)
            .department(department)
            .position(request.position().trim())
            .phone(StringUtils.hasText(request.phone()) ? request.phone().trim() : null)
            .build();

        teacherRepository.save(teacher);
        return "Профиль преподавателя успешно заполнен";
    }

    private boolean isIncomplete(UserAccount user) {
        return switch (user.getRole().getRoleCode()) {
            case "student" -> !studentRepository.existsByUserAccountUserAccountId(user.getUserAccountId());
            case "teacher" -> !teacherRepository.existsByUserAccountUserAccountId(user.getUserAccountId());
            default -> false;
        };
    }

    private void validateRole(UserAccount user, String expectedRole) {
        if (!user.isApproved()) {
            throw new BadRequestException("Пользователь должен быть сначала одобрен");
        }

        if (!expectedRole.equals(user.getRole().getRoleCode())) {
            throw new BadRequestException("Неверная роль пользователя для этой операции");
        }
    }
}