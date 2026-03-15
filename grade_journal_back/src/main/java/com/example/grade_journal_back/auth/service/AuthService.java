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
import com.example.grade_journal_back.user.entity.UserAccount;
import com.example.grade_journal_back.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final RegistrationRequestRepository registrationRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public RegisterResponse createRegistrationRequest(RegisterRequestDto request) {
        validateDesiredRole(request.desiredRole());

        if (userAccountRepository.existsByUsername(request.username())
            || registrationRequestRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Пользователь с таким логином уже существует");
        }

        if (StringUtils.hasText(request.email())) {
            if (userAccountRepository.existsByEmailIgnoreCase(request.email())
                || registrationRequestRepository.existsByEmailIgnoreCase(request.email())) {
                throw new BadRequestException("Пользователь с таким email уже существует");
            }
        }

        RegistrationRequest registrationRequest = RegistrationRequest.builder()
            .desiredRole(request.desiredRole())
            .username(request.username())
            .passwordHash(passwordEncoder.encode(request.password()))
            .fullName(request.fullName())
            .email(StringUtils.hasText(request.email()) ? request.email() : null)
            .status("pending")
            .createdAt(Instant.now())
            .build();

        registrationRequestRepository.save(registrationRequest);

        return new RegisterResponse(
            "Заявка на регистрацию отправлена администратору",
            registrationRequest.getStatus()
        );
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        UserAccount userAccount = userAccountRepository.findByUsername(request.username())
            .orElseThrow(() -> new UnauthorizedException("Неверный логин или пароль"));

        if (!userAccount.isApproved()) {
            throw new UnauthorizedException("Аккаунт еще не одобрен администратором");
        }

        if (!userAccount.isActive()) {
            throw new UnauthorizedException("Аккаунт деактивирован");
        }

        if (!passwordEncoder.matches(request.password(), userAccount.getPasswordHash())) {
            throw new UnauthorizedException("Неверный логин или пароль");
        }

        refreshTokenService.revokeAllActiveTokens(userAccount.getUserAccountId());

        String accessToken = jwtService.generateAccessToken(userAccount);
        String refreshToken = refreshTokenService.createRefreshToken(userAccount);

        return new LoginResponse(
            userAccount.getUserAccountId(),
            userAccount.getUsername(),
            userAccount.getFullName(),
            userAccount.getRole().getRoleCode(),
            accessToken,
            refreshToken,
            "Bearer"
        );
    }

    private void validateDesiredRole(String desiredRole) {
        if (!"teacher".equals(desiredRole) && !"student".equals(desiredRole)) {
            throw new BadRequestException("Можно зарегистрироваться только как teacher или student");
        }
    }
}