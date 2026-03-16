package com.example.grade_journal_back.profile;

import com.example.grade_journal_back.auth.JwtAuthenticationFilter;
import com.example.grade_journal_back.teacher.entity.Department;
import com.example.grade_journal_back.teacher.entity.Teacher;
import com.example.grade_journal_back.teacher.repository.TeacherRepository;
import com.example.grade_journal_back.student.repository.StudentRepository;
import com.example.grade_journal_back.user.entity.Role;
import com.example.grade_journal_back.user.entity.UserAccount;
import com.example.grade_journal_back.user.repository.UserAccountRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void me_shouldReturnTeacherProfile() throws Exception {
        Role role = mock(Role.class);
        when(role.getRoleCode()).thenReturn("teacher");

        UserAccount user = UserAccount.builder()
            .userAccountId(10)
            .role(role)
            .username("teacher10")
            .passwordHash("encoded-password")
            .fullName("Преподаватель 10")
            .email("teacher10@example.com")
            .active(true)
            .approved(true)
            .createdAt(Instant.now())
            .build();

        Department department = mock(Department.class);
        when(department.getDepartmentCode()).thenReturn("EI");
        when(department.getDepartmentName()).thenReturn("Кафедра экономической информатики");

        Teacher teacher = Teacher.builder()
            .teacherId(10)
            .userAccount(user)
            .department(department)
            .position("Доцент")
            .phone("+375291112233")
            .build();

        when(userAccountRepository.findByUsername("teacher10")).thenReturn(Optional.of(user));
        when(studentRepository.findByUserAccountUserAccountId(10)).thenReturn(Optional.empty());
        when(teacherRepository.findByUserAccountUserAccountId(10)).thenReturn(Optional.of(teacher));

        mockMvc.perform(get("/api/profile/me").principal(() -> "teacher10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("teacher10"))
            .andExpect(jsonPath("$.role").value("teacher"))
            .andExpect(jsonPath("$.teacher.teacherId").value(10))
            .andExpect(jsonPath("$.teacher.departmentCode").value("EI"))
            .andExpect(jsonPath("$.teacher.departmentName").value("Кафедра экономической информатики"))
            .andExpect(jsonPath("$.teacher.position").value("Доцент"));
    }
}