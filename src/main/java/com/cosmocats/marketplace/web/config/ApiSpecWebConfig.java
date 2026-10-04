package com.cosmocats.marketplace.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the OpenAPI contract from resources/api-specs so Swagger UI can display it.
 */
@Configuration
public class ApiSpecWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api-specs/**")
                .addResourceLocations("classpath:/api-specs/");
    }
}
