package com.example.grade_journal_back.report.service;

import com.example.grade_journal_back.report.dto.ExportReportRequest;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelReportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final JdbcTemplate jdbcTemplate;

    public ExcelReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public byte[] generateWorkbook(ExportReportRequest request) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            CellStyle headerStyle = createHeaderStyle(workbook);

            if (request.students()) {
                writeSheet(workbook, headerStyle, "Студенты", jdbcTemplate.queryForList(
                        """
                        select ua.full_name as "ФИО",
                               ua.username as "Логин",
                               coalesce(ua.email, '') as "Email",
                               sg.group_code as "Группа",
                               sg.course_no as "Курс",
                               s.student_card as "Студенческий билет",
                               case when ua.is_active then 'Да' else 'Нет' end as "Активен",
                               case when ua.is_approved then 'Да' else 'Нет' end as "Одобрен"
                        from student s
                        join user_account ua on ua.user_account_id = s.user_account_id
                        join study_group sg on sg.group_id = s.group_id
                        order by sg.course_no, sg.group_code, ua.full_name
                        """
                ));
            }

            if (request.teachers()) {
                writeSheet(workbook, headerStyle, "Преподаватели", jdbcTemplate.queryForList(
                        """
                        select ua.full_name as "ФИО",
                               ua.username as "Логин",
                               coalesce(ua.email, '') as "Email",
                               d.department_name as "Кафедра",
                               t.position as "Должность",
                               coalesce(t.phone, '') as "Телефон",
                               case when ua.is_active then 'Да' else 'Нет' end as "Активен",
                               case when ua.is_approved then 'Да' else 'Нет' end as "Одобрен"
                        from teacher t
                        join user_account ua on ua.user_account_id = t.user_account_id
                        join department d on d.department_id = t.department_id
                        order by d.department_name, ua.full_name
                        """
                ));
            }

            if (request.courses()) {
                writeSheet(workbook, headerStyle, "Курсы", jdbcTemplate.queryForList(
                        """
                        select course_code as "Код",
                               course_name as "Название",
                               study_year as "Курс",
                               credits as "Кредиты",
                               total_hours as "Часы",
                               control_form as "Форма контроля"
                        from course
                        order by study_year, course_name
                        """
                ));
            }

            if (request.schedule()) {
                writeSheet(workbook, headerStyle, "Расписание", jdbcTemplate.queryForList(
                        """
                        select sg.group_code as "Группа",
                               c.course_name as "Предмет",
                               ua.full_name as "Преподаватель",
                               se.lesson_date as "Дата",
                               se.time_slot as "Время",
                               se.lesson_type as "Тип занятия",
                               coalesce(se.room, '') as "Аудитория",
                               coalesce(se.topic, '') as "Тема"
                        from schedule_entry se
                        join course_offering co on co.offering_id = se.offering_id
                        join course c on c.course_id = co.course_id
                        join study_group sg on sg.group_id = co.group_id
                        join teacher t on t.teacher_id = co.teacher_id
                        join user_account ua on ua.user_account_id = t.user_account_id
                        order by sg.group_code, se.lesson_date, se.time_slot, c.course_name
                        """
                ));
            }

            if (request.performance()) {
                writeSheet(workbook, headerStyle, "Успеваемость", jdbcTemplate.queryForList(
                        """
                        select stu.full_name as "Студент",
                               stu.group_code as "Группа",
                               c.course_name as "Предмет",
                               ai.title as "Контрольная точка",
                               at.type_name as "Тип оценки",
                               ai.due_date as "Дата",
                               g.grade_value as "Оценка",
                               pp.current_average as "Средний балл сейчас",
                               pp.predicted_final_grade as "Прогноз",
                               pp.risk_level as "Риск"
                        from grade g
                        join enrollment e on e.enrollment_id = g.enrollment_id
                        join assessment_item ai on ai.assessment_item_id = g.assessment_item_id
                        join assessment_type at on at.assessment_type_id = ai.assessment_type_id
                        join course_offering co on co.offering_id = e.offering_id
                        join course c on c.course_id = co.course_id
                        join performance_prediction pp on pp.enrollment_id = e.enrollment_id
                        join (
                            select s.student_id,
                                   ua.full_name,
                                   sg.group_code
                            from student s
                            join user_account ua on ua.user_account_id = s.user_account_id
                            join study_group sg on sg.group_id = s.group_id
                        ) stu on stu.student_id = e.student_id
                        order by stu.group_code, stu.full_name, c.course_name, ai.due_date, ai.title
                        """
                ));
            }

            if (request.attendance()) {
                writeSheet(workbook, headerStyle, "Посещаемость", jdbcTemplate.queryForList(
                        """
                        select stu.full_name as "Студент",
                               stu.group_code as "Группа",
                               c.course_name as "Предмет",
                               se.lesson_date as "Дата",
                               se.time_slot as "Время",
                               se.lesson_type as "Тип занятия",
                               case when a.is_present then 'Присутствовал' else 'Пропуск' end as "Статус",
                               coalesce(a.note, '') as "Примечание"
                        from attendance a
                        join enrollment e on e.enrollment_id = a.enrollment_id
                        join schedule_entry se on se.schedule_id = a.schedule_id
                        join course_offering co on co.offering_id = e.offering_id
                        join course c on c.course_id = co.course_id
                        join (
                            select s.student_id,
                                   ua.full_name,
                                   sg.group_code
                            from student s
                            join user_account ua on ua.user_account_id = s.user_account_id
                            join study_group sg on sg.group_id = s.group_id
                        ) stu on stu.student_id = e.student_id
                        order by stu.group_code, stu.full_name, se.lesson_date, se.time_slot, c.course_name
                        """
                ));
            }

            if (workbook.getNumberOfSheets() == 0) {
                writeSheet(workbook, headerStyle, "Пустой отчет", List.of(Map.of("Сообщение", "Не выбраны разделы для экспорта")));
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось сформировать Excel-отчет", exception);
        }
    }

    private void writeSheet(XSSFWorkbook workbook, CellStyle headerStyle, String sheetName, List<Map<String, Object>> rows) {
        XSSFSheet sheet = workbook.createSheet(sheetName);
        List<Map<String, Object>> preparedRows = rows.isEmpty() ? List.of(Map.of("Нет данных", "В выбранном разделе пока нет данных")) : normalizeRows(rows);

        Row headerRow = sheet.createRow(0);
        List<String> headers = new ArrayList<>(preparedRows.get(0).keySet());
        for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
            Cell cell = headerRow.createCell(columnIndex);
            cell.setCellValue(headers.get(columnIndex));
            cell.setCellStyle(headerStyle);
        }

        for (int rowIndex = 0; rowIndex < preparedRows.size(); rowIndex++) {
            Row row = sheet.createRow(rowIndex + 1);
            Map<String, Object> rowData = preparedRows.get(rowIndex);
            for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
                Cell cell = row.createCell(columnIndex);
                Object value = rowData.get(headers.get(columnIndex));
                cell.setCellValue(formatValue(value));
            }
        }

        for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
            sheet.autoSizeColumn(columnIndex);
            int width = Math.min(sheet.getColumnWidth(columnIndex) + 1024, 16000);
            sheet.setColumnWidth(columnIndex, width);
        }
    }

    private List<Map<String, Object>> normalizeRows(List<Map<String, Object>> rows) {
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> linkedMap = new LinkedHashMap<>();
            linkedMap.putAll(row);
            normalized.add(linkedMap);
        }
        return normalized;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Timestamp timestamp) {
            LocalDateTime dateTime = timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            return DATE_TIME_FORMATTER.format(dateTime);
        }
        if (value instanceof Date date) {
            return DATE_FORMATTER.format(date.toLocalDate());
        }
        if (value instanceof LocalDate localDate) {
            return DATE_FORMATTER.format(localDate);
        }
        return String.valueOf(value);
    }
}