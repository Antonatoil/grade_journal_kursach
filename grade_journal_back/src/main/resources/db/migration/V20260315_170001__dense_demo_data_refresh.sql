begin;

with current_term as (
    select term_id
    from academic_term
    where is_current = true
)
delete from student_recommendation sr
using enrollment e, course_offering co, current_term ct
where sr.enrollment_id = e.enrollment_id
  and e.offering_id = co.offering_id
  and co.term_id = ct.term_id;

with current_term as (
    select term_id
    from academic_term
    where is_current = true
)
delete from performance_prediction pp
using enrollment e, course_offering co, current_term ct
where pp.enrollment_id = e.enrollment_id
  and e.offering_id = co.offering_id
  and co.term_id = ct.term_id;

with current_term as (
    select term_id
    from academic_term
    where is_current = true
)
delete from grade g
using enrollment e, course_offering co, current_term ct
where g.enrollment_id = e.enrollment_id
  and e.offering_id = co.offering_id
  and co.term_id = ct.term_id;

with current_term as (
    select term_id
    from academic_term
    where is_current = true
)
delete from attendance a
using enrollment e, course_offering co, current_term ct
where a.enrollment_id = e.enrollment_id
  and e.offering_id = co.offering_id
  and co.term_id = ct.term_id;

with current_term as (
    select term_id
    from academic_term
    where is_current = true
)
delete from assessment_item ai
using course_offering co, current_term ct
where ai.offering_id = co.offering_id
  and co.term_id = ct.term_id;

with current_term as (
    select term_id
    from academic_term
    where is_current = true
)
delete from schedule_entry se
using course_offering co, current_term ct
where se.offering_id = co.offering_id
  and co.term_id = ct.term_id;

with current_term as (
    select term_id, start_date
    from academic_term
    where is_current = true
),
offering_rank as (
    select
        co.offering_id,
        co.group_id,
        c.course_name,
        c.course_id,
        row_number() over (partition by co.group_id order by c.course_id) as course_rank
    from course_offering co
    join course c on c.course_id = co.course_id
    join current_term ct on ct.term_id = co.term_id
),
lesson_numbers as (
    select generate_series(1, 18) as lesson_no
)
insert into schedule_entry (offering_id, lesson_date, time_slot, lesson_type, room, topic)
select
    orr.offering_id,
    ct.start_date + ((ln.lesson_no - 1) * 3 + (orr.course_rank - 1)) as lesson_date,
    case orr.course_rank
        when 1 then '09:00-10:20'
        when 2 then '10:35-11:55'
        when 3 then '12:25-13:45'
        else        '14:00-15:20'
    end as time_slot,
    case
        when ln.lesson_no in (1, 7, 13) then 'lecture'
        when ln.lesson_no in (3, 6, 9, 12, 15, 18) then 'lab'
        else 'practice'
    end as lesson_type,
    'А-' || lpad((100 + ((orr.group_id + orr.course_rank + ln.lesson_no) % 40))::text, 3, '0') as room,
    'Занятие ' || ln.lesson_no || ': ' || orr.course_name as topic
from offering_rank orr
cross join lesson_numbers ln
cross join current_term ct
order by orr.offering_id, lesson_date;

