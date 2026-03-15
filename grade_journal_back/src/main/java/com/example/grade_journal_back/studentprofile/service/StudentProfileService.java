package com.example.grade_journal_back.studentprofile.service;

import com.example.grade_journal_back.common.exception.ForbiddenException;
import com.example.grade_journal_back.common.exception.NotFoundException;
import com.example.grade_journal_back.student.entity.Student;
import com.example.grade_journal_back.student.repository.StudentRepository;
import com.example.grade_journal_back.studentprofile.dto.StudentProfileCourseDto;
import com.example.grade_journal_back.studentprofile.dto.StudentProfileGradeDto;
import com.example.grade_journal_back.studentprofile.dto.StudentProfileOptionDto;
import com.example.grade_journal_back.studentprofile.dto.StudentProfileResponseDto;
import com.example.grade_journal_back.user.entity.UserAccount;
import com.example.grade_journal_back.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentProfileService {

    private final JdbcTemplate jdbcTemplate;
    private final UserAccountRepository userAccountRepository;
    private final StudentRepository studentRepository;

    public List<StudentProfileOptionDto> getOptions(String username, String query) {
        AccessContext context = resolveAccessContext(username);

        String normalized = query == null ? "" : query.trim().toLowerCase();
        String likePattern = "%" + normalized + "%";

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
                (rs, rowNum) -> new StudentProfileOptionDto(
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
            where ? = ''
               or lower(ua.full_name) like ?
               or lower(coalesce(ua.email, '')) like ?
               or lower(sg.group_code) like ?
               or lower(coalesce(s.student_card, '')) like ?
            order by sg.course_no, sg.group_code, ua.full_name
            """,
            (rs, rowNum) -> new StudentProfileOptionDto(
                rs.getInt("student_id"),
                rs.getString("full_name"),
                rs.getString("group_code"),
                rs.getInt("course_no")
            ),
            normalized,
            likePattern,
            likePattern,
            likePattern,
            likePattern
        );
    }

    public StudentProfileResponseDto getProfile(String username, Integer studentId) {
        AccessContext context = resolveAccessContext(username);
        Integer allowedStudentId = ensureStudentAccess(context, studentId);

        List<StudentProfileResponseDto> headers = jdbcTemplate.query(
            """
            select ua.user_account_id,
                   ua.username,
                   ua.full_name,
                   ua.email,
                   ua.is_active,
                   ua.is_approved,
                   s.student_id,
                   s.student_card,
                   sg.group_code,
                   sg.course_no,
                   sg.faculty_name,
                   sg.specialization_name
            from student s
            join user_account ua on ua.user_account_id = s.user_account_id
            join study_group sg on sg.group_id = s.group_id
            where s.student_id = ?
            """,
            (rs, rowNum) -> new StudentProfileResponseDto(
                rs.getInt("user_account_id"),
                rs.getString("username"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getBoolean("is_active"),
                rs.getBoolean("is_approved"),
                rs.getInt("student_id"),
                rs.getString("student_card"),
                rs.getString("group_code"),
                rs.getInt("course_no"),
                rs.getString("faculty_name"),
                rs.getString("specialization_name"),
                List.of()
            ),
            allowedStudentId
        );

        if (headers.isEmpty()) {
            throw new NotFoundException("Студент не найден");
        }

        StudentProfileResponseDto header = headers.get(0);

        List<CourseRow> courseRows = jdbcTemplate.query(
            """
            select e.enrollment_id,
                   c.course_id,
                   c.course_code,
                   c.course_name,
                   round(coalesce(avg(g.grade_value), 0)::numeric, 2) as average_grade,
                   round(coalesce(pp.predicted_final_grade, coalesce(avg(g.grade_value), 0), 0)::numeric, 2) as predicted_grade,
                   coalesce(pp.attendance_rate, 100.00) as attendance_rate,
                   coalesce(pp.missed_hours, 0) as missed_hours,
                   coalesce(pp.risk_level, 'low') as risk_level
            from enrollment e
            join course_offering co on co.offering_id = e.offering_id
            join course c on c.course_id = co.course_id
            left join grade g on g.enrollment_id = e.enrollment_id
            left join lateral (
                select predicted_final_grade, attendance_rate, missed_hours, risk_level
                from performance_prediction pp
                where pp.enrollment_id = e.enrollment_id
                order by pp.generated_at desc
                limit 1
            ) pp on true
            where e.student_id = ?
            group by e.enrollment_id, c.course_id, c.course_code, c.course_name, pp.predicted_final_grade, pp.attendance_rate, pp.missed_hours, pp.risk_level
            order by c.course_name
            """,
            (rs, rowNum) -> new CourseRow(
                rs.getInt("enrollment_id"),
                rs.getInt("course_id"),
                rs.getString("course_code"),
                rs.getString("course_name"),
                rs.getDouble("average_grade"),
                rs.getDouble("predicted_grade"),
                rs.getDouble("attendance_rate"),
                rs.getInt("missed_hours"),
                rs.getString("risk_level")
            ),
            allowedStudentId
        );

        List<StudentProfileCourseDto> subjects = courseRows.stream()
            .map(this::mapCourse)
            .toList();

        return new StudentProfileResponseDto(
            header.userId(),
            header.username(),
            header.fullName(),
            header.email(),
            header.active(),
            header.approved(),
            header.studentId(),
            header.studentCard(),
            header.groupCode(),
            header.courseNo(),
            header.facultyName(),
            header.specializationName(),
            subjects
        );
    }

    private StudentProfileCourseDto mapCourse(CourseRow row) {
        List<StudentProfileGradeDto> grades = jdbcTemplate.query(
            """
            select g.graded_at::date as graded_date,
                   g.grade_value,
                   atp.type_name,
                   ai.title
            from grade g
            join assessment_item ai on ai.assessment_item_id = g.assessment_item_id
            join assessment_type atp on atp.assessment_type_id = ai.assessment_type_id
            where g.enrollment_id = ?
            order by g.graded_at, g.grade_id
            """,
            (rs, rowNum) -> new StudentProfileGradeDto(
                rs.getObject("graded_date", LocalDate.class),
                rs.getDouble("grade_value"),
                rs.getString("type_name"),
                rs.getString("title")
            ),
            row.enrollmentId()
        );

        List<String> recommendations = jdbcTemplate.query(
            """
            select sr.recommendation_text
            from student_recommendation sr
            where sr.enrollment_id = ?
            order by sr.priority desc, sr.created_at desc
            limit 5
            """,
            (rs, rowNum) -> rs.getString("recommendation_text"),
            row.enrollmentId()
        );

        return new StudentProfileCourseDto(
            row.courseId(),
            row.courseCode(),
            row.courseName(),
            round2(row.averageGrade()),
            round2(row.predictedGrade()),
            round2(row.attendanceRate()),
            row.missedHours(),
            row.riskLevel(),
            recommendations,
            grades
        );
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

        return new AccessContext(roleCode, studentId);
    }

    private Integer ensureStudentAccess(AccessContext context, Integer studentId) {
        if ("student".equals(context.roleCode())) {
            if (context.studentId() == null) {
                throw new ForbiddenException("Профиль студента еще не заполнен");
            }
            if (!context.studentId().equals(studentId)) {
                throw new ForbiddenException("Студент может просматривать только свой профиль");
            }
        }
        return studentId;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record AccessContext(
        String roleCode,
        Integer studentId
    ) {
    }

    private record CourseRow(
        Integer enrollmentId,
        Integer courseId,
        String courseCode,
        String courseName,
        Double averageGrade,
        Double predictedGrade,
        Double attendanceRate,
        Integer missedHours,
        String riskLevel
    ) {
    }
}