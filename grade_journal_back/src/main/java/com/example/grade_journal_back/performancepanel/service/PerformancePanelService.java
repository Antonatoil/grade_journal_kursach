package com.example.grade_journal_back.performancepanel.service;

import com.example.grade_journal_back.performancepanel.dto.PerformanceCourseOptionDto;
import com.example.grade_journal_back.performancepanel.dto.PerformanceDetailsDto;
import com.example.grade_journal_back.performancepanel.dto.PerformancePointDto;
import com.example.grade_journal_back.performancepanel.dto.PerformanceStudentOptionDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PerformancePanelService {

    private final JdbcTemplate jdbcTemplate;

    public PerformancePanelService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PerformanceStudentOptionDto> getStudents() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (isStudent()) {
            String sql = """
                    select
                        s.student_id,
                        ua.full_name,
                        sg.group_code
                    from student s
                    join user_account ua on ua.user_account_id = s.user_account_id
                    join study_group sg on sg.group_id = s.group_id
                    where ua.username = ?
                    order by ua.full_name
                    """;

            return jdbcTemplate.query(sql, (rs, rowNum) -> new PerformanceStudentOptionDto(
                    rs.getInt("student_id"),
                    rs.getString("full_name"),
                    rs.getString("group_code")
            ), username);
        }

        String sql = """
                select
                    s.student_id,
                    ua.full_name,
                    sg.group_code
                from student s
                join user_account ua on ua.user_account_id = s.user_account_id
                join study_group sg on sg.group_id = s.group_id
                order by sg.group_code, ua.full_name
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new PerformanceStudentOptionDto(
                rs.getInt("student_id"),
                rs.getString("full_name"),
                rs.getString("group_code")
        ));
    }

    public List<PerformanceCourseOptionDto> getCourses(Integer studentId) {
        Integer resolvedStudentId = resolveAllowedStudentId(studentId);

        String sql = """
                select distinct
                    c.course_id,
                    c.course_name
                from enrollment e
                join course_offering co on co.offering_id = e.offering_id
                join course c on c.course_id = co.course_id
                join academic_term at on at.term_id = co.term_id
                where e.student_id = ?
                  and at.is_current = true
                order by c.course_name
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new PerformanceCourseOptionDto(
                rs.getInt("course_id"),
                rs.getString("course_name")
        ), resolvedStudentId);
    }

    public PerformanceDetailsDto getDetails(Integer studentId, Integer courseId) {
        Integer resolvedStudentId = resolveAllowedStudentId(studentId);

        Integer enrollmentId = jdbcTemplate.query("""
                select e.enrollment_id
                from enrollment e
                join course_offering co on co.offering_id = e.offering_id
                join academic_term at on at.term_id = co.term_id
                where e.student_id = ?
                  and co.course_id = ?
                  and at.is_current = true
                limit 1
                """, rs -> rs.next() ? rs.getInt("enrollment_id") : null, resolvedStudentId, courseId);

        if (enrollmentId == null) {
            throw new IllegalStateException("Не удалось найти дисциплину для выбранного студента.");
        }

        Object[] header = jdbcTemplate.queryForObject("""
                select
                    ua.full_name as student_name,
                    sg.group_code,
                    c.course_name
                from enrollment e
                join student s on s.student_id = e.student_id
                join user_account ua on ua.user_account_id = s.user_account_id
                join study_group sg on sg.group_id = s.group_id
                join course_offering co on co.offering_id = e.offering_id
                join course c on c.course_id = co.course_id
                where e.enrollment_id = ?
                """, (rs, rowNum) -> new Object[] {
                rs.getString("student_name"),
                rs.getString("group_code"),
                rs.getString("course_name")
        }, enrollmentId);

        Double averageAllCourses = jdbcTemplate.query("""
                select round(avg(g.grade_value)::numeric, 2)
                from grade g
                join enrollment e on e.enrollment_id = g.enrollment_id
                join course_offering co on co.offering_id = e.offering_id
                join academic_term at on at.term_id = co.term_id
                where e.student_id = ?
                  and at.is_current = true
                """, rs -> {
            if (!rs.next()) {
                return 0.0;
            }
            double value = rs.getDouble(1);
            return rs.wasNull() ? 0.0 : value;
        }, resolvedStudentId);

        Double averageSelectedCourse = jdbcTemplate.query("""
                select round(avg(g.grade_value)::numeric, 2)
                from grade g
                where g.enrollment_id = ?
                """, rs -> {
            if (!rs.next()) {
                return 0.0;
            }
            double value = rs.getDouble(1);
            return rs.wasNull() ? 0.0 : value;
        }, enrollmentId);

        Integer gradeCount = jdbcTemplate.query("""
                select count(*)
                from grade
                where enrollment_id = ?
                """, rs -> rs.next() ? rs.getInt(1) : 0, enrollmentId);

        Double attendanceRate = jdbcTemplate.query("""
                select round(
                    case
                        when count(a.attendance_id) = 0 then 100.00
                        else count(a.attendance_id) filter (where a.is_present)::numeric * 100.00 / count(a.attendance_id)
                    end,
                    2
                )
                from attendance a
                where a.enrollment_id = ?
                """, rs -> {
            if (!rs.next()) {
                return 100.0;
            }
            double value = rs.getDouble(1);
            return rs.wasNull() ? 100.0 : value;
        }, enrollmentId);

        Integer missedHours = jdbcTemplate.query("""
                select missed_hours
                from enrollment
                where enrollment_id = ?
                """, rs -> rs.next() ? rs.getInt(1) : 0, enrollmentId);

        List<Integer> lastThreeValues = jdbcTemplate.query("""
                select round(g.grade_value, 0)::int
                from grade g
                where g.enrollment_id = ?
                order by g.graded_at desc, g.grade_id desc
                limit 3
                """, (rs, rowNum) -> rs.getInt(1), enrollmentId);

        String lastThreeGrades = lastThreeValues.isEmpty()
                ? "нет данных"
                : String.join(", ", lastThreeValues.stream().map(String::valueOf).toList());

        String trend = buildTrend(
                lastThreeValues,
                averageSelectedCourse == null ? 0.0 : averageSelectedCourse,
                gradeCount == null ? 0 : gradeCount
        );

        int predictedFinalGrade = calculatePredictedGrade(
                averageSelectedCourse == null ? 0.0 : averageSelectedCourse,
                attendanceRate == null ? 100.0 : attendanceRate,
                missedHours == null ? 0 : missedHours,
                trend,
                gradeCount == null ? 0 : gradeCount
        );

        List<String> recommendations = buildRecommendations(
                (String) header[2],
                averageSelectedCourse == null ? 0.0 : averageSelectedCourse,
                attendanceRate == null ? 100.0 : attendanceRate,
                missedHours == null ? 0 : missedHours,
                lastThreeValues,
                predictedFinalGrade,
                gradeCount == null ? 0 : gradeCount
        );

        String recommendationSummary = buildSummary(
                (String) header[2],
                averageSelectedCourse == null ? 0.0 : averageSelectedCourse,
                attendanceRate == null ? 100.0 : attendanceRate,
                missedHours == null ? 0 : missedHours,
                lastThreeGrades,
                predictedFinalGrade,
                trend,
                gradeCount == null ? 0 : gradeCount
        );

        List<PerformancePointDto> points = jdbcTemplate.query("""
                select
                    row_number() over (order by g.graded_at, g.grade_id) as order_no,
                    round(g.grade_value, 0)::int as value,
                    at.type_name as grade_type,
                    to_char(g.graded_at, 'YYYY-MM-DD HH24:MI') as graded_at
                from grade g
                join assessment_item ai on ai.assessment_item_id = g.assessment_item_id
                join assessment_type at on at.assessment_type_id = ai.assessment_type_id
                where g.enrollment_id = ?
                order by g.graded_at, g.grade_id
                """, (rs, rowNum) -> new PerformancePointDto(
                rs.getInt("order_no"),
                rs.getInt("value"),
                rs.getString("grade_type"),
                rs.getString("graded_at")
        ), enrollmentId);

        return new PerformanceDetailsDto(
                (String) header[0],
                (String) header[1],
                (String) header[2],
                averageAllCourses == null ? 0.0 : averageAllCourses,
                averageSelectedCourse == null ? 0.0 : averageSelectedCourse,
                predictedFinalGrade,
                recommendationSummary,
                recommendations,
                points,
                gradeCount == null ? 0 : gradeCount,
                attendanceRate == null ? 100.0 : attendanceRate,
                missedHours == null ? 0 : missedHours,
                lastThreeGrades,
                trend,
                "live-recommendations-v3"
        );
    }

    private int calculatePredictedGrade(
            double averageSelectedCourse,
            double attendanceRate,
            int missedHours,
            String trend,
            int gradeCount
    ) {
        if (gradeCount == 0) {
            return 0;
        }

        double predicted =
                averageSelectedCourse * 0.82
                        + (attendanceRate / 100.0) * 1.8
                        - missedHours * 0.03
                        + switch (trend) {
                            case "рост" -> 0.4;
                            case "снижение" -> -0.4;
                            default -> 0.0;
                        };

        if (predicted < 1) {
            predicted = 1;
        }
        if (predicted > 10) {
            predicted = 10;
        }

        return (int) Math.round(predicted);
    }

    private String buildTrend(List<Integer> lastThreeValues, double avg, int gradeCount) {
        if (gradeCount == 0 || lastThreeValues.isEmpty()) {
            return "нет данных";
        }

        if (lastThreeValues.size() < 2) {
            return "недостаточно данных";
        }

        double recentAverage = lastThreeValues.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(avg);

        if (recentAverage > avg + 0.3) {
            return "рост";
        }
        if (recentAverage < avg - 0.3) {
            return "снижение";
        }
        return "стабильно";
    }

    private List<String> buildRecommendations(
            String courseName,
            double average,
            double attendanceRate,
            int missedHours,
            List<Integer> lastThreeValues,
            int predictedFinalGrade,
            int gradeCount
    ) {
        List<String> recommendations = new ArrayList<>();

        if (gradeCount == 0) {
            recommendations.add(
                    "По предмету \"" + courseName + "\" пока нет ни одной оценки. " +
                    "Сначала нужно получить первые результаты по контрольным точкам, после чего система сможет построить более точный прогноз."
            );

            if (attendanceRate < 85) {
                recommendations.add(
                        "Даже без оценок уже видно, что посещаемость составляет " + attendanceRate +
                        "%. Стоит сократить пропуски, чтобы не ухудшить будущий результат."
                );
            }

            return recommendations;
        }

        if (average >= 7.5 && attendanceRate >= 85) {
            recommendations.add(
                    "По предмету \"" + courseName + "\" средний балл уже равен " + average +
                    ", а посещаемость составляет " + attendanceRate +
                    "%. Рекомендации не требуются: результат хороший."
            );
            return recommendations;
        }

        if (average < 6) {
            recommendations.add(
                    "По предмету \"" + courseName + "\" текущий средний балл составляет " + average +
                    ". Это низкий уровень, поэтому ближайшая цель — получить минимум 7 за следующие работы."
            );
        } else if (average < 7.5) {
            recommendations.add(
                    "По предмету \"" + courseName + "\" средний балл составляет " + average +
                    ". Результат пока средний, и его можно улучшить за счет более сильных ближайших работ."
            );
        }

        if (attendanceRate < 70) {
            recommendations.add(
                    "Посещаемость по предмету сейчас только " + attendanceRate +
                    "%, пропущено " + missedHours +
                    " часов. Это уже заметно влияет на итоговый результат, поэтому нужно сократить пропуски."
            );
        } else if (attendanceRate < 85) {
            recommendations.add(
                    "Посещаемость составляет " + attendanceRate +
                    "%. Желательно повысить регулярность присутствия на занятиях."
            );
        }

        if (lastThreeValues.size() == 3) {
            int oldest = lastThreeValues.get(2);
            int middle = lastThreeValues.get(1);
            int newest = lastThreeValues.get(0);

            if (newest < oldest) {
                recommendations.add(
                        "Последние оценки: " + oldest + ", " + middle + ", " + newest +
                        ". Видно снижение результата, поэтому стоит уделить внимание последним темам."
                );
            } else if (newest > oldest) {
                recommendations.add(
                        "Последние оценки: " + oldest + ", " + middle + ", " + newest +
                        ". Наблюдается положительная динамика — важно сохранить этот темп."
                );
            } else {
                recommendations.add(
                        "Последние оценки: " + oldest + ", " + middle + ", " + newest +
                        ". Динамика пока стабильная без заметного роста."
                );
            }
        } else if (lastThreeValues.size() == 2) {
            recommendations.add(
                    "Пока есть только две оценки: " + lastThreeValues.get(1) + " и " + lastThreeValues.get(0) +
                    ". После появления еще одной оценки прогноз станет точнее."
            );
        } else if (lastThreeValues.size() == 1) {
            recommendations.add(
                    "Пока есть только одна оценка: " + lastThreeValues.get(0) +
                    ". Для более точной аналитики нужно накопить больше результатов."
            );
        }

        if (predictedFinalGrade > 0 && predictedFinalGrade < 6) {
            recommendations.add(
                    "Прогнозная следующая оценка сейчас около " + predictedFinalGrade +
                    ". Чтобы выйти хотя бы на уровень 6–7, нужно усилить подготовку уже к ближайшему занятию."
            );
        } else if (predictedFinalGrade >= 6 && predictedFinalGrade < 8) {
            recommendations.add(
                    "Сейчас прогнозная следующая оценка около " + predictedFinalGrade +
                    ". При более сильной ближайшей работе можно поднять общий результат выше."
            );
        }

        if (recommendations.isEmpty()) {
            recommendations.add(
                    "Текущая ситуация по предмету \"" + courseName +
                    "\" стабильная. Достаточно сохранить текущий темп подготовки."
            );
        }

        return recommendations;
    }

    private String buildSummary(
            String courseName,
            double average,
            double attendanceRate,
            int missedHours,
            String lastThreeGrades,
            int predictedFinalGrade,
            String trend,
            int gradeCount
    ) {
        if (gradeCount == 0) {
            return "По дисциплине \"" + courseName +
                    "\" пока нет оценок. Посещаемость составляет " + attendanceRate +
                    "%, пропущено часов: " + missedHours +
                    ". Для персонального прогноза нужно накопить первые оценки.";
        }

        return "По дисциплине \"" + courseName +
                "\" средний балл " + average +
                ", количество оценок: " + gradeCount +
                ", посещаемость " + attendanceRate +
                "%, пропущено часов: " + missedHours +
                ". Последние оценки: " + lastThreeGrades +
                ". Текущая динамика: " + trend +
                ". Прогнозная следующая оценка: " + predictedFinalGrade + ".";
    }

    private Integer resolveAllowedStudentId(Integer requestedStudentId) {
        if (!isStudent()) {
            return requestedStudentId;
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        Integer ownStudentId = jdbcTemplate.query("""
                select s.student_id
                from student s
                join user_account ua on ua.user_account_id = s.user_account_id
                where ua.username = ?
                limit 1
                """, rs -> rs.next() ? rs.getInt(1) : null, username);

        if (ownStudentId == null) {
            throw new IllegalStateException("Не удалось определить студента текущего пользователя.");
        }

        return ownStudentId;
    }

    private boolean isStudent() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority ->
                        authority.equalsIgnoreCase("ROLE_STUDENT")
                                || authority.equalsIgnoreCase("student"));
    }
}