/* =====================================================================
   Dense demo academic activity for current term
   - more lessons per subject
   - more assessment points
   - attendance with absences
   - grades bound to assessment lessons
   - predictions and recommendations rebuilt
   ===================================================================== */

-- ---------------------------------------------------------------------
-- 1. Clean only current-term academic activity and derived analytics
-- ---------------------------------------------------------------------
delete from student_recommendation;
delete from performance_prediction;

delete from course_offering
where term_id = (
    select term_id
    from academic_term
    where is_current = true
    limit 1
);

-- ---------------------------------------------------------------------
-- 2. Recreate current-term course offerings
-- Every group gets 4 course offerings for its study year.
-- Teachers are distributed across offerings so each teacher has workload.
-- ---------------------------------------------------------------------
with current_term as (
    select term_id
    from academic_term
    where is_current = true
),
curriculum as (
    select
        sg.group_id,
        sg.course_no,
        c.course_id,
        row_number() over (partition by sg.group_id order by c.course_id) as course_rank,
        row_number() over (order by sg.group_id, c.course_id) as global_rank
    from study_group sg
    join course c on c.study_year = sg.course_no
)
insert into course_offering (course_id, teacher_id, group_id, term_id, status)
select
    cur.course_id,
    ((cur.global_rank - 1) % (select count(*) from teacher)) + 1 as teacher_id,
    cur.group_id,
    ct.term_id,
    'active'
from curriculum cur
cross join current_term ct
order by cur.group_id, cur.course_id;

-- ---------------------------------------------------------------------
-- 3. Recreate schedule
-- 14 lessons per offering (2 lessons per week for 7 weeks)
-- Group schedule has no collisions by construction.
-- ---------------------------------------------------------------------
with current_term as (
    select term_id, start_date
    from academic_term
    where is_current = true
),
offering_base as (
    select
        co.offering_id,
        co.group_id,
        co.teacher_id,
        c.course_name,
        row_number() over (partition by co.group_id order by c.course_id) as course_rank
    from course_offering co
    join course c on c.course_id = co.course_id
    join current_term ct on ct.term_id = co.term_id
),
weeks as (
    select generate_series(0, 6) as week_no
),
weekly_pairs as (
    select 1 as pair_no
    union all
    select 2 as pair_no
),
schedule_plan as (
    select
        ob.offering_id,
        ob.group_id,
        ob.teacher_id,
        ob.course_name,
        ob.course_rank,
        w.week_no,
        wp.pair_no,
        case
            when ob.course_rank = 1 and wp.pair_no = 1 then 0
            when ob.course_rank = 1 and wp.pair_no = 2 then 2
            when ob.course_rank = 2 and wp.pair_no = 1 then 0
            when ob.course_rank = 2 and wp.pair_no = 2 then 2
            when ob.course_rank = 3 and wp.pair_no = 1 then 1
            when ob.course_rank = 3 and wp.pair_no = 2 then 3
            when ob.course_rank = 4 and wp.pair_no = 1 then 1
            when ob.course_rank = 4 and wp.pair_no = 2 then 3
        end as day_offset,
        case
            when ob.course_rank = 1 and wp.pair_no = 1 then '09:00-10:20'
            when ob.course_rank = 1 and wp.pair_no = 2 then '12:25-13:45'
            when ob.course_rank = 2 and wp.pair_no = 1 then '10:35-11:55'
            when ob.course_rank = 2 and wp.pair_no = 2 then '14:00-15:20'
            when ob.course_rank = 3 and wp.pair_no = 1 then '09:00-10:20'
            when ob.course_rank = 3 and wp.pair_no = 2 then '15:50-17:10'
            when ob.course_rank = 4 and wp.pair_no = 1 then '10:35-11:55'
            when ob.course_rank = 4 and wp.pair_no = 2 then '17:25-18:45'
        end as time_slot,
        case
            when ob.course_rank in (1, 2) and wp.pair_no = 1 then 'lecture'
            when ob.course_rank in (2, 3) and wp.pair_no = 2 then 'lab'
            else 'practice'
        end as lesson_type
    from offering_base ob
    cross join weeks w
    cross join weekly_pairs wp
),
numbered_schedule as (
    select
        sp.*,
        row_number() over (
            partition by sp.offering_id
            order by sp.week_no, sp.day_offset, sp.time_slot
        ) as lesson_no
    from schedule_plan sp
)
insert into schedule_entry (offering_id, lesson_date, time_slot, lesson_type, room, topic)
select
    ns.offering_id,
    ct.start_date + (ns.week_no * 7) + ns.day_offset as lesson_date,
    ns.time_slot,
    ns.lesson_type,
    'А-' || lpad((100 + ((ns.group_id + ns.course_rank + ns.week_no + ns.pair_no) % 35))::text, 3, '0') as room,
    'Занятие ' || ns.lesson_no || ': ' || ns.course_name as topic
