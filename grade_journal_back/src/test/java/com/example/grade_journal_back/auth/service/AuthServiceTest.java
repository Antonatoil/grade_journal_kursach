package com.example.grade_journal_back.auth.service;

import com.example.grade_journal_back.auth.JwtService;
import com.example.grade_journal_back.auth.dto.LoginRequest;
import com.example.grade_journal_back.auth.dto.LoginResponse;
import com.example.grade_journal_back.auth.dto.RegisterRequestDto;
import com.example.grade_journal_back.auth.dto.RegisterResponse;
import com.example.grade_journal_back.common.exception.BadRequestException;
import com.example.grade_journal_back.common.exception.UnauthorizedException;
import com.example.grade_journal_back.registration.entity.RegistrationRequest;
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

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
    void login_shouldReturnTokens_whenCredentialsValid() {
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

        LoginRequest request = new LoginRequest("teacher10", "teach10");

        when(userAccountRepository.findByUsername("teacher10")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("teach10", "encoded-password")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh-token");

        LoginResponse response = authService.login(request);

        assertEquals("teacher10", response.username());
        assertEquals("teacher", response.role());
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());

        verify(refreshTokenService).revokeAllActiveTokens(10);
    }

    @Test
    void login_shouldThrow_whenUserNotApproved() {
        Role role = mock(Role.class);

        UserAccount user = UserAccount.builder()
            .userAccountId(10)
            .role(role)
            .username("teacher10")
            .passwordHash("encoded-password")
            .fullName("Преподаватель 10")
            .email("teacher10@example.com")
            .active(true)
            .approved(false)
            .createdAt(Instant.now())
            .build();

        LoginRequest request = new LoginRequest("teacher10", "teach10");

        when(userAccountRepository.findByUsername("teacher10")).thenReturn(Optional.of(user));

        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> authService.login(request)
        );

        assertEquals("Аккаунт еще не одобрен администратором", exception.getMessage());

        verify(userAccountRepository).findByUsername("teacher10");
        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    void createRegistrationRequest_shouldSaveRequest_whenDataValid() {
        RegisterRequestDto request = new RegisterRequestDto(
            "newuser",
            "password123",
            "Новый Пользователь",
            "newuser@example.com",
            "teacher"
        );

        when(userAccountRepository.existsByUsername("newuser")).thenReturn(false);
        when(registrationRequestRepository.existsByUsername("newuser")).thenReturn(false);
        when(userAccountRepository.existsByEmailIgnoreCase("newuser@example.com")).thenReturn(false);
        when(registrationRequestRepository.existsByEmailIgnoreCase("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");

        RegisterResponse response = authService.createRegistrationRequest(request);

        assertEquals("pending", response.status());

        ArgumentCaptor<RegistrationRequest> captor = ArgumentCaptor.forClass(RegistrationRequest.class);
        verify(registrationRequestRepository).save(captor.capture());

        RegistrationRequest saved = captor.getValue();
        assertEquals("newuser", saved.getUsername());
        assertEquals("Новый Пользователь", saved.getFullName());
        assertEquals("newuser@example.com", saved.getEmail());
        assertEquals("teacher", saved.getDesiredRole());
        assertEquals("encoded-password", saved.getPasswordHash());
        assertEquals("pending", saved.getStatus());
    }

    @Test
    void createRegistrationRequest_shouldThrow_whenUsernameAlreadyExists() {
        RegisterRequestDto request = new RegisterRequestDto(
            "existingUser",
            "password123",
            "Существующий Пользователь",
            "existing@example.com",
            "teacher"
        );

        when(userAccountRepository.existsByUsername("existingUser")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.createRegistrationRequest(request));

        verify(registrationRequestRepository, never()).save(any());
    }
}