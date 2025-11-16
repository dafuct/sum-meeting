package com.zoomtranscriber.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for development profile.
 *
 * This application uses stateless JWT authentication across all profiles.
 * HTTP sessions are not used - authentication is handled via JWT tokens.
 * This configuration exists primarily for documentation and clarity.
 */
@Configuration
@Profile("dev")
public class DevSessionConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DevSessionConfiguration.class);

    public DevSessionConfiguration() {
        logger.info("Development profile detected - Application uses stateless JWT authentication (no HTTP sessions)");
    }
}
