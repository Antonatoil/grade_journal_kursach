package com.example.grade_journal_back.auth;

import com.example.grade_journal_back.auth.dto.LoginRequest;
import com.example.grade_journal_back.auth.dto.LoginResponse;
import com.example.grade_journal_back.auth.dto.RegisterResponse;
import com.example.grade_journal_back.auth.service.AuthService;
import com.example.grade_journal_back.auth.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @Test
    void login_shouldReturnTokens() throws Exception {
        LoginResponse response = new LoginResponse(
            10,
            "teacher01",
            "Преподаватель 1",
            "teacher",
            "access-token",
            "refresh-token",
            "Bearer"
        );

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "teacher01",
                      "password": "teach01"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(10))
            .andExpect(jsonPath("$.username").value("teacher01"))
            .andExpect(jsonPath("$.fullName").value("Преподаватель 1"))
            .andExpect(jsonPath("$.role").value("teacher"))
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"));

        ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(authService).login(captor.capture());

        assertThat(captor.getValue().username()).isEqualTo("teacher01");
        assertThat(captor.getValue().password()).isEqualTo("teach01");
    }

    @Test
    void login_shouldReturn400_whenBodyInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "",
                      "password": ""
                    }
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void registerRequest_shouldReturnResponse() throws Exception {
        RegisterResponse response = new RegisterResponse(
            "Заявка создана",
            "pending"
        );

        when(authService.createRegistrationRequest(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "new_student",
                      "password": "password123",
                      "fullName": "Новый Студент",
                      "email": "new@student.by",
                      "desiredRole": "student"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Заявка создана"))
            .andExpect(jsonPath("$.status").value("pending"));

        verify(authService).createRegistrationRequest(any());
    }

    @Test
    void refresh_shouldReturnNewTokens() throws Exception {
        LoginResponse response = new LoginResponse(
            1,
            "admin",
            "Администратор",
            "admin",
            "new-access",
            "new-refresh",
            "Bearer"
        );

        when(refreshTokenService.refresh("old-refresh")).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "old-refresh"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("new-access"))
            .andExpect(jsonPath("$.refreshToken").value("new-refresh"));

        verify(refreshTokenService).refresh("old-refresh");
    }

    @Test
    void logout_shouldReturn200() throws Exception {
        doNothing().when(refreshTokenService).revoke("refresh-token");

        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "refresh-token"
                    }
                    """))
            .andExpect(status().isOk());

        verify(refreshTokenService).revoke("refresh-token");
    }
}