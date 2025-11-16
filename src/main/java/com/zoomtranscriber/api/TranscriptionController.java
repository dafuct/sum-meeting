package com.zoomtranscriber.api;

import com.zoomtranscriber.core.transcription.TranscriptionSegment;
import com.zoomtranscriber.core.transcription.TranscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST API controller for transcription operations.
 * Provides endpoints for managing real-time transcription.
 */
@RestController("apiTranscriptionController")
@RequestMapping("/api/transcription")
@CrossOrigin(origins = "*")
public class TranscriptionController {
    
    private static final Logger logger = LoggerFactory.getLogger(TranscriptionController.class);
    
    private final TranscriptionService transcriptionService;
    
    public TranscriptionController(TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }
    
    /**
     * Starts transcription for a meeting.
     * 
     * @param request start transcription request
     * @return response indicating success or failure
     */
    @PostMapping("/start")
    public Mono<ResponseEntity<ApiResponse>> startTranscription(@RequestBody StartTranscriptionRequest request) {
        logger.info("Starting transcription for meeting: {}", request.meetingId());
        
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
        
        return transcriptionService.startTranscription(request.meetingId(), config)
            .map(result -> ResponseEntity.ok(
                new ApiResponse(true, "Transcription started successfully")))
            .onErrorResume(error -> {
                logger.error("Failed to start transcription", error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to start transcription: " + error.getMessage())));
            });
    }
    
    /**
     * Stops transcription for a meeting.
     * 
     * @param request stop transcription request
     * @return response indicating success or failure
     */
    @PostMapping("/stop")
    public Mono<ResponseEntity<ApiResponse>> stopTranscription(@RequestBody StopTranscriptionRequest request) {
        logger.info("Stopping transcription for meeting: {}", request.meetingId());
        
        return transcriptionService.stopTranscription(request.meetingId())
            .map(result -> ResponseEntity.ok(
                new ApiResponse(true, "Transcription stopped successfully")))
            .onErrorResume(error -> {
                logger.error("Failed to stop transcription", error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to stop transcription: " + error.getMessage())));
            });
    }
    
    /**
     * Gets transcription status for a meeting.
     * 
     * @param meetingId meeting identifier
     * @return transcription status
     */
    @GetMapping("/{meetingId}/status")
    public Mono<ResponseEntity<TranscriptionStatusResponse>> getTranscriptionStatus(@PathVariable UUID meetingId) {
        logger.debug("Getting transcription status for meeting: {}", meetingId);
        
        return transcriptionService.getTranscriptionStatus(meetingId)
            .map(status -> ResponseEntity.ok(new TranscriptionStatusResponse(
                meetingId,
                status.toString(),
                LocalDateTime.now()
            )))
            .onErrorResume(error -> {
                logger.error("Failed to get transcription status", error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new TranscriptionStatusResponse(
                        meetingId,
                        "ERROR",
                        LocalDateTime.now()
                    )));
            });
    }
    
    /**
     * Gets all transcription segments for a meeting.
     * 
     * @param meetingId meeting identifier
     * @param pagination pagination parameters
     * @return list of transcription segments
     */
    @GetMapping("/{meetingId}/segments")
    public Mono<ResponseEntity<TranscriptionSegmentsResponse>> getTranscriptionSegments(
            @PathVariable UUID meetingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search) {
        
        logger.debug("Getting transcription segments for meeting: {}, page: {}, size: {}", 
            meetingId, page, size);
        
        var segmentsFlux = transcriptionService.getAllTranscriptionSegments(meetingId);
        
        // Apply search filter if provided
        if (search != null && !search.trim().isEmpty()) {
            var searchLower = search.toLowerCase();
            segmentsFlux = segmentsFlux.filter(segment -> 
                segment.getText() != null && segment.getText().toLowerCase().contains(searchLower));
        }
        
        return segmentsFlux
            .skip(page * size)
            .take(size)
            .collectList()
            .map(segments -> {
                var segmentDtos = segments.stream()
                    .map(TranscriptionSegmentDto::from)
                    .toList();
                return ResponseEntity.ok(new TranscriptionSegmentsResponse(
                    meetingId,
                    segmentDtos,
                    page,
                    size,
                    segmentDtos.size()
                ));
            })
            .onErrorResume(error -> {
                logger.error("Failed to get transcription segments", error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new TranscriptionSegmentsResponse(
                        meetingId,
                        List.of(),
                        page,
                        size,
                        0
                    )));
            });
    }
    
    /**
     * Gets real-time transcription stream for a meeting.
     * 
     * @param meetingId meeting identifier
     * @return server-sent events stream of transcription segments
     */
    @GetMapping(value = "/{meetingId}/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public reactor.core.publisher.Flux<org.springframework.http.codec.ServerSentEvent<TranscriptionSegmentDto>> getTranscriptionStream(
            @PathVariable UUID meetingId) {
        logger.info("Transcription stream requested for meeting: {}", meetingId);
        
        return transcriptionService.getTranscriptionStream(meetingId)
            .map(TranscriptionSegmentDto::from)
            .map(segment -> org.springframework.http.codec.ServerSentEvent.<TranscriptionSegmentDto>builder(segment)
                .id(segment.id().toString())
                .event("transcription")
                .build());
    }
    
