
-- =========================================================
-- 1. Удаляем только demo-оценки текущего семестра
-- =========================================================
with current_term as (
    select term_id
    from academic_term
    where is_current = true
)
delete from grade g
using assessment_item ai, course_offering co, current_term ct
where g.assessment_item_id = ai.assessment_item_id
  and ai.offering_id = co.offering_id
  and co.term_id = ct.term_id
  and ai.title like 'Demo оценивание %';

-- =========================================================
-- 2. Удаляем demo assessment items текущего семестра
-- =========================================================
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

-- =========================================================
-- 3. Создаем demo assessment items для первых 8 прошедших занятий
--    Если прошло меньше 8 занятий, будет меньше.
-- =========================================================
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
demo_schedule as (
    select *
    from ranked_schedule
    where lesson_no <= 8
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
),
test_type as (
    select assessment_type_id
    from assessment_type
    where type_code = 'test'
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
    ds.offering_id,
    case
        when ds.lesson_no in (2, 4, 7) then (select assessment_type_id from lab_type)
        when ds.lesson_no = 6 then (select assessment_type_id from test_type)
        else (select assessment_type_id from quiz_type)
    end as assessment_type_id,
    'Demo оценивание ' || ds.lesson_no,
    10.00,
    case
        when ds.lesson_no = 6 then 0.18
        when ds.lesson_no in (2, 4, 7) then 0.15
        else 0.10
    end,
    ds.lesson_date,
    ds.schedule_id,
    false
from demo_schedule ds;

-- =========================================================
-- 4. Вычисляем целевое количество оценок на каждый предмет:
--    от 3 до 8, но не больше числа доступных прошедших занятий
-- =========================================================
with current_term as (
    select term_id
    from academic_term
    where is_current = true
),
demo_items as (
    select
        ai.assessment_item_id,
        ai.offering_id,
        ai.schedule_id,
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
available_per_offering as (
    select
        di.offering_id,
        count(*) as available_count
    from demo_items di
    group by di.offering_id
),
targets as (
    select
        e.enrollment_id,
        e.student_id,
        co.course_id,
        e.offering_id,
        apo.available_count,
        case
            when apo.available_count <= 3 then apo.available_count
            when apo.available_count = 4 then 3 + ((e.student_id + co.course_id) % 2)
            when apo.available_count = 5 then 3 + ((e.student_id + co.course_id) % 3)
            when apo.available_count = 6 then 3 + ((e.student_id + co.course_id) % 4)
            when apo.available_count = 7 then 3 + ((e.student_id + co.course_id) % 5)
            else 3 + ((e.student_id + co.course_id) % 6)
        end as target_grade_count
    from enrollment e
    join course_offering co on co.offering_id = e.offering_id
    join current_term ct on ct.term_id = co.term_id
    join available_per_offering apo on apo.offering_id = e.offering_id
),
demo_schedule_ranked as (
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
)
insert into attendance (enrollment_id, schedule_id, is_present, note)
select
    t.enrollment_id,
    dsr.schedule_id,
    case
        when dsr.lesson_no <= t.target_grade_count then true
        when ((t.student_id + t.course_id + dsr.lesson_no) % 4) = 0 then false
        else true
    end as is_present,
    case
        when dsr.lesson_no > t.target_grade_count
             and ((t.student_id + t.course_id + dsr.lesson_no) % 4) = 0
            then 'Пропуск demo занятия'
        else null
    end as note
from targets t
join demo_schedule_ranked dsr on dsr.offering_id = t.offering_id
where dsr.lesson_no <= least(8, t.available_count)
on conflict (enrollment_id, schedule_id)
do update set
    is_present = excluded.is_present,
    note = excluded.note;

-- =========================================================
-- 5. Вставляем оценки:
--    только первые target_grade_count demo-работ
-- =========================================================
with current_term as (
    select term_id
    from academic_term
    where is_current = true
),
demo_items as (
    select
        ai.assessment_item_id,
        ai.offering_id,
        ai.schedule_id,
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
available_per_offering as (
    select
        di.offering_id,
        count(*) as available_count
    from demo_items di
    group by di.offering_id
),
targets as (
    select
        e.enrollment_id,
        e.student_id,
        co.course_id,
        e.offering_id,
        apo.available_count,
        case
            when apo.available_count <= 3 then apo.available_count
            when apo.available_count = 4 then 3 + ((e.student_id + co.course_id) % 2)
            when apo.available_count = 5 then 3 + ((e.student_id + co.course_id) % 3)
            when apo.available_count = 6 then 3 + ((e.student_id + co.course_id) % 4)
            when apo.available_count = 7 then 3 + ((e.student_id + co.course_id) % 5)
            else 3 + ((e.student_id + co.course_id) % 6)
        end as target_grade_count
    from enrollment e
    join course_offering co on co.offering_id = e.offering_id
    join current_term ct on ct.term_id = co.term_id
    join available_per_offering apo on apo.offering_id = e.offering_id
)
insert into grade (
    enrollment_id,
    assessment_item_id,
    grade_value,
    teacher_comment,
    graded_at
)
select
    t.enrollment_id,
    di.assessment_item_id,
    round(
        greatest(
            4::numeric,
            least(
                10::numeric,
                (
                    4
                    + ((t.student_id + t.course_id + di.point_no) % 5)
                    + case
                        when t.target_grade_count >= 6 then 1
                        else 0
                      end
                    + case
                        when t.target_grade_count >= 7 and di.point_no >= 5 then 1
                        else 0
                      end
                    - case
                        when t.target_grade_count <= 4 and di.point_no = t.target_grade_count then 1
                        else 0
                      end
                )::numeric
            )
        ),
        0
    ) as grade_value,
    case
        when t.target_grade_count <= 4 and di.point_no = t.target_grade_count
            then 'Нужно усилить подготовку по следующим занятиям'
        when t.target_grade_count >= 7 and di.point_no >= 5
            then 'Хорошая серия результатов'
        when di.point_no = t.target_grade_count
            then 'Текущий уровень по предмету можно улучшить'
        else '—'
    end as teacher_comment,
    (di.due_date::timestamp + interval '18 hour')::timestamptz
from targets t
join demo_items di
    on di.offering_id = t.offering_id
   and di.point_no <= t.target_grade_count
join attendance a
    on a.enrollment_id = t.enrollment_id
   and a.schedule_id = di.schedule_id
   and a.is_present = true;

-- =========================================================
-- 6. Все оценки переводим в целые
-- =========================================================
update grade
set grade_value = round(grade_value, 0);

-- =========================================================
-- 7. Пересчитываем пропущенные часы по текущему семестру
-- =========================================================
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

