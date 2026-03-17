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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Slf4j
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
        log.info("Loading users for admin panel with query='{}'", query);

        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        List<AdminUserRowDto> result = userAccountRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(user -> matches(user, normalized))
                .map(this::toRow)
                .toList();

        log.info("Users loaded for admin panel: count={}", result.size());
        return result;
    }

    @Transactional
    public String createUser(AdminUserUpsertRequest request) {
        log.info(
                "Creating user with username='{}', role='{}'",
                request.username(),
                request.role()
        );

        validateRole(request.role());
        validateCreateRequest(request);

        if (userAccountRepository.existsByUsername(request.username().trim())) {
            log.warn("User creation rejected: username='{}' already exists", request.username());
            throw new BadRequestException("Пользователь с таким логином уже существует");
        }

        String email = normalizeNullable(request.email());
        if (email != null && userAccountRepository.existsByEmailIgnoreCase(email)) {
            log.warn("User creation rejected: email already exists for username='{}'", request.username());
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

        log.info(
                "Base user account created successfully for userId={}, username='{}'",
                user.getUserAccountId(),
                user.getUsername()
        );

        applySpecificData(user, request, true);

        log.info(
                "User created successfully for userId={}, username='{}', role='{}'",
                user.getUserAccountId(),
                user.getUsername(),
                user.getRole().getRoleCode()
        );

        return "Пользователь успешно создан";
    }

    @Transactional
    public String updateUser(Integer userId, AdminUserUpsertRequest request) {
        log.info(
                "Updating user userId={} with username='{}', role='{}'",
                userId,
                request.username(),
                request.role()
        );

        validateRole(request.role());

        UserAccount user = userAccountRepository.findByUserAccountId(userId)
                .orElseThrow(() -> {
                    log.warn("User update failed: userId={} not found", userId);
                    return new NotFoundException("Пользователь не найден");
                });

        UserAccount duplicateUsername = userAccountRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(item -> item.getUsername().equalsIgnoreCase(request.username().trim()) && !item.getUserAccountId().equals(userId))
                .findFirst()
                .orElse(null);

        if (duplicateUsername != null) {
            log.warn(
                    "User update rejected: duplicate username='{}' for userId={}",
                    request.username(),
                    userId
            );
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
            log.warn("User update rejected: duplicate email for userId={}", userId);
            throw new BadRequestException("Пользователь с таким email уже существует");
        }

        String currentRole = user.getRole().getRoleCode();
        String newRole = request.role().trim().toLowerCase(Locale.ROOT);

        if (!currentRole.equals(newRole)
                && (studentRepository.existsByUserAccountUserAccountId(userId)
                || teacherRepository.existsByUserAccountUserAccountId(userId))) {
            log.warn(
                    "User update rejected: role change from '{}' to '{}' is not allowed for userId={} because profile already exists",
                    currentRole,
                    newRole,
                    userId
            );
            throw new BadRequestException("Нельзя менять роль пользователю, у которого уже заполнен профиль. Сначала скорректируйте профиль под текущую роль.");
        }

        user.setRole(getRole(newRole));
        user.setUsername(request.username().trim());
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setActive(Boolean.TRUE.equals(request.active()));
        user.setApproved(Boolean.TRUE.equals(request.approved()));

        if (StringUtils.hasText(request.password())) {
            log.info("Updating password for userId={}", userId);
            user.setPasswordHash(passwordEncoder.encode(request.password().trim()));
        }

        applySpecificData(user, request, false);

        log.info(
                "User updated successfully for userId={}, username='{}', role='{}'",
                user.getUserAccountId(),
                user.getUsername(),
                user.getRole().getRoleCode()
        );

        return "Данные пользователя успешно обновлены";
    }

    private void applySpecificData(UserAccount user, AdminUserUpsertRequest request, boolean creating) {
        String roleCode = user.getRole().getRoleCode();

        log.info(
                "Applying role-specific data for userId={}, role='{}', creating={}",
                user.getUserAccountId(),
                roleCode,
                creating
        );

        switch (roleCode) {
            case "admin" -> {
                log.debug("No additional profile data required for admin userId={}", user.getUserAccountId());
            }
            case "student" -> upsertStudent(user, request);
            case "teacher" -> upsertTeacher(user, request);
            default -> {
                log.warn("Unsupported role encountered: '{}'", roleCode);
                throw new BadRequestException("Неподдерживаемая роль");
            }
        }

        if (creating && !"admin".equals(roleCode) && !user.isApproved()) {
            log.warn(
                    "User creation rejected: non-admin userId={} is not approved at creation time",
                    user.getUserAccountId()
            );
            throw new BadRequestException("Для преподавателя или студента при создании рекомендуется сразу оставлять пользователя одобренным");
        }
    }

    private void upsertStudent(UserAccount user, AdminUserUpsertRequest request) {
        log.info("Upserting student profile for userId={}", user.getUserAccountId());

        if (request.groupId() == null) {
            log.warn("Student upsert failed: groupId is null for userId={}", user.getUserAccountId());
            throw new BadRequestException("Для студента необходимо выбрать группу");
        }

        if (!StringUtils.hasText(request.studentCard())) {
            log.warn("Student upsert failed: studentCard is blank for userId={}", user.getUserAccountId());
            throw new BadRequestException("Для студента необходимо указать студенческий билет");
        }

        StudyGroup group = studyGroupRepository.findByGroupId(request.groupId())
                .orElseThrow(() -> {
                    log.warn("Student upsert failed: study group not found, groupId={}", request.groupId());
                    return new NotFoundException("Учебная группа не найдена");
                });

        String studentCard = request.studentCard().trim();

        Student existing = studentRepository.findByUserAccountUserAccountId(user.getUserAccountId()).orElse(null);

        if (studentRepository.existsByStudentCard(studentCard)
                && (existing == null || !studentCard.equalsIgnoreCase(existing.getStudentCard()))) {
            log.warn(
                    "Student upsert failed: studentCard already in use for userId={}",
                    user.getUserAccountId()
            );
            throw new BadRequestException("Студенческий билет уже используется");
        }

        if (existing == null) {
            log.info("Creating new student profile for userId={}", user.getUserAccountId());
            existing = Student.builder()
                    .userAccount(user)
                    .build();
        } else {
            log.info("Updating existing student profile for userId={}", user.getUserAccountId());
        }

        existing.setGroup(group);
        existing.setStudentCard(studentCard);
        studentRepository.save(existing);

        log.info(
                "Student profile saved successfully for userId={}, groupId={}",
                user.getUserAccountId(),
                group.getGroupId()
        );
    }

    private void upsertTeacher(UserAccount user, AdminUserUpsertRequest request) {
        log.info("Upserting teacher profile for userId={}", user.getUserAccountId());

        if (request.departmentId() == null) {
            log.warn("Teacher upsert failed: departmentId is null for userId={}", user.getUserAccountId());
            throw new BadRequestException("Для преподавателя необходимо выбрать кафедру");
        }

        if (!StringUtils.hasText(request.position())) {
            log.warn("Teacher upsert failed: position is blank for userId={}", user.getUserAccountId());
            throw new BadRequestException("Для преподавателя необходимо указать должность");
        }

        Department department = departmentRepository.findByDepartmentId(request.departmentId())
                .orElseThrow(() -> {
                    log.warn("Teacher upsert failed: department not found, departmentId={}", request.departmentId());
                    return new NotFoundException("Кафедра не найдена");
                });

        Teacher existing = teacherRepository.findByUserAccountUserAccountId(user.getUserAccountId()).orElse(null);

        if (existing == null) {
            log.info("Creating new teacher profile for userId={}", user.getUserAccountId());
            existing = Teacher.builder()
                    .userAccount(user)
                    .build();
        } else {
            log.info("Updating existing teacher profile for userId={}", user.getUserAccountId());
        }

        existing.setDepartment(department);
        existing.setPosition(request.position().trim());
        existing.setPhone(normalizeNullable(request.phone()));
        teacherRepository.save(existing);

        log.info(
                "Teacher profile saved successfully for userId={}, departmentId={}",
                user.getUserAccountId(),
                department.getDepartmentId()
        );
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
            log.warn("User creation validation failed: password is blank for username='{}'", request.username());
            throw new BadRequestException("Пароль обязателен при создании пользователя");
        }
    }

    private void validateRole(String role) {
        String normalized = role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
        if (!List.of("admin", "teacher", "student").contains(normalized)) {
            log.warn("Invalid role received: '{}'", role);
            throw new BadRequestException("Допустимые роли: admin, teacher, student");
        }
    }

    private Role getRole(String roleCode) {
        return roleRepository.findByRoleCode(roleCode.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> {
                    log.warn("Role not found: '{}'", roleCode);
                    return new NotFoundException("Роль не найдена");
                });
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}