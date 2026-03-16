package com.example.grade_journal_back.integration.github;

public record GithubRepoDto(
        String name,
        String htmlUrl,
        String description,
        String language,
        Integer stars,
        String updatedAt
) {
}