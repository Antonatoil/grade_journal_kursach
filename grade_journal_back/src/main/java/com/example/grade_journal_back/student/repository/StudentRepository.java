package com.example.grade_journal_back.student.repository;

import com.example.grade_journal_back.student.entity.Student;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Integer> {

    @EntityGraph(attributePaths = {"group", "userAccount"})
    Optional<Student> findByUserAccountUserAccountId(Integer userAccountId);

    boolean existsByUserAccountUserAccountId(Integer userAccountId);

    boolean existsByStudentCard(String studentCard);

    long countByGroupGroupId(Integer groupId);
}