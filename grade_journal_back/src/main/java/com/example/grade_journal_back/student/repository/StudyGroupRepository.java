package com.example.grade_journal_back.student.repository;

import com.example.grade_journal_back.student.entity.StudyGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Integer> {

    Optional<StudyGroup> findByGroupId(Integer groupId);

    Optional<StudyGroup> findFirstByOrderByGroupIdAsc();
}