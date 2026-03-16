package com.example.grade_journal_back.schedule.service;

import com.example.grade_journal_back.common.exception.BadRequestException;
import com.example.grade_journal_back.common.exception.NotFoundException;
import com.example.grade_journal_back.schedule.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;

import org.springframework.cache.annotation.Cacheable;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private static final List<String> TIME_SLOTS = List.of(
        "09:00-10:20",
        "10:35-11:55",
        "12:25-13:45",
        "14:00-15:20",
        "15:50-17:10",
        "17:25-18:45",
        "19:00-20:20"
    );

    private static final Set<String> TIME_SLOT_SET = Set.copyOf(TIME_SLOTS);

    private static final List<String> LESSON_TYPES = List.of(
        "lecture",
        "practice",
        "lab",
        "seminar",
        "exam",
        "consultation"
    );

    private static final Set<String> LESSON_TYPE_SET = Set.copyOf(LESSON_TYPES);

    private final JdbcTemplate jdbcTemplate;

    @Cacheable("scheduleMeta")
    public ScheduleMetaResponse getMeta() {
        List<ScheduleGroupOptionDto> groups = jdbcTemplate.query(
            """
            select group_id, group_code, course_no, admission_year, faculty_name, specialization_name
            from study_group
            order by course_no, group_code
            """,
            (rs, rowNum) -> new ScheduleGroupOptionDto(
                rs.getInt("group_id"),
                rs.getString("group_code"),
                rs.getInt("course_no"),
                rs.getInt("admission_year"),
                rs.getString("faculty_name"),
                rs.getString("specialization_name")
            )
        );

        List<ScheduleTeacherOptionDto> teachers = jdbcTemplate.query(
            """
            select t.teacher_id, ua.full_name, d.department_name, t.position
            from teacher t
            join user_account ua on ua.user_account_id = t.user_account_id
            join department d on d.department_id = t.department_id
            order by ua.full_name
            """,
            (rs, rowNum) -> new ScheduleTeacherOptionDto(
                rs.getInt("teacher_id"),
                rs.getString("full_name"),
                rs.getString("department_name"),
                rs.getString("position")
            )
        );

        List<ScheduleCourseOptionDto> courses = jdbcTemplate.query(
            """
            select course_id, course_code, course_name, study_year, control_form
            from course
            order by study_year, course_name
            """,
            (rs, rowNum) -> new ScheduleCourseOptionDto(
                rs.getInt("course_id"),
                rs.getString("course_code"),
                rs.getString("course_name"),
                rs.getInt("study_year"),
                rs.getString("control_form")
            )
        );

        return new ScheduleMetaResponse(groups, teachers, courses, TIME_SLOTS, LESSON_TYPES);
    }

    public List<ScheduleEntryDto> getScheduleByGroup(Integer groupId) {
        if (!existsGroup(groupId)) {
            throw new NotFoundException("Группа не найдена");
        }

        CurrentTerm currentTerm = findCurrentTerm();

        return jdbcTemplate.query(
            """
            select
                se.schedule_id,
                se.lesson_date,
                se.time_slot,
                sg.group_id,
                sg.group_code,
                c.course_name,
                ua.full_name as teacher_full_name,
                se.room,
                se.lesson_type,
                se.topic
            from schedule_entry se
            join course_offering co on co.offering_id = se.offering_id
            join study_group sg on sg.group_id = co.group_id
            join course c on c.course_id = co.course_id
            join teacher t on t.teacher_id = co.teacher_id
            join user_account ua on ua.user_account_id = t.user_account_id
            where co.term_id = ? and co.group_id = ?
            order by se.lesson_date, se.time_slot
            """,
            (rs, rowNum) -> new ScheduleEntryDto(
                rs.getInt("schedule_id"),
                rs.getObject("lesson_date", LocalDate.class),
                rs.getString("time_slot"),
                rs.getInt("group_id"),
                rs.getString("group_code"),
                rs.getString("course_name"),
                rs.getString("teacher_full_name"),
                rs.getString("room"),
                rs.getString("lesson_type"),
                rs.getString("topic")
            ),
            currentTerm.termId(),
            groupId
        );
    }

    @CacheEvict(value = "scheduleMeta", allEntries = true)
    @Transactional
    public String createSchedule(CreateScheduleRequest request) {
        CurrentTerm currentTerm = findCurrentTerm();
        validateRequest(request, currentTerm);

        Integer offeringId = findOfferingId(request, currentTerm.termId());
        if (offeringId == null) {
            offeringId = createOffering(request, currentTerm.termId());
        }

        jdbcTemplate.update(
            """
            insert into schedule_entry (offering_id, lesson_date, time_slot, lesson_type, room, topic)
            values (?, ?, ?, ?, ?, ?)
            """,
            offeringId,
            request.lessonDate(),
            normalize(request.timeSlot()),
            normalize(request.lessonType()),
            trimToNull(request.room()),
            trimToNull(request.topic())
        );

        return "Расписание успешно добавлено";
    }

    private void validateRequest(CreateScheduleRequest request, CurrentTerm currentTerm) {
        if (request.lessonDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Дата занятия не может быть в прошлом");
        }

        if (request.lessonDate().isBefore(currentTerm.startDate()) || request.lessonDate().isAfter(currentTerm.endDate())) {
            throw new BadRequestException("Дата занятия должна попадать в текущий учебный семестр");
        }

        String timeSlot = normalize(request.timeSlot());
        if (!TIME_SLOT_SET.contains(timeSlot)) {
            throw new BadRequestException("Выбран недопустимый временной интервал");
        }

        String lessonType = normalize(request.lessonType());
        if (!LESSON_TYPE_SET.contains(lessonType)) {
            throw new BadRequestException("Выбран недопустимый тип занятия");
        }

        if (!existsGroup(request.groupId())) {
            throw new NotFoundException("Группа не найдена");
        }

        if (!existsTeacher(request.teacherId())) {
            throw new NotFoundException("Преподаватель не найден");
        }

        if (!existsCourse(request.courseId())) {
            throw new NotFoundException("Дисциплина не найдена");
        }

        Integer groupCourse = jdbcTemplate.queryForObject(
            "select course_no from study_group where group_id = ?",
            Integer.class,
            request.groupId()
        );

        Integer courseStudyYear = jdbcTemplate.queryForObject(
            "select study_year from course where course_id = ?",
            Integer.class,
            request.courseId()
        );

        if (groupCourse != null && courseStudyYear != null && !groupCourse.equals(courseStudyYear)) {
            throw new BadRequestException("Выбранная дисциплина не соответствует курсу выбранной группы");
        }

        Long groupConflict = jdbcTemplate.queryForObject(
            """
            select count(*)
            from schedule_entry se
            join course_offering co on co.offering_id = se.offering_id
            where co.term_id = ?
              and co.group_id = ?
              and se.lesson_date = ?
              and se.time_slot = ?
            """,
            Long.class,
            currentTerm.termId(),
            request.groupId(),
            request.lessonDate(),
            timeSlot
        );

        if (groupConflict != null && groupConflict > 0) {
            throw new BadRequestException("У выбранной группы уже есть пара на это время");
        }

        Long teacherConflict = jdbcTemplate.queryForObject(
            """
            select count(*)
            from schedule_entry se
            join course_offering co on co.offering_id = se.offering_id
            where co.term_id = ?
              and co.teacher_id = ?
              and se.lesson_date = ?
              and se.time_slot = ?
            """,
            Long.class,
            currentTerm.termId(),
            request.teacherId(),
            request.lessonDate(),
            timeSlot
        );

        if (teacherConflict != null && teacherConflict > 0) {
            throw new BadRequestException("У выбранного преподавателя уже есть пара на это время");
        }
    }

    private Integer findOfferingId(CreateScheduleRequest request, Integer termId) {
        List<Integer> ids = jdbcTemplate.query(
            """
            select offering_id
            from course_offering
            where course_id = ? and teacher_id = ? and group_id = ? and term_id = ?
            order by offering_id
            limit 1
            """,
            (rs, rowNum) -> rs.getInt("offering_id"),
            request.courseId(),
            request.teacherId(),
            request.groupId(),
            termId
        );

        return ids.isEmpty() ? null : ids.get(0);
    }

    private Integer createOffering(CreateScheduleRequest request, Integer termId) {
        return jdbcTemplate.queryForObject(
            """
            insert into course_offering (course_id, teacher_id, group_id, term_id, status)
            values (?, ?, ?, ?, 'active')
            returning offering_id
            """,
            Integer.class,
            request.courseId(),
            request.teacherId(),
            request.groupId(),
            termId
        );
    }

    private boolean existsGroup(Integer groupId) {
        Long count = jdbcTemplate.queryForObject(
            "select count(*) from study_group where group_id = ?",
            Long.class,
            groupId
        );
        return count != null && count > 0;
    }

    private boolean existsTeacher(Integer teacherId) {
        Long count = jdbcTemplate.queryForObject(
            "select count(*) from teacher where teacher_id = ?",
            Long.class,
            teacherId
        );
        return count != null && count > 0;
    }

    private boolean existsCourse(Integer courseId) {
        Long count = jdbcTemplate.queryForObject(
            "select count(*) from course where course_id = ?",
            Long.class,
            courseId
        );
        return count != null && count > 0;
    }

    private CurrentTerm findCurrentTerm() {
        List<CurrentTerm> terms = jdbcTemplate.query(
            """
            select term_id, start_date, end_date
            from academic_term
            where is_current = true
            order by term_id desc
            limit 1
            """,
            (rs, rowNum) -> new CurrentTerm(
                rs.getInt("term_id"),
                rs.getObject("start_date", LocalDate.class),
                rs.getObject("end_date", LocalDate.class)
            )
        );

        if (terms.isEmpty()) {
            throw new BadRequestException("В системе не задан текущий семестр");
        }

        return terms.get(0);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record CurrentTerm(Integer termId, LocalDate startDate, LocalDate endDate) {
    }
}