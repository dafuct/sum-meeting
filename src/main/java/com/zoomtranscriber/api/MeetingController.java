package com.zoomtranscriber.api;

import com.zoomtranscriber.core.detection.ZoomDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST API controller for meeting detection operations.
 * Provides endpoints for managing meeting detection and retrieving meeting information.
 */
@RestController
@RequestMapping("/api/meetings")
@CrossOrigin(origins = "*")
public class MeetingController {
    
    private static final Logger logger = LoggerFactory.getLogger(MeetingController.class);
    
    private final ZoomDetectionService zoomDetectionService;
    
    public MeetingController(ZoomDetectionService zoomDetectionService) {
        this.zoomDetectionService = zoomDetectionService;
    }
    
    /**
     * Starts meeting monitoring.
     * 
     * @return response indicating success or failure
     */
    @PostMapping("/monitoring/start")
    public Mono<ResponseEntity<ApiResponse>> startMonitoring() {
        logger.info("Starting meeting monitoring via REST API");
        
        return zoomDetectionService.startMonitoring()
            .map(result -> ResponseEntity.ok(
                new ApiResponse(true, "Meeting monitoring started successfully")))
            .onErrorResume(error -> {
                logger.error("Failed to start meeting monitoring", error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to start monitoring: " + error.getMessage())));
            });
    }
    
    /**
     * Stops meeting monitoring.
     * 
     * @return response indicating success or failure
     */
    @PostMapping("/monitoring/stop")
    public Mono<ResponseEntity<ApiResponse>> stopMonitoring() {
        logger.info("Stopping meeting monitoring via REST API");
        
        return zoomDetectionService.stopMonitoring()
            .map(result -> ResponseEntity.ok(
                new ApiResponse(true, "Meeting monitoring stopped successfully")))
            .onErrorResume(error -> {
                logger.error("Failed to stop meeting monitoring", error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to stop monitoring: " + error.getMessage())));
            });
    }
    
    /**
     * Gets the current monitoring status.
     * 
     * @return monitoring status
     */
    @GetMapping("/monitoring/status")
    public ResponseEntity<MonitoringStatusResponse> getMonitoringStatus() {
        var isMonitoring = zoomDetectionService.isMonitoring();
        var response = new MonitoringStatusResponse(isMonitoring);
        
        logger.debug("Monitoring status requested: {}", isMonitoring);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Triggers a manual detection scan.
     * 
     * @return response indicating success or failure
     */
    @PostMapping("/scan")
    public Mono<ResponseEntity<ApiResponse>> triggerDetectionScan() {
        logger.info("Triggering manual detection scan via REST API");
        
        return zoomDetectionService.triggerDetectionScan()
            .map(result -> ResponseEntity.ok(
                new ApiResponse(true, "Detection scan completed successfully")))
            .onErrorResume(error -> {
                logger.error("Failed to trigger detection scan", error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to trigger scan: " + error.getMessage())));
            });
    }
    
    /**
     * Gets all currently active meetings.
     * 
     * @return list of active meetings
     */
    @GetMapping("/active")
    public ResponseEntity<ActiveMeetingsResponse> getActiveMeetings() {
        logger.debug("Retrieving active meetings via REST API");
        
        var meetings = zoomDetectionService.getActiveMeetings()
            .map(MeetingDto::from)
            .collectList()
            .block(); // Block for REST API response
        
        var response = new ActiveMeetingsResponse(meetings);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Gets details for a specific meeting.
     * 
     * @param meetingId the meeting ID
     * @return meeting details or 404 if not found
     */
    @GetMapping("/{meetingId}")
    public Mono<ResponseEntity<MeetingDto>> getMeeting(@PathVariable UUID meetingId) {
        logger.debug("Retrieving meeting details for ID: {}", meetingId);
        
        return zoomDetectionService.getCurrentMeetingState(meetingId)
            .map(state -> ResponseEntity.ok(MeetingDto.from(state)))
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
            .onErrorResume(error -> {
                logger.error("Error retrieving meeting {}", meetingId, error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            });
    }
    
    /**
     * Gets meeting events as a server-sent events stream.
     * 
     * @return SSE stream of meeting events
     */
    @GetMapping(value = "/events", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public reactor.core.publisher.Flux<org.springframework.http.codec.ServerSentEvent<MeetingEventDto>> getMeetingEvents() {
        logger.info("Meeting events stream requested");
        
        return zoomDetectionService.getMeetingEvents()
            .map(MeetingEventDto::from)
            .map(event -> org.springframework.http.codec.ServerSentEvent.<MeetingEventDto>builder(event)
                .id(event.meetingId().toString())
                .event(event.eventType().toString())
                .build());
    }
    
    /**
     * Gets system health information related to meeting detection.
     * 
     * @return health status
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        logger.debug("Health check requested for meeting detection");
        
        var isMonitoring = zoomDetectionService.isMonitoring();
        var activeMeetings = zoomDetectionService.getActiveMeetings()
            .count()
            .block(); // Block for health check
        
        var health = new HealthResponse(
            isMonitoring ? "HEALTHY" : "STOPPED",
            isMonitoring,
            activeMeetings.intValue()
        );
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Generic API response wrapper.
     */
    public record ApiResponse(
        boolean success,
        String message
    ) {}
    
    /**
     * Monitoring status response.
     */
    public record MonitoringStatusResponse(
        boolean isMonitoring
    ) {}
    
    /**
     * Active meetings response.
     */
    public record ActiveMeetingsResponse(
        java.util.List<MeetingDto> meetings
    ) {}
    
    /**
     * Meeting DTO for API responses.
     */
    public record MeetingDto(
        UUID meetingId,
        String title,
        String status,
        java.time.LocalDateTime startTime,
        java.time.LocalDateTime endTime,
        String processId,
        int participantCount,
        java.time.LocalDateTime lastUpdated
    ) {
        static MeetingDto from(ZoomDetectionService.MeetingState state) {
            return new MeetingDto(
                state.meetingId(),
                state.title(),
                state.status().toString(),
                state.startTime(),
                state.endTime(),
                state.processId(),
                state.participantCount(),
                state.lastUpdated()
            );
        }
    }
    
    /**
     * Meeting event DTO for API responses.
     */
    public record MeetingEventDto(
        UUID meetingId,
        String eventType,
        java.time.LocalDateTime timestamp,
        String processId,
        String windowTitle
    ) {
        static MeetingEventDto from(ZoomDetectionService.MeetingEvent event) {
            return new MeetingEventDto(
                event.meetingId(),
                event.eventType().toString(),
                event.timestamp(),
                event.processId(),
                event.windowTitle()
            );
        }
    }
    
    /**
     * Health check response.
     */
    public record HealthResponse(
        String status,
        boolean isMonitoring,
        int activeMeetingCount
    ) {}
}