    /**
     * Updates transcription settings for a meeting.
     * 
     * @param request settings update request
     * @return response indicating success or failure
     */
    @PutMapping("/{meetingId}/settings")
    public Mono<ResponseEntity<ApiResponse>> updateTranscriptionSettings(
            @PathVariable UUID meetingId,
            @RequestBody TranscriptionSettingsRequest request) {
        
        logger.info("Updating transcription settings for meeting: {}", meetingId);
        
        var languageMono = request.language() != null ? 
            transcriptionService.setLanguage(meetingId, request.language()) : 
            Mono.empty();
        
        var speakerDiarizationMono = request.enableSpeakerDiarization() != null ?
            transcriptionService.setSpeakerDiarization(meetingId, request.enableSpeakerDiarization()) :
            Mono.empty();
        
        var confidenceMono = request.confidenceThreshold() != null ?
            transcriptionService.setConfidenceThreshold(meetingId, request.confidenceThreshold()) :
            Mono.empty();
        
        return Mono.when(languageMono, speakerDiarizationMono, confidenceMono)
            .then(Mono.just(ResponseEntity.ok(
                new ApiResponse(true, "Transcription settings updated successfully"))))
            .onErrorResume(error -> {
                logger.error("Failed to update transcription settings", error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to update settings: " + error.getMessage())));
            });
    }
    
    /**
     * Exports transcription for a meeting.
     * 
     * @param request export request
     * @return exported data or error
     */
    @PostMapping("/{meetingId}/export")
    public Mono<ResponseEntity<ExportResponse>> exportTranscription(
            @PathVariable UUID meetingId,
            @RequestBody ExportRequest request) {
        
        logger.info("Exporting transcription for meeting: {} in format: {}", 
            meetingId, request.format());
        
        return transcriptionService.exportTranscription(meetingId, request.format())
            .map(data -> {
                var filename = generateExportFilename(meetingId, request.format());
                return ResponseEntity.ok()
                    .header("Content-Disposition", 
                        "attachment; filename=\"" + filename + "\"")
                    .header("Content-Type", getContentType(request.format()))
                    .body(new ExportResponse(filename, request.format(), data));
            })
            .onErrorResume(error -> {
                logger.error("Failed to export transcription", error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ExportResponse("", request.format(), new byte[0])));
            });
    }
    
