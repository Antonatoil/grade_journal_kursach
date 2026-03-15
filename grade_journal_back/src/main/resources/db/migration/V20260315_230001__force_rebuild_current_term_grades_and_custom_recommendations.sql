begin;

-- =========================================================
-- 1. Удаляем прогнозы и рекомендации текущего семестра
-- =========================================================
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

-- =========================================================
-- 2. Для текущего семестра удаляем оценки,
--    чтобы пересобрать их заново в demo-виде
-- =========================================================
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

-- =========================================================
-- 3. Создаем/обновляем посещаемость на первых 5 прошедших занятиях
--    Делаем по умолчанию присутствие = true, чтобы гарантировать 5 оценок
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
    note = excluded.note;

-- =========================================================
-- 4. Удаляем старые "первые 5" demo assessment_item,
--    если они уже были созданы предыдущими патчами
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
-- 5. Создаем 5 assessment_item на первых 5 прошедших занятиях
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

-- =========================================================
-- 6. Создаем РОВНО 5 оценок на каждый предмет у каждого студента
--    Оценки целые, разнообразные, но устойчивые
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
)
insert into grade (
    enrollment_id,
    assessment_item_id,
    grade_value,
    teacher_comment,
    graded_at
)
select
    e.enrollment_id,
    di.assessment_item_id,
    greatest(
        4::numeric,
        least(
            10::numeric,
            (
                5
                + ((e.student_id + di.point_no + co.course_id) % 5)
                + case
                    when di.point_no in (4, 5) and ((e.student_id + co.course_id) % 3) = 0 then -1
                    when di.point_no in (4, 5) and ((e.student_id + co.course_id) % 4) = 0 then 1
                    else 0
                  end
            )::numeric
        )
    ) as grade_value,
    case
        when di.point_no = 5 and ((e.student_id + co.course_id) % 4) = 0
            then 'Последняя работа выполнена заметно слабее предыдущих'
        when di.point_no = 5 and ((e.student_id + co.course_id) % 5) = 0
            then 'Есть положительная динамика по последней работе'
        else '—'
    end,
    (di.due_date::timestamp + interval '18 hour')::timestamptz
from enrollment e
join course_offering co on co.offering_id = e.offering_id
join current_term ct on ct.term_id = co.term_id
join demo_items di on di.offering_id = e.offering_id
order by e.enrollment_id, di.point_no;

-- =========================================================
-- 7. Переводим все оценки в целые числа
-- =========================================================
update grade
set grade_value = round(grade_value, 0);

