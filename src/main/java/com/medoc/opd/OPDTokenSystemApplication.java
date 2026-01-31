package com.medoc.opd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OPD Token Allocation System
 * Backend Intern Assignment - Medoc Health
 *
 * A dynamic token allocation engine for hospital OPD that supports:
 * - Elastic capacity management
 * - Multi-source token allocation with priority
 * - Real-time reallocation for emergencies
 * - Complete REST API with validation
 *
 * Access Swagger UI: http://localhost:8080/swagger-ui.html
 */
@SpringBootApplication
public class OPDTokenSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(OPDTokenSystemApplication.class, args);
        System.out.println("\n" +
                "╔═══════════════════════════════════════════════════════════════════╗\n" +
                "║         OPD Token Allocation System - Started Successfully        ║\n" +
                "╠═══════════════════════════════════════════════════════════════════╣\n" +
                "║  API Documentation: http://localhost:8080/swagger-ui.html        ║\n" +
                "║  Health Check:      http://localhost:8080/actuator/health        ║\n" +
                "╚═══════════════════════════════════════════════════════════════════╝\n");
    }
}
