package com.bohuai.ocs.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI document at /v3/api-docs, interactive UI at /swagger-ui.html. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ocsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("OpenCourse API")
                        .version("1.0")
                        .description("Online Course Management System. Sign in with POST /api/auth/login and "
                                + "use the returned token as a Bearer token (browsers use httpOnly cookies)."))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