from numbered_schedule ns
cross join current_term ct
order by ns.offering_id, ns.lesson_no;

-- ---------------------------------------------------------------------
-- 4. Enroll students into current-term offerings of their group
-- ---------------------------------------------------------------------
insert into enrollment (student_id, offering_id, enrollment_date, current_status)
select
    s.student_id,
    co.offering_id,
    current_date,
    'active'
from student s
join course_offering co on co.group_id = s.group_id
join academic_term t on t.term_id = co.term_id and t.is_current = true
order by s.student_id, co.offering_id;

-- ---------------------------------------------------------------------
-- 5. Create many assessment points bound to concrete lessons
-- 8 checkpoints per subject, total weight = 1.00
-- ---------------------------------------------------------------------
with ranked_schedule as (
    select
        se.schedule_id,
        se.offering_id,
        se.lesson_date,
        row_number() over (partition by se.offering_id order by se.lesson_date, se.time_slot) as lesson_no
    from schedule_entry se
    join course_offering co on co.offering_id = se.offering_id
    join academic_term t on t.term_id = co.term_id and t.is_current = true
)
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
    rs.offering_id,
    case rs.lesson_no
        when 2  then (select assessment_type_id from assessment_type where type_code = 'quiz')
        when 4  then (select assessment_type_id from assessment_type where type_code = 'lab')
        when 5  then (select assessment_type_id from assessment_type where type_code = 'quiz')
        when 7  then (select assessment_type_id from assessment_type where type_code = 'lab')
        when 8  then (select assessment_type_id from assessment_type where type_code = 'quiz')
        when 10 then (select assessment_type_id from assessment_type where type_code = 'test')
        when 12 then (select assessment_type_id from assessment_type where type_code = 'test')
        when 14 then (select assessment_type_id from assessment_type where type_code = 'exam')
    end as assessment_type_id,
    case rs.lesson_no
        when 2  then 'Краткий тест 1'
        when 4  then 'Лабораторная работа 1'
        when 5  then 'Краткий тест 2'
        when 7  then 'Лабораторная работа 2'
        when 8  then 'Краткий тест 3'
        when 10 then 'Контрольная работа 1'
        when 12 then 'Контрольная работа 2'
        when 14 then 'Экзамен'
    end as title,
    10.00 as max_score,
    case rs.lesson_no
        when 2  then 0.08
        when 4  then 0.12
        when 5  then 0.08
        when 7  then 0.12
        when 8  then 0.10
        when 10 then 0.15
        when 12 then 0.15
        when 14 then 0.20
    end as weight,
    rs.lesson_date,
    rs.schedule_id,
    true
from ranked_schedule rs
where rs.lesson_no in (2, 4, 5, 7, 8, 10, 12, 14)
order by rs.offering_id, rs.lesson_no;

-- ---------------------------------------------------------------------
-- 6. Attendance for all lessons already held
-- More absences so missed hours are visible.
-- Triggers update enrollment.missed_hours automatically.
-- ---------------------------------------------------------------------
insert into attendance (enrollment_id, schedule_id, is_present, note)
select
    e.enrollment_id,
    se.schedule_id,
    (
        random()
        > case
            when (e.enrollment_id % 11) = 0 then 0.35
            when (e.enrollment_id % 7) = 0 then 0.25
            else 0.15
          end
    ) as is_present,
    case
        when random() < 0.08 then 'Опоздание'
        when random() < 0.18 then 'Пропуск без уважительной причины'
        else null
    end as note
from enrollment e
join course_offering co on co.offering_id = e.offering_id
join academic_term t on t.term_id = co.term_id and t.is_current = true
join schedule_entry se on se.offering_id = e.offering_id
where se.lesson_date <= current_date
order by e.enrollment_id, se.lesson_date, se.time_slot
on conflict (enrollment_id, schedule_id) do nothing;

