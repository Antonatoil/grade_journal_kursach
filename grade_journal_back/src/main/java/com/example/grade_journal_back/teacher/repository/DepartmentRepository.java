package com.example.grade_journal_back.teacher.repository;

import com.example.grade_journal_back.teacher.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    Optional<Department> findByDepartmentId(Integer departmentId);

    Optional<Department> findFirstByOrderByDepartmentIdAsc();
}