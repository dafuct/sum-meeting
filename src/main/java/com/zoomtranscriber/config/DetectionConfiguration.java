package com.zoomtranscriber.config;

import com.zoomtranscriber.core.detection.MeetingStateTracker;
import com.zoomtranscriber.core.detection.ProcessMonitor;
import com.zoomtranscriber.platform.macos.MacOSProcessMonitor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration class for meeting detection components.
 * Wires up the necessary beans for Zoom meeting detection functionality.
 */
@Configuration
public class DetectionConfiguration {

    /**
     * Creates the platform-specific process monitor bean.
     * Currently configured for macOS since the user is on a Darwin system.
     * In a production environment, this would be determined based on the operating system.
     */
    @Bean
    @Primary
    public ProcessMonitor processMonitor() {
        // Since we're on macOS, use the macOS implementation
        return new MacOSProcessMonitor();
    }

    /**
     * Creates the meeting state tracker bean.
     * This component manages the state of detected meetings.
     */
    @Bean
    public MeetingStateTracker meetingStateTracker() {
        return new MeetingStateTracker();
    }
}