with ranked_schedule as (
    select
        se.schedule_id,
        se.offering_id,
        se.lesson_date,
        row_number() over (
            partition by se.offering_id
            order by se.lesson_date, se.time_slot
        ) as lesson_no
    from schedule_entry se
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
    case
        when rs.lesson_no in (2, 6, 10, 14) then (select assessment_type_id from assessment_type where type_code = 'quiz')
        when rs.lesson_no in (4, 8, 12, 16) then (select assessment_type_id from assessment_type where type_code = 'lab')
        when rs.lesson_no in (17) then (select assessment_type_id from assessment_type where type_code = 'test')
        when rs.lesson_no in (18) then (select assessment_type_id from assessment_type where type_code = 'exam')
    end as assessment_type_id,
    case
        when rs.lesson_no = 2  then 'Краткий тест 1'
        when rs.lesson_no = 4  then 'Лабораторная работа 1'
        when rs.lesson_no = 6  then 'Краткий тест 2'
        when rs.lesson_no = 8  then 'Лабораторная работа 2'
        when rs.lesson_no = 10 then 'Краткий тест 3'
        when rs.lesson_no = 12 then 'Лабораторная работа 3'
        when rs.lesson_no = 14 then 'Краткий тест 4'
        when rs.lesson_no = 16 then 'Лабораторная работа 4'
        when rs.lesson_no = 17 then 'Контрольная работа'
        when rs.lesson_no = 18 then 'Экзамен'
    end as title,
    10.00 as max_score,
    case
        when rs.lesson_no in (2, 6, 10, 14) then 0.05
        when rs.lesson_no in (4, 8, 12, 16) then 0.08
        when rs.lesson_no = 17 then 0.15
        when rs.lesson_no = 18 then 0.20
    end as weight,
    rs.lesson_date,
    rs.schedule_id,
    true
from ranked_schedule rs
where rs.lesson_no in (2, 4, 6, 8, 10, 12, 14, 16, 17, 18)
order by rs.offering_id, rs.lesson_no;

insert into attendance (enrollment_id, schedule_id, is_present, note)
select
    e.enrollment_id,
    se.schedule_id,
    (random() > 0.18) as is_present,
    case
        when random() < 0.06 then 'Уважительная причина'
        else null
    end
from enrollment e
join course_offering co on co.offering_id = e.offering_id
join academic_term at on at.term_id = co.term_id and at.is_current = true
join schedule_entry se on se.offering_id = e.offering_id
where se.lesson_date < current_date
order by e.enrollment_id, se.lesson_date
on conflict (enrollment_id, schedule_id) do nothing;

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
                    6.20
                    + random() * 3.20
                    - e.missed_hours * 0.015
                    + case atype.type_code
                        when 'lab' then 0.30
                        when 'exam' then -0.10
                        else 0.00
                      end
                )::numeric
            )
        ),
        2
    ) as grade_value,
    case
        when random() > 0.86 then 'Стоит повторить тему занятия'
        else '—'
    end as teacher_comment,
    (ai.due_date::timestamp + interval '18 hour')::timestamptz as graded_at
from enrollment e
join course_offering co
    on co.offering_id = e.offering_id
join academic_term term
    on term.term_id = co.term_id
   and term.is_current = true
join assessment_item ai
    on ai.offering_id = e.offering_id
   and ai.due_date < current_date
join assessment_type atype
    on atype.assessment_type_id = ai.assessment_type_id
join attendance att
    on att.enrollment_id = e.enrollment_id
   and att.schedule_id = ai.schedule_id
   and att.is_present = true
order by e.enrollment_id, ai.assessment_item_id
on conflict (enrollment_id, assessment_item_id) do update
set grade_value = excluded.grade_value,
    teacher_comment = excluded.teacher_comment,
    graded_at = excluded.graded_at;

