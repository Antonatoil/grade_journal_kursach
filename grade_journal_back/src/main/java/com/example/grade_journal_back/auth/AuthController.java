package com.example.grade_journal_back.auth;

import com.example.grade_journal_back.auth.dto.LoginRequest;
import com.example.grade_journal_back.auth.dto.LoginResponse;
import com.example.grade_journal_back.auth.dto.LogoutRequest;
import com.example.grade_journal_back.auth.dto.RefreshRequest;
import com.example.grade_journal_back.auth.dto.RegisterRequestDto;
import com.example.grade_journal_back.auth.dto.RegisterResponse;
import com.example.grade_journal_back.auth.service.AuthService;
import com.example.grade_journal_back.auth.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        log.info("Received login request for username='{}'", request.username());

        LoginResponse response = authService.login(request);

        log.info("Login request processed successfully for username='{}'", request.username());
        return response;
    }

    @PostMapping("/register-request")
    public RegisterResponse registerRequest(@Valid @RequestBody RegisterRequestDto request) {
        log.info(
                "Received registration request for username='{}', desiredRole='{}'",
                request.username(),
                request.desiredRole()
        );

        RegisterResponse response = authService.createRegistrationRequest(request);

        log.info("Registration request processed successfully for username='{}'", request.username());
        return response;
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        log.info("Received refresh token request");

        LoginResponse response = refreshTokenService.refresh(request.refreshToken());

        log.info("Refresh token request processed successfully");
        return response;
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody LogoutRequest request) {
        log.info("Received logout request");

        refreshTokenService.revoke(request.refreshToken());

        log.info("Logout request processed successfully");
    }
}