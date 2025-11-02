package com.zoomtranscriber.core.detection;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service interface for detecting Zoom meetings and tracking their state.
 * Provides reactive streams for meeting detection events and state management.
 */
public interface ZoomDetectionService {
    
    /**
     * Starts monitoring for Zoom meetings.
     * 
     * @return Mono that completes when monitoring starts successfully
     */
    Mono<Void> startMonitoring();
    
    /**
     * Stops monitoring for Zoom meetings.
     * 
     * @return Mono that completes when monitoring stops successfully
     */
    Mono<Void> stopMonitoring();
    
    /**
     * Gets the current monitoring status.
     * 
     * @return true if monitoring is active, false otherwise
     */
    boolean isMonitoring();
    
    /**
     * Gets a stream of meeting detection events.
     * 
     * @return Flux of MeetingEvent objects representing meeting state changes
     */
    Flux<MeetingEvent> getMeetingEvents();
    
    /**
     * Gets the current state of a specific meeting.
     * 
     * @param meetingId the UUID of the meeting
     * @return Mono containing the current MeetingState or empty if not found
     */
    Mono<MeetingState> getCurrentMeetingState(UUID meetingId);
    
    /**
     * Gets all currently active meetings.
     * 
     * @return Flux of MeetingState objects for all active meetings
     */
    Flux<MeetingState> getActiveMeetings();
    
    /**
     * Manually triggers a meeting detection scan.
     * 
     * @return Mono that completes when scan finishes
     */
    Mono<Void> triggerDetectionScan();
    
    /**
     * Represents a meeting detection event.
     */
    record MeetingEvent(
        UUID meetingId,
        MeetingEventType eventType,
        LocalDateTime timestamp,
        String processId,
        String windowTitle
    ) {
        public enum MeetingEventType {
            MEETING_STARTED,
            MEETING_ENDED,
            MEETING_PAUSED,
            MEETING_RESUMED,
            PARTICIPANT_JOINED,
            PARTICIPANT_LEFT
        }
    }
    
    /**
     * Represents the current state of a meeting.
     */
    record MeetingState(
        UUID meetingId,
        String title,
        MeetingStatus status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String processId,
        int participantCount,
        LocalDateTime lastUpdated
    ) {
        public enum MeetingStatus {
            DETECTED,
            ACTIVE,
            PAUSED,
            ENDED,
            ERROR
        }
    }
}