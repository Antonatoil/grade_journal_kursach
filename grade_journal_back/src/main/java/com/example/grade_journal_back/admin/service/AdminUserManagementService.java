package com.example.grade_journal_back.admin.service;

import com.example.grade_journal_back.admin.dto.AdminUserRowDto;
import com.example.grade_journal_back.admin.dto.AdminUserUpsertRequest;
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
import com.example.grade_journal_back.user.entity.Role;
import com.example.grade_journal_back.user.entity.UserAccount;
import com.example.grade_journal_back.user.repository.RoleRepository;
import com.example.grade_journal_back.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminUserManagementService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<AdminUserRowDto> getUsers(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        return userAccountRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .filter(user -> matches(user, normalized))
            .map(this::toRow)
            .toList();
    }

    @Transactional
    public String createUser(AdminUserUpsertRequest request) {
        validateRole(request.role());
        validateCreateRequest(request);

        if (userAccountRepository.existsByUsername(request.username().trim())) {
            throw new BadRequestException("Пользователь с таким логином уже существует");
        }

        String email = normalizeNullable(request.email());
        if (email != null && userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Пользователь с таким email уже существует");
        }

        Role role = getRole(request.role());

        UserAccount user = UserAccount.builder()
            .role(role)
            .username(request.username().trim())
            .passwordHash(passwordEncoder.encode(request.password().trim()))
            .fullName(request.fullName().trim())
            .email(email)
            .active(Boolean.TRUE.equals(request.active()))
            .approved(Boolean.TRUE.equals(request.approved()))
            .createdAt(Instant.now())
            .build();

        userAccountRepository.save(user);
        applySpecificData(user, request, true);

        return "Пользователь успешно создан";
    }

    @Transactional
    public String updateUser(Integer userId, AdminUserUpsertRequest request) {
        validateRole(request.role());

        UserAccount user = userAccountRepository.findByUserAccountId(userId)
            .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        UserAccount duplicateUsername = userAccountRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .filter(item -> item.getUsername().equalsIgnoreCase(request.username().trim()) && !item.getUserAccountId().equals(userId))
            .findFirst()
            .orElse(null);

        if (duplicateUsername != null) {
            throw new BadRequestException("Пользователь с таким логином уже существует");
        }

        String email = normalizeNullable(request.email());
        UserAccount duplicateEmail = email == null ? null : userAccountRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .filter(item -> item.getEmail() != null
                && item.getEmail().equalsIgnoreCase(email)
                && !item.getUserAccountId().equals(userId))
            .findFirst()
            .orElse(null);

        if (duplicateEmail != null) {
            throw new BadRequestException("Пользователь с таким email уже существует");
        }

        String currentRole = user.getRole().getRoleCode();
        String newRole = request.role().trim().toLowerCase(Locale.ROOT);

        if (!currentRole.equals(newRole) && (studentRepository.existsByUserAccountUserAccountId(userId) || teacherRepository.existsByUserAccountUserAccountId(userId))) {
            throw new BadRequestException("Нельзя менять роль пользователю, у которого уже заполнен профиль. Сначала скорректируйте профиль под текущую роль.");
        }

        user.setRole(getRole(newRole));
        user.setUsername(request.username().trim());
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setActive(Boolean.TRUE.equals(request.active()));
        user.setApproved(Boolean.TRUE.equals(request.approved()));

        if (StringUtils.hasText(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password().trim()));
        }

        applySpecificData(user, request, false);
        return "Данные пользователя успешно обновлены";
    }

    private void applySpecificData(UserAccount user, AdminUserUpsertRequest request, boolean creating) {
        String roleCode = user.getRole().getRoleCode();

        switch (roleCode) {
            case "admin" -> {
            }
            case "student" -> upsertStudent(user, request);
            case "teacher" -> upsertTeacher(user, request);
            default -> throw new BadRequestException("Неподдерживаемая роль");
        }

        if (creating && "admin".equals(roleCode) == false && !user.isApproved()) {
            throw new BadRequestException("Для преподавателя или студента при создании рекомендуется сразу оставлять пользователя одобренным");
        }
    }

    private void upsertStudent(UserAccount user, AdminUserUpsertRequest request) {
        if (request.groupId() == null) {
            throw new BadRequestException("Для студента необходимо выбрать группу");
        }

        if (!StringUtils.hasText(request.studentCard())) {
            throw new BadRequestException("Для студента необходимо указать студенческий билет");
        }

        StudyGroup group = studyGroupRepository.findByGroupId(request.groupId())
            .orElseThrow(() -> new NotFoundException("Учебная группа не найдена"));

        String studentCard = request.studentCard().trim();

        Student existing = studentRepository.findByUserAccountUserAccountId(user.getUserAccountId()).orElse(null);

        if (studentRepository.existsByStudentCard(studentCard) && (existing == null || !studentCard.equalsIgnoreCase(existing.getStudentCard()))) {
            throw new BadRequestException("Студенческий билет уже используется");
        }

        if (existing == null) {
            existing = Student.builder()
                .userAccount(user)
                .build();
        }

        existing.setGroup(group);
        existing.setStudentCard(studentCard);
        studentRepository.save(existing);
    }

    private void upsertTeacher(UserAccount user, AdminUserUpsertRequest request) {
        if (request.departmentId() == null) {
            throw new BadRequestException("Для преподавателя необходимо выбрать кафедру");
        }

        if (!StringUtils.hasText(request.position())) {
            throw new BadRequestException("Для преподавателя необходимо указать должность");
        }

        Department department = departmentRepository.findByDepartmentId(request.departmentId())
            .orElseThrow(() -> new NotFoundException("Кафедра не найдена"));

        Teacher existing = teacherRepository.findByUserAccountUserAccountId(user.getUserAccountId()).orElse(null);

        if (existing == null) {
            existing = Teacher.builder()
                .userAccount(user)
                .build();
        }

        existing.setDepartment(department);
        existing.setPosition(request.position().trim());
        existing.setPhone(normalizeNullable(request.phone()));
        teacherRepository.save(existing);
    }

    private AdminUserRowDto toRow(UserAccount user) {
        Student student = studentRepository.findByUserAccountUserAccountId(user.getUserAccountId()).orElse(null);
        Teacher teacher = teacherRepository.findByUserAccountUserAccountId(user.getUserAccountId()).orElse(null);

        return new AdminUserRowDto(
            user.getUserAccountId(),
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            user.getRole().getRoleCode(),
            user.isActive(),
            user.isApproved(),
            isProfileCompleted(user, student, teacher),
            user.getCreatedAt(),
            student != null ? student.getStudentId() : null,
            student != null ? student.getStudentCard() : null,
            student != null ? student.getGroup().getGroupId() : null,
            student != null ? student.getGroup().getGroupCode() : null,
            student != null ? student.getGroup().getCourseNo() : null,
            student != null ? student.getGroup().getFacultyName() : null,
            student != null ? student.getGroup().getSpecializationName() : null,
            teacher != null ? teacher.getTeacherId() : null,
            teacher != null ? teacher.getDepartment().getDepartmentId() : null,
            teacher != null ? teacher.getDepartment().getDepartmentCode() : null,
            teacher != null ? teacher.getDepartment().getDepartmentName() : null,
            teacher != null ? teacher.getPosition() : null,
            teacher != null ? teacher.getPhone() : null
        );
    }

    private boolean isProfileCompleted(UserAccount user, Student student, Teacher teacher) {
        return switch (user.getRole().getRoleCode()) {
            case "student" -> student != null;
            case "teacher" -> teacher != null;
            default -> true;
        };
    }

    private boolean matches(UserAccount user, String normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            return true;
        }

        return contains(user.getUsername(), normalizedQuery)
            || contains(user.getFullName(), normalizedQuery)
            || contains(user.getEmail(), normalizedQuery)
            || contains(user.getRole().getRoleCode(), normalizedQuery);
    }

    private boolean contains(String value, String normalizedQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private void validateCreateRequest(AdminUserUpsertRequest request) {
        if (!StringUtils.hasText(request.password())) {
            throw new BadRequestException("Пароль обязателен при создании пользователя");
        }
    }

    private void validateRole(String role) {
        String normalized = role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
        if (!List.of("admin", "teacher", "student").contains(normalized)) {
            throw new BadRequestException("Допустимые роли: admin, teacher, student");
        }
    }

    private Role getRole(String roleCode) {
        return roleRepository.findByRoleCode(roleCode.trim().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new NotFoundException("Роль не найдена"));
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}