package com.zoomtranscriber;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple Spring Boot application for testing.
 */
@RestController
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    JpaRepositoriesAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration.class
})
public class SimpleZoomTranscriberApplication {

    @GetMapping("/")
    public String home() {
        return "Zoom Transcriber Backend is Running!";
    }

    @GetMapping("/api/health")
    public String health() {
        return "{\"status\":\"UP\"}";
    }

    public static void main(String[] args) {
        SpringApplication.run(SimpleZoomTranscriberApplication.class, args);
    }
}