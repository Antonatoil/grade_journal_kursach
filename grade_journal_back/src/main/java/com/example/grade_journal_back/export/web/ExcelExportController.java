package com.example.grade_journal_back.export.web;

import com.example.grade_journal_back.export.service.ExcelExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export")
public class ExcelExportController {

    private final ExcelExportService excelExportService;

    public ExcelExportController(ExcelExportService excelExportService) {
        this.excelExportService = excelExportService;
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(defaultValue = "false") boolean students,
            @RequestParam(defaultValue = "false") boolean teachers,
            @RequestParam(defaultValue = "false") boolean courses,
            @RequestParam(defaultValue = "false") boolean schedule,
            @RequestParam(defaultValue = "false") boolean performance,
            @RequestParam(defaultValue = "false") boolean attendance
    ) {
        byte[] bytes = excelExportService.export(
                students,
                teachers,
                courses,
                schedule,
                performance,
                attendance
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=grade-journal-report.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .body(bytes);
    }
}