package com.example.grade_journal_back.user.repository;

import com.example.grade_journal_back.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByRoleCode(String roleCode);
}