    /**
     * Gets transcription statistics for a meeting.
     * 
     * @param meetingId meeting identifier
     * @return transcription statistics
     */
    @GetMapping("/{meetingId}/statistics")
    public Mono<ResponseEntity<TranscriptionStatsResponse>> getTranscriptionStatistics(@PathVariable UUID meetingId) {
        logger.debug("Getting transcription statistics for meeting: {}", meetingId);
        
        return transcriptionService.getTranscriptionStats(meetingId)
            .map(stats -> ResponseEntity.ok(new TranscriptionStatsResponse(
                meetingId,
                stats.totalSegments(),
                stats.totalWords(),
                stats.averageConfidence(),
                stats.startTime(),
                stats.endTime(),
                stats.totalDuration(),
                stats.speakerCount(),
                stats.detectedLanguages()
            )))
            .onErrorResume(error -> {
                logger.error("Failed to get transcription statistics", error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new TranscriptionStatsResponse(
                        meetingId,
                        0,
                        0,
                        0.0,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        java.time.Duration.ZERO,
                        0,
                        List.of()
                    )));
            });
    }
    
    /**
     * Searches transcription content across all meetings.
     * 
     * @param request search request
     * @return search results
     */
    @PostMapping("/search")
    public Mono<ResponseEntity<SearchResponse>> searchTranscription(@RequestBody SearchRequest request) {
        logger.info("Searching transcription with query: {}", request.query());
        
        // This would typically search across all meetings in a database
        // For now, return a mock response
        var mockResults = List.of(
            new SearchResult(
                UUID.randomUUID(),
                "Sample Meeting 1",
                "This is a sample transcription segment containing the search query.",
                LocalDateTime.now().minusMinutes(30),
                0.85
            ),
            new SearchResult(
                UUID.randomUUID(),
                "Sample Meeting 2", 
                "Another sample segment that matches the search criteria.",
                LocalDateTime.now().minusMinutes(15),
                0.92
            )
        );
        
        return Mono.just(ResponseEntity.ok(new SearchResponse(
            request.query(),
            mockResults,
            mockResults.size()
        )));
    }
    
    /**
     * Gets supported languages for transcription.
     * 
     * @return list of supported languages
     */
    @GetMapping("/languages")
    public Mono<ResponseEntity<LanguagesResponse>> getSupportedLanguages() {
        logger.debug("Getting supported transcription languages");
        
        var languages = List.of(
            new Language("en-US", "English (US)", true),
            new Language("en-GB", "English (UK)", false),
            new Language("es-ES", "Spanish", false),
            new Language("fr-FR", "French", false),
            new Language("de-DE", "German", false),
            new Language("zh-CN", "Chinese", false),
            new Language("ja-JP", "Japanese", false)
        );
        
        return Mono.just(ResponseEntity.ok(new LanguagesResponse(languages)));
    }
    
    /**
     * Gets supported export formats.
     * 
     * @return list of supported export formats
     */
    @GetMapping("/export-formats")
    public Mono<ResponseEntity<ExportFormatsResponse>> getSupportedExportFormats() {
        logger.debug("Getting supported export formats");
        
        var formats = List.of(
            new ExportFormatInfo("txt", "Plain Text", "Plain text format with timestamps"),
            new ExportFormatInfo("srt", "SubRip", "SubRip subtitle format"),
            new ExportFormatInfo("vtt", "WebVTT", "Web Video Text Tracks format"),
            new ExportFormatInfo("json", "JSON", "Structured JSON format"),
            new ExportFormatInfo("csv", "CSV", "Comma-separated values format"),
            new ExportFormatInfo("docx", "Word Document", "Microsoft Word document format")
        );
        
        return Mono.just(ResponseEntity.ok(new ExportFormatsResponse(formats)));
    }
    
    /**
     * Generates export filename.
     */
    private String generateExportFilename(UUID meetingId, com.zoomtranscriber.core.transcription.TranscriptionService.ExportFormat format) {
        var timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        var extension = switch (format) {
            case TXT -> "txt";
            case SRT -> "srt";
            case VTT -> "vtt";
            case JSON -> "json";
            case CSV -> "csv";
            case DOCX -> "docx";
        };
        
        return String.format("transcription_%s_%s.%s", 
            meetingId.toString().substring(0, 8), timestamp, extension);
    }
    
    /**
     * Gets content type for export format.
     */
    private String getContentType(com.zoomtranscriber.core.transcription.TranscriptionService.ExportFormat format) {
        return switch (format) {
            case TXT -> "text/plain";
            case SRT -> "application/x-subrip";
            case VTT -> "text/vtt";
            case JSON -> "application/json";
            case CSV -> "text/csv";
            case DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        };
    }
    
    // Response DTOs
    public record ApiResponse(
        boolean success,
        String message
    ) {}
    
    public record TranscriptionStatusResponse(
        UUID meetingId,
        String status,
        LocalDateTime timestamp
    ) {}
    
    public record TranscriptionSegmentsResponse(
        UUID meetingId,
        List<TranscriptionSegmentDto> segments,
        int page,
        int size,
        int total
    ) {}
    
    public record TranscriptionStatsResponse(
        UUID meetingId,
        int totalSegments,
        int totalWords,
        double averageConfidence,
        LocalDateTime startTime,
        LocalDateTime endTime,
        java.time.Duration totalDuration,
        int speakerCount,
        List<String> detectedLanguages
    ) {}
    
    public record ExportResponse(
        String filename,
        com.zoomtranscriber.core.transcription.TranscriptionService.ExportFormat format,
        byte[] data
    ) {}
    
    public record SearchResponse(
        String query,
        List<SearchResult> results,
        int total
    ) {}
    
    public record LanguagesResponse(
        List<Language> languages
    ) {}
    
    public record ExportFormatsResponse(
        List<ExportFormatInfo> formats
    ) {}
    
    // Request DTOs
    public record StartTranscriptionRequest(
        UUID meetingId,
        String language,
        boolean enableSpeakerDiarization,
        double confidenceThreshold,
        boolean enablePunctuation,
        boolean enableCapitalization,
        boolean enableTimestamps,
        int maxSpeakers,
        String model
    ) {}
    
    public record StopTranscriptionRequest(
        UUID meetingId
    ) {}
    
    public record TranscriptionSettingsRequest(
        String language,
        Boolean enableSpeakerDiarization,
        Double confidenceThreshold
    ) {}
    
    public record ExportRequest(
        com.zoomtranscriber.core.transcription.TranscriptionService.ExportFormat format
    ) {}
    
    public record SearchRequest(
        String query,
        int maxResults,
        String meetingId
    ) {}
    
    // Additional DTOs
    public record TranscriptionSegmentDto(
        UUID id,
        UUID meetingId,
        LocalDateTime timestamp,
        String speakerId,
        String text,
        double confidence,
        int segmentNumber,
        boolean isFinal,
        java.time.Duration duration,
        String language
    ) {
        static TranscriptionSegmentDto from(TranscriptionSegment segment) {
            return new TranscriptionSegmentDto(
                segment.getId(),
                segment.getMeetingId(),
                segment.getTimestamp(),
                segment.getSpeakerId(),
                segment.getText(),
                segment.getConfidence(),
                segment.getSegmentNumber(),
                segment.isFinal(),
                segment.getDuration(),
                segment.getLanguage()
            );
        }
    }
    
    public record SearchResult(
        UUID meetingId,
        String meetingTitle,
        String snippet,
        LocalDateTime timestamp,
        double confidence
    ) {}
    
    public record Language(
        String code,
        String name,
        boolean isDefault
    ) {}
    
    public record ExportFormatInfo(
        String format,
        String name,
        String description
    ) {}
}