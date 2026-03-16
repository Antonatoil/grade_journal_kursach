package com.example.grade_journal_back.integration.oauth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class OAuth2IntegrationSecurityConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain oauth2SecurityFilterChain(
            HttpSecurity http,
            GithubOAuth2SuccessHandler successHandler
    ) throws Exception {
        http.securityMatcher("/oauth2/**", "/login/oauth2/**", "/api/auth/oauth2/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Login(oauth -> oauth.successHandler(successHandler))
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}