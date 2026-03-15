package com.example.grade_journal_back.registration.service;

import com.example.grade_journal_back.common.exception.NotFoundException;
import com.example.grade_journal_back.teacher.entity.Department;
import com.example.grade_journal_back.teacher.entity.Teacher;
import com.example.grade_journal_back.teacher.repository.DepartmentRepository;
import com.example.grade_journal_back.teacher.repository.TeacherRepository;
import com.example.grade_journal_back.user.entity.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TeacherProvisioner implements UserProvisioner {

    private final DepartmentRepository departmentRepository;
    private final TeacherRepository teacherRepository;

    @Override
    public String supportedRole() {
        return "teacher";
    }

    @Override
    @Transactional
    public void provision(UserAccount userAccount) {
        Department department = departmentRepository.findFirstByOrderByDepartmentIdAsc()
            .orElseThrow(() -> new NotFoundException("В базе нет кафедр"));

        Teacher teacher = Teacher.builder()
            .userAccount(userAccount)
            .department(department)
            .position("Преподаватель")
            .phone(null)
            .build();

        teacherRepository.save(teacher);
    }
}