package com.example.grade_journal_back.integration.holidays;

import com.example.grade_journal_back.integration.holidays.dto.NagerHolidayResponse;
import com.example.grade_journal_back.integration.holidays.dto.PublicHolidayDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Locale;

@Service
public class PublicHolidayService {

    private final RestTemplate restTemplate = new RestTemplate();

    public List<PublicHolidayDto> getPublicHolidays(Integer year, String countryCode) {
        validateYear(year);
        String normalizedCountryCode = normalizeCountryCode(countryCode);

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://date.nager.at/api/v3/PublicHolidays/{year}/{countryCode}")
                    .buildAndExpand(year, normalizedCountryCode)
                    .toUriString();

            List<NagerHolidayResponse> holidays = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<NagerHolidayResponse>>() {}
            ).getBody();

            if (holidays == null) {
                return List.of();
            }

            return holidays.stream()
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

        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Не удалось получить праздники из внешнего API: " + exception.getMessage(),
                    exception
            );
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Ошибка обработки ответа внешнего API: " + exception.getMessage(),
                    exception
            );
        }
    }

    private void validateYear(Integer year) {
        if (year == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Параметр year обязателен");
        }

        if (year < 2000 || year > 2100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Параметр year должен быть в диапазоне от 2000 до 2100"
            );
        }
    }

    private String normalizeCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Параметр countryCode обязателен");
        }

        String normalized = countryCode.trim().toUpperCase(Locale.ROOT);

        if (!normalized.matches("^[A-Z]{2}$")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "countryCode должен состоять из 2 латинских букв, например BY, AT, DE"
            );
        }

        return normalized;
    }
}