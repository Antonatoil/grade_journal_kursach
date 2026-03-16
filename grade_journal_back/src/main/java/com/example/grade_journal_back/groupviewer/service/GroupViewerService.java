package com.example.grade_journal_back.groupviewer.service;

import com.example.grade_journal_back.groupviewer.dto.GroupOptionResponse;
import com.example.grade_journal_back.groupviewer.dto.GroupStudentRowResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;

@Service
public class GroupViewerService {

    private final JdbcTemplate jdbcTemplate;

    public GroupViewerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Cacheable("groupViewerGroups")
    public List<GroupOptionResponse> getGroups() {
        return jdbcTemplate.query(
                """
                select sg.group_id,
                       sg.group_code,
                       sg.course_no,
                       sg.faculty_name,
                       sg.specialization_name
                from study_group sg
                order by sg.course_no, sg.group_code
                """,
                (rs, rowNum) -> new GroupOptionResponse(
                        rs.getInt("group_id"),
                        rs.getString("group_code"),
                        rs.getInt("course_no"),
                        rs.getString("faculty_name"),
                        rs.getString("specialization_name")
                )
        );
    }

    public List<GroupStudentRowResponse> getStudentsByGroup(Integer groupId) {
        return jdbcTemplate.query(
                """
                select s.student_id,
                       ua.username,
                       ua.full_name,
                       ua.email,
                       s.student_card,
                       sg.group_code,
                       sg.course_no,
                       ua.is_active,
                       ua.is_approved
                from student s
                join user_account ua on ua.user_account_id = s.user_account_id
                join study_group sg on sg.group_id = s.group_id
                where s.group_id = ?
                order by ua.full_name, s.student_card
                """,
                (rs, rowNum) -> new GroupStudentRowResponse(
                        rs.getInt("student_id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("student_card"),
                        rs.getString("group_code"),
                        rs.getInt("course_no"),
                        rs.getBoolean("is_active"),
                        rs.getBoolean("is_approved")
                ),
                groupId
        );
    }
}