-- =========================================================
-- 8. Немного разнообразим пропуски ВНЕ первых 5 занятий,
--    чтобы они были в системе, но не мешали 5 оценкам
-- =========================================================
with current_term as (
    select term_id
    from academic_term
    where is_current = true
),
later_schedule as (
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
update attendance a
set is_present = case
    when ((a.enrollment_id + a.schedule_id) % 6) = 0 then false
    else true
end
from later_schedule ls
where a.schedule_id = ls.schedule_id
  and ls.lesson_no > 5;

-- =========================================================
-- 9. Пересчитываем missed_hours с нуля для текущего семестра
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

-- =========================================================
-- 10. Сводка по оценкам/посещаемости/последним 3 оценкам
-- =========================================================
with grade_stats as (
    select
        e.enrollment_id,
        round(avg(g.grade_value), 2) as avg_grade,
        count(g.grade_id) as graded_items
    from enrollment e
    join grade g on g.enrollment_id = e.enrollment_id
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
last_three as (
    select
        t.enrollment_id,
        string_agg(t.grade_value::text, ', ' order by t.graded_at) as last_three_values,
        avg(t.grade_value) as last_three_avg
    from (
        select
            g.enrollment_id,
            g.grade_value,
            g.graded_at,
            row_number() over (
                partition by g.enrollment_id
                order by g.graded_at desc, g.grade_id desc
            ) as rn
        from grade g
    ) t
    where t.rn <= 3
    group by t.enrollment_id
),
base as (
    select
        e.enrollment_id,
        s.student_id,
        ua.full_name as student_name,
        c.course_name,
        gs.avg_grade as current_average,
        round(
            case
                when coalesce(ast.total_lessons, 0) = 0 then 100.00
                else ast.present_lessons::numeric * 100.00 / ast.total_lessons
            end,
            2
        ) as attendance_rate,
        e.missed_hours,
        coalesce(lt.last_three_values, '') as last_three_values,
        coalesce(lt.last_three_avg, gs.avg_grade) as last_three_avg,
        round(
            least(
                10.00::numeric,
                greatest(
                    1.00::numeric,
                    (
                        gs.avg_grade * 0.80
                        + (
                            case
                                when coalesce(ast.total_lessons, 0) = 0 then 1.00
                                else ast.present_lessons::numeric / ast.total_lessons
                            end
                          ) * 1.80
                        - e.missed_hours * 0.03
                        + case
                            when coalesce(lt.last_three_avg, gs.avg_grade) > gs.avg_grade then 0.4
                            when coalesce(lt.last_three_avg, gs.avg_grade) < gs.avg_grade then -0.4
                            else 0
                          end
                    )::numeric
                )
            ),
            0
        ) as predicted_final_grade
    from enrollment e
    join student s on s.student_id = e.student_id
    join user_account ua on ua.user_account_id = s.user_account_id
    join course_offering co on co.offering_id = e.offering_id
    join academic_term at on at.term_id = co.term_id
    join course c on c.course_id = co.course_id
    join grade_stats gs on gs.enrollment_id = e.enrollment_id
    left join attendance_stats ast on ast.enrollment_id = e.enrollment_id
    left join last_three lt on lt.enrollment_id = e.enrollment_id
    where at.is_current = true
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
        when b.current_average < 6 or b.attendance_rate < 70 then 'high'
        when b.current_average < 7.5 or b.attendance_rate < 85 then 'medium'
        else 'low'
    end,
    0.92,
    0,
    case
        when b.current_average >= 7.5 and b.attendance_rate >= 85 then
            'По дисциплине "' || b.course_name || '" средний балл ' || b.current_average ||
            ', посещаемость ' || b.attendance_rate || '%. Последние оценки: ' || b.last_three_values ||
            '. Рекомендации не требуются: результат хороший.'
        when b.current_average < 6 and b.attendance_rate < 70 then
            'По дисциплине "' || b.course_name || '" средний балл ' || b.current_average ||
            ', посещаемость ' || b.attendance_rate || '%, пропущено часов: ' || b.missed_hours ||
            '. Последние оценки: ' || b.last_three_values ||
            '. Необходимо срочно повысить посещаемость и подготовку по предмету.'
        when b.current_average < 6 then
            'По дисциплине "' || b.course_name || '" средний балл ' || b.current_average ||
            '. Последние оценки: ' || b.last_three_values ||
            '. Основная проблема связана с низкими результатами по работам.'
        when b.attendance_rate < 70 then
            'По дисциплине "' || b.course_name || '" посещаемость составляет ' || b.attendance_rate ||
            '%, пропущено часов: ' || b.missed_hours ||
            '. Даже при текущих оценках нужно сократить пропуски.'
        when b.last_three_avg < b.current_average then
            'По дисциплине "' || b.course_name || '" наблюдается снижение по последним оценкам: ' ||
            b.last_three_values || '. Нужно стабилизировать результат.'
        else
            'По дисциплине "' || b.course_name || '" средний балл ' || b.current_average ||
            ', посещаемость ' || b.attendance_rate || '%. Последние оценки: ' || b.last_three_values ||
            '. Нужно немного усилить текущую подготовку.'
    end,
    'custom-subject-recommendations-v5',
    now()
from base b;

-- =========================================================
-- 11. Детальные рекомендации по каждому предмету
-- =========================================================
with latest_prediction as (
    select distinct on (pp.enrollment_id)
        pp.prediction_id,
        pp.enrollment_id,
        pp.current_average,
        pp.attendance_rate,
        pp.missed_hours,
        pp.predicted_final_grade,
        pp.risk_level
    from performance_prediction pp
    order by pp.enrollment_id, pp.generated_at desc, pp.prediction_id desc
),
last_three as (
    select
        t.enrollment_id,
        string_agg(t.grade_value::text, ', ' order by t.graded_at) as last_three_values,
        avg(t.grade_value) as last_three_avg
    from (
        select
            g.enrollment_id,
            g.grade_value,
            g.graded_at,
            row_number() over (
                partition by g.enrollment_id
                order by g.graded_at desc, g.grade_id desc
            ) as rn
        from grade g
    ) t
    where t.rn <= 3
    group by t.enrollment_id
)
insert into student_recommendation (
    enrollment_id,
    prediction_id,
    recommendation_type,
    recommendation_text,
    priority,
    status
)
select
    lp.enrollment_id,
    lp.prediction_id,
    'performance',
    case
        when lp.current_average >= 7.5 and lp.attendance_rate >= 85 then
            'Рекомендации не требуются: по предмету результат стабильный.'
        when lp.current_average < 6 and lp.attendance_rate < 70 then
            'Средний балл ниже 6 и посещаемость ниже 70%. Нужно посещать каждое ближайшее занятие и постараться получить не ниже 7 за следующие работы.'
        when lp.current_average < 6 then
            'Средний балл ниже 6. Нужно поднять качество подготовки и ориентироваться минимум на 7 за следующие оценки.'
        when lp.attendance_rate < 70 then
            'Низкая посещаемость. Важно сократить пропуски, иначе даже хорошие отдельные оценки не дадут устойчивого результата.'
        when coalesce(lt.last_three_avg, lp.current_average) < lp.current_average then
            'Последние оценки (' || coalesce(lt.last_three_values, 'нет данных') || ') слабее общего среднего. Нужно стабилизировать последние результаты.'
        else
            'Есть потенциал улучшения. Достаточно немного усилить подготовку по ближайшим темам.'
    end,
    case
        when lp.current_average < 6 or lp.attendance_rate < 70 then 5
        when lp.current_average < 7.5 or lp.attendance_rate < 85 then 3
        else 1
    end,
    'new'
from latest_prediction lp
left join last_three lt on lt.enrollment_id = lp.enrollment_id;

commit;