package com.example.grade_journal_back.integration.holidays.dto;

import java.time.LocalDate;
import java.util.List;

public record PublicHolidayDto(
        LocalDate date,
        String localName,
        String name,
        String countryCode,
        Boolean fixed,
        Boolean global,
        Integer launchYear,
        List<String> types
) {
}