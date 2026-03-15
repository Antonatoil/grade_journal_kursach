package com.example.grade_journal_back.teacher.repository;

import com.example.grade_journal_back.teacher.entity.Teacher;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Integer> {

    @EntityGraph(attributePaths = {"department", "userAccount"})
    Optional<Teacher> findByUserAccountUserAccountId(Integer userAccountId);

    boolean existsByUserAccountUserAccountId(Integer userAccountId);
}