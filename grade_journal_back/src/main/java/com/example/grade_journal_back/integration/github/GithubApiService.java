package com.example.grade_journal_back.integration.github;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GithubApiService {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.github.com")
            .defaultHeader("Accept", "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .build();

    public GithubProfileDto fetchProfile(String username) {
        log.info("Fetching GitHub profile for username='{}'", username);

        Map<String, Object> profile = restClient.get()
                .uri("/users/{username}", username)
                .retrieve()
                .body(Map.class);

        log.info("GitHub profile data fetched for username='{}'", username);

        List<Map<String, Object>> repos = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{username}/repos")
                        .queryParam("sort", "updated")
                        .queryParam("per_page", 6)
                        .build(username))
                .retrieve()
                .body(List.class);

        List<GithubRepoDto> repositoryDtos = repos == null
                ? Collections.emptyList()
                : repos.stream().map(this::toRepoDto).toList();

        log.info(
                "GitHub repositories fetched for username='{}', count={}",
                username,
                repositoryDtos.size()
        );

        GithubProfileDto result = new GithubProfileDto(
                asString(profile.get("login")),
                asString(profile.get("name")),
                asString(profile.get("avatar_url")),
                asString(profile.get("html_url")),
                asString(profile.get("bio")),
                asInteger(profile.get("public_repos")),
                asInteger(profile.get("followers")),
                asInteger(profile.get("following")),
                repositoryDtos
        );

        log.info("GitHub profile assembled successfully for username='{}'", username);
        return result;
    }

    private GithubRepoDto toRepoDto(Map<String, Object> source) {
        return new GithubRepoDto(
                asString(source.get("name")),
                asString(source.get("html_url")),
                asString(source.get("description")),
                asString(source.get("language")),
                asInteger(source.get("stargazers_count")),
                asString(source.get("updated_at"))
        );
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        return Integer.parseInt(String.valueOf(value));
    }
}