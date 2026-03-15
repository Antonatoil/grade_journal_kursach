package com.example.grade_journal_back.user.repository;

import com.example.grade_journal_back.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    Optional<RefreshToken> findByTokenValue(String tokenValue);

    List<RefreshToken> findAllByUserAccountUserAccountIdAndRevokedAtIsNull(Integer userId);
}