package com.example.grade_journal_back.profile;

import com.example.grade_journal_back.student.entity.Student;
import com.example.grade_journal_back.student.entity.StudyGroup;
import com.example.grade_journal_back.student.repository.StudentRepository;
import com.example.grade_journal_back.teacher.entity.Department;
import com.example.grade_journal_back.teacher.entity.Teacher;
import com.example.grade_journal_back.teacher.repository.TeacherRepository;
import com.example.grade_journal_back.user.entity.Role;
import com.example.grade_journal_back.user.entity.UserAccount;
import com.example.grade_journal_back.user.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @MockBean
    private StudentRepository studentRepository;

    @MockBean
    private TeacherRepository teacherRepository;

    @Test
    void me_shouldReturnTeacherProfile() throws Exception {
        UserAccount user = mock(UserAccount.class);
        Role role = mock(Role.class);
        Teacher teacher = mock(Teacher.class);
        Department department = mock(Department.class);

        when(user.getUserAccountId()).thenReturn(10);
        when(user.getUsername()).thenReturn("teacher01");
        when(user.getFullName()).thenReturn("Преподаватель 1");
        when(user.getEmail()).thenReturn("teacher01@ejournal.by");
        when(user.isActive()).thenReturn(true);
        when(user.isApproved()).thenReturn(true);
        when(user.getRole()).thenReturn(role);

        when(role.getRoleCode()).thenReturn("teacher");

        when(teacher.getTeacherId()).thenReturn(5);
        when(teacher.getDepartment()).thenReturn(department);
        when(teacher.getPosition()).thenReturn("Доцент");
        when(teacher.getPhone()).thenReturn("+375291112233");

        when(department.getDepartmentCode()).thenReturn("PI");
        when(department.getDepartmentName()).thenReturn("Кафедра программной инженерии");

        when(userAccountRepository.findByUsername("teacher01")).thenReturn(Optional.of(user));
        when(studentRepository.findByUserAccountUserAccountId(10)).thenReturn(Optional.empty());
        when(teacherRepository.findByUserAccountUserAccountId(10)).thenReturn(Optional.of(teacher));

        Principal principal = () -> "teacher01";

        mockMvc.perform(get("/api/profile/me").principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(10))
            .andExpect(jsonPath("$.username").value("teacher01"))
            .andExpect(jsonPath("$.fullName").value("Преподаватель 1"))
            .andExpect(jsonPath("$.email").value("teacher01@ejournal.by"))
            .andExpect(jsonPath("$.role").value("teacher"))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.approved").value(true))
            .andExpect(jsonPath("$.teacher.teacherId").value(5))
            .andExpect(jsonPath("$.teacher.departmentCode").value("PI"))
            .andExpect(jsonPath("$.teacher.departmentName").value("Кафедра программной инженерии"))
            .andExpect(jsonPath("$.teacher.position").value("Доцент"))
            .andExpect(jsonPath("$.teacher.phone").value("+375291112233"));
    }

    @Test
    void me_shouldReturnStudentProfile() throws Exception {
        UserAccount user = mock(UserAccount.class);
        Role role = mock(Role.class);
        Student student = mock(Student.class);
        StudyGroup group = mock(StudyGroup.class);

        when(user.getUserAccountId()).thenReturn(20);
        when(user.getUsername()).thenReturn("student001");
        when(user.getFullName()).thenReturn("Студент 1");
        when(user.getEmail()).thenReturn("student001@ejournal.by");
        when(user.isActive()).thenReturn(true);
        when(user.isApproved()).thenReturn(true);
        when(user.getRole()).thenReturn(role);

        when(role.getRoleCode()).thenReturn("student");

        when(student.getStudentId()).thenReturn(100);
        when(student.getStudentCard()).thenReturn("25100101");
        when(student.getGroup()).thenReturn(group);

        when(group.getGroupCode()).thenReturn("251001");
        when(group.getCourseNo()).thenReturn((short) 1);
        when(group.getFacultyName()).thenReturn("Инженерно-экономический факультет");
        when(group.getSpecializationName()).thenReturn("Экономическая информатика");

        when(userAccountRepository.findByUsername("student001")).thenReturn(Optional.of(user));
        when(studentRepository.findByUserAccountUserAccountId(20)).thenReturn(Optional.of(student));
        when(teacherRepository.findByUserAccountUserAccountId(20)).thenReturn(Optional.empty());

        Principal principal = () -> "student001";

        mockMvc.perform(get("/api/profile/me").principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(20))
            .andExpect(jsonPath("$.username").value("student001"))
            .andExpect(jsonPath("$.fullName").value("Студент 1"))
            .andExpect(jsonPath("$.role").value("student"))
            .andExpect(jsonPath("$.student.studentId").value(100))
            .andExpect(jsonPath("$.student.studentCard").value("25100101"))
            .andExpect(jsonPath("$.student.groupCode").value("251001"))
            .andExpect(jsonPath("$.student.courseNo").value(1))
            .andExpect(jsonPath("$.student.facultyName").value("Инженерно-экономический факультет"))
            .andExpect(jsonPath("$.student.specializationName").value("Экономическая информатика"));
    }

    @Test
    void me_shouldReturnAdminProfileWithoutNestedData() throws Exception {
        UserAccount user = mock(UserAccount.class);
        Role role = mock(Role.class);

        when(user.getUserAccountId()).thenReturn(1);
        when(user.getUsername()).thenReturn("admin");
        when(user.getFullName()).thenReturn("Системный администратор");
        when(user.getEmail()).thenReturn("admin@ejournal.by");
        when(user.isActive()).thenReturn(true);
        when(user.isApproved()).thenReturn(true);
        when(user.getRole()).thenReturn(role);

        when(role.getRoleCode()).thenReturn("admin");

        when(userAccountRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(studentRepository.findByUserAccountUserAccountId(1)).thenReturn(Optional.empty());
        when(teacherRepository.findByUserAccountUserAccountId(1)).thenReturn(Optional.empty());

        Principal principal = () -> "admin";

        mockMvc.perform(get("/api/profile/me").principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.username").value("admin"))
            .andExpect(jsonPath("$.fullName").value("Системный администратор"))
            .andExpect(jsonPath("$.role").value("admin"))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.approved").value(true));
    }
}