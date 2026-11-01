package com.yp.selfheal.dc.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Self-Healing Distributed Cache (DC) API")
                        .version("1.0.0")
                        .description("REST API and cluster telemetry for self-healing distributed caching engine")
                        .license(new License().name("Apache 2.0")));
    }
}