with grade_stats as (
    select
        e.enrollment_id,
        coalesce(round(avg(g.grade_value), 2), 0.00) as avg_grade,
        count(g.grade_id) as graded_items
    from enrollment e
    left join grade g on g.enrollment_id = e.enrollment_id
    group by e.enrollment_id
),
attendance_stats as (
    select
        e.enrollment_id,
        count(a.attendance_id) as total_lessons,
        count(a.attendance_id) filter (where a.is_present) as present_lessons
    from enrollment e
    left join attendance a on a.enrollment_id = e.enrollment_id
    group by e.enrollment_id
),
required_stats as (
    select
        e.enrollment_id,
        count(ai.assessment_item_id) filter (
            where ai.is_required = true
              and ai.due_date < current_date
        ) as expected_items,
        count(g.grade_id) filter (
            where ai.is_required = true
              and ai.due_date < current_date
        ) as completed_items
    from enrollment e
    left join assessment_item ai
        on ai.offering_id = e.offering_id
    left join grade g
        on g.enrollment_id = e.enrollment_id
       and g.assessment_item_id = ai.assessment_item_id
    group by e.enrollment_id
),
base as (
    select
        e.enrollment_id,
        coalesce(gs.avg_grade, 0.00) as current_average,
        round(
            case
                when coalesce(ast.total_lessons, 0) = 0 then 100.00
                else ast.present_lessons::numeric * 100.00 / ast.total_lessons
            end,
            2
        ) as attendance_rate,
        e.missed_hours,
        greatest(coalesce(rs.expected_items, 0) - coalesce(rs.completed_items, 0), 0) as missing_required_count,
        round(
            least(
                10.00::numeric,
                greatest(
                    1.00::numeric,
                    (
                        (case when coalesce(gs.avg_grade, 0) = 0 then 6.00 else gs.avg_grade end) * 0.80
                        + (
                            case
                                when coalesce(ast.total_lessons, 0) = 0 then 1.00
                                else ast.present_lessons::numeric / ast.total_lessons
                            end
                          ) * 1.80
                        - greatest(coalesce(rs.expected_items, 0) - coalesce(rs.completed_items, 0), 0) * 0.20
                        - e.missed_hours * 0.01
                    )::numeric
                )
            ),
            2
        ) as predicted_final_grade,
        round(
            least(
                0.97::numeric,
                (
                    0.60
                    + least(coalesce(ast.total_lessons, 0), 18) * 0.01
                    + least(coalesce(gs.graded_items, 0), 10) * 0.02
                )::numeric
            ),
            2
        ) as confidence_score
    from enrollment e
    left join grade_stats gs on gs.enrollment_id = e.enrollment_id
    left join attendance_stats ast on ast.enrollment_id = e.enrollment_id
    left join required_stats rs on rs.enrollment_id = e.enrollment_id
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
        when b.predicted_final_grade < 6.00
          or b.attendance_rate < 70.00
          or b.missing_required_count >= 2 then 'high'
        when b.predicted_final_grade < 7.50
          or b.attendance_rate < 85.00
          or b.missing_required_count = 1 then 'medium'
        else 'low'
    end as risk_level,
    b.confidence_score,
    b.missing_required_count,
    case
        when b.predicted_final_grade < 6.00 then 'Есть риск снижения итоговой оценки. Нужно усилить подготовку.'
        when b.attendance_rate < 70.00 then 'Низкая посещаемость негативно влияет на результат.'
        when b.missing_required_count >= 2 then 'Нужно закрыть обязательные контрольные точки.'
        when b.predicted_final_grade < 7.50 then 'Результат удовлетворительный, но есть потенциал для роста.'
        else 'Текущая траектория стабильна. Рекомендуется сохранять темп.'
    end as recommendation_summary,
    'dense-rule-based-v2',
    now()
from base b;

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
    'Повысить посещаемость занятий и сократить количество пропусков.',
    case
        when pp.attendance_rate < 70 then 5
        else 4
    end,
    'new'
from performance_prediction pp
where pp.attendance_rate < 85;

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
    'Рекомендуется усилить подготовку и проработать темы с низкими оценками.',
    case
        when pp.current_average < 6.0 then 5
        else 4
    end,
    'new'
from performance_prediction pp
where pp.current_average < 7.5;

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
    'Необходимо закрыть пропущенные или незавершенные обязательные контрольные точки.',
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
    'Результаты хорошие. Поддерживайте текущий уровень подготовки.',
    2,
    'new'
from performance_prediction pp
where pp.risk_level = 'low'
  and not exists (
      select 1
      from student_recommendation sr
      where sr.prediction_id = pp.prediction_id
  );

commit;