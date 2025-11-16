package com.zoomtranscriber.core.ai;

import com.zoomtranscriber.core.transcription.TranscriptionService;
import com.zoomtranscriber.core.transcription.TranscriptionSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for generating meeting summaries using Ollama.
 * Supports different summary types and configurable processing strategies.
 */
@Service
public class SummaryGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(SummaryGenerator.class);
    
    private final OllamaService ollamaService;
    private final TranscriptionService transcriptionService;
    private final SummaryConfig summaryConfig;
    
    @Autowired
    public SummaryGenerator(OllamaService ollamaService, 
                           TranscriptionService transcriptionService,
                           SummaryConfig summaryConfig) {
        this.ollamaService = ollamaService;
        this.transcriptionService = transcriptionService;
        this.summaryConfig = summaryConfig;
    }
    
    /**
     * Generates a summary for a meeting with the specified type.
     * 
     * @param meetingId the meeting identifier
     * @param summaryType the type of summary to generate
     * @return Mono containing the generated summary
     */
    public Mono<MeetingSummary> generateSummary(UUID meetingId, SummaryType summaryType) {
        logger.info("Generating {} summary for meeting: {}", summaryType, meetingId);
        
        return transcriptionService.getAllTranscriptionSegments(meetingId)
            .collectList()
            .flatMap(segments -> {
                if (segments.isEmpty()) {
                    logger.warn("No transcription segments found for meeting: {}", meetingId);
                    return Mono.empty();
                }
                
                String transcriptionText = formatTranscriptionForSummary(segments);
                String prompt = buildPrompt(transcriptionText, summaryType);
                
                return ollamaService.generateText(
                    summaryConfig.getModel(),
                    prompt
                ).map(summary -> new MeetingSummary(
                    meetingId,
                    summaryType,
                    summary,
                    LocalDateTime.now(),
                    segments.size(),
                    calculateWordCount(segments),
                    calculateDuration(segments),
                    extractSpeakers(segments)
                ));
            })
            .doOnSuccess(summary -> logger.info("Successfully generated {} summary for meeting: {}", summaryType, meetingId))
            .doOnError(error -> logger.error("Error generating summary for meeting: {}", meetingId, error))
            .onErrorResume(error -> {
                logger.error("Failed to generate summary, returning fallback", error);
                return createFallbackSummary(meetingId, summaryType);
            });
    }
    
    /**
     * Generates a summary with streaming response.
     * 
     * @param meetingId the meeting identifier
     * @param summaryType the type of summary to generate
     * @return Flux of summary chunks as they are generated
     */
    public Flux<String> generateSummaryStream(UUID meetingId, SummaryType summaryType) {
        logger.info("Generating {} summary stream for meeting: {}", summaryType, meetingId);
        
        return transcriptionService.getAllTranscriptionSegments(meetingId)
            .collectList()
            .flatMapMany(segments -> {
                if (segments.isEmpty()) {
                    logger.warn("No transcription segments found for meeting: {}", meetingId);
                    return Flux.empty();
                }
                
                String transcriptionText = formatTranscriptionForSummary(segments);
                String prompt = buildPrompt(transcriptionText, summaryType);
                
                return ollamaService.generateTextStream(
                    summaryConfig.getModel(),
                    prompt
                );
            })
            .doOnComplete(() -> logger.info("Completed streaming summary for meeting: {}", meetingId))
            .doOnError(error -> logger.error("Error in summary stream for meeting: {}", meetingId, error))
            .onErrorResume(error -> Flux.just("Error generating summary: " + error.getMessage()));
    }
    
    /**
     * Generates multiple summary types for a meeting.
     * 
     * @param meetingId the meeting identifier
     * @param summaryTypes list of summary types to generate
     * @return Flux of MeetingSummary objects
     */
    public Flux<MeetingSummary> generateMultipleSummaries(UUID meetingId, List<SummaryType> summaryTypes) {
        logger.info("Generating {} summary types for meeting: {}", summaryTypes.size(), meetingId);
        
        return Flux.fromIterable(summaryTypes)
            .parallel()
            .runOn(Schedulers.parallel())
            .flatMap(summaryType -> generateSummary(meetingId, summaryType))
            .sequential()
            .doOnComplete(() -> logger.info("Completed generating all summaries for meeting: {}", meetingId));
    }
    
    /**
     * Generates a custom summary with a specific prompt.
     * 
     * @param meetingId the meeting identifier
     * @param customPrompt the custom prompt to use
     * @param includeTimestamps whether to include timestamps in the transcription
     * @return Mono containing the generated summary
     */
    public Mono<MeetingSummary> generateCustomSummary(UUID meetingId, String customPrompt, boolean includeTimestamps) {
        logger.info("Generating custom summary for meeting: {}", meetingId);
        
        return transcriptionService.getAllTranscriptionSegments(meetingId)
            .collectList()
            .flatMap(segments -> {
                if (segments.isEmpty()) {
                    return Mono.empty();
                }
                
                String transcriptionText = includeTimestamps ? 
                    formatTranscriptionWithTimestamps(segments) : 
                    formatTranscriptionForSummary(segments);
                
                String fullPrompt = String.format(
                    "%s\n\nTranscription:\n%s", 
                    customPrompt, 
                    transcriptionText
                );
                
                return ollamaService.generateText(
                    summaryConfig.getModel(),
                    fullPrompt
                ).map(summary -> new MeetingSummary(
                    meetingId,
                    SummaryType.CUSTOM,
                    summary,
                    LocalDateTime.now(),
                    segments.size(),
                    calculateWordCount(segments),
                    calculateDuration(segments),
                    extractSpeakers(segments)
                ));
            });
    }
    
    /**
     * Validates if a summary can be generated for the meeting.
     * 
     * @param meetingId the meeting identifier
     * @return Mono containing true if summary can be generated
     */
    public Mono<Boolean> canGenerateSummary(UUID meetingId) {
        return transcriptionService.getAllTranscriptionSegments(meetingId)
            .hasElements()
            .flatMap(hasSegments -> {
                if (!hasSegments) {
                    return Mono.just(false);
                }
                
                return ollamaService.isModelAvailable(summaryConfig.getModel());
            });
    }
    
    /**
     * Gets the estimated time to generate a summary.
     * 
     * @param meetingId the meeting identifier
     * @param summaryType the type of summary
     * @return Mono containing estimated duration in seconds
     */
    public Mono<Integer> estimateGenerationTime(UUID meetingId, SummaryType summaryType) {
        return transcriptionService.getAllTranscriptionSegments(meetingId)
            .collectList()
            .map(segments -> {
                int wordCount = calculateWordCount(segments);
                return estimateTimeFromWordCount(wordCount, summaryType);
            });
    }
    
    /**
     * Builds the appropriate prompt for the given summary type.
     * 
     * @param transcription the transcription text
     * @param summaryType the type of summary
     * @return formatted prompt string
     */
    private String buildPrompt(String transcription, SummaryType summaryType) {
        String basePrompt = summaryConfig.getBasePrompt();
        String specificPrompt = switch (summaryType) {
            case FULL -> summaryConfig.getFullSummaryPrompt();
            case KEY_POINTS -> summaryConfig.getKeyPointsPrompt();
            case DECISIONS -> summaryConfig.getDecisionsPrompt();
            case ACTION_ITEMS -> summaryConfig.getActionItemsPrompt();
            case EXECUTIVE -> summaryConfig.getExecutivePrompt();
            case TECHNICAL -> summaryConfig.getTechnicalPrompt();
            case CUSTOM -> "";
        };
        
        return String.format(
            "%s\n\n%s\n\nTranscription:\n%s",
            basePrompt,
            specificPrompt,
            transcription
        );
    }
    
    /**
     * Formats transcription segments for summary generation.
     * 
     * @param segments list of transcription segments
     * @return formatted transcription text
     */
    private String formatTranscriptionForSummary(List<TranscriptionSegment> segments) {
        return segments.stream()
            .<String>map(segment -> {
                String speaker = segment.getSpeakerId() != null ? segment.getSpeakerId() + ": " : "";
                return speaker + segment.getText();
            })
            .collect(Collectors.joining("\n"));
    }
    
    /**
     * Formats transcription segments with timestamps.
     * 
     * @param segments list of transcription segments
     * @return formatted transcription text with timestamps
     */
    private String formatTranscriptionWithTimestamps(List<TranscriptionSegment> segments) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return segments.stream()
            .<String>map(segment -> {
                String timestamp = segment.getTimestamp() != null ? 
                    "[" + segment.getTimestamp().format(formatter) + "] " : "";
                String speaker = segment.getSpeakerId() != null ? segment.getSpeakerId() + ": " : "";
                return timestamp + speaker + segment.getText();
            })
            .collect(Collectors.joining("\n"));
    }
    
    /**
     * Calculates the total word count from segments.
     * 
     * @param segments list of transcription segments
     * @return total word count
     */
    private int calculateWordCount(List<TranscriptionSegment> segments) {
        return segments.stream()
            .mapToInt(segment -> segment.getText().split("\\s+").length)
            .sum();
    }
    
    /**
     * Calculates the total duration of the meeting.
     * 
     * @param segments list of transcription segments
     * @return duration in seconds
     */
    private long calculateDuration(List<TranscriptionSegment> segments) {
        if (segments.isEmpty()) return 0;
        
        var startSegment = segments.get(0);
        var endSegment = segments.get(segments.size() - 1);
        
        if (startSegment.getTimestamp() != null && endSegment.getTimestamp() != null) {
            return java.time.Duration.between(startSegment.getTimestamp(), endSegment.getTimestamp()).getSeconds();
        }
        
        return 0;
    }
    
    /**
     * Extracts unique speakers from segments.
     * 
     * @param segments list of transcription segments
     * @return list of unique speakers
     */
    private List<String> extractSpeakers(List<TranscriptionSegment> segments) {
        return segments.stream()
            .map(TranscriptionSegment::getSpeakerId)
            .filter(speaker -> speaker != null && !speaker.isEmpty())
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * Creates a fallback summary when AI generation fails.
     * 
     * @param meetingId the meeting identifier
     * @param summaryType the type of summary
     * @return Mono containing fallback summary
     */
    private Mono<MeetingSummary> createFallbackSummary(UUID meetingId, SummaryType summaryType) {
        return transcriptionService.getAllTranscriptionSegments(meetingId)
            .collectList()
            .map(segments -> {
                String fallbackText = String.format(
                    "Unable to generate %s summary at this time. Meeting contains %d segments with %d words.",
                    summaryType.name().toLowerCase().replace("_", " "),
                    segments.size(),
                    calculateWordCount(segments)
                );
                
                return new MeetingSummary(
                    meetingId,
                    summaryType,
                    fallbackText,
                    LocalDateTime.now(),
                    segments.size(),
                    calculateWordCount(segments),
                    calculateDuration(segments),
                    extractSpeakers(segments)
                );
            });
    }
    
    /**
     * Estimates generation time based on word count.
     * 
     * @param wordCount total word count
     * @param summaryType the type of summary
     * @return estimated time in seconds
     */
    private int estimateTimeFromWordCount(int wordCount, SummaryType summaryType) {
        double baseTimePerWord = switch (summaryType) {
            case FULL -> 0.02; // 20ms per word
            case KEY_POINTS -> 0.015;
            case DECISIONS -> 0.01;
            case ACTION_ITEMS -> 0.01;
            case EXECUTIVE -> 0.025;
            case TECHNICAL -> 0.03;
            case CUSTOM -> 0.02;
        };
        
        return (int) Math.ceil(wordCount * baseTimePerWord);
    }
    
    /**
     * Represents the type of summary to generate.
     */
    public enum SummaryType {
        FULL("Complete meeting summary with all details"),
        KEY_POINTS("Key points and main topics discussed"),
        DECISIONS("Decisions made and outcomes"),
        ACTION_ITEMS("Action items and responsibilities"),
        EXECUTIVE("High-level executive summary"),
        TECHNICAL("Technical details and specifications"),
        CUSTOM("Custom summary with user-defined prompt");
        
        private final String description;
        
        SummaryType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Represents a generated meeting summary.
     */
    public record MeetingSummary(
        UUID meetingId,
        SummaryType summaryType,
        String content,
        LocalDateTime generatedAt,
        int segmentCount,
        int wordCount,
        long durationSeconds,
        List<String> speakers
    ) {
        /**
         * Gets the formatted duration.
         * 
         * @return formatted duration string
         */
        public String getFormattedDuration() {
            long hours = durationSeconds / 3600;
            long minutes = (durationSeconds % 3600) / 60;
            long seconds = durationSeconds % 60;
            
            if (hours > 0) {
                return String.format("%dh %dm %ds", hours, minutes, seconds);
            } else if (minutes > 0) {
                return String.format("%dm %ds", minutes, seconds);
            } else {
                return String.format("%ds", seconds);
            }
        }
        
        /**
         * Gets the word count as a formatted string.
         * 
         * @return formatted word count
         */
        public String getFormattedWordCount() {
            if (wordCount >= 1000) {
                return String.format("%.1fk words", wordCount / 1000.0);
            } else {
                return String.format("%d words", wordCount);
            }
        }
    }
    
    /**
     * Configuration class for summary generation.
     */
    public static class SummaryConfig {
        private String model = "qwen2.5:0.5b";
        private String basePrompt = "You are an expert meeting assistant. Generate a clear, concise, and accurate summary based on the meeting transcription provided.";
        private String fullSummaryPrompt = "Create a comprehensive summary of the meeting including all main topics, discussions, and outcomes. Organize it logically with clear sections.";
        private String keyPointsPrompt = "Extract and list the key points and main topics discussed in this meeting. Focus on the most important information.";
        private String decisionsPrompt = "Identify and list all decisions made during this meeting, including who made them and what was decided.";
        private String actionItemsPrompt = "Extract all action items from this meeting, including who is responsible and any deadlines mentioned.";
        private String executivePrompt = "Create a high-level executive summary suitable for busy stakeholders. Focus on strategic outcomes and key takeaways.";
        private String technicalPrompt = "Create a detailed technical summary focusing on technical details, specifications, and implementation aspects discussed.";
        private int maxRetries = 3;
        private int timeoutSeconds = 60;
        
        public SummaryConfig() {}
        
        // Getters and setters
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        
        public String getBasePrompt() { return basePrompt; }
        public void setBasePrompt(String basePrompt) { this.basePrompt = basePrompt; }
        
        public String getFullSummaryPrompt() { return fullSummaryPrompt; }
        public void setFullSummaryPrompt(String fullSummaryPrompt) { this.fullSummaryPrompt = fullSummaryPrompt; }
        
        public String getKeyPointsPrompt() { return keyPointsPrompt; }
        public void setKeyPointsPrompt(String keyPointsPrompt) { this.keyPointsPrompt = keyPointsPrompt; }
        
        public String getDecisionsPrompt() { return decisionsPrompt; }
        public void setDecisionsPrompt(String decisionsPrompt) { this.decisionsPrompt = decisionsPrompt; }
        
        public String getActionItemsPrompt() { return actionItemsPrompt; }
        public void setActionItemsPrompt(String actionItemsPrompt) { this.actionItemsPrompt = actionItemsPrompt; }
        
        public String getExecutivePrompt() { return executivePrompt; }
        public void setExecutivePrompt(String executivePrompt) { this.executivePrompt = executivePrompt; }
        
        public String getTechnicalPrompt() { return technicalPrompt; }
        public void setTechnicalPrompt(String technicalPrompt) { this.technicalPrompt = technicalPrompt; }
        
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }
}