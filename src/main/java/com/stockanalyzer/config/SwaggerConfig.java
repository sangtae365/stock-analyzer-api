package com.stockanalyzer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Stock Analyzer API")
                        .description("KIS OpenAPI 기반 주식 시세 분석 서비스")
                        .version("v1.0.0"));
    }
}
