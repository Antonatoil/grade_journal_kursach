package com.example.grade_journal_back.performance.service;

import com.example.grade_journal_back.common.exception.BadRequestException;
import com.example.grade_journal_back.common.exception.ForbiddenException;
import com.example.grade_journal_back.common.exception.NotFoundException;
import com.example.grade_journal_back.performance.dto.*;
import com.example.grade_journal_back.student.entity.Student;
import com.example.grade_journal_back.student.repository.StudentRepository;
import com.example.grade_journal_back.user.entity.UserAccount;
import com.example.grade_journal_back.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final JdbcTemplate jdbcTemplate;
    private final UserAccountRepository userAccountRepository;
    private final StudentRepository studentRepository;

    public PerformanceMetaResponse getMeta(String username) {
        AccessContext context = resolveAccessContext(username);

        List<PerformanceStudentOptionDto> students = loadAllowedStudents(context);
        List<PerformanceCourseOptionDto> courses = students.isEmpty()
            ? List.of()
            : loadAllowedCourses(context, students.get(0).studentId());

        return new PerformanceMetaResponse(students, courses);
    }

    public List<PerformanceCourseOptionDto> getCoursesForStudent(String username, Integer studentId) {
        if (studentId == null) {
            throw new BadRequestException("Необходимо выбрать студента");
        }

        AccessContext context = resolveAccessContext(username);
        Integer allowedStudentId = ensureStudentAccess(context, studentId);

        return loadAllowedCourses(context, allowedStudentId);
    }

    public PerformanceResponseDto getPerformance(String username, Integer studentId, Integer courseId) {
        if (studentId == null || courseId == null) {
            throw new BadRequestException("Необходимо выбрать студента и предмет");
        }

        AccessContext context = resolveAccessContext(username);
        Integer allowedStudentId = ensureStudentAccess(context, studentId);

        EnrollmentRow enrollment = findEnrollment(allowedStudentId, courseId);
        if (enrollment == null) {
            throw new NotFoundException("Для выбранного студента нет данных по выбранному предмету");
        }

        List<PerformanceGradePointDto> grades = jdbcTemplate.query(
            """
            select g.grade_id,
                   g.graded_at::date as graded_date,
                   g.grade_value,
                   atp.type_name,
                   ai.title
            from grade g
            join assessment_item ai on ai.assessment_item_id = g.assessment_item_id
            join assessment_type atp on atp.assessment_type_id = ai.assessment_type_id
            where g.enrollment_id = ?
            order by g.graded_at, g.grade_id
            """,
            (rs, rowNum) -> new PerformanceGradePointDto(
                rs.getInt("grade_id"),
                rs.getObject("graded_date", LocalDate.class),
                rs.getDouble("grade_value"),
                rs.getString("type_name"),
                rs.getString("title"),
                rowNum + 1,
                false
            ),
            enrollment.enrollmentId()
        );

        Double averageAllSubjects = queryNullableDouble(
            """
            select round(avg(g.grade_value)::numeric, 2)
            from grade g
            join enrollment e on e.enrollment_id = g.enrollment_id
            where e.student_id = ?
            """,
            allowedStudentId
        );

        Double averageSelectedSubject = queryNullableDouble(
            """
            select round(avg(g.grade_value)::numeric, 2)
            from grade g
            where g.enrollment_id = ?
            """,
            enrollment.enrollmentId()
        );

        Double predictedFinal = queryNullableDouble(
            """
            select predicted_final_grade
            from performance_prediction
            where enrollment_id = ?
            order by generated_at desc
            limit 1
            """,
            enrollment.enrollmentId()
        );

        double subjectAverage = averageSelectedSubject != null ? averageSelectedSubject : 0.0;
        double predictedNextGrade = predictedFinal != null
            ? predictedFinal
            : (subjectAverage > 0 ? subjectAverage : 6.0);

        PerformanceSummaryDto summary = new PerformanceSummaryDto(
            allowedStudentId,
            enrollment.studentFullName(),
            enrollment.groupCode(),
            courseId,
            enrollment.courseName(),
            round2(averageAllSubjects != null ? averageAllSubjects : 0.0),
            round2(subjectAverage),
            round2(predictedNextGrade)
        );

        List<PerformanceGradePointDto> chartPoints = new ArrayList<>(grades);
        chartPoints.add(
            new PerformanceGradePointDto(
                -1,
                grades.isEmpty() ? LocalDate.now() : grades.get(grades.size() - 1).gradedDate(),
                round2(predictedNextGrade),
                "Прогноз",
                "Прогноз следующей оценки",
                grades.size() + 1,
                true
            )
        );

        return new PerformanceResponseDto(summary, grades, chartPoints);
    }

    private List<PerformanceStudentOptionDto> loadAllowedStudents(AccessContext context) {
        if ("student".equals(context.roleCode())) {
            return jdbcTemplate.query(
                """
                select s.student_id,
                       ua.full_name,
                       sg.group_code,
                       sg.course_no
                from student s
                join user_account ua on ua.user_account_id = s.user_account_id
                join study_group sg on sg.group_id = s.group_id
                where s.student_id = ?
                order by ua.full_name
                """,
                (rs, rowNum) -> new PerformanceStudentOptionDto(
                    rs.getInt("student_id"),
                    rs.getString("full_name"),
                    rs.getString("group_code"),
                    rs.getInt("course_no")
                ),
                context.studentId()
            );
        }

        return jdbcTemplate.query(
            """
            select s.student_id,
                   ua.full_name,
                   sg.group_code,
                   sg.course_no
            from student s
            join user_account ua on ua.user_account_id = s.user_account_id
            join study_group sg on sg.group_id = s.group_id
            order by sg.course_no, sg.group_code, ua.full_name
            """,
            (rs, rowNum) -> new PerformanceStudentOptionDto(
                rs.getInt("student_id"),
                rs.getString("full_name"),
                rs.getString("group_code"),
                rs.getInt("course_no")
            )
        );
    }

    private List<PerformanceCourseOptionDto> loadAllowedCourses(AccessContext context, Integer studentId) {
        Integer allowedStudentId = ensureStudentAccess(context, studentId);

        return jdbcTemplate.query(
            """
            select distinct c.course_id,
                            c.course_code,
                            c.course_name,
                            c.study_year
            from enrollment e
            join course_offering co on co.offering_id = e.offering_id
            join course c on c.course_id = co.course_id
            where e.student_id = ?
            order by c.study_year, c.course_name
            """,
            (rs, rowNum) -> new PerformanceCourseOptionDto(
                rs.getInt("course_id"),
                rs.getString("course_code"),
                rs.getString("course_name"),
                rs.getInt("study_year")
            ),
            allowedStudentId
        );
    }

    private EnrollmentRow findEnrollment(Integer studentId, Integer courseId) {
        List<EnrollmentRow> rows = jdbcTemplate.query(
            """
            select e.enrollment_id,
                   ua.full_name,
                   sg.group_code,
                   c.course_name
            from enrollment e
            join student s on s.student_id = e.student_id
            join user_account ua on ua.user_account_id = s.user_account_id
            join study_group sg on sg.group_id = s.group_id
            join course_offering co on co.offering_id = e.offering_id
            join course c on c.course_id = co.course_id
            join academic_term t on t.term_id = co.term_id
            where s.student_id = ?
              and c.course_id = ?
            order by t.is_current desc, t.end_date desc, co.offering_id desc
            limit 1
            """,
            (rs, rowNum) -> new EnrollmentRow(
                rs.getInt("enrollment_id"),
                rs.getString("full_name"),
                rs.getString("group_code"),
                rs.getString("course_name")
            ),
            studentId,
            courseId
        );

        return rows.isEmpty() ? null : rows.get(0);
    }

    private AccessContext resolveAccessContext(String username) {
        UserAccount user = userAccountRepository.findByUsername(username)
            .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        String roleCode = user.getRole().getRoleCode();
        Integer studentId = null;

        if ("student".equals(roleCode)) {
            Student student = studentRepository.findByUserAccountUserAccountId(user.getUserAccountId())
                .orElseThrow(() -> new ForbiddenException("Профиль студента еще не заполнен"));
            studentId = student.getStudentId();
        }

        return new AccessContext(user.getUserAccountId(), roleCode, studentId);
    }

    private Integer ensureStudentAccess(AccessContext context, Integer requestedStudentId) {
        if (requestedStudentId == null) {
            throw new BadRequestException("Необходимо выбрать студента");
        }

        if ("student".equals(context.roleCode())) {
            if (context.studentId() == null) {
                throw new ForbiddenException("Профиль студента еще не заполнен");
            }
            if (!context.studentId().equals(requestedStudentId)) {
                throw new ForbiddenException("Студент может просматривать только свои данные");
            }
        }

        return requestedStudentId;
    }

    private Double queryNullableDouble(String sql, Object... args) {
        List<Double> values = jdbcTemplate.query(sql, (rs, rowNum) -> {
            double value = rs.getDouble(1);
            return rs.wasNull() ? null : value;
        }, args);

        if (values.isEmpty()) {
            return null;
        }

        return values.get(0);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record EnrollmentRow(
        Integer enrollmentId,
        String studentFullName,
        String groupCode,
        String courseName
    ) {
    }

    private record AccessContext(
        Integer userId,
        String roleCode,
        Integer studentId
    ) {
    }
}