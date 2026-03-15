package com.example.grade_journal_back.report.controller;

import com.example.grade_journal_back.report.dto.ExportReportRequest;
import com.example.grade_journal_back.report.service.ExcelReportService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reports")
public class ReportExportController {

    private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ExcelReportService excelReportService;

    public ReportExportController(ExcelReportService excelReportService) {
        this.excelReportService = excelReportService;
    }

    @PostMapping("/export")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER', 'admin', 'teacher')")
    public ResponseEntity<ByteArrayResource> export(@RequestBody ExportReportRequest request) {
        if (!request.hasAnySelection()) {
            throw new IllegalArgumentException("Не выбран ни один раздел для экспорта");
        }

        byte[] bytes = excelReportService.generateWorkbook(request);
        String fileName = "grade-journal-report-" + FILE_FORMATTER.format(LocalDateTime.now()) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }
}