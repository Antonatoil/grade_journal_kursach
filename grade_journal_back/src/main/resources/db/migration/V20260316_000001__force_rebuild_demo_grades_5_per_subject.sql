begin;

-- ---------------------------------------------------------
-- 1. Удаляем прогнозы и рекомендации текущего семестра
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
-- 2. Удаляем все оценки текущего семестра
-- ---------------------------------------------------------
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

-- ---------------------------------------------------------
-- 3. Удаляем demo assessment items текущего семестра
-- ---------------------------------------------------------
with current_term as (
    select term_id
    from academic_term
    where is_current = true
)
delete from assessment_item ai
using course_offering co, current_term ct
where ai.offering_id = co.offering_id
  and co.term_id = ct.term_id
  and ai.title like 'Demo оценивание %';

-- ---------------------------------------------------------
-- 4. Берем первые 5 прошедших занятий на каждой дисциплине
-- ---------------------------------------------------------
with current_term as (
    select term_id
    from academic_term
    where is_current = true
),
ranked_schedule as (
    select
        se.schedule_id,
        se.offering_id,
        se.lesson_date,
        row_number() over (
            partition by se.offering_id
            order by se.lesson_date, se.time_slot
        ) as lesson_no
    from schedule_entry se
    join course_offering co on co.offering_id = se.offering_id
    join current_term ct on ct.term_id = co.term_id
    where se.lesson_date < current_date
),
first_five as (
    select *
    from ranked_schedule
    where lesson_no <= 5
),
quiz_type as (
    select assessment_type_id
    from assessment_type
    where type_code = 'quiz'
    limit 1
),
lab_type as (
    select assessment_type_id
    from assessment_type
    where type_code = 'lab'
    limit 1
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
    f.offering_id,
    case
        when f.lesson_no in (2, 4) then (select assessment_type_id from lab_type)
        else (select assessment_type_id from quiz_type)
    end,
    'Demo оценивание ' || f.lesson_no,
    10.00,
    0.10,
    f.lesson_date,
    f.schedule_id,
    false
from first_five f;

-- ---------------------------------------------------------
-- 5. Пересоздаем attendance для первых 5 занятий
--    Чтобы у всех гарантированно были оценки
-- ---------------------------------------------------------
with current_term as (
    select term_id
    from academic_term
    where is_current = true
),
ranked_schedule as (
    select
        se.schedule_id,
        se.offering_id,
        row_number() over (
            partition by se.offering_id
            order by se.lesson_date, se.time_slot
        ) as lesson_no
    from schedule_entry se
    join course_offering co on co.offering_id = se.offering_id
    join current_term ct on ct.term_id = co.term_id
    where se.lesson_date < current_date
),
first_five as (
    select *
    from ranked_schedule
    where lesson_no <= 5
)
insert into attendance (enrollment_id, schedule_id, is_present, note)
select
    e.enrollment_id,
    f.schedule_id,
    true,
    null
from enrollment e
join first_five f on f.offering_id = e.offering_id
on conflict (enrollment_id, schedule_id)
do update set
    is_present = true,
    note = null;

-- ---------------------------------------------------------
-- 6. Создаем ровно 5 оценок на каждый enrollment
--    enrollment = студент + предмет
-- ---------------------------------------------------------
with current_term as (
    select term_id
    from academic_term
    where is_current = true
),
demo_items as (
    select
        ai.assessment_item_id,
        ai.offering_id,
        ai.due_date,
        row_number() over (
            partition by ai.offering_id
            order by ai.due_date, ai.assessment_item_id
        ) as point_no
    from assessment_item ai
    join course_offering co on co.offering_id = ai.offering_id
    join current_term ct on ct.term_id = co.term_id
    where ai.title like 'Demo оценивание %'
),
student_course_base as (
    select
        e.enrollment_id,
        e.student_id,
        co.course_id
    from enrollment e
    join course_offering co on co.offering_id = e.offering_id
    join current_term ct on ct.term_id = co.term_id
)
insert into grade (
    enrollment_id,
    assessment_item_id,
    grade_value,
    teacher_comment,
    graded_at
)
select
    scb.enrollment_id,
    di.assessment_item_id,
    round(
        greatest(
            4::numeric,
            least(
                10::numeric,
                (
                    5
                    + ((scb.student_id + scb.course_id + di.point_no) % 5)
                    + case
                        when di.point_no = 5 and ((scb.student_id + scb.course_id) % 4) = 0 then -1
                        when di.point_no = 5 and ((scb.student_id + scb.course_id) % 5) = 0 then 1
                        else 0
                      end
                )::numeric
            )
        ),
        0
    ),
    case
        when di.point_no = 5 and ((scb.student_id + scb.course_id) % 4) = 0
            then 'Последняя работа выполнена слабее предыдущих'
        when di.point_no = 5 and ((scb.student_id + scb.course_id) % 5) = 0
            then 'Есть положительная динамика по последней работе'
        else '—'
    end,
    (di.due_date::timestamp + interval '18 hour')::timestamptz
from student_course_base scb
join demo_items di on di.offering_id = (
    select e2.offering_id
    from enrollment e2
    where e2.enrollment_id = scb.enrollment_id
);

-- ---------------------------------------------------------
-- 7. Все оценки делаем целыми
-- ---------------------------------------------------------
update grade
set grade_value = round(grade_value, 0);

-- ---------------------------------------------------------
-- 8. Пересчитываем пропущенные часы с нуля
-- ---------------------------------------------------------
with current_term as (
    select term_id
    from academic_term
    where is_current = true
),
missed as (
    select
        e.enrollment_id,
        coalesce(count(*) filter (where a.is_present = false) * 2, 0) as missed_hours_value
    from enrollment e
    join course_offering co on co.offering_id = e.offering_id
    join current_term ct on ct.term_id = co.term_id
    left join attendance a on a.enrollment_id = e.enrollment_id
    group by e.enrollment_id
)
update enrollment e
set missed_hours = m.missed_hours_value
from missed m
where e.enrollment_id = m.enrollment_id;

commit;