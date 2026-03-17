package com.example.grade_journal_back.integration.holidays;

import com.example.grade_journal_back.integration.holidays.dto.NagerHolidayResponse;
import com.example.grade_journal_back.integration.holidays.dto.PublicHolidayDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
public class PublicHolidayService {

    private final RestTemplate restTemplate = new RestTemplate();

    public List<PublicHolidayDto> getPublicHolidays(Integer year, String countryCode) {
        log.info("Fetching public holidays for year={}, countryCode='{}'", year, countryCode);

        validateYear(year);
        String normalizedCountryCode = normalizeCountryCode(countryCode);

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://date.nager.at/api/v3/PublicHolidays/{year}/{countryCode}")
                    .buildAndExpand(year, normalizedCountryCode)
                    .toUriString();

            log.info(
                    "Sending request to public holiday API for year={}, countryCode='{}'",
                    year,
                    normalizedCountryCode
            );

            List<NagerHolidayResponse> holidays = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<NagerHolidayResponse>>() {}
            ).getBody();

            if (holidays == null) {
                log.info(
                        "Public holiday API returned no data for year={}, countryCode='{}'",
                        year,
                        normalizedCountryCode
                );
                return List.of();
            }

            List<PublicHolidayDto> result = holidays.stream()
                    .map(item -> new PublicHolidayDto(
                            item.date(),
                            item.localName(),
                            item.name(),
                            item.countryCode(),
                            item.fixed(),
                            item.global(),
                            item.launchYear(),
                            item.types() == null ? List.of() : item.types()
                    ))
                    .toList();

            log.info(
                    "Public holidays fetched successfully for year={}, countryCode='{}', count={}",
                    year,
                    normalizedCountryCode,
                    result.size()
            );

            return result;

        } catch (RestClientException exception) {
            log.error(
                    "Failed to fetch public holidays from external API for year={}, countryCode='{}': {}",
                    year,
                    normalizedCountryCode,
                    exception.getMessage(),
                    exception
            );
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Не удалось получить праздники из внешнего API: " + exception.getMessage(),
                    exception
            );
        } catch (Exception exception) {
            log.error(
                    "Unexpected error while processing public holiday API response for year={}, countryCode='{}': {}",
                    year,
                    normalizedCountryCode,
                    exception.getMessage(),
                    exception
            );
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Ошибка обработки ответа внешнего API: " + exception.getMessage(),
                    exception
            );
        }
    }

    private void validateYear(Integer year) {
        if (year == null) {
            log.warn("Public holiday request validation failed: year is null");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Параметр year обязателен");
        }

        if (year < 2000 || year > 2100) {
            log.warn("Public holiday request validation failed: year={} is out of range", year);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Параметр year должен быть в диапазоне от 2000 до 2100"
            );
        }
    }

    private String normalizeCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            log.warn("Public holiday request validation failed: countryCode is blank");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Параметр countryCode обязателен");
        }

        String normalized = countryCode.trim().toUpperCase(Locale.ROOT);

        if (!normalized.matches("^[A-Z]{2}$")) {
            log.warn("Public holiday request validation failed: invalid countryCode='{}'", countryCode);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "countryCode должен состоять из 2 латинских букв, например BY, AT, DE"
            );
        }

        return normalized;
    }
}