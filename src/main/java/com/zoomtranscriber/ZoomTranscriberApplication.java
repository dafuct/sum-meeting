package com.zoomtranscriber;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ZoomTranscriberApplication {

    public static void main(String[] args) {
        new SpringApplication(ZoomTranscriberApplication.class).run(args);
    }
}