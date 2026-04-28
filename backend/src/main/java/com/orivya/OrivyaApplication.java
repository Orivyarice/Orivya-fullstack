package com.orivya;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * OrivyaApplication — Main entry point for the Spring Boot application.
 *
 * @SpringBootApplication combines:
 *   - @Configuration      : marks this as a config class
 *   - @EnableAutoConfiguration : enables Spring Boot auto-config
 *   - @ComponentScan      : scans all sub-packages for beans
 */
@SpringBootApplication
@EnableAsync
public class OrivyaApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrivyaApplication.class, args);
        System.out.println("🌾 Orivya Rice Backend is running at http://localhost:8080");
    }
}
