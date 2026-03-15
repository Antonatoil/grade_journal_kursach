begin;

-- ---------------------------------------------------------
-- 1. Все существующие оценки переводим в целые безопасно
-- ---------------------------------------------------------
update grade
set grade_value = round(grade_value, 0);

-- ---------------------------------------------------------
-- 2. Для текущего семестра добиваем оценки так,
--    чтобы у каждого enrollment (студент + предмет)
--    было минимум 3-5 оценок, если есть достаточно занятий
-- ---------------------------------------------------------
with current_term as (
    select term_id
    from academic_term
    where is_current = true
),
eligible_items as (
    select
        e.enrollment_id,
        ai.assessment_item_id,
        ai.due_date,
        at.type_code,
        count(*) over (partition by e.enrollment_id) as eligible_count,
        row_number() over (
            partition by e.enrollment_id
            order by ai.due_date, ai.assessment_item_id
        ) as rn
    from enrollment e
    join course_offering co
        on co.offering_id = e.offering_id
    join current_term ct
        on ct.term_id = co.term_id
    join assessment_item ai
        on ai.offering_id = e.offering_id
       and ai.due_date < current_date
    join assessment_type at
        on at.assessment_type_id = ai.assessment_type_id
    join attendance a
        on a.enrollment_id = e.enrollment_id
       and a.schedule_id = ai.schedule_id
       and a.is_present = true
),
target_items as (
    select *
    from eligible_items
    where rn <= least(eligible_count, 5)
),
missing_target_items as (
    select
        ti.enrollment_id,
        ti.assessment_item_id,
        ti.type_code,
        ti.due_date
    from target_items ti
    left join grade g
        on g.enrollment_id = ti.enrollment_id
       and g.assessment_item_id = ti.assessment_item_id
    where g.grade_id is null
)
insert into grade (
    enrollment_id,
    assessment_item_id,
    grade_value,
    teacher_comment,
    graded_at
)
select
    mti.enrollment_id,
    mti.assessment_item_id,
    round(
        greatest(
            4::numeric,
            least(
                10::numeric,
                (
                    6
                    + random() * 4
                    + case
                        when mti.type_code = 'lab' then 1
                        when mti.type_code = 'exam' then -1
                        else 0
                      end
                )::numeric
            )
        ),
        0
    ) as grade_value,
    case
        when random() > 0.87 then 'Рекомендуется повторить материал по теме занятия'
        else '—'
    end as teacher_comment,
    now()
from missing_target_items mti
on conflict (enrollment_id, assessment_item_id) do nothing;

-- ---------------------------------------------------------
-- 3. Еще раз округляем все оценки, чтобы гарантированно
--    убрать любые дробные значения после старых данных
-- ---------------------------------------------------------
update grade
set grade_value = round(grade_value, 0);

-- ---------------------------------------------------------
-- 4. Удаляем старые прогнозы/рекомендации для текущего семестра
-- ---------------------------------------------------------
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

-- ---------------------------------------------------------
-- 5. Пересчитываем прогнозы по каждому предмету
--    enrollment = один студент по одной дисциплине
-- ---------------------------------------------------------
with grade_stats as (
    select
        e.enrollment_id,
        coalesce(round(avg(g.grade_value), 2), 0.00) as avg_grade,
        count(g.grade_id) as graded_items
    from enrollment e
    left join grade g
        on g.enrollment_id = e.enrollment_id
    group by e.enrollment_id
),
attendance_stats as (
    select
        e.enrollment_id,
        count(a.attendance_id) as total_lessons,
        count(a.attendance_id) filter (where a.is_present) as present_lessons
    from enrollment e
    left join attendance a
        on a.enrollment_id = e.enrollment_id
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
                          ) * 1.70
                        - greatest(coalesce(rs.expected_items, 0) - coalesce(rs.completed_items, 0), 0) * 0.35
                        - e.missed_hours * 0.03
                    )::numeric
                )
            ),
            0
        ) as predicted_final_grade,
        round(
            least(
                0.97::numeric,
                (
                    0.60
                    + least(coalesce(ast.total_lessons, 0), 18) * 0.01
                    + least(coalesce(gs.graded_items, 0), 5) * 0.04
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
        when b.predicted_final_grade < 8.00
          or b.attendance_rate < 85.00
          or b.missing_required_count = 1 then 'medium'
        else 'low'
    end as risk_level,
    b.confidence_score,
    b.missing_required_count,
    trim(
        both ' ' from concat_ws(
            ' ',
            case
                when b.current_average < 6.00
                    then 'Нужно повысить текущий средний балл.'
            end,
            case
                when b.attendance_rate < 70.00
                    then 'Нужно сократить пропуски и стабилизировать посещаемость.'
            end,
            case
                when b.missing_required_count > 0
                    then 'Нужно закрыть обязательные контрольные точки.'
            end,
            case
                when b.current_average >= 8.00
                 and b.attendance_rate >= 85.00
                 and b.missing_required_count = 0
                    then 'Рекомендации не требуются: результаты по предмету хорошие.'
            end
        )
    ) as recommendation_summary,
    'integer-subject-recommendations-v3',
    now()
from base b;

-- ---------------------------------------------------------
-- 6. Создаем рекомендации ПО КАЖДОМУ ПРЕДМЕТУ
--    (через enrollment = студент + предмет)
-- ---------------------------------------------------------
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
    'По дисциплине "' || c.course_name || '" необходимо повысить посещаемость: регулярно присутствовать на занятиях и уменьшить количество пропусков.',
    case
        when pp.attendance_rate < 70 then 5
        else 4
    end,
    'new'
from performance_prediction pp
join enrollment e
    on e.enrollment_id = pp.enrollment_id
join course_offering co
    on co.offering_id = e.offering_id
join course c
    on c.course_id = co.course_id
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
    'По дисциплине "' || c.course_name || '" рекомендуется усилить подготовку: повторить теорию, разобрать практические задания и обратиться к преподавателю за консультацией.',
    case
        when pp.current_average < 6.00 then 5
        else 4
    end,
    'new'
from performance_prediction pp
join enrollment e
    on e.enrollment_id = pp.enrollment_id
join course_offering co
    on co.offering_id = e.offering_id
join course c
    on c.course_id = co.course_id
where pp.current_average < 8.00;

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
    'По дисциплине "' || c.course_name || '" необходимо как можно скорее закрыть незавершенные обязательные контрольные точки.',
    5,
    'new'
from performance_prediction pp
join enrollment e
    on e.enrollment_id = pp.enrollment_id
join course_offering co
    on co.offering_id = e.offering_id
join course c
    on c.course_id = co.course_id
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
    'По дисциплине "' || c.course_name || '" рекомендации не требуются: текущие результаты хорошие, достаточно сохранять текущий темп.',
    1,
    'new'
from performance_prediction pp
join enrollment e
    on e.enrollment_id = pp.enrollment_id
join course_offering co
    on co.offering_id = e.offering_id
join course c
    on c.course_id = co.course_id
where pp.current_average >= 8.00
  and pp.attendance_rate >= 85.00
  and pp.missing_required_count = 0
  and not exists (
      select 1
      from student_recommendation sr
      where sr.prediction_id = pp.prediction_id
  );

commit;