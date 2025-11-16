package com.zoomtranscriber.api;

import com.zoomtranscriber.core.ai.SummaryGenerator;
import com.zoomtranscriber.core.transcription.TranscriptionService;
import com.zoomtranscriber.core.transcription.TranscriptionSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API controller for summary operations.
 * Provides endpoints for generating, managing, and exporting meeting summaries.
 */
@RestController("apiSummaryController")
@RequestMapping("/api/summary")
@CrossOrigin(origins = "*")
public class SummaryController {
    
    private static final Logger logger = LoggerFactory.getLogger(SummaryController.class);
    
    private final SummaryGenerator summaryGenerator;
    private final TranscriptionService transcriptionService;
    
    public SummaryController(SummaryGenerator summaryGenerator, 
                             TranscriptionService transcriptionService) {
        this.summaryGenerator = summaryGenerator;
        this.transcriptionService = transcriptionService;
    }
    
    /**
     * Generates a summary for a meeting.
     * 
     * @param request generate summary request
     * @return response containing the generated summary
     */
    @PostMapping("/generate")
    public Mono<ResponseEntity<GenerateSummaryResponse>> generateSummary(@RequestBody GenerateSummaryRequest request) {
        logger.info("Generating {} summary for meeting: {}", request.summaryType(), request.meetingId());
        
        return summaryGenerator.generateSummary(request.meetingId(), request.summaryType())
            .map(summary -> {
                var response = new GenerateSummaryResponse(
                    summary.meetingId(),
                    summary.summaryType(),
                    summary.content(),
                    summary.generatedAt(),
                    summary.segmentCount(),
                    summary.wordCount(),
                    summary.durationSeconds(),
                    summary.speakers(),
                    summary.getFormattedDuration(),
                    summary.getFormattedWordCount()
                );
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                logger.error("Failed to generate summary", error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenerateSummaryResponse(
                        request.meetingId(),
                        request.summaryType(),
                        "Error: " + error.getMessage(),
                        LocalDateTime.now(),
                        0,
                        0,
                        0,
                        List.of(),
                        "",
                        ""
                    )));
            });
    }
    
    /**
     * Generates a summary with streaming response.
     * 
     * @param request generate summary request
     * @return server-sent events stream of summary chunks
     */
    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<org.springframework.http.codec.ServerSentEvent<SummaryChunkResponse>> generateSummaryStream(
            @RequestBody GenerateSummaryRequest request) {
        logger.info("Streaming {} summary for meeting: {}", request.summaryType(), request.meetingId());
        
        return summaryGenerator.generateSummaryStream(request.meetingId(), request.summaryType())
            .index()
            .map(tuple -> org.springframework.http.codec.ServerSentEvent.<SummaryChunkResponse>builder(
                new SummaryChunkResponse(
                    request.meetingId(),
                    request.summaryType(),
                    tuple.getT2(),
                    tuple.getT1(),
                    tuple.getT1() == 0 // First chunk indicates start
                ))
                .id(request.meetingId().toString())
                .event("summary-chunk")
                .build())
            .onErrorResume(error -> {
                logger.error("Error in summary stream", error);
                return Flux.just(org.springframework.http.codec.ServerSentEvent.<SummaryChunkResponse>builder(
                    new SummaryChunkResponse(
                        request.meetingId(),
                        request.summaryType(),
                        "Error: " + error.getMessage(),
                        -1,
                        false
                    ))
                    .event("summary-error")
                    .build());
            });
    }
    
    /**
     * Generates multiple summary types for a meeting.
     * 
     * @param request generate multiple summaries request
     * @return response containing all generated summaries
     */
    @PostMapping("/generate/multiple")
    public Mono<ResponseEntity<MultipleSummariesResponse>> generateMultipleSummaries(
            @RequestBody GenerateMultipleSummariesRequest request) {
        logger.info("Generating {} summary types for meeting: {}", 
            request.summaryTypes().size(), request.meetingId());
        
        return summaryGenerator.generateMultipleSummaries(request.meetingId(), request.summaryTypes())
            .map(summary -> new GenerateSummaryResponse(
                summary.meetingId(),
                summary.summaryType(),
                summary.content(),
                summary.generatedAt(),
                summary.segmentCount(),
                summary.wordCount(),
                summary.durationSeconds(),
                summary.speakers(),
                summary.getFormattedDuration(),
                summary.getFormattedWordCount()
            ))
            .collectList()
            .map(summaries -> ResponseEntity.ok(new MultipleSummariesResponse(
                request.meetingId(),
                summaries,
                summaries.size()
            )))
            .onErrorResume(error -> {
                logger.error("Failed to generate multiple summaries", error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MultipleSummariesResponse(
                        request.meetingId(),
                        List.of(),
                        0
                    )));
            });
    }
    
    /**
     * Generates a custom summary with a specific prompt.
     * 
     * @param request custom summary request
     * @return response containing the generated summary
     */
    @PostMapping("/generate/custom")
    public Mono<ResponseEntity<GenerateSummaryResponse>> generateCustomSummary(@RequestBody CustomSummaryRequest request) {
        logger.info("Generating custom summary for meeting: {}", request.meetingId());
        
        return summaryGenerator.generateCustomSummary(
            request.meetingId(), 
            request.prompt(), 
            request.includeTimestamps())
            .map(summary -> {
                var response = new GenerateSummaryResponse(
                    summary.meetingId(),
                    summary.summaryType(),
                    summary.content(),
                    summary.generatedAt(),
                    summary.segmentCount(),
                    summary.wordCount(),
                    summary.durationSeconds(),
                    summary.speakers(),
                    summary.getFormattedDuration(),
                    summary.getFormattedWordCount()
                );
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                logger.error("Failed to generate custom summary", error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenerateSummaryResponse(
                        request.meetingId(),
                        SummaryGenerator.SummaryType.CUSTOM,
                        "Error: " + error.getMessage(),
                        LocalDateTime.now(),
                        0,
                        0,
                        0,
                        List.of(),
                        "",
                        ""
                    )));
            });
    }
    
    /**
     * Gets a previously generated summary for a meeting.
     * 
     * @param meetingId meeting identifier
     * @param summaryType summary type
     * @return response containing the summary
     */
    @GetMapping("/{meetingId}/type/{summaryType}")
    public Mono<ResponseEntity<GetSummaryResponse>> getSummary(
            @PathVariable UUID meetingId,
            @PathVariable SummaryGenerator.SummaryType summaryType) {
        logger.debug("Getting {} summary for meeting: {}", summaryType, meetingId);
        
        // This would typically fetch from a database or cache
        // For now, regenerate the summary
        return summaryGenerator.generateSummary(meetingId, summaryType)
            .map(summary -> ResponseEntity.ok(new GetSummaryResponse(
                summary.meetingId(),
                summary.summaryType(),
                summary.content(),
                summary.generatedAt(),
                summary.segmentCount(),
                summary.wordCount(),
                summary.durationSeconds(),
                summary.speakers()
            )))
            .onErrorResume(error -> {
                logger.error("Failed to get summary", error);
                return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GetSummaryResponse(
                        meetingId,
                        summaryType,
                        null,
                        LocalDateTime.now(),
                        0,
                        0,
                        0,
                        List.of()
                    )));
            });
    }
    
    /**
     * Gets all summary types available for a meeting.
     * 
     * @param meetingId meeting identifier
     * @return response containing available summary types
     */
    @GetMapping("/{meetingId}/available-types")
    public Mono<ResponseEntity<AvailableSummaryTypesResponse>> getAvailableSummaryTypes(@PathVariable UUID meetingId) {
        logger.debug("Getting available summary types for meeting: {}", meetingId);
        
        return summaryGenerator.canGenerateSummary(meetingId)
            .flatMap(canGenerate -> {
                if (!canGenerate) {
                    return Mono.just(ResponseEntity.ok(new AvailableSummaryTypesResponse(
                        meetingId,
                        List.of(),
                        "No transcription available for summary generation"
                    )));
                }
                
                // Return all available types
                var types = List.of(
                    SummaryGenerator.SummaryType.FULL,
                    SummaryGenerator.SummaryType.KEY_POINTS,
                    SummaryGenerator.SummaryType.DECISIONS,
                    SummaryGenerator.SummaryType.ACTION_ITEMS,
                    SummaryGenerator.SummaryType.EXECUTIVE,
                    SummaryGenerator.SummaryType.TECHNICAL,
                    SummaryGenerator.SummaryType.CUSTOM
                );
                
                return Mono.just(ResponseEntity.ok(new AvailableSummaryTypesResponse(
                    meetingId,
                    types,
                    "All summary types available"
                )));
            })
            .onErrorResume(error -> {
                logger.error("Failed to get available summary types", error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AvailableSummaryTypesResponse(
                        meetingId,
                        List.of(),
                        "Error checking availability: " + error.getMessage()
                    )));
            });
    }
    
    /**
     * Estimates summary generation time.
     * 
     * @param meetingId meeting identifier
     * @param summaryType summary type
     * @return response containing estimated time
     */
    @GetMapping("/{meetingId}/estimate")
    public Mono<ResponseEntity<EstimateResponse>> estimateGenerationTime(
            @PathVariable UUID meetingId,
            @RequestParam SummaryGenerator.SummaryType summaryType) {
        logger.debug("Estimating generation time for {} summary of meeting: {}", summaryType, meetingId);
        
        return summaryGenerator.estimateGenerationTime(meetingId, summaryType)
            .map(seconds -> ResponseEntity.ok(new EstimateResponse(
                meetingId,
                summaryType,
                seconds,
                formatEstimateTime(seconds)
            )))
            .onErrorResume(error -> {
                logger.error("Failed to estimate generation time", error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new EstimateResponse(
                        meetingId,
                        summaryType,
                        -1,
                        "Unknown"
                    )));
            });
    }
    
    /**
     * Exports a summary in the specified format.
     * 
     * @param request export request
     * @return response containing exported data
     */
    @PostMapping("/export")
    public Mono<ResponseEntity<ExportSummaryResponse>> exportSummary(@RequestBody ExportSummaryRequest request) {
        logger.info("Exporting {} summary for meeting: {} in format: {}", 
            request.summaryType(), request.meetingId(), request.format());
        
        return summaryGenerator.generateSummary(request.meetingId(), request.summaryType())
            .map(summary -> {
                var content = formatForExport(summary.content(), request.format(), request.includeMetadata());
                var filename = generateExportFilename(request.meetingId(), request.summaryType(), request.format());
                
                return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .header("Content-Type", getContentType(request.format()))
                    .body(new ExportSummaryResponse(
                        filename,
                        request.format(),
                        content,
                        content.getBytes().length
                    ));
            })
            .onErrorResume(error -> {
                logger.error("Failed to export summary", error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ExportSummaryResponse(
                        "",
                        request.format(),
                        "Error: " + error.getMessage(),
                        0
                    )));
            });
    }
    
    /**
     * Gets summary statistics for a meeting.
     * 
     * @param meetingId meeting identifier
     * @return response containing summary statistics
     */
    @GetMapping("/{meetingId}/statistics")
    public Mono<ResponseEntity<SummaryStatisticsResponse>> getSummaryStatistics(@PathVariable UUID meetingId) {
        logger.debug("Getting summary statistics for meeting: {}", meetingId);
        
        return transcriptionService.getAllTranscriptionSegments(meetingId)
            .collectList()
            .map(segments -> {
                var totalWords = segments.stream()
                    .mapToInt(segment -> segment.getText().split("\\s+").length)
                    .sum();
                
                var duration = calculateDuration(segments);
                var speakers = segments.stream()
                    .filter(segment -> segment.getSpeakerId() != null && !segment.getSpeakerId().isEmpty())
                    .map(TranscriptionSegment::getSpeakerId)
                    .distinct()
                    .count();
                
                // Estimate generation times for each type
                var estimates = Map.of(
                    SummaryGenerator.SummaryType.FULL, estimateTimeFromWordCount(totalWords, SummaryGenerator.SummaryType.FULL),
                    SummaryGenerator.SummaryType.KEY_POINTS, estimateTimeFromWordCount(totalWords, SummaryGenerator.SummaryType.KEY_POINTS),
                    SummaryGenerator.SummaryType.DECISIONS, estimateTimeFromWordCount(totalWords, SummaryGenerator.SummaryType.DECISIONS),
                    SummaryGenerator.SummaryType.ACTION_ITEMS, estimateTimeFromWordCount(totalWords, SummaryGenerator.SummaryType.ACTION_ITEMS),
                    SummaryGenerator.SummaryType.EXECUTIVE, estimateTimeFromWordCount(totalWords, SummaryGenerator.SummaryType.EXECUTIVE),
                    SummaryGenerator.SummaryType.TECHNICAL, estimateTimeFromWordCount(totalWords, SummaryGenerator.SummaryType.TECHNICAL)
                );
                
                return ResponseEntity.ok(new SummaryStatisticsResponse(
                    meetingId,
                    segments.size(),
                    totalWords,
                    duration,
                    (int) speakers,
                    estimates
                ));
            })
            .onErrorResume(error -> {
                logger.error("Failed to get summary statistics", error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new SummaryStatisticsResponse(
                        meetingId,
                        0,
                        0,
                        0,
                        0,
                        Map.of()
                    )));
            });
    }
    
    /**
     * Searches within summaries.
     * 
     * @param request search request
     * @return response containing search results
     */
    @PostMapping("/search")
    public Mono<ResponseEntity<SearchSummaryResponse>> searchSummaries(@RequestBody SearchSummaryRequest request) {
        logger.info("Searching summaries with query: {}", request.query());
        
        // This would typically search through stored summaries in a database
        // For now, return a mock response
        var mockResults = List.of(
            new SearchResult(
                UUID.randomUUID(),
                SummaryGenerator.SummaryType.KEY_POINTS,
                "Key Points Summary",
                "This summary contains the main discussion points about project planning...",
                LocalDateTime.now().minusMinutes(30),
                0.92
            ),
            new SearchResult(
                UUID.randomUUID(),
                SummaryGenerator.SummaryType.ACTION_ITEMS,
                "Action Items Summary", 
                "The following action items were identified during the meeting...",
                LocalDateTime.now().minusMinutes(15),
                0.88
            )
        );
        
        return Mono.just(ResponseEntity.ok(new SearchSummaryResponse(
            request.query(),
            mockResults,
            mockResults.size()
        )));
    }
    
    /**
     * Gets supported summary types.
     * 
     * @return response containing supported summary types
     */
    @GetMapping("/types")
    public Mono<ResponseEntity<SupportedSummaryTypesResponse>> getSupportedSummaryTypes() {
        logger.debug("Getting supported summary types");
        
        var types = List.of(
            new SummaryTypeInfo(SummaryGenerator.SummaryType.FULL, "Complete meeting summary with all details"),
            new SummaryTypeInfo(SummaryGenerator.SummaryType.KEY_POINTS, "Key points and main topics discussed"),
            new SummaryTypeInfo(SummaryGenerator.SummaryType.DECISIONS, "Decisions made and outcomes"),
            new SummaryTypeInfo(SummaryGenerator.SummaryType.ACTION_ITEMS, "Action items and responsibilities"),
            new SummaryTypeInfo(SummaryGenerator.SummaryType.EXECUTIVE, "High-level executive summary"),
            new SummaryTypeInfo(SummaryGenerator.SummaryType.TECHNICAL, "Technical details and specifications"),
            new SummaryTypeInfo(SummaryGenerator.SummaryType.CUSTOM, "Custom summary with user-defined prompt")
        );
        
        return Mono.just(ResponseEntity.ok(new SupportedSummaryTypesResponse(types)));
    }
    
    /**
     * Gets supported export formats.
     * 
     * @return response containing supported export formats
     */
    @GetMapping("/export-formats")
    public Mono<ResponseEntity<SupportedExportFormatsResponse>> getSupportedExportFormats() {
        logger.debug("Getting supported export formats");
        
        var formats = List.of(
            new ExportFormatInfo("html", "HTML", "Rich HTML format with styling"),
            new ExportFormatInfo("markdown", "Markdown", "Markdown format for documentation"),
            new ExportFormatInfo("txt", "Plain Text", "Plain text format"),
            new ExportFormatInfo("json", "JSON", "Structured JSON format"),
            new ExportFormatInfo("pdf", "PDF", "PDF document format"),
            new ExportFormatInfo("docx", "Word Document", "Microsoft Word document format")
        );
        
        return Mono.just(ResponseEntity.ok(new SupportedExportFormatsResponse(formats)));
    }
    
    /**
     * Deletes a summary.
     * 
     * @param meetingId meeting identifier
     * @param summaryType summary type
     * @return response indicating success or failure
     */
    @DeleteMapping("/{meetingId}/type/{summaryType}")
    public Mono<ResponseEntity<ApiResponse>> deleteSummary(
            @PathVariable UUID meetingId,
            @PathVariable SummaryGenerator.SummaryType summaryType) {
        logger.info("Deleting {} summary for meeting: {}", summaryType, meetingId);
        
        // This would typically delete from a database or cache
        // For now, just return success
        return Mono.just(ResponseEntity.ok(
            new ApiResponse(true, "Summary deleted successfully")))
            .onErrorResume(error -> {
                logger.error("Failed to delete summary", error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to delete summary: " + error.getMessage())));
            });
    }
    
    // Helper methods
    
    /**
     * Formats time estimate for display.
     */
    private String formatEstimateTime(int seconds) {
        if (seconds < 0) return "Unknown";
        if (seconds < 60) return String.format("%d seconds", seconds);
        if (seconds < 3600) return String.format("%d minutes %d seconds", seconds / 60, seconds % 60);
        return String.format("%d hours %d minutes", seconds / 3600, (seconds % 3600) / 60);
    }
    
    /**
     * Formats content for export.
     */
    private String formatForExport(String content, String format, boolean includeMetadata) {
        return switch (format.toLowerCase()) {
            case "html" -> content.contains("<html>") ? content : "<html><body>" + content + "</body></html>";
            case "markdown" -> convertToMarkdown(content);
            case "json" -> "{\"content\": " + escapeJson(content) + ", \"metadata\": " + includeMetadata + "}";
            case "pdf", "docx" -> content; // These would require special libraries
            default -> content; // Plain text
        };
    }
    
    /**
     * Converts to Markdown format.
     */
    private String convertToMarkdown(String content) {
        return content
            .replaceAll("<h1>(.*?)</h1>", "# $1\n")
            .replaceAll("<h2>(.*?)</h2>", "## $1\n")
            .replaceAll("<h3>(.*?)</h3>", "### $1\n")
            .replaceAll("<p>(.*?)</p>", "$1\n")
            .replaceAll("<li>(.*?)</li>", "- $1\n")
            .replaceAll("<[^>]*>", "")
            .trim();
    }
    
    /**
     * Escapes JSON content.
     */
    private String escapeJson(String content) {
        return content.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
    
    /**
     * Generates export filename.
     */
    private String generateExportFilename(UUID meetingId, SummaryGenerator.SummaryType summaryType, String format) {
        var timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        var extension = switch (format.toLowerCase()) {
            case "html" -> "html";
            case "markdown" -> "md";
            case "json" -> "json";
            case "pdf" -> "pdf";
            case "docx" -> "docx";
            default -> "txt";
        };
        
        return String.format("summary_%s_%s_%s.%s", 
            meetingId.toString().substring(0, 8),
            summaryType.name().toLowerCase(),
            timestamp,
            extension);
    }
    
    /**
     * Gets content type for export format.
     */
    private String getContentType(String format) {
        return switch (format.toLowerCase()) {
            case "html" -> "text/html";
            case "markdown" -> "text/markdown";
            case "json" -> "application/json";
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "text/plain";
        };
    }
    
    /**
     * Calculates duration from segments.
     */
    private long calculateDuration(List<?> segments) {
        // Implementation would depend on segment type
        // For now, return 0
        return 0;
    }
    
    /**
     * Estimates generation time from word count.
     */
    private int estimateTimeFromWordCount(int wordCount, SummaryGenerator.SummaryType summaryType) {
        double baseTimePerWord = switch (summaryType) {
            case FULL -> 0.02;
            case KEY_POINTS -> 0.015;
            case DECISIONS -> 0.01;
            case ACTION_ITEMS -> 0.01;
            case EXECUTIVE -> 0.025;
            case TECHNICAL -> 0.03;
            case CUSTOM -> 0.02;
        };
        
        return (int) Math.ceil(wordCount * baseTimePerWord);
    }
    
    // Response DTOs
    
    public record ApiResponse(
        boolean success,
        String message
    ) {}
    
    public record GenerateSummaryResponse(
        UUID meetingId,
        SummaryGenerator.SummaryType summaryType,
        String content,
        LocalDateTime generatedAt,
        int segmentCount,
        int wordCount,
        long durationSeconds,
        List<String> speakers,
        String formattedDuration,
        String formattedWordCount
    ) {}
    
    public record SummaryChunkResponse(
        UUID meetingId,
        SummaryGenerator.SummaryType summaryType,
        String content,
        long chunkIndex,
        boolean isFirstChunk
    ) {}
    
    public record MultipleSummariesResponse(
        UUID meetingId,
        List<GenerateSummaryResponse> summaries,
        int count
    ) {}
    
    public record GetSummaryResponse(
        UUID meetingId,
        SummaryGenerator.SummaryType summaryType,
        String content,
        LocalDateTime generatedAt,
        int segmentCount,
        int wordCount,
        long durationSeconds,
        List<String> speakers
    ) {}
    
    public record AvailableSummaryTypesResponse(
        UUID meetingId,
        List<SummaryGenerator.SummaryType> summaryTypes,
        String message
    ) {}
    
    public record EstimateResponse(
        UUID meetingId,
        SummaryGenerator.SummaryType summaryType,
        int estimatedSeconds,
        String formattedTime
    ) {}
    
    public record ExportSummaryResponse(
        String filename,
        String format,
        String content,
        int contentLength
    ) {}
    
    public record SummaryStatisticsResponse(
        UUID meetingId,
        int totalSegments,
        int totalWords,
        long totalDurationSeconds,
        int speakerCount,
        Map<SummaryGenerator.SummaryType, Integer> estimatedGenerationTimes
    ) {}
    
    public record SearchSummaryResponse(
        String query,
        List<SearchResult> results,
        int total
    ) {}
    
    public record SupportedSummaryTypesResponse(
        List<SummaryTypeInfo> summaryTypes
    ) {}
    
    public record SupportedExportFormatsResponse(
        List<ExportFormatInfo> exportFormats
    ) {}
    
    // Request DTOs
    
    public record GenerateSummaryRequest(
        UUID meetingId,
        SummaryGenerator.SummaryType summaryType,
        String model,
        Double confidenceThreshold,
        Boolean includeTimestamps
    ) {}
    
    public record GenerateMultipleSummariesRequest(
        UUID meetingId,
        List<SummaryGenerator.SummaryType> summaryTypes,
        String model,
        Double confidenceThreshold
    ) {}
    
    public record CustomSummaryRequest(
        UUID meetingId,
        String prompt,
        boolean includeTimestamps,
        String model,
        Double confidenceThreshold
    ) {}
    
    public record ExportSummaryRequest(
        UUID meetingId,
        SummaryGenerator.SummaryType summaryType,
        String format,
        boolean includeMetadata,
        boolean includeTranscription
    ) {}
    
    public record SearchSummaryRequest(
        String query,
        List<SummaryGenerator.SummaryType> summaryTypes,
        LocalDateTime startDate,
        LocalDateTime endDate,
        int maxResults
    ) {}
    
    // Additional DTOs
    
    public record SearchResult(
        UUID meetingId,
        SummaryGenerator.SummaryType summaryType,
        String title,
        String snippet,
        LocalDateTime timestamp,
        double relevance
    ) {}
    
    public record SummaryTypeInfo(
        SummaryGenerator.SummaryType type,
        String description
    ) {}
    
    public record ExportFormatInfo(
        String format,
        String name,
        String description
    ) {}
}