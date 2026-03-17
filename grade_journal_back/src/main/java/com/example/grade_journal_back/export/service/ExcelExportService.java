package com.example.grade_journal_back.export.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ExcelExportService {

    private final JdbcTemplate jdbcTemplate;

    public ExcelExportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public byte[] export(
            boolean students,
            boolean teachers,
            boolean courses,
            boolean schedule,
            boolean performance,
            boolean attendance
    ) {
        log.info(
                "Starting Excel export: students={}, teachers={}, courses={}, schedule={}, performance={}, attendance={}",
                students,
                teachers,
                courses,
                schedule,
                performance,
                attendance
        );

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            if (students) {
                createSheet(workbook, "Студенты", """
                        select
                            ua.full_name as "ФИО",
                            ua.username as "Логин",
                            ua.email as "Email",
                            sg.group_code as "Группа",
                            sg.course_no as "Курс",
                            s.student_card as "Студенческий билет",
                            ua.is_active as "Активен",
                            ua.is_approved as "Одобрен"
                        from student s
                        join user_account ua on ua.user_account_id = s.user_account_id
                        join study_group sg on sg.group_id = s.group_id
                        order by sg.group_code, ua.full_name
                        """);
            }

            if (teachers) {
                createSheet(workbook, "Преподаватели", """
                        select
                            ua.full_name as "ФИО",
                            ua.username as "Логин",
                            ua.email as "Email",
                            d.department_name as "Кафедра",
                            t.position as "Должность",
                            t.phone as "Телефон",
                            ua.is_active as "Активен",
                            ua.is_approved as "Одобрен"
                        from teacher t
                        join user_account ua on ua.user_account_id = t.user_account_id
                        join department d on d.department_id = t.department_id
                        order by ua.full_name
                        """);
            }

            if (courses) {
                createSheet(workbook, "Курсы", """
                        select
                            course_code as "Код",
                            course_name as "Название",
                            credits as "Кредиты",
                            total_hours as "Часы",
                            study_year as "Курс",
                            control_form as "Форма контроля"
                        from course
                        order by study_year, course_name
                        """);
            }

            if (schedule) {
                createSheet(workbook, "Расписание", """
                        select
                            sg.group_code as "Группа",
                            se.lesson_date as "Дата",
                            se.time_slot as "Время",
                            c.course_name as "Предмет",
                            ua.full_name as "Преподаватель",
                            se.lesson_type as "Тип пары",
                            se.room as "Аудитория",
                            se.topic as "Тема"
                        from schedule_entry se
                        join course_offering co on co.offering_id = se.offering_id
                        join course c on c.course_id = co.course_id
                        join study_group sg on sg.group_id = co.group_id
                        join teacher t on t.teacher_id = co.teacher_id
                        join user_account ua on ua.user_account_id = t.user_account_id
                        order by sg.group_code, se.lesson_date, se.time_slot
                        """);
            }

            if (performance) {
                createSheet(workbook, "Успеваемость", """
                        select
                            ua.full_name as "ФИО",
                            sg.group_code as "Группа",
                            c.course_name as "Предмет",
                            pp.current_average as "Текущий средний балл",
                            pp.predicted_final_grade as "Прогнозный балл",
                            pp.risk_level as "Уровень риска",
                            pp.attendance_rate as "Посещаемость, %",
                            pp.generated_at as "Дата прогноза"
                        from performance_prediction pp
                        join enrollment e on e.enrollment_id = pp.enrollment_id
                        join student s on s.student_id = e.student_id
                        join user_account ua on ua.user_account_id = s.user_account_id
                        join study_group sg on sg.group_id = s.group_id
                        join course_offering co on co.offering_id = e.offering_id
                        join course c on c.course_id = co.course_id
                        order by sg.group_code, ua.full_name, c.course_name
                        """);
            }

            if (attendance) {
                createSheet(workbook, "Посещаемость", """
                        select
                            ua.full_name as "ФИО",
                            sg.group_code as "Группа",
                            c.course_name as "Предмет",
                            se.lesson_date as "Дата",
                            se.time_slot as "Время",
                            case when a.is_present then 'Да' else 'Нет' end as "Присутствовал"
                        from attendance a
                        join enrollment e on e.enrollment_id = a.enrollment_id
                        join student s on s.student_id = e.student_id
                        join user_account ua on ua.user_account_id = s.user_account_id
                        join study_group sg on sg.group_id = s.group_id
                        join schedule_entry se on se.schedule_id = a.schedule_id
                        join course_offering co on co.offering_id = se.offering_id
                        join course c on c.course_id = co.course_id
                        order by sg.group_code, ua.full_name, se.lesson_date, se.time_slot
                        """);
            }

            if (workbook.getNumberOfSheets() == 0) {
                log.info("No export sections selected, creating fallback sheet");

                Sheet sheet = workbook.createSheet("Отчет");
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Выберите хотя бы один раздел для экспорта.");
                sheet.autoSizeColumn(0);
            }

            workbook.write(outputStream);

            log.info("Excel export completed successfully, sheets={}", workbook.getNumberOfSheets());

            return outputStream.toByteArray();
        } catch (IOException exception) {
            log.error("Excel export failed: {}", exception.getMessage(), exception);
            throw new RuntimeException("Не удалось сформировать Excel-отчет.", exception);
        }
    }

    private void createSheet(Workbook workbook, String sheetName, String sql) {
        log.info("Creating Excel sheet '{}'", sheetName);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        Sheet sheet = workbook.createSheet(sheetName);

        if (rows.isEmpty()) {
            log.info("No data found for Excel sheet '{}'", sheetName);

            Row emptyRow = sheet.createRow(0);
            emptyRow.createCell(0).setCellValue("Нет данных");
            sheet.autoSizeColumn(0);
            return;
        }

        List<String> headers = rows.get(0).keySet().stream().toList();

        Row headerRow = sheet.createRow(0);
        for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
            headerRow.createCell(columnIndex).setCellValue(headers.get(columnIndex));
        }

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Row row = sheet.createRow(rowIndex + 1);
            Map<String, Object> data = rows.get(rowIndex);

            for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
                Object value = data.get(headers.get(columnIndex));
                row.createCell(columnIndex).setCellValue(value == null ? "" : String.valueOf(value));
            }
        }

        for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
            sheet.autoSizeColumn(columnIndex);
        }

        log.info(
                "Excel sheet '{}' created successfully with rows={} and columns={}",
                sheetName,
                rows.size(),
                headers.size()
        );
    }
}