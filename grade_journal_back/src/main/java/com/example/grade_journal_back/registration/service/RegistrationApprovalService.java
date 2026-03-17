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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationApprovalService {

    private final RegistrationRequestRepository registrationRequestRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<PendingRegistrationDto> getPendingRequests() {
        log.info("Loading pending registration requests");

        List<PendingRegistrationDto> result = registrationRequestRepository.findAllByStatusOrderByCreatedAtAsc("pending")
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

        log.info("Pending registration requests loaded: count={}", result.size());
        return result;
    }

    @Transactional
    public AdminActionResponse approve(Integer requestId, String adminUsername) {
        log.info(
                "Approving registration requestId={} by admin='{}'",
                requestId,
                adminUsername
        );

        RegistrationRequest request = registrationRequestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.warn("Registration approval failed: requestId={} not found", requestId);
                    return new NotFoundException("Заявка не найдена");
                });

        if (!"pending".equals(request.getStatus())) {
            log.warn(
                    "Registration approval rejected: requestId={} already processed with status='{}'",
                    requestId,
                    request.getStatus()
            );
            throw new BadRequestException("Заявка уже обработана");
        }

        if (userAccountRepository.existsByUsername(request.getUsername())) {
            log.warn(
                    "Registration approval rejected: username='{}' already exists",
                    request.getUsername()
            );
            throw new BadRequestException("Пользователь с таким логином уже существует");
        }

        if (request.getEmail() != null && userAccountRepository.existsByEmailIgnoreCase(request.getEmail())) {
            log.warn(
                    "Registration approval rejected: email already exists for requestId={}",
                    requestId
            );
            throw new BadRequestException("Пользователь с таким email уже существует");
        }

        Role role = roleRepository.findByRoleCode(request.getDesiredRole())
                .orElseThrow(() -> {
                    log.warn("Registration approval failed: role not found '{}'", request.getDesiredRole());
                    return new NotFoundException("Роль не найдена: " + request.getDesiredRole());
                });

        UserAccount admin = userAccountRepository.findByUsername(adminUsername)
                .orElseThrow(() -> {
                    log.warn("Registration approval failed: admin '{}' not found", adminUsername);
                    return new NotFoundException("Администратор не найден");
                });

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

        log.info(
                "Registration request approved successfully: requestId={}, created userId={}, username='{}'",
                request.getRequestId(),
                userAccount.getUserAccountId(),
                userAccount.getUsername()
        );

        return new AdminActionResponse(
                request.getRequestId(),
                request.getStatus(),
                "Заявка одобрена. Теперь при необходимости можно заполнить профиль в панели администратора."
        );
    }

    @Transactional
    public AdminActionResponse reject(Integer requestId, String adminUsername) {
        log.info(
                "Rejecting registration requestId={} by admin='{}'",
                requestId,
                adminUsername
        );

        RegistrationRequest request = registrationRequestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.warn("Registration rejection failed: requestId={} not found", requestId);
                    return new NotFoundException("Заявка не найдена");
                });

        if (!"pending".equals(request.getStatus())) {
            log.warn(
                    "Registration rejection rejected: requestId={} already processed with status='{}'",
                    requestId,
                    request.getStatus()
            );
            throw new BadRequestException("Заявка уже обработана");
        }

        UserAccount admin = userAccountRepository.findByUsername(adminUsername)
                .orElseThrow(() -> {
                    log.warn("Registration rejection failed: admin '{}' not found", adminUsername);
                    return new NotFoundException("Администратор не найден");
                });

        request.setStatus("rejected");
        request.setApprovedBy(admin);
        request.setApprovedAt(Instant.now());

        log.info(
                "Registration request rejected successfully: requestId={}, admin='{}'",
                request.getRequestId(),
                adminUsername
        );

        return new AdminActionResponse(
                request.getRequestId(),
                request.getStatus(),
                "Заявка отклонена"
        );
    }
}