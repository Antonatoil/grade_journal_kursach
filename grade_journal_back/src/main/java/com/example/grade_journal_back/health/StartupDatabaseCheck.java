package com.example.grade_journal_back.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupDatabaseCheck {

    private final DatabaseHealthService databaseHealthService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        Map<String, Object> result = databaseHealthService.check();
        log.info("Database check result: {}", result);
    }
}