package com.zoomtranscriber.websocket;

import com.zoomtranscriber.core.detection.ZoomDetectionService;
import com.zoomtranscriber.core.transcription.TranscriptionSegment;
import com.zoomtranscriber.core.transcription.TranscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * WebSocket controller for real-time communication.
 * Handles real-time updates for transcription, meeting detection, and summaries.
 */
@Controller
public class RealTimeController {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeController.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ZoomDetectionService zoomDetectionService;
    private final TranscriptionService transcriptionService;

    // Store active transcription sessions
    private final ConcurrentHashMap<UUID, List<String>> activeTranscriptionSessions = new ConcurrentHashMap<>();

    public RealTimeController(SimpMessagingTemplate messagingTemplate,
                             ZoomDetectionService zoomDetectionService,
                             TranscriptionService transcriptionService) {
        this.messagingTemplate = messagingTemplate;
        this.zoomDetectionService = zoomDetectionService;
        this.transcriptionService = transcriptionService;
    }

    /**
     * Handles client connection requests.
     */
    @MessageMapping("/connect")
    public void handleConnection(@Payload ConnectionMessage message) {
        logger.info("Client connected: {}", message.clientId());
        
        // Send acknowledgment
        messagingTemplate.convertAndSendToUser(
            message.clientId(),
            "/queue/connected",
            new ConnectionAckMessage(true, "Connected successfully", LocalDateTime.now())
        );

        // Send current active meetings
        sendActiveMeetingsUpdate(message.clientId());
    }

    /**
     * Handles meeting monitoring start requests.
     */
    @MessageMapping("/meetings/start-monitoring")
    @SendTo("/topic/meetings/monitoring")
    public MeetingMonitoringMessage startMeetingMonitoring(@Payload MeetingMonitoringRequest request) {
        logger.info("Starting meeting monitoring for client: {}", request.clientId());

        try {
            zoomDetectionService.startMonitoring().subscribe();
            
            return new MeetingMonitoringMessage(
                request.meetingId(),
                "monitoring_started",
                "Meeting monitoring started successfully",
                LocalDateTime.now()
            );
        } catch (Exception e) {
            logger.error("Failed to start meeting monitoring", e);
            return new MeetingMonitoringMessage(
                request.meetingId(),
                "monitoring_error",
                "Failed to start monitoring: " + e.getMessage(),
                LocalDateTime.now()
            );
        }
    }

    /**
     * Handles meeting monitoring stop requests.
     */
    @MessageMapping("/meetings/stop-monitoring")
    @SendTo("/topic/meetings/monitoring")
    public MeetingMonitoringMessage stopMeetingMonitoring(@Payload MeetingMonitoringRequest request) {
        logger.info("Stopping meeting monitoring for client: {}", request.clientId());

        try {
            zoomDetectionService.stopMonitoring().subscribe();
            
            return new MeetingMonitoringMessage(
                request.meetingId(),
                "monitoring_stopped",
                "Meeting monitoring stopped successfully",
                LocalDateTime.now()
            );
        } catch (Exception e) {
            logger.error("Failed to stop meeting monitoring", e);
            return new MeetingMonitoringMessage(
                request.meetingId(),
                "monitoring_error",
                "Failed to stop monitoring: " + e.getMessage(),
                LocalDateTime.now()
            );
        }
    }

    /**
     * Handles transcription start requests.
     */
    @MessageMapping("/transcription/start")
    @SendTo("/topic/transcription/status")
    public TranscriptionStatusMessage startTranscription(@Payload TranscriptionRequest request) {
        logger.info("Starting transcription for meeting: {}", request.meetingId());

        try {
            // Add client to active transcription session
            activeTranscriptionSessions.computeIfAbsent(request.meetingId(), k -> new CopyOnWriteArrayList<>())
                    .add(request.clientId());

            // Start transcription
            var config = new com.zoomtranscriber.core.transcription.TranscriptionService.TranscriptionConfig(
                request.language(),
                request.enableSpeakerDiarization(),
                request.confidenceThreshold(),
                request.enablePunctuation(),
                request.enableCapitalization(),
                request.enableTimestamps(),
                request.maxSpeakers(),
                request.model()
            );

            transcriptionService.startTranscription(request.meetingId(), config).subscribe();

            return new TranscriptionStatusMessage(
                request.meetingId(),
                "transcription_started",
                "Transcription started successfully",
                LocalDateTime.now()
            );

        } catch (Exception e) {
            logger.error("Failed to start transcription", e);
            return new TranscriptionStatusMessage(
                request.meetingId(),
                "transcription_error",
                "Failed to start transcription: " + e.getMessage(),
                LocalDateTime.now()
            );
        }
    }

    /**
     * Handles transcription stop requests.
     */
    @MessageMapping("/transcription/stop")
    @SendTo("/topic/transcription/status")
    public TranscriptionStatusMessage stopTranscription(@Payload TranscriptionRequest request) {
        logger.info("Stopping transcription for meeting: {}", request.meetingId());

        try {
            // Remove client from active transcription session
            List<String> clients = activeTranscriptionSessions.get(request.meetingId());
            if (clients != null) {
                clients.remove(request.clientId());
                if (clients.isEmpty()) {
                    activeTranscriptionSessions.remove(request.meetingId());
                    // Stop transcription only when no clients are listening
                    transcriptionService.stopTranscription(request.meetingId()).subscribe();
                }
            }

            return new TranscriptionStatusMessage(
                request.meetingId(),
                "transcription_stopped",
                "Transcription stopped successfully",
                LocalDateTime.now()
            );

        } catch (Exception e) {
            logger.error("Failed to stop transcription", e);
            return new TranscriptionStatusMessage(
                request.meetingId(),
                "transcription_error",
                "Failed to stop transcription: " + e.getMessage(),
                LocalDateTime.now()
            );
        }
    }

    /**
     * Handles summary generation requests.
     */
    @MessageMapping("/summary/generate")
    public void generateSummary(@Payload SummaryRequest request) {
        logger.info("Generating {} summary for meeting: {}", request.summaryType(), request.meetingId());

        // Start summary generation and send progress updates
        try {
            // Send start notification
            messagingTemplate.convertAndSendToUser(
                request.clientId(),
                "/queue/summary/progress",
                new SummaryProgressMessage(
                    request.meetingId(),
                    request.summaryType(),
                    0.0,
                    "Starting summary generation...",
                    LocalDateTime.now()
                )
            );

            // Generate summary (this would be implemented to send progress updates)
            // For now, just send a completion message
            messagingTemplate.convertAndSendToUser(
                request.clientId(),
                "/queue/summary/progress",
                new SummaryProgressMessage(
                    request.meetingId(),
                    request.summaryType(),
                    1.0,
                    "Summary generation completed",
                    LocalDateTime.now()
                )
            );

        } catch (Exception e) {
            logger.error("Failed to generate summary", e);
            messagingTemplate.convertAndSendToUser(
                request.clientId(),
                "/queue/summary/progress",
                new SummaryProgressMessage(
                    request.meetingId(),
                    request.summaryType(),
                    -1.0,
                    "Failed to generate summary: " + e.getMessage(),
                    LocalDateTime.now()
                )
            );
        }
    }

    /**
     * Sends transcription segment updates to clients.
     */
    public void sendTranscriptionSegment(UUID meetingId, TranscriptionSegment segment) {
        List<String> clients = activeTranscriptionSessions.get(meetingId);
        if (clients != null && !clients.isEmpty()) {
            TranscriptionSegmentMessage message = new TranscriptionSegmentMessage(
                meetingId,
                segment.getId(),
                segment.getText(),
                segment.getSpeakerId(),
                segment.getConfidence(),
                segment.getTimestamp(),
                segment.isFinal()
            );

            messagingTemplate.convertAndSend("/topic/transcription/" + meetingId, message);
        }
    }

    /**
     * Sends meeting event updates to clients.
     */
    public void sendMeetingEvent(String eventType, Object eventData) {
        messagingTemplate.convertAndSend("/topic/meetings/events", 
            new MeetingEventMessage(eventType, eventData, LocalDateTime.now()));
    }

    /**
     * Sends active meetings update to a specific client.
     */
    private void sendActiveMeetingsUpdate(String clientId) {
        try {
            var meetings = zoomDetectionService.getActiveMeetings()
                .map(MeetingInfo::from)
                .collectList()
                .block();

            messagingTemplate.convertAndSendToUser(
                clientId,
                "/queue/meetings/active",
                new ActiveMeetingsMessage(meetings, LocalDateTime.now())
            );
        } catch (Exception e) {
            logger.error("Failed to send active meetings update", e);
        }
    }

    // Message DTOs

    public record ConnectionMessage(String clientId) {}
    
    public record ConnectionAckMessage(boolean connected, String message, LocalDateTime timestamp) {}
    
    public record MeetingMonitoringRequest(String clientId, UUID meetingId) {}
    
    public record MeetingMonitoringMessage(UUID meetingId, String status, String message, LocalDateTime timestamp) {}
    
    public record TranscriptionRequest(String clientId, UUID meetingId, String language, 
                                   boolean enableSpeakerDiarization, double confidenceThreshold,
                                   boolean enablePunctuation, boolean enableCapitalization,
                                   boolean enableTimestamps, int maxSpeakers, String model) {}
    
    public record TranscriptionStatusMessage(UUID meetingId, String status, String message, LocalDateTime timestamp) {}
    
    public record TranscriptionSegmentMessage(UUID meetingId, UUID segmentId, String text, 
                                         String speakerId, double confidence, LocalDateTime timestamp, 
                                         boolean isFinal) {}
    
    public record SummaryRequest(String clientId, UUID meetingId, String summaryType, String customPrompt) {}
    
    public record SummaryProgressMessage(UUID meetingId, String summaryType, double progress, 
                                    String message, LocalDateTime timestamp) {}
    
    public record MeetingEventMessage(String eventType, Object data, LocalDateTime timestamp) {}
    
    public record ActiveMeetingsMessage(List<MeetingInfo> meetings, LocalDateTime timestamp) {}
    
    public record MeetingInfo(UUID meetingId, String title, String status, LocalDateTime startTime, 
                           int participantCount) {
        static MeetingInfo from(ZoomDetectionService.MeetingState state) {
            return new MeetingInfo(
                state.meetingId(),
                state.title(),
                state.status().toString(),
                state.startTime(),
                state.participantCount()
            );
        }
    }
}