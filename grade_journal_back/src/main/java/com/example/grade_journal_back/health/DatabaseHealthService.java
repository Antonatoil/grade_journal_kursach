package com.example.grade_journal_back.health;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DatabaseHealthService {

    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> check() {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            String databaseName = jdbcTemplate.queryForObject("select current_database()", String.class);
            Integer one = jdbcTemplate.queryForObject("select 1", Integer.class);

            response.put("connected", true);
            response.put("database", databaseName);
            response.put("probe", one);
            response.put("message", "Подключение к PostgreSQL успешно");
        } catch (Exception ex) {
            response.put("connected", false);
            response.put("message", ex.getMessage());
        }

        return response;
    }
}