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
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
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
        log.info(
                "Creating registration request for username='{}', desiredRole='{}'",
                request.username(),
                request.desiredRole()
        );

        validateDesiredRole(request.desiredRole());

        if (userAccountRepository.existsByUsername(request.username())
                || registrationRequestRepository.existsByUsername(request.username())) {
            log.warn("Registration request rejected: username='{}' already exists", request.username());
            throw new BadRequestException("Пользователь с таким логином уже существует");
        }

        if (StringUtils.hasText(request.email())) {
            if (userAccountRepository.existsByEmailIgnoreCase(request.email())
                    || registrationRequestRepository.existsByEmailIgnoreCase(request.email())) {
                log.warn("Registration request rejected: email already exists for username='{}'", request.username());
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

        log.info(
                "Registration request created successfully for username='{}' with status='{}'",
                request.username(),
                registrationRequest.getStatus()
        );

        return new RegisterResponse(
                "Заявка на регистрацию отправлена администратору",
                registrationRequest.getStatus()
        );
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for username='{}'", request.username());

        UserAccount userAccount = userAccountRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.warn("Login failed: username='{}' not found", request.username());
                    return new UnauthorizedException("Неверный логин или пароль");
                });

        if (!userAccount.isApproved()) {
            log.warn("Login denied: username='{}' is not approved", request.username());
            throw new UnauthorizedException("Аккаунт еще не одобрен администратором");
        }

        if (!userAccount.isActive()) {
            log.warn("Login denied: username='{}' is inactive", request.username());
            throw new UnauthorizedException("Аккаунт деактивирован");
        }

        if (!passwordEncoder.matches(request.password(), userAccount.getPasswordHash())) {
            log.warn("Login failed: invalid password for username='{}'", request.username());
            throw new UnauthorizedException("Неверный логин или пароль");
        }

        refreshTokenService.revokeAllActiveTokens(userAccount.getUserAccountId());

        String accessToken = jwtService.generateAccessToken(userAccount);
        String refreshToken = refreshTokenService.createRefreshToken(userAccount);

        log.info(
                "Login successful for username='{}', userId={}",
                userAccount.getUsername(),
                userAccount.getUserAccountId()
        );

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
            log.warn("Invalid desired role received: '{}'", desiredRole);
            throw new BadRequestException("Можно зарегистрироваться только как teacher или student");
        }
    }
}