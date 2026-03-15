package com.example.grade_journal_back.analytics.service;

import com.example.grade_journal_back.analytics.dto.GroupComparisonDto;
import com.example.grade_journal_back.analytics.dto.RiskGroupStudentDto;
import com.example.grade_journal_back.analytics.dto.StudentComparisonDto;
import com.example.grade_journal_back.analytics.dto.TeacherLessonDto;
import com.example.grade_journal_back.analytics.dto.TeacherOptionDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TeacherOptionDto> getTeacherOptions() {
        String sql = """
                select
                    t.teacher_id,
                    ua.full_name,
                    d.department_name,
                    t.position
                from teacher t
                join user_account ua on ua.user_account_id = t.user_account_id
                join department d on d.department_id = t.department_id
                order by ua.full_name
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new TeacherOptionDto(
                rs.getInt("teacher_id"),
                rs.getString("full_name"),
                rs.getString("department_name"),
                rs.getString("position")
        ));
    }

    public List<TeacherLessonDto> getTeacherSchedule(Integer teacherId) {
        String sql = """
                select
                    se.lesson_date,
                    se.time_slot,
                    c.course_name,
                    sg.group_code,
                    se.room,
                    se.lesson_type,
                    se.topic
                from schedule_entry se
                join course_offering co on co.offering_id = se.offering_id
                join course c on c.course_id = co.course_id
                join study_group sg on sg.group_id = co.group_id
                where co.teacher_id = ?
                order by se.lesson_date, se.time_slot, sg.group_code
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new TeacherLessonDto(
                rs.getDate("lesson_date").toLocalDate(),
                rs.getString("time_slot"),
                rs.getString("course_name"),
                rs.getString("group_code"),
                rs.getString("room"),
                rs.getString("lesson_type"),
                rs.getString("topic")
        ), teacherId);
    }

    public List<GroupComparisonDto> getGroupComparison() {
        String sql = """
                select
                    sg.group_id,
                    sg.group_code,
                    sg.course_no,
                    count(distinct s.student_id) as student_count,
                    coalesce(round(avg(g.grade_value), 2), 0.00) as average_grade
                from study_group sg
                left join student s on s.group_id = sg.group_id
                left join enrollment e on e.student_id = s.student_id
                left join grade g on g.enrollment_id = e.enrollment_id
                group by sg.group_id, sg.group_code, sg.course_no
                order by average_grade desc, sg.group_code
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new GroupComparisonDto(
                rs.getInt("group_id"),
                rs.getString("group_code"),
                rs.getInt("course_no"),
                rs.getInt("student_count"),
                rs.getBigDecimal("average_grade")
        ));
    }

    public List<StudentComparisonDto> getStudentComparison(String sortDirection) {
        String direction = "asc".equalsIgnoreCase(sortDirection) ? "asc" : "desc";

        String sql = """
                select
                    s.student_id,
                    ua.full_name,
                    sg.group_code,
                    s.student_card,
                    coalesce(round(avg(g.grade_value), 2), 0.00) as average_grade
                from student s
                join user_account ua on ua.user_account_id = s.user_account_id
                join study_group sg on sg.group_id = s.group_id
                left join enrollment e on e.student_id = s.student_id
                left join grade g on g.enrollment_id = e.enrollment_id
                group by s.student_id, ua.full_name, sg.group_code, s.student_card
                order by average_grade
                """ + direction + """
                , ua.full_name
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new StudentComparisonDto(
                rs.getInt("student_id"),
                rs.getString("full_name"),
                rs.getString("group_code"),
                rs.getString("student_card"),
                rs.getBigDecimal("average_grade")
        ));
    }

    public List<RiskGroupStudentDto> getRiskGroups() {
        String sql = """
                with student_avg as (
                    select
                        s.student_id,
                        ua.full_name,
                        sg.group_code,
                        s.student_card,
                        coalesce(round(avg(g.grade_value), 2), 0.00) as average_grade
                    from student s
                    join user_account ua on ua.user_account_id = s.user_account_id
                    join study_group sg on sg.group_id = s.group_id
                    left join enrollment e on e.student_id = s.student_id
                    left join grade g on g.enrollment_id = e.enrollment_id
                    group by s.student_id, ua.full_name, sg.group_code, s.student_card
                )
                select
                    case
                        when average_grade > 7.5 then 'high'
                        when average_grade >= 6 and average_grade <= 7.5 then 'medium'
                        else 'low'
                    end as risk_band,
                    student_id,
                    full_name,
                    group_code,
                    student_card,
                    average_grade
                from student_avg
                order by
                    case
                        when average_grade > 7.5 then 1
                        when average_grade >= 6 and average_grade <= 7.5 then 2
                        else 3
                    end,
                    average_grade desc,
                    full_name
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new RiskGroupStudentDto(
                rs.getString("risk_band"),
                rs.getInt("student_id"),
                rs.getString("full_name"),
                rs.getString("group_code"),
                rs.getString("student_card"),
                rs.getBigDecimal("average_grade")
        ));
    }
}