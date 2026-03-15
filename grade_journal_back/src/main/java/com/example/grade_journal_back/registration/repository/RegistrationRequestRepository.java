package com.example.grade_journal_back.registration.repository;

import com.example.grade_journal_back.registration.entity.RegistrationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, Integer> {

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    List<RegistrationRequest> findAllByStatusOrderByCreatedAtAsc(String status);
}