-- ---------------------------------------------------------------------
-- 7. Grades for all passed assessment lessons where student was present.
-- Most students now receive many grades per subject.
-- ---------------------------------------------------------------------
insert into grade (enrollment_id, assessment_item_id, grade_value, teacher_comment, graded_at)
select
    e.enrollment_id,
    ai.assessment_item_id,
    round(
        greatest(
            2.00::numeric,
            least(
                10.00::numeric,
                (
                    5.20
                    + random() * 3.80
                    - e.missed_hours * 0.025
                    + case at.type_code
                        when 'lab'  then 0.35
                        when 'quiz' then 0.20
                        when 'test' then 0.00
                        when 'exam' then -0.15
                        else 0.00
                      end
                    + case
                        when (e.enrollment_id % 13) = 0 then -1.10
                        when (e.enrollment_id % 9) = 0 then -0.60
                        when (e.enrollment_id % 5) = 0 then 0.25
                        else 0.00
                      end
                )::numeric
            )
        ),
        2
    ) as grade_value,
    case
        when random() > 0.90 then 'Следует усилить самостоятельную подготовку'
        when random() > 0.82 then 'Рекомендуется повторить материал занятия и выполнить дополнительные задания'
        else '—'
    end as teacher_comment,
    (ai.due_date::timestamp + interval '18 hour')::timestamptz as graded_at
from enrollment e
join course_offering co on co.offering_id = e.offering_id
join academic_term t on t.term_id = co.term_id and t.is_current = true
join assessment_item ai
    on ai.offering_id = e.offering_id
   and ai.due_date <= current_date
join assessment_type at
    on at.assessment_type_id = ai.assessment_type_id
join attendance att
    on att.enrollment_id = e.enrollment_id
   and att.schedule_id = ai.schedule_id
   and att.is_present = true
where random() > 0.04
order by e.enrollment_id, ai.assessment_item_id
on conflict (enrollment_id, assessment_item_id) do nothing;

-- ---------------------------------------------------------------------
-- 8. Rebuild predictions for current-term enrollments
-- ---------------------------------------------------------------------
with current_enrollments as (
    select e.enrollment_id, e.offering_id, e.missed_hours
    from enrollment e
    join course_offering co on co.offering_id = e.offering_id
    join academic_term t on t.term_id = co.term_id and t.is_current = true
),
grade_stats as (
    select
        ce.enrollment_id,
        coalesce(round(avg(g.grade_value), 2), 0.00) as avg_grade,
        count(g.grade_id) as graded_items
    from current_enrollments ce
    left join grade g on g.enrollment_id = ce.enrollment_id
    group by ce.enrollment_id
),
attendance_stats as (
    select
        ce.enrollment_id,
        count(a.attendance_id) as total_lessons,
        count(a.attendance_id) filter (where a.is_present) as present_lessons
    from current_enrollments ce
    left join attendance a on a.enrollment_id = ce.enrollment_id
    group by ce.enrollment_id
),
required_stats as (
    select
        ce.enrollment_id,
        count(ai.assessment_item_id) filter (
            where ai.is_required = true
              and ai.due_date <= current_date
        ) as expected_items,
        count(g.grade_id) filter (
            where ai.is_required = true
              and ai.due_date <= current_date
        ) as completed_items
    from current_enrollments ce
    left join assessment_item ai
        on ai.offering_id = ce.offering_id
    left join grade g
        on g.enrollment_id = ce.enrollment_id
       and g.assessment_item_id = ai.assessment_item_id
    group by ce.enrollment_id
),
base as (
    select
        ce.enrollment_id,
        coalesce(gs.avg_grade, 0.00) as current_average,
        round(
            case
                when coalesce(ast.total_lessons, 0) = 0 then 100.00
                else ast.present_lessons::numeric * 100.00 / ast.total_lessons
            end,
            2
        ) as attendance_rate,
        ce.missed_hours,
        greatest(coalesce(rs.expected_items, 0) - coalesce(rs.completed_items, 0), 0) as missing_required_count,
        round(
            least(
                10.00::numeric,
                greatest(
                    1.00::numeric,
                    (
                        (case when coalesce(gs.avg_grade, 0) = 0 then 6.00 else gs.avg_grade end) * 0.78
                        + (
                            case
                                when coalesce(ast.total_lessons, 0) = 0 then 1.00
                                else ast.present_lessons::numeric / ast.total_lessons
                            end
                          ) * 1.80
                        - greatest(coalesce(rs.expected_items, 0) - coalesce(rs.completed_items, 0), 0) * 0.30
                        - ce.missed_hours * 0.02
                    )::numeric
                )
            ),
            2
        ) as predicted_final_grade,
        round(
            least(
                0.97::numeric,
                (
                    0.50
                    + least(coalesce(ast.total_lessons, 0), 14) * 0.018
                    + least(coalesce(gs.graded_items, 0), 8) * 0.03
                )::numeric
            ),
            2
        ) as confidence_score
    from current_enrollments ce
    left join grade_stats gs on gs.enrollment_id = ce.enrollment_id
    left join attendance_stats ast on ast.enrollment_id = ce.enrollment_id
    left join required_stats rs on rs.enrollment_id = ce.enrollment_id
)
insert into performance_prediction (
    enrollment_id,
    current_average,
    attendance_rate,
    missed_hours,
    predicted_final_grade,
    risk_level,
    confidence_score,
    missing_required_count,
    recommendation_summary,
    model_version,
    generated_at
)
select
    b.enrollment_id,
    b.current_average,
    b.attendance_rate,
    b.missed_hours,
    b.predicted_final_grade,
    case
        when b.predicted_final_grade < 4.50
          or b.attendance_rate < 60.00
          or b.missing_required_count >= 2 then 'high'
        when b.predicted_final_grade < 6.50
          or b.attendance_rate < 80.00
          or b.missing_required_count = 1 then 'medium'
        else 'low'
    end as risk_level,
    b.confidence_score,
    b.missing_required_count,
    case
        when b.predicted_final_grade < 4.50 then 'Высокий риск неудовлетворительного результата'
        when b.attendance_rate < 60.00 then 'Низкая посещаемость существенно влияет на итоговый прогноз'
        when b.missing_required_count >= 2 then 'Есть незакрытые обязательные контрольные точки'
        when b.predicted_final_grade < 6.50 then 'Требуется стабилизировать текущую успеваемость'
        else 'Результаты стабильны, рекомендуется сохранять текущий темп'
    end as recommendation_summary,
    'rule-based-v2-dense-demo',
    now()
from base b
order by b.enrollment_id;

-- ---------------------------------------------------------------------
-- 9. Rebuild recommendations
-- ---------------------------------------------------------------------
insert into student_recommendation (
    enrollment_id,
    prediction_id,
    recommendation_type,
    recommendation_text,
    priority,
    status
)
select
    pp.enrollment_id,
    pp.prediction_id,
    'attendance',
    'Повысить посещаемость занятий: регулярное присутствие напрямую влияет на итоговую оценку и качество прогноза.',
    case when pp.attendance_rate < 60 then 5 else 4 end,
    'new'
from performance_prediction pp
where pp.attendance_rate < 80;

insert into student_recommendation (
    enrollment_id,
    prediction_id,
    recommendation_type,
    recommendation_text,
    priority,
    status
)
select
    pp.enrollment_id,
    pp.prediction_id,
    'performance',
    'Рекомендуется усилить подготовку по дисциплине: повторить теорию, разобрать типовые задания и посетить консультацию преподавателя.',
    case when pp.current_average < 4.5 then 5 else 4 end,
    'new'
from performance_prediction pp
where pp.current_average < 6.5;

insert into student_recommendation (
    enrollment_id,
    prediction_id,
    recommendation_type,
    recommendation_text,
    priority,
    status
)
select
    pp.enrollment_id,
    pp.prediction_id,
    'assignment',
    'Необходимо закрыть пропущенные или незавершенные обязательные контрольные точки в ближайшее время.',
    5,
    'new'
from performance_prediction pp
where pp.missing_required_count > 0;

insert into student_recommendation (
    enrollment_id,
    prediction_id,
    recommendation_type,
    recommendation_text,
    priority,
    status
)
select
    pp.enrollment_id,
    pp.prediction_id,
    'support',
    'Текущая траектория обучения положительная. Рекомендуется сохранять темп подготовки и поддерживать высокий уровень посещаемости.',
    2,
    'new'
from performance_prediction pp
where pp.risk_level = 'low'
  and not exists (
      select 1
      from student_recommendation sr
      where sr.prediction_id = pp.prediction_id
  );