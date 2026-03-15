package com.example.grade_journal_back.health;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DbHealthController {

    private final DatabaseHealthService databaseHealthService;

    @GetMapping("/api/health/db")
    public Map<String, Object> checkDatabase() {
        return databaseHealthService.check();
    }
}