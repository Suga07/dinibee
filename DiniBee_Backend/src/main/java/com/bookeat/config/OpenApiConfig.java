package com.bookeat.config;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
    @Bean public OpenAPI bookeatOpenApi() {
        return new OpenAPI().info(new Info().title("BookEat / DiniBee API").version("v1 / phase-1-2")
                .description("Foundation and authentication. Business modules follow in later phases."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
