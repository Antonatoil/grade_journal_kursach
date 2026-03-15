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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register-request")
    public RegisterResponse registerRequest(@Valid @RequestBody RegisterRequestDto request) {
        return authService.createRegistrationRequest(request);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return refreshTokenService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }
}