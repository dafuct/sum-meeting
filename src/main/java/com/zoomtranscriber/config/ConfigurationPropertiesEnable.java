package com.zoomtranscriber.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to enable all configuration properties.
 * This ensures Spring Boot processes all @ConfigurationProperties classes.
 */
@Configuration
@EnableConfigurationProperties({
    // Application-level configurations
    AudioConfig.class,
    OllamaConfig.class
})
public class ConfigurationPropertiesEnable {
    // This class only serves to enable all configuration properties
    // Spring Boot will automatically detect and configure the properties classes
}