package com.example.grade_journal_back.teachergrading.service;

import com.example.grade_journal_back.teachergrading.dto.SaveTeacherLessonRequest;
import com.example.grade_journal_back.teachergrading.dto.TeacherGroupOptionDto;
import com.example.grade_journal_back.teachergrading.dto.TeacherLessonOptionDto;
import com.example.grade_journal_back.teachergrading.dto.TeacherLessonStudentDto;
import com.example.grade_journal_back.teachergrading.dto.TeacherLessonStudentUpdateDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class TeacherGradingService {

    private final JdbcTemplate jdbcTemplate;

    public TeacherGradingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TeacherGroupOptionDto> getMyGroups() {
        Integer teacherId = resolveCurrentTeacherId();

        String sql = """
                select distinct
                    sg.group_id,
                    sg.group_code,
                    sg.course_no
                from course_offering co
                join study_group sg on sg.group_id = co.group_id
                where co.teacher_id = ?
                order by sg.course_no, sg.group_code
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new TeacherGroupOptionDto(
                rs.getInt("group_id"),
                rs.getString("group_code"),
                rs.getInt("course_no")
        ), teacherId);
    }

    public List<TeacherLessonOptionDto> getMyLessons(Integer groupId) {
        Integer teacherId = resolveCurrentTeacherId();

        String sql = """
                select
                    se.schedule_id,
                    se.lesson_date,
                    se.time_slot,
                    c.course_name,
                    sg.group_code,
                    se.topic
                from schedule_entry se
                join course_offering co on co.offering_id = se.offering_id
                join course c on c.course_id = co.course_id
                join study_group sg on sg.group_id = co.group_id
                where co.teacher_id = ?
                  and co.group_id = ?
                order by se.lesson_date, se.time_slot
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new TeacherLessonOptionDto(
                rs.getInt("schedule_id"),
                rs.getDate("lesson_date").toLocalDate(),
                rs.getString("time_slot"),
                rs.getString("course_name"),
                rs.getString("group_code"),
                rs.getString("topic")
        ), teacherId, groupId);
    }

    public List<TeacherLessonStudentDto> getLessonStudents(Integer scheduleId) {
        Integer teacherId = resolveCurrentTeacherId();
        validateTeacherOwnsSchedule(teacherId, scheduleId);

        Integer assessmentItemId = findAssessmentItemIdBySchedule(scheduleId);

        String sql = """
                select
                    s.student_id,
                    e.enrollment_id,
                    ua.full_name,
                    s.student_card,
                    a.is_present,
                    e.missed_hours,
                    g.grade_value,
                    g.teacher_comment
                from schedule_entry se
                join course_offering co on co.offering_id = se.offering_id
                join enrollment e on e.offering_id = co.offering_id
                join student s on s.student_id = e.student_id
                join user_account ua on ua.user_account_id = s.user_account_id
                left join attendance a
                    on a.enrollment_id = e.enrollment_id
                   and a.schedule_id = se.schedule_id
                left join grade g
                    on g.enrollment_id = e.enrollment_id
                   and (? is not null and g.assessment_item_id = ?)
                where se.schedule_id = ?
                  and co.teacher_id = ?
                order by ua.full_name
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new TeacherLessonStudentDto(
                rs.getInt("student_id"),
                rs.getInt("enrollment_id"),
                rs.getString("full_name"),
                rs.getString("student_card"),
                rs.getObject("is_present") == null ? Boolean.FALSE : rs.getBoolean("is_present"),
                rs.getInt("missed_hours"),
                rs.getBigDecimal("grade_value"),
                rs.getString("teacher_comment")
        ), assessmentItemId, assessmentItemId, scheduleId, teacherId);
    }

    @Transactional
    public void saveLesson(Integer scheduleId, SaveTeacherLessonRequest request) {
        Integer teacherId = resolveCurrentTeacherId();
        validateTeacherOwnsSchedule(teacherId, scheduleId);

        Integer assessmentItemId = ensureAssessmentItem(scheduleId);

        if (request == null || request.students() == null) {
            return;
        }

        for (TeacherLessonStudentUpdateDto row : request.students()) {
            jdbcTemplate.update("""
                    insert into attendance (enrollment_id, schedule_id, is_present, note)
                    values (?, ?, ?, null)
                    on conflict (enrollment_id, schedule_id)
                    do update set is_present = excluded.is_present
                    """,
                    row.enrollmentId(),
                    scheduleId,
                    row.present() != null ? row.present() : false
            );

            BigDecimal normalizedGrade = normalizeGrade(row.gradeValue());

            if (normalizedGrade != null) {
                jdbcTemplate.update("""
                        insert into grade (enrollment_id, assessment_item_id, grade_value, teacher_comment, graded_at)
                        values (?, ?, ?, ?, now())
                        on conflict (enrollment_id, assessment_item_id)
                        do update set
                            grade_value = excluded.grade_value,
                            teacher_comment = excluded.teacher_comment,
                            graded_at = now()
                        """,
                        row.enrollmentId(),
                        assessmentItemId,
                        normalizedGrade,
                        row.teacherComment()
                );
            } else {
                jdbcTemplate.update("""
                        delete from grade
                        where enrollment_id = ?
                          and assessment_item_id = ?
                        """,
                        row.enrollmentId(),
                        assessmentItemId
                );
            }
        }
    }

    private BigDecimal normalizeGrade(BigDecimal value) {
        if (value == null) {
            return null;
        }

        BigDecimal rounded = value.setScale(0, RoundingMode.HALF_UP);

        if (rounded.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        if (rounded.compareTo(BigDecimal.TEN) > 0) {
            return BigDecimal.TEN;
        }

        return rounded;
    }
    private Integer resolveCurrentTeacherId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        Integer teacherId = jdbcTemplate.query("""
                select t.teacher_id
                from teacher t
                join user_account ua on ua.user_account_id = t.user_account_id
                where ua.username = ?
                """, rs -> rs.next() ? rs.getInt("teacher_id") : null, username);

        if (teacherId == null) {
            throw new IllegalStateException("Текущий пользователь не является преподавателем.");
        }

        return teacherId;
    }

    private void validateTeacherOwnsSchedule(Integer teacherId, Integer scheduleId) {
        Integer count = jdbcTemplate.query("""
                select count(*)
                from schedule_entry se
                join course_offering co on co.offering_id = se.offering_id
                where se.schedule_id = ?
                  and co.teacher_id = ?
                """, rs -> rs.next() ? rs.getInt(1) : 0, scheduleId, teacherId);

        if (count == null || count == 0) {
            throw new IllegalStateException("У преподавателя нет доступа к выбранной паре.");
        }
    }

    private Integer findAssessmentItemIdBySchedule(Integer scheduleId) {
        return jdbcTemplate.query("""
                select ai.assessment_item_id
                from assessment_item ai
                where ai.schedule_id = ?
                order by ai.assessment_item_id
                limit 1
                """, rs -> rs.next() ? rs.getInt("assessment_item_id") : null, scheduleId);
    }

    private Integer ensureAssessmentItem(Integer scheduleId) {
        Integer existing = findAssessmentItemIdBySchedule(scheduleId);
        if (existing != null) {
            return existing;
        }

        Integer offeringId = jdbcTemplate.query("""
                select offering_id
                from schedule_entry
                where schedule_id = ?
                """, rs -> rs.next() ? rs.getInt("offering_id") : null, scheduleId);

        if (offeringId == null) {
            throw new IllegalStateException("Не найдена учебная пара.");
        }

        Integer assessmentTypeId = jdbcTemplate.query("""
                select assessment_type_id
                from assessment_type
                where type_code = 'quiz'
                limit 1
                """, rs -> rs.next() ? rs.getInt("assessment_type_id") : null);

        if (assessmentTypeId == null) {
            throw new IllegalStateException("Не найден тип оценивания quiz.");
        }

        Integer generatedId = jdbcTemplate.query("""
                insert into assessment_item (
                    offering_id,
                    assessment_type_id,
                    title,
                    max_score,
                    weight,
                    due_date,
                    schedule_id,
                    is_required
                )
                select
                    se.offering_id,
                    ?,
                    'Оценивание за занятие',
                    10.00,
                    0.05,
                    se.lesson_date,
                    se.schedule_id,
                    false
                from schedule_entry se
                where se.schedule_id = ?
                returning assessment_item_id
                """, rs -> rs.next() ? rs.getInt("assessment_item_id") : null, assessmentTypeId, scheduleId);

        if (generatedId == null) {
            throw new IllegalStateException("Не удалось создать контрольную точку для занятия.");
        }

        return generatedId;
    }
}