package com.example.grade_journal_back.integration.github;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integrations/github")
public class GithubIntegrationController {

    private final GithubApiService githubApiService;

    public GithubIntegrationController(GithubApiService githubApiService) {
        this.githubApiService = githubApiService;
    }

    @GetMapping("/users/{username}")
    public GithubProfileDto getGithubProfile(@PathVariable String username) {
        return githubApiService.fetchProfile(username);
    }
}