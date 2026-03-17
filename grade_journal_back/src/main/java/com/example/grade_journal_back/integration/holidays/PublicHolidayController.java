package com.example.grade_journal_back.integration.holidays;

import com.example.grade_journal_back.integration.holidays.dto.PublicHolidayDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/integrations")
public class PublicHolidayController {

    private final PublicHolidayService publicHolidayService;

    public PublicHolidayController(PublicHolidayService publicHolidayService) {
        this.publicHolidayService = publicHolidayService;
    }

    @GetMapping("/public-holidays")
    public List<PublicHolidayDto> getPublicHolidays(
            @RequestParam Integer year,
            @RequestParam String countryCode
    ) {
        return publicHolidayService.getPublicHolidays(year, countryCode);
    }
}