package com.medoc.opd.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for interactive API documentation.
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI opdTokenSystemAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OPD Token Allocation System API")
                        .description("Dynamic token allocation engine for hospital OPD with elastic capacity management, " +
                                "priority-based allocation, and real-time queue management.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Medoc Health")
                                .email("hr+assignment@medochealth.com"))
                        .license(new License()
                                .name("Backend Intern Assignment")
                                .url("https://medochealth.com")));
    }
}
