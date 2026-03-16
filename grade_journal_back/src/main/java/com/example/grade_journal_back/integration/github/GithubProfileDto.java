package com.example.grade_journal_back.integration.github;

import java.util.List;

public record GithubProfileDto(
        String username,
        String displayName,
        String avatarUrl,
        String htmlUrl,
        String bio,
        Integer publicRepos,
        Integer followers,
        Integer following,
        List<GithubRepoDto> repositories
) {
}