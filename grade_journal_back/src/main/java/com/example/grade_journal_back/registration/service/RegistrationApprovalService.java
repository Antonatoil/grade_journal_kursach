package com.example.grade_journal_back.registration.service;

import com.example.grade_journal_back.admin.dto.AdminActionResponse;
import com.example.grade_journal_back.admin.dto.PendingRegistrationDto;
import com.example.grade_journal_back.common.exception.BadRequestException;
import com.example.grade_journal_back.common.exception.NotFoundException;
import com.example.grade_journal_back.registration.entity.RegistrationRequest;
import com.example.grade_journal_back.registration.repository.RegistrationRequestRepository;
import com.example.grade_journal_back.user.entity.Role;
import com.example.grade_journal_back.user.entity.UserAccount;
import com.example.grade_journal_back.user.repository.RoleRepository;
import com.example.grade_journal_back.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationApprovalService {

    private final RegistrationRequestRepository registrationRequestRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<PendingRegistrationDto> getPendingRequests() {
        return registrationRequestRepository.findAllByStatusOrderByCreatedAtAsc("pending")
            .stream()
            .map(request -> new PendingRegistrationDto(
                request.getRequestId(),
                request.getDesiredRole(),
                request.getUsername(),
                request.getFullName(),
                request.getEmail(),
                request.getStatus(),
                request.getCreatedAt()
            ))
            .toList();
    }

    @Transactional
    public AdminActionResponse approve(Integer requestId, String adminUsername) {
        RegistrationRequest request = registrationRequestRepository.findById(requestId)
            .orElseThrow(() -> new NotFoundException("Заявка не найдена"));

        if (!"pending".equals(request.getStatus())) {
            throw new BadRequestException("Заявка уже обработана");
        }

        if (userAccountRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Пользователь с таким логином уже существует");
        }

        if (request.getEmail() != null && userAccountRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException("Пользователь с таким email уже существует");
        }

        Role role = roleRepository.findByRoleCode(request.getDesiredRole())
            .orElseThrow(() -> new NotFoundException("Роль не найдена: " + request.getDesiredRole()));

        UserAccount admin = userAccountRepository.findByUsername(adminUsername)
            .orElseThrow(() -> new NotFoundException("Администратор не найден"));

        UserAccount userAccount = UserAccount.builder()
            .role(role)
            .username(request.getUsername())
            .passwordHash(request.getPasswordHash())
            .fullName(request.getFullName())
            .email(request.getEmail())
            .active(true)
            .approved(true)
            .createdAt(Instant.now())
            .build();

        userAccountRepository.save(userAccount);

        request.setStatus("approved");
        request.setApprovedBy(admin);
        request.setApprovedAt(Instant.now());

        return new AdminActionResponse(
            request.getRequestId(),
            request.getStatus(),
            "Заявка одобрена. Теперь при необходимости можно заполнить профиль в панели администратора."
        );
    }

    @Transactional
    public AdminActionResponse reject(Integer requestId, String adminUsername) {
        RegistrationRequest request = registrationRequestRepository.findById(requestId)
            .orElseThrow(() -> new NotFoundException("Заявка не найдена"));

        if (!"pending".equals(request.getStatus())) {
            throw new BadRequestException("Заявка уже обработана");
        }

        UserAccount admin = userAccountRepository.findByUsername(adminUsername)
            .orElseThrow(() -> new NotFoundException("Администратор не найден"));

        request.setStatus("rejected");
        request.setApprovedBy(admin);
        request.setApprovedAt(Instant.now());

        return new AdminActionResponse(
            request.getRequestId(),
            request.getStatus(),
            "Заявка отклонена"
        );
    }
}