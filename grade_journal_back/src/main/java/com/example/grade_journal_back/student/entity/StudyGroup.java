package com.example.grade_journal_back.student.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "study_group")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Integer groupId;

    @Column(name = "group_code", nullable = false, unique = true, length = 20)
    private String groupCode;

    @Column(name = "course_no", nullable = false)
    private Short courseNo;

    @Column(name = "admission_year", nullable = false)
    private Short admissionYear;

    @Column(name = "faculty_name", nullable = false, length = 150)
    private String facultyName;

    @Column(name = "specialization_name", nullable = false, length = 150)
    private String specializationName;
}