package com.example.grade_journal_back.health;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupDatabaseCheck {

    private final DatabaseHealthService databaseHealthService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Running startup database health check");

        Map<String, Object> result = databaseHealthService.check();

        log.info("Startup database health check completed: {}", result);
    }
}