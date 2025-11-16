package com.zoomtranscriber;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.zoomtranscriber.config.ApplicationConfig;

@SpringBootApplication
@EnableConfigurationProperties({ApplicationConfig.class})
@EnableAsync
@EnableScheduling
public class ZoomTranscriberApplication {

    public static void main(String[] args) {
        var app = new SpringApplication(ZoomTranscriberApplication.class);

        app.setDefaultProperties(java.util.Map.of(
            "spring.application.name", "zoom-transcriber",
            "server.port", "8080",
            "management.endpoints.web.exposure.include", "health,info,metrics",
            "management.endpoint.health.show-details", "when-authorized",
            "logging.level.com.zoomtranscriber", "INFO",
            "logging.level.org.springframework.web", "DEBUG",
            "logging.level.org.hibernate.SQL", "DEBUG",
            "logging.level.org.hibernate.type.descriptor.sql.BasicBinder", "TRACE"
        ));

        app.run(args);
    }
}