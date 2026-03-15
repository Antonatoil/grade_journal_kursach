with current_term as (
    select term_id, start_date, end_date
    from academic_term
    where is_current = true
), current_offerings as (
    select co.offering_id, co.group_id, co.course_id, co.teacher_id
    from course_offering co
    join current_term ct on ct.term_id = co.term_id
), current_enrollments as (
    select e.enrollment_id, e.offering_id
    from enrollment e
    join current_offerings co on co.offering_id = e.offering_id
), current_predictions as (
    select pp.prediction_id
    from performance_prediction pp
    join current_enrollments ce on ce.enrollment_id = pp.enrollment_id
)
delete from student_recommendation
where prediction_id in (select prediction_id from current_predictions);

with current_term as (
    select term_id
    from academic_term
    where is_current = true
), current_offerings as (
    select co.offering_id
    from course_offering co
    join current_term ct on ct.term_id = co.term_id
), current_enrollments as (
    select e.enrollment_id
    from enrollment e
    join current_offerings co on co.offering_id = e.offering_id
)
delete from performance_prediction
where enrollment_id in (select enrollment_id from current_enrollments);

with current_term as (
    select term_id
    from academic_term
    where is_current = true
), current_offerings as (
    select co.offering_id
    from course_offering co
    join current_term ct on ct.term_id = co.term_id
)
delete from assessment_item
where offering_id in (select offering_id from current_offerings);

with current_term as (
    select term_id
    from academic_term
    where is_current = true
), current_offerings as (
    select co.offering_id
    from course_offering co
    join current_term ct on ct.term_id = co.term_id
)
delete from schedule_entry
where offering_id in (select offering_id from current_offerings);

with current_term as (
    select term_id
    from academic_term
    where is_current = true
), current_offerings as (
    select co.offering_id
    from course_offering co
    join current_term ct on ct.term_id = co.term_id
)
update enrollment e
set missed_hours = 0
where e.offering_id in (select offering_id from current_offerings);

with current_term as (
    select term_id, start_date
    from academic_term
    where is_current = true
), offering_rank as (
    select co.offering_id,
           co.group_id,
           c.course_name,
           row_number() over (partition by co.group_id order by c.course_id) as course_rank
    from course_offering co
    join current_term ct on ct.term_id = co.term_id
    join course c on c.course_id = co.course_id
), lesson_grid as (
    select generate_series(0, 5) as week_no,
           generate_series(1, 3) as lesson_in_week
)
insert into schedule_entry (offering_id, lesson_date, time_slot, lesson_type, room, topic)
select
    orr.offering_id,
    ct.start_date
        + (lg.week_no * 7)
        + case
            when orr.course_rank = 1 and lg.lesson_in_week = 1 then 0
            when orr.course_rank = 1 and lg.lesson_in_week = 2 then 2
            when orr.course_rank = 1 and lg.lesson_in_week = 3 then 4
            when orr.course_rank = 2 and lg.lesson_in_week = 1 then 0
            when orr.course_rank = 2 and lg.lesson_in_week = 2 then 2
            when orr.course_rank = 2 and lg.lesson_in_week = 3 then 4
            when orr.course_rank = 3 and lg.lesson_in_week = 1 then 1
            when orr.course_rank = 3 and lg.lesson_in_week = 2 then 3
            when orr.course_rank = 3 and lg.lesson_in_week = 3 then 5
            when orr.course_rank = 4 and lg.lesson_in_week = 1 then 1
            when orr.course_rank = 4 and lg.lesson_in_week = 2 then 3
            else 5
        end,
    case orr.course_rank
        when 1 then '09:00-10:20'
        when 2 then '10:35-11:55'
        when 3 then '12:25-13:45'
        else        '14:00-15:20'
    end as time_slot,
    case
        when lg.lesson_in_week = 1 then 'lecture'
        when lg.lesson_in_week = 2 then 'practice'
        else 'lab'
    end as lesson_type,
    'У-' || lpad((200 + ((orr.group_id * 3 + orr.course_rank * 7 + lg.week_no * 2 + lg.lesson_in_week) % 50))::text, 3, '0') as room,
    'Занятие ' || (lg.week_no * 3 + lg.lesson_in_week) || ': ' || orr.course_name as topic
from offering_rank orr
cross join lesson_grid lg
cross join current_term ct
order by orr.offering_id, lg.week_no, lg.lesson_in_week;

with ranked_schedule as (
    select se.schedule_id,
           se.offering_id,
           se.lesson_date,
           row_number() over (partition by se.offering_id order by se.lesson_date, se.time_slot) as lesson_no
    from schedule_entry se
), assessment_plan as (
    select *
    from (values
        (2,  'quiz', 'Краткий тест 1',       0.06::numeric),
        (4,  'lab',  'Лабораторная работа 1', 0.08::numeric),
        (6,  'quiz', 'Краткий тест 2',       0.06::numeric),
        (8,  'lab',  'Лабораторная работа 2', 0.10::numeric),
        (10, 'test', 'Контрольная работа 1', 0.12::numeric),
        (12, 'quiz', 'Краткий тест 3',       0.08::numeric),
        (14, 'lab',  'Лабораторная работа 3', 0.10::numeric),
        (16, 'test', 'Контрольная работа 2', 0.12::numeric),
        (17, 'quiz', 'Итоговый тест',        0.08::numeric),
        (18, 'exam', 'Экзамен',              0.20::numeric)
    ) as ap(lesson_no, type_code, title, weight)
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
    at.assessment_type_id,
    ap.title,
    10.00,
    ap.weight,
    rs.lesson_date,
    rs.schedule_id,
    true
from ranked_schedule rs
join assessment_plan ap on ap.lesson_no = rs.lesson_no
join assessment_type at on at.type_code = ap.type_code
order by rs.offering_id, rs.lesson_no;

insert into attendance (enrollment_id, schedule_id, is_present, note)
select
    e.enrollment_id,
    se.schedule_id,
    (random() > 0.18) as is_present,
    case
        when random() < 0.08 then 'Пропуск без уважительной причины'
        when random() < 0.12 then 'Предупреждение о необходимости отработки'
        else null
    end
from enrollment e
join schedule_entry se on se.offering_id = e.offering_id
where se.lesson_date <= current_date
on conflict (enrollment_id, schedule_id) do update
set is_present = excluded.is_present,
    note = excluded.note;

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
                    + random() * 3.10
                    - e.missed_hours * 0.025
                    + case at.type_code
                        when 'lab'  then 0.35
                        when 'quiz' then 0.15
                        when 'test' then -0.05
                        when 'exam' then -0.15
                        else 0.00
                    end
                )::numeric
            )
        ),
        2
    ) as grade_value,
    case
        when random() < 0.10 then 'Нужно усилить самостоятельную подготовку'
        when random() < 0.18 then 'Есть потенциал для повышения оценки на следующей контрольной точке'
        else '—'
    end,
    (ai.due_date::timestamp + interval '18 hour')::timestamptz
from enrollment e
join assessment_item ai on ai.offering_id = e.offering_id
join assessment_type at on at.assessment_type_id = ai.assessment_type_id
join attendance att on att.enrollment_id = e.enrollment_id
                   and att.schedule_id = ai.schedule_id
                   and att.is_present = true
where ai.due_date <= current_date
  and random() > 0.03
on conflict (enrollment_id, assessment_item_id) do update
set grade_value = excluded.grade_value,
    teacher_comment = excluded.teacher_comment,
    graded_at = excluded.graded_at;

with grade_stats as (
    select e.enrollment_id,
           coalesce(round(avg(g.grade_value), 2), 0.00) as avg_grade,
           count(g.grade_id) as graded_items
    from enrollment e
    left join grade g on g.enrollment_id = e.enrollment_id
    group by e.enrollment_id
), attendance_stats as (
    select e.enrollment_id,
           count(a.attendance_id) as total_lessons,
           count(a.attendance_id) filter (where a.is_present) as present_lessons
    from enrollment e
    left join attendance a on a.enrollment_id = e.enrollment_id
    group by e.enrollment_id
), required_stats as (
    select e.enrollment_id,
           count(ai.assessment_item_id) filter (
               where ai.is_required = true
                 and ai.due_date <= current_date
           ) as expected_items,
           count(g.grade_id) filter (
               where ai.is_required = true
                 and ai.due_date <= current_date
           ) as completed_items
    from enrollment e
    left join assessment_item ai on ai.offering_id = e.offering_id
    left join grade g on g.enrollment_id = e.enrollment_id
                        and g.assessment_item_id = ai.assessment_item_id
    group by e.enrollment_id
), base as (
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
                    2.00::numeric,
                    (
                        (case when coalesce(gs.avg_grade, 0) = 0 then 6.20 else gs.avg_grade end) * 0.78
                        + (
                            case
                                when coalesce(ast.total_lessons, 0) = 0 then 1.00
                                else ast.present_lessons::numeric / ast.total_lessons
                            end
                          ) * 2.10
                        - greatest(coalesce(rs.expected_items, 0) - coalesce(rs.completed_items, 0), 0) * 0.30
                        - e.missed_hours * 0.02
                    )::numeric
                )
            ),
            2
        ) as predicted_final_grade,
        round(
            least(
                0.97::numeric,
                (
                    0.58
                    + least(coalesce(ast.total_lessons, 0), 18) * 0.015
                    + least(coalesce(gs.graded_items, 0), 10) * 0.025
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
        when b.predicted_final_grade < 4.50
          or b.attendance_rate < 60.00
          or b.missing_required_count >= 3 then 'high'
        when b.predicted_final_grade < 6.50
          or b.attendance_rate < 80.00
          or b.missing_required_count between 1 and 2 then 'medium'
        else 'low'
    end,
    b.confidence_score,
    b.missing_required_count,
    case
        when b.predicted_final_grade < 4.50 then 'Высокий риск неудовлетворительного результата'
        when b.attendance_rate < 60.00 then 'Низкая посещаемость заметно влияет на итоговый прогноз'
        when b.missing_required_count >= 3 then 'Есть несколько незакрытых обязательных контрольных точек'
        when b.predicted_final_grade < 6.50 then 'Нужно стабилизировать текущую успеваемость'
        else 'Результаты устойчивые, рекомендуется сохранить текущий темп'
    end,
    'dense-demo-v2',
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
    'Повысить посещаемость занятий: каждая пропущенная пара ухудшает накопленный результат и точность прогноза.',
    case when pp.attendance_rate < 60 then 5 else 4 end,
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
    'Повторить теоретический материал и отработать типовые задания перед следующими контрольными точками.',
    case when pp.current_average < 4.5 then 5 else 4 end,
    'new'
from performance_prediction pp
where pp.current_average < 6.8;

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
    'Закрыть незавершенные обязательные контрольные точки в ближайшее время.',
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
    'Траектория обучения положительная. Продолжайте удерживать темп и уровень посещаемости.',
    2,
    'new'
from performance_prediction pp
where pp.risk_level = 'low'
  and not exists (
      select 1
      from student_recommendation sr
      where sr.prediction_id = pp.prediction_id
  );