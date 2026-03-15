package com.example.grade_journal_back.registration.service;

import com.example.grade_journal_back.common.exception.NotFoundException;
import com.example.grade_journal_back.student.entity.Student;
import com.example.grade_journal_back.student.entity.StudyGroup;
import com.example.grade_journal_back.student.repository.StudentRepository;
import com.example.grade_journal_back.student.repository.StudyGroupRepository;
import com.example.grade_journal_back.user.entity.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StudentProvisioner implements UserProvisioner {

    private final StudyGroupRepository studyGroupRepository;
    private final StudentRepository studentRepository;

    @Override
    public String supportedRole() {
        return "student";
    }

    @Override
    @Transactional
    public void provision(UserAccount userAccount) {
        StudyGroup studyGroup = studyGroupRepository.findFirstByOrderByGroupIdAsc()
            .orElseThrow(() -> new NotFoundException("В базе нет учебных групп"));

        long nextNumber = studentRepository.countByGroupGroupId(studyGroup.getGroupId()) + 1;
        String studentCard = studyGroup.getGroupCode() + String.format("%02d", nextNumber);

        Student student = Student.builder()
            .userAccount(userAccount)
            .group(studyGroup)
            .studentCard(studentCard)
            .build();

        studentRepository.save(student);
    }
}