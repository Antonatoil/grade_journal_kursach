package com.example.grade_journal_back.user.repository;

import com.example.grade_journal_back.user.entity.UserAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Integer> {

    @EntityGraph(attributePaths = "role")
    Optional<UserAccount> findByUsername(String username);

    @EntityGraph(attributePaths = "role")
    Optional<UserAccount> findByUserAccountId(Integer userAccountId);

    @EntityGraph(attributePaths = "role")
    List<UserAccount> findAllByApprovedTrueAndRoleRoleCodeInOrderByCreatedAtAsc(List<String> roleCodes);

    @EntityGraph(attributePaths = "role")
    List<UserAccount> findAllByOrderByCreatedAtDesc();

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);
}