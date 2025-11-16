package com.zoomtranscriber.core.transcription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of TranscriptionService.
 * Provides mock transcription functionality for development and testing.
 */
@Service
public class DefaultTranscriptionService implements TranscriptionService {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultTranscriptionService.class);
    
    private final ConcurrentHashMap<UUID, TranscriptionSession> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Sinks.Many<TranscriptionSegment>> transcriptionSinks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, TranscriptionConfig> sessionConfigs = new ConcurrentHashMap<>();
    
    @Override
    public Mono<Void> startTranscription(UUID meetingId, TranscriptionConfig config) {
        return Mono.fromRunnable(() -> {
            if (activeSessions.containsKey(meetingId)) {
                logger.warn("Transcription already active for meeting: {}", meetingId);
                return;
            }
            
            logger.info("Starting transcription for meeting: {} with config: {}", meetingId, config);
            
            var session = new TranscriptionSession(
                meetingId,
                config,
                LocalDateTime.now(),
                TranscriptionStatus.ACTIVE,
                0,
                0
            );
            
            activeSessions.put(meetingId, session);
            sessionConfigs.put(meetingId, config);
            transcriptionSinks.put(meetingId, Sinks.many().multicast().onBackpressureBuffer());
            
            // Start mock transcription simulation
            startMockTranscription(meetingId, config);
            
            logger.info("Transcription started successfully for meeting: {}", meetingId);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    @Override
    public Mono<Void> stopTranscription(UUID meetingId) {
        return Mono.fromRunnable(() -> {
            var session = activeSessions.remove(meetingId);
            if (session == null) {
                logger.warn("No active transcription found for meeting: {}", meetingId);
                return;
            }
            
            logger.info("Stopping transcription for meeting: {}", meetingId);
            
            // Update session status
            activeSessions.put(meetingId, session.withStatus(TranscriptionStatus.COMPLETED));
            
            // Complete the transcription stream
            var sink = transcriptionSinks.remove(meetingId);
            if (sink != null) {
                sink.tryEmitComplete();
            }
            
            // Clean up
            sessionConfigs.remove(meetingId);
            
            logger.info("Transcription stopped successfully for meeting: {}", meetingId);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    @Override
    public Mono<TranscriptionStatus> getTranscriptionStatus(UUID meetingId) {
        return Mono.fromCallable(() -> {
            var session = activeSessions.get(meetingId);
            if (session == null) {
                return TranscriptionStatus.NOT_STARTED;
            }
            
            return session.status();
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Flux<TranscriptionSegment> getTranscriptionStream(UUID meetingId) {
        return Mono.fromCallable(() -> {
            var sink = transcriptionSinks.get(meetingId);
            if (sink == null) {
                throw new IllegalStateException("No active transcription for meeting: " + meetingId);
            }
            
            return sink.asFlux();
        })
        .flatMapMany(flux -> flux)
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Flux<TranscriptionSegment> getAllTranscriptionSegments(UUID meetingId) {
        return getTranscriptionStream(meetingId);
    }
    
    @Override
    public Flux<TranscriptionSegment> processAudio(UUID meetingId, byte[] audioData) {
        return Mono.fromCallable(() -> {
            var session = activeSessions.get(meetingId);
            if (session == null) {
                logger.warn("No active transcription session for meeting: {}", meetingId);
                return null;
            }
            
            var config = sessionConfigs.get(meetingId);
            if (config == null) {
                config = TranscriptionConfig.defaultConfig();
            }
            
            // Mock transcription from audio data
            return createMockTranscriptionSegment(meetingId, config, session.segmentCount());
        })
        .filter(segment -> segment != null)
        .flux()
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<Void> setLanguage(UUID meetingId, String language) {
        return Mono.fromCallable(() -> {
            var config = sessionConfigs.get(meetingId);
            if (config != null) {
                var newConfig = new TranscriptionConfig(
                    language,
                    config.enableSpeakerDiarization(),
                    config.confidenceThreshold(),
                    config.enablePunctuation(),
                    config.enableCapitalization(),
                    config.enableTimestamps(),
                    config.maxSpeakers(),
                    config.model()
                );
                sessionConfigs.put(meetingId, newConfig);
            }
            
            logger.info("Language set to {} for meeting: {}", language, meetingId);
            return null;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    @Override
    public Mono<String> getLanguage(UUID meetingId) {
        return Mono.fromCallable(() -> {
            var config = sessionConfigs.get(meetingId);
            return config != null ? config.language() : "en-US";
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<Void> setSpeakerDiarization(UUID meetingId, boolean enabled) {
        return Mono.fromCallable(() -> {
            var config = sessionConfigs.get(meetingId);
            if (config != null) {
                var newConfig = new TranscriptionConfig(
                    config.language(),
                    enabled,
                    config.confidenceThreshold(),
                    config.enablePunctuation(),
                    config.enableCapitalization(),
                    config.enableTimestamps(),
                    config.maxSpeakers(),
                    config.model()
                );
                sessionConfigs.put(meetingId, newConfig);
            }
            
            logger.info("Speaker diarization {} for meeting: {}", enabled ? "enabled" : "disabled", meetingId);
            return null;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    @Override
    public Mono<Boolean> isSpeakerDiarizationEnabled(UUID meetingId) {
        return Mono.fromCallable(() -> {
            var config = sessionConfigs.get(meetingId);
            return config != null && config.enableSpeakerDiarization();
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<Void> setConfidenceThreshold(UUID meetingId, double threshold) {
        return Mono.fromCallable(() -> {
            var config = sessionConfigs.get(meetingId);
            if (config != null) {
                var newConfig = new TranscriptionConfig(
                    config.language(),
                    config.enableSpeakerDiarization(),
                    threshold,
                    config.enablePunctuation(),
                    config.enableCapitalization(),
                    config.enableTimestamps(),
                    config.maxSpeakers(),
                    config.model()
                );
                sessionConfigs.put(meetingId, newConfig);
            }
            
            logger.info("Confidence threshold set to {} for meeting: {}", threshold, meetingId);
            return null;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    @Override
    public Mono<Double> getConfidenceThreshold(UUID meetingId) {
        return Mono.fromCallable(() -> {
            var config = sessionConfigs.get(meetingId);
            return config != null ? config.confidenceThreshold() : 0.7;
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<TranscriptionStats> getTranscriptionStats(UUID meetingId) {
        return Mono.fromCallable(() -> {
            var session = activeSessions.get(meetingId);
            if (session == null) {
                return new TranscriptionStats(
                    meetingId,
                    0,
                    0,
                    0.0,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    java.time.Duration.ZERO,
                    0,
                    List.of("en-US")
                );
            }
            
            return new TranscriptionStats(
                meetingId,
                session.segmentCount(),
                session.segmentCount() * 5, // Estimate 5 words per segment
                0.85, // Mock average confidence
                session.startTime(),
                LocalDateTime.now(),
                java.time.Duration.between(session.startTime(), LocalDateTime.now()),
                sessionConfigs.get(meetingId).enableSpeakerDiarization() ? 5 : 1,
                List.of(sessionConfigs.get(meetingId).language())
            );
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<byte[]> exportTranscription(UUID meetingId, ExportFormat format) {
        return Mono.fromCallable(() -> {
            var session = activeSessions.get(meetingId);
            if (session == null) {
                throw new IllegalStateException("No active transcription for meeting: " + meetingId);
            }
            
            // Mock export data
            var content = switch (format) {
                case TXT -> exportAsText(meetingId);
                case JSON -> exportAsJson(meetingId);
                case SRT -> exportAsSrt(meetingId);
                case VTT -> exportAsVtt(meetingId);
                case CSV -> exportAsCsv(meetingId);
                case DOCX -> exportAsText(meetingId); // Simplified
            };
            
            return content.getBytes();
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * Starts mock transcription simulation.
     */
    private void startMockTranscription(UUID meetingId, TranscriptionConfig config) {
        var mockTexts = List.of(
            "Hello everyone, thank you for joining this meeting.",
            "Let's start by reviewing the agenda for today.",
            "First, we'll discuss the project timeline.",
            "We need to make sure everyone is aligned on the deliverables.",
            "Does anyone have any questions about the approach?",
            "Great, let's move on to the next topic.",
            "I'll share my screen to show the current progress.",
            "As you can see, we're on track to meet our deadline.",
            "Let me know if you need any clarification on this point.",
            "We'll take a short break and resume in ten minutes."
        );
        
        var sink = transcriptionSinks.get(meetingId);
        if (sink == null) return;
        
        var index = 0;
        for (var text : mockTexts) {
            try {
                Thread.sleep(3000); // 3 second delay between segments
                
                var segment = createMockTranscriptionSegment(meetingId, config, index);
                if (segment != null) {
                    sink.tryEmitNext(segment);
                    
                    // Update session
                    var session = activeSessions.get(meetingId);
                    if (session != null) {
                        activeSessions.put(meetingId, session.withSegmentCount(index + 1));
                    }
                }
                index++;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    /**
     * Creates a mock transcription segment.
     */
    private TranscriptionSegment createMockTranscriptionSegment(UUID meetingId, TranscriptionConfig config, int segmentNumber) {
        var mockTexts = List.of(
            "Hello everyone, thank you for joining this meeting.",
            "Let's start by reviewing the agenda for today.",
            "First, we'll discuss the project timeline.",
            "We need to make sure everyone is aligned on the deliverables.",
            "Does anyone have any questions about the approach?",
            "Great, let's move on to the next topic.",
            "I'll share my screen to show the current progress.",
            "As you can see, we're on track to meet our deadline.",
            "Let me know if you need any clarification on this point.",
            "We'll take a short break and resume in ten minutes."
        );
        
        if (segmentNumber >= mockTexts.size()) {
            return null;
        }
        
        var text = mockTexts.get(segmentNumber % mockTexts.size());
        var confidence = 0.8 + (Math.random() * 0.15); // 0.8 to 0.95
        var speakerId = config.enableSpeakerDiarization() ? 
            "Speaker_" + ((segmentNumber % 3) + 1) : null;
        
        return new TranscriptionSegment(
            UUID.randomUUID(),
            meetingId,
            LocalDateTime.now(),
            speakerId,
            text,
            confidence,
            segmentNumber,
            true,
            java.time.Duration.ofSeconds(2 + (int)(Math.random() * 3)), // 2-5 seconds
            config.language()
        );
    }
    
    /**
     * Exports transcription as plain text.
     */
    private String exportAsText(UUID meetingId) {
        var session = activeSessions.get(meetingId);
        if (session == null) {
            return "No transcription available for this meeting.";
        }
        
        var config = sessionConfigs.get(meetingId);
        var sb = new StringBuilder();
        sb.append("Transcription for Meeting: ").append(meetingId).append("\n");
        sb.append("Language: ").append(config != null ? config.language() : "en-US").append("\n");
        sb.append("Segments: ").append(session.segmentCount()).append("\n");
        sb.append("Duration: ").append(java.time.Duration.between(session.startTime(), LocalDateTime.now()).toMinutes()).append(" minutes\n\n");
        
        // Add mock segments
        var mockTexts = List.of(
            "Hello everyone, thank you for joining this meeting.",
            "Let's start by reviewing the agenda for today.",
            "First, we'll discuss the project timeline.",
            "We need to make sure everyone is aligned on the deliverables.",
            "Does anyone have any questions about the approach?"
        );
        
        for (int i = 0; i < Math.min(session.segmentCount(), mockTexts.size()); i++) {
            sb.append(i + 1).append(". ").append(mockTexts.get(i)).append("\n\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Exports transcription as JSON.
     */
    private String exportAsJson(UUID meetingId) {
        return """
            {
                "meetingId": "%s",
                "segments": [],
                "stats": {
                    "totalSegments": %d,
                    "totalDuration": "PT%dM"
                }
            }
            """.formatted(meetingId, activeSessions.get(meetingId).segmentCount(), 30);
    }
    
    /**
     * Exports transcription as SRT.
     */
    private String exportAsSrt(UUID meetingId) {
        var sb = new StringBuilder();
        sb.append("1\n");
        sb.append("00:00:00,000 --> 00:00:03,000\n");
        sb.append("Hello everyone, thank you for joining this meeting.\n\n");
        return sb.toString();
    }
    
    /**
     * Exports transcription as VTT.
     */
    private String exportAsVtt(UUID meetingId) {
        var sb = new StringBuilder();
        sb.append("WEBVTT\n\n");
        sb.append("00:00:00.000 --> 00:00:03.000\n");
        sb.append("Hello everyone, thank you for joining this meeting.\n\n");
        return sb.toString();
    }
    
    /**
     * Exports transcription as CSV.
     */
    private String exportAsCsv(UUID meetingId) {
        var sb = new StringBuilder();
        sb.append("Segment,Speaker,Text,Confidence\n");
        sb.append("1,Speaker 1,\"Hello everyone, thank you for joining this meeting.\",0.85\n");
        return sb.toString();
    }
    
    /**
     * Represents an active transcription session.
     */
    private record TranscriptionSession(
        UUID meetingId,
        TranscriptionConfig config,
        LocalDateTime startTime,
        TranscriptionStatus status,
        int segmentCount,
        int totalWords
    ) {
        public TranscriptionSession withStatus(TranscriptionStatus newStatus) {
            return new TranscriptionSession(meetingId, config, startTime, newStatus, segmentCount, totalWords);
        }
        
        public TranscriptionSession withSegmentCount(int newCount) {
            return new TranscriptionSession(meetingId, config, startTime, status, newCount, totalWords);
        }
    }
}