package com.example.grade_journal_back.auth.service;

import com.example.grade_journal_back.auth.JwtService;
import com.example.grade_journal_back.auth.dto.LoginRequest;
import com.example.grade_journal_back.auth.dto.LoginResponse;
import com.example.grade_journal_back.auth.dto.RegisterRequestDto;
import com.example.grade_journal_back.common.exception.BadRequestException;
import com.example.grade_journal_back.common.exception.UnauthorizedException;
import com.example.grade_journal_back.registration.repository.RegistrationRequestRepository;
import com.example.grade_journal_back.user.entity.Role;
import com.example.grade_journal_back.user.entity.UserAccount;
import com.example.grade_journal_back.user.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private RegistrationRequestRepository registrationRequestRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_shouldReturnResponse_whenCredentialsValid() {
        UserAccount user = mock(UserAccount.class);
        Role role = mock(Role.class);

        when(userAccountRepository.findByUsername("teacher01")).thenReturn(Optional.of(user));
        when(user.isApproved()).thenReturn(true);
        when(user.isActive()).thenReturn(true);
        when(user.getPasswordHash()).thenReturn("encoded-password");
        when(passwordEncoder.matches("teach01", "encoded-password")).thenReturn(true);
        when(user.getUserAccountId()).thenReturn(10);
        when(user.getUsername()).thenReturn("teacher01");
        when(user.getFullName()).thenReturn("Преподаватель 1");
        when(user.getRole()).thenReturn(role);
        when(role.getRoleCode()).thenReturn("teacher");
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh-token");

        LoginResponse response = authService.login(new LoginRequest("teacher01", "teach01"));

        assertThat(response.userId()).isEqualTo(10);
        assertThat(response.username()).isEqualTo("teacher01");
        assertThat(response.fullName()).isEqualTo("Преподаватель 1");
        assertThat(response.role()).isEqualTo("teacher");
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");

        verify(refreshTokenService).revokeAllActiveTokens(10);
        verify(jwtService).generateAccessToken(user);
        verify(refreshTokenService).createRefreshToken(user);
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {
        when(userAccountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown", "123")))
            .isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    void login_shouldThrow_whenUserNotApproved() {
        UserAccount user = mock(UserAccount.class);

        when(userAccountRepository.findByUsername("teacher01")).thenReturn(Optional.of(user));
        when(user.isApproved()).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("teacher01", "teach01")))
            .isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    void login_shouldThrow_whenUserInactive() {
        UserAccount user = mock(UserAccount.class);

        when(userAccountRepository.findByUsername("teacher01")).thenReturn(Optional.of(user));
        when(user.isApproved()).thenReturn(true);
        when(user.isActive()).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("teacher01", "teach01")))
            .isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    void login_shouldThrow_whenPasswordInvalid() {
        UserAccount user = mock(UserAccount.class);

        when(userAccountRepository.findByUsername("teacher01")).thenReturn(Optional.of(user));
        when(user.isApproved()).thenReturn(true);
        when(user.isActive()).thenReturn(true);
        when(user.getPasswordHash()).thenReturn("encoded-password");
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("teacher01", "wrong-password")))
            .isInstanceOf(UnauthorizedException.class);

        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    void createRegistrationRequest_shouldThrow_whenUsernameAlreadyExists() {
        RegisterRequestDto request = new RegisterRequestDto(
            "teacher",
            "teacher01",
            "password123",
            "Преподаватель 1",
            "teacher01@ejournal.by"
        );

        when(userAccountRepository.existsByUsername("teacher01")).thenReturn(true);

        assertThatThrownBy(() -> authService.createRegistrationRequest(request))
            .isInstanceOf(BadRequestException.class);

        verify(registrationRequestRepository, never()).save(any());
    }

    @Test
    void createRegistrationRequest_shouldSaveRequest_whenDataValid() {
        RegisterRequestDto request = new RegisterRequestDto(
            "student",
            "new_student",
            "password123",
            "Новый Студент",
            "new@student.by"
        );

        when(userAccountRepository.existsByUsername("new_student")).thenReturn(false);
        when(registrationRequestRepository.existsByUsername("new_student")).thenReturn(false);
        when(userAccountRepository.existsByEmailIgnoreCase("new@student.by")).thenReturn(false);
        when(registrationRequestRepository.existsByEmailIgnoreCase("new@student.by")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");

        authService.createRegistrationRequest(request);

        verify(passwordEncoder).encode("password123");
        verify(registrationRequestRepository).save(any());

        ArgumentCaptor<com.example.grade_journal_back.registration.entity.RegistrationRequest> captor =
            ArgumentCaptor.forClass(com.example.grade_journal_back.registration.entity.RegistrationRequest.class);

        verify(registrationRequestRepository).save(captor.capture());

        assertThat(captor.getValue().getDesiredRole()).isEqualTo("student");
        assertThat(captor.getValue().getUsername()).isEqualTo("new_student");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded-password");
        assertThat(captor.getValue().getFullName()).isEqualTo("Новый Студент");
        assertThat(captor.getValue().getEmail()).isEqualTo("new@student.by");
        assertThat(captor.getValue().getStatus()).isEqualTo("pending");
    }
}