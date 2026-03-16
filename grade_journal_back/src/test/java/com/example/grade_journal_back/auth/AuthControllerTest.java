package com.example.grade_journal_back.auth;

import com.example.grade_journal_back.auth.dto.LoginRequest;
import com.example.grade_journal_back.auth.dto.LoginResponse;
import com.example.grade_journal_back.auth.dto.RefreshRequest;
import com.example.grade_journal_back.auth.dto.RegisterRequestDto;
import com.example.grade_journal_back.auth.dto.RegisterResponse;
import com.example.grade_journal_back.auth.service.AuthService;
import com.example.grade_journal_back.auth.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void login_shouldReturnOk() throws Exception {
        String requestJson = """
            {
              "username": "teacher10",
              "password": "teach10"
            }
            """;

        LoginResponse response = instantiateRecord(
            LoginResponse.class,
            Map.of(
                "userId", 10,
                "username", "teacher10",
                "fullName", "Преподаватель 10",
                "role", "teacher",
                "accessToken", "access-token",
                "refreshToken", "refresh-token",
                "tokenType", "Bearer"
            )
        );

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("teacher10"))
            .andExpect(jsonPath("$.role").value("teacher"))
            .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void registerRequest_shouldReturnOk() throws Exception {
        String requestJson = """
            {
              "username": "student01",
              "password": "pass123",
              "fullName": "Студент 1",
              "email": "student01@example.com",
              "desiredRole": "student"
            }
            """;

        RegisterResponse response = instantiateRecord(
            RegisterResponse.class,
            Map.of(
                "message", "Заявка на регистрацию отправлена администратору",
                "status", "pending"
            )
        );

        when(authService.createRegistrationRequest(any(RegisterRequestDto.class))).thenReturn(response);

        mockMvc.perform(
                post("/api/auth/register-request")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("pending"));
    }

    @Test
    void refresh_shouldReturnOk() throws Exception {
        RefreshRequest request = instantiateRecord(
            RefreshRequest.class,
            Map.of("refreshToken", "refresh-token")
        );

        LoginResponse response = instantiateRecord(
            LoginResponse.class,
            Map.of(
                "userId", 10,
                "username", "teacher10",
                "fullName", "Преподаватель 10",
                "role", "teacher",
                "accessToken", "new-access-token",
                "refreshToken", "new-refresh-token",
                "tokenType", "Bearer"
            )
        );

        when(refreshTokenService.refresh("refresh-token")).thenReturn(response);

        mockMvc.perform(
                post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("new-access-token"))
            .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    void logout_shouldReturnOk() throws Exception {
        String requestJson = """
            {
              "refreshToken": "refresh-token"
            }
            """;

        doNothing().when(refreshTokenService).revoke("refresh-token");

        mockMvc.perform(
                post("/api/auth/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
            .andExpect(status().isOk());
    }

    private static <T> T instantiateRecord(Class<T> type, Map<String, Object> values) {
        try {
            RecordComponent[] components = type.getRecordComponents();
            Class<?>[] parameterTypes = Arrays.stream(components)
                .map(RecordComponent::getType)
                .toArray(Class[]::new);

            Object[] args = Arrays.stream(components)
                .map(component -> values.get(component.getName()))
                .toArray();

            Constructor<T> constructor = type.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (Exception exception) {
            throw new RuntimeException("Не удалось создать " + type.getSimpleName(), exception);
        }
    }
}