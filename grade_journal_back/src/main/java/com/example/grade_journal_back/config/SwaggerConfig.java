package com.example.grade_journal_back.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI gradeJournalOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Grade Journal API")
                .version("1.0")
                .description("Документация backend для электронного журнала успеваемости"))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
            .schemaRequirement(
                BEARER_SCHEME_NAME,
                new SecurityScheme()
                    .name(BEARER_SCHEME_NAME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
            );
    }
}