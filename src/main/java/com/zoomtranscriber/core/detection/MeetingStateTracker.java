package com.zoomtranscriber.core.detection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Tracks the state of Zoom meetings based on process events and window titles.
 * Manages meeting lifecycle and state transitions.
 */
@Component
public class MeetingStateTracker {
    
    private static final Logger logger = LoggerFactory.getLogger(MeetingStateTracker.class);
    
    // Patterns to detect Zoom meeting windows
    private static final Pattern ZOOM_MEETING_PATTERN = Pattern.compile(
        ".*Zoom.*Meeting.*|.*Zoom Meeting.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern ZOOM_WINDOW_PATTERN = Pattern.compile(
        ".*zoom.*", Pattern.CASE_INSENSITIVE);
    
    private final ConcurrentHashMap<UUID, MeetingState> activeMeetings = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UUID> processToMeetingMap = new ConcurrentHashMap<>();
    
    /**
     * Processes a process event and updates meeting states accordingly.
     * 
     * @param processEvent the process event to process
     * @return Flux of MeetingEvent objects representing state changes
     */
    public Flux<MeetingEvent> processProcessEvent(ProcessMonitor.ProcessEvent processEvent) {
        return Mono.fromCallable(() -> {
            logger.debug("Processing process event: {}", processEvent);
            
            return switch (processEvent.eventType()) {
                case PROCESS_STARTED -> handleProcessStarted(processEvent);
                case PROCESS_ENDED -> handleProcessEnded(processEvent);
                case PROCESS_MODIFIED -> handleProcessModified(processEvent);
            };
        })
        .flatMapMany(events -> Flux.fromIterable(events))
        .doOnNext(event -> logger.debug("Generated meeting event: {}", event));
    }
    
    /**
     * Handles a process start event.
     * 
     * @param processEvent the process start event
     * @return list of meeting events generated
     */
    private java.util.List<MeetingEvent> handleProcessStarted(ProcessMonitor.ProcessEvent processEvent) {
        var events = new java.util.ArrayList<MeetingEvent>();
        
        // Check if this is a Zoom process
        if (isZoomProcess(processEvent.processName())) {
            logger.info("Detected Zoom process start: {}", processEvent.processId());
            
            // Create a new meeting for this process
            var meetingId = UUID.randomUUID();
            var meetingState = new MeetingState(
                meetingId,
                "Zoom Meeting",
                ZoomDetectionService.MeetingState.MeetingStatus.DETECTED,
                LocalDateTime.now(),
                null,
                processEvent.processId(),
                0,
                LocalDateTime.now()
            );
            
            activeMeetings.put(meetingId, meetingState);
            processToMeetingMap.put(processEvent.processId(), meetingId);
            
            var meetingEvent = new MeetingEvent(
                meetingId,
                ZoomDetectionService.MeetingEvent.MeetingEventType.MEETING_STARTED,
                LocalDateTime.now(),
                processEvent.processId(),
                "Zoom Meeting"
            );
            
            events.add(meetingEvent);
            logger.info("Created new meeting {} for process {}", meetingId, processEvent.processId());
        }
        
        return events;
    }
    
    /**
     * Handles a process end event.
     * 
     * @param processEvent the process end event
     * @return list of meeting events generated
     */
    private java.util.List<MeetingEvent> handleProcessEnded(ProcessMonitor.ProcessEvent processEvent) {
        var events = new java.util.ArrayList<MeetingEvent>();
        
        var meetingId = processToMeetingMap.remove(processEvent.processId());
        if (meetingId != null) {
            var meetingState = activeMeetings.remove(meetingId);
            if (meetingState != null) {
                logger.info("Zoom process ended, ending meeting {}: {}", meetingId, processEvent.processId());
                
                // Update meeting state
                var updatedState = new MeetingState(
                    meetingState.meetingId(),
                    meetingState.title(),
                    ZoomDetectionService.MeetingState.MeetingStatus.ENDED,
                    meetingState.startTime(),
                    LocalDateTime.now(),
                    meetingState.processId(),
                    meetingState.participantCount(),
                    LocalDateTime.now()
                );
                
                activeMeetings.put(meetingId, updatedState);
                
                var meetingEvent = new MeetingEvent(
                    meetingId,
                    ZoomDetectionService.MeetingEvent.MeetingEventType.MEETING_ENDED,
                    LocalDateTime.now(),
                    processEvent.processId(),
                    meetingState.title()
                );
                
                events.add(meetingEvent);
                
                // Remove from active meetings after a delay
                Mono.delay(java.time.Duration.ofSeconds(30))
                    .subscribe(tick -> {
                        activeMeetings.remove(meetingId);
                        logger.debug("Cleaned up ended meeting {}", meetingId);
                    });
            }
        }
        
        return events;
    }
    
    /**
     * Handles a process modification event.
     * 
     * @param processEvent the process modification event
     * @return list of meeting events generated
     */
    private java.util.List<MeetingEvent> handleProcessModified(ProcessMonitor.ProcessEvent processEvent) {
        var events = new java.util.ArrayList<MeetingEvent>();
        
        var meetingId = processToMeetingMap.get(processEvent.processId());
        if (meetingId != null) {
            var meetingState = activeMeetings.get(meetingId);
            if (meetingState != null) {
                // Update last updated time
                var updatedState = new MeetingState(
                    meetingState.meetingId(),
                    meetingState.title(),
                    meetingState.status(),
                    meetingState.startTime(),
                    meetingState.endTime(),
                    meetingState.processId(),
                    meetingState.participantCount(),
                    LocalDateTime.now()
                );
                
                activeMeetings.put(meetingId, updatedState);
                logger.debug("Updated meeting {} state", meetingId);
            }
        }
        
        return events;
    }
    
    /**
     * Updates meeting information based on window title.
     * 
     * @param processId the process ID
     * @param windowTitle the window title
     * @return Flux of meeting events generated
     */
    public Flux<MeetingEvent> updateMeetingFromWindowTitle(String processId, String windowTitle) {
        return Mono.fromCallable(() -> {
            var events = new java.util.ArrayList<MeetingEvent>();
            
            var meetingId = processToMeetingMap.get(processId);
            if (meetingId != null) {
                var meetingState = activeMeetings.get(meetingId);
                if (meetingState != null) {
                    var newStatus = determineMeetingStatus(windowTitle);
                    var newTitle = extractMeetingTitle(windowTitle);
                    
                    // Check if status changed
                    if (meetingState.status() != newStatus) {
                        var eventType = switch (newStatus) {
                            case ACTIVE -> ZoomDetectionService.MeetingEvent.MeetingEventType.MEETING_RESUMED;
                            case PAUSED -> ZoomDetectionService.MeetingEvent.MeetingEventType.MEETING_PAUSED;
                            default -> null;
                        };
                        
                        if (eventType != null) {
                            var meetingEvent = new MeetingEvent(
                                meetingId,
                                eventType,
                                LocalDateTime.now(),
                                processId,
                                newTitle
                            );
                            events.add(meetingEvent);
                        }
                    }
                    
                    // Update meeting state
                    var updatedState = new MeetingState(
                        meetingState.meetingId(),
                        newTitle,
                        newStatus,
                        meetingState.startTime(),
                        meetingState.endTime(),
                        meetingState.processId(),
                        meetingState.participantCount(),
                        LocalDateTime.now()
                    );
                    
                    activeMeetings.put(meetingId, updatedState);
                    logger.debug("Updated meeting {} from window title: {}", meetingId, windowTitle);
                }
            }
            
            return events;
        })
        .flatMapMany(events -> Flux.fromIterable(events));
    }
    
    /**
     * Gets the current state of a meeting.
     * 
     * @param meetingId the meeting ID
     * @return Mono containing the meeting state or empty if not found
     */
    public Mono<ZoomDetectionService.MeetingState> getMeetingState(UUID meetingId) {
        return Mono.justOrEmpty(activeMeetings.get(meetingId))
            .map(state -> new ZoomDetectionService.MeetingState(
                state.meetingId(),
                state.title(),
                convertStatus(state.status()),
                state.startTime(),
                state.endTime(),
                state.processId(),
                state.participantCount(),
                state.lastUpdated()
            ));
    }
    
    /**
     * Gets all currently active meetings.
     * 
     * @return Flux of meeting states for all active meetings
     */
    public Flux<ZoomDetectionService.MeetingState> getActiveMeetings() {
        return Flux.fromIterable(activeMeetings.values())
            .filter(state -> state.status() != ZoomDetectionService.MeetingState.MeetingStatus.ENDED)
            .map(state -> new ZoomDetectionService.MeetingState(
                state.meetingId(),
                state.title(),
                convertStatus(state.status()),
                state.startTime(),
                state.endTime(),
                state.processId(),
                state.participantCount(),
                state.lastUpdated()
            ));
    }
    
    /**
     * Checks if a process name indicates a Zoom process.
     * 
     * @param processName the process name
     * @return true if it's a Zoom process
     */
    private boolean isZoomProcess(String processName) {
        return processName != null && 
               (processName.toLowerCase().contains("zoom") || 
                ZOOM_WINDOW_PATTERN.matcher(processName).matches());
    }
    
    /**
     * Determines meeting status from window title.
     * 
     * @param windowTitle the window title
     * @return the meeting status
     */
    private ZoomDetectionService.MeetingState.MeetingStatus determineMeetingStatus(String windowTitle) {
        if (windowTitle == null) {
            return ZoomDetectionService.MeetingState.MeetingStatus.DETECTED;
        }
        
        var title = windowTitle.toLowerCase();
        if (title.contains("meeting") && !title.contains("waiting") && !title.contains("lobby")) {
            return ZoomDetectionService.MeetingState.MeetingStatus.ACTIVE;
        } else if (title.contains("waiting") || title.contains("lobby")) {
            return ZoomDetectionService.MeetingState.MeetingStatus.DETECTED;
        } else if (title.contains("paused") || title.contains("inactive")) {
            return ZoomDetectionService.MeetingState.MeetingStatus.PAUSED;
        }
        
        return ZoomDetectionService.MeetingState.MeetingStatus.DETECTED;
    }
    
    /**
     * Extracts meeting title from window title.
     * 
     * @param windowTitle the window title
     * @return the extracted meeting title
     */
    private String extractMeetingTitle(String windowTitle) {
        if (windowTitle == null) {
            return "Zoom Meeting";
        }
        
        // Try to extract a more meaningful title
        if (ZOOM_MEETING_PATTERN.matcher(windowTitle).matches()) {
            return windowTitle;
        }
        
        return "Zoom Meeting";
    }
    
    /**
     * Converts internal meeting status to public meeting status.
     * 
     * @param internalStatus the internal status
     * @return the public status
     */
    private ZoomDetectionService.MeetingState.MeetingStatus convertStatus(
            ZoomDetectionService.MeetingState.MeetingStatus internalStatus) {
        return internalStatus;
    }
    
    /**
     * Internal meeting state representation.
     */
    private record MeetingState(
        UUID meetingId,
        String title,
        ZoomDetectionService.MeetingState.MeetingStatus status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String processId,
        int participantCount,
        LocalDateTime lastUpdated
    ) {}
    
    /**
     * Meeting event for state changes.
     */
    public record MeetingEvent(
        UUID meetingId,
        ZoomDetectionService.MeetingEvent.MeetingEventType eventType,
        LocalDateTime timestamp,
        String processId,
        String windowTitle
    ) {}
}