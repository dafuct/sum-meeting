package com.zoomtranscriber.core.transcription;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for real-time speech transcription.
 * Provides reactive streams for continuous transcription processing.
 */
public interface TranscriptionService {
    
    /**
     * Starts transcription for a meeting session.
     * 
     * @param meetingId the meeting identifier
     * @param config transcription configuration
     * @return Mono that completes when transcription starts successfully
     */
    Mono<Void> startTranscription(UUID meetingId, TranscriptionConfig config);
    
    /**
     * Stops transcription for a meeting session.
     * 
     * @param meetingId the meeting identifier
     * @return Mono that completes when transcription stops successfully
     */
    Mono<Void> stopTranscription(UUID meetingId);
    
    /**
     * Gets the current transcription status.
     * 
     * @param meetingId the meeting identifier
     * @return Mono containing the transcription status
     */
    Mono<TranscriptionStatus> getTranscriptionStatus(UUID meetingId);
    
    /**
     * Gets a stream of transcription segments for a meeting.
     * 
     * @param meetingId the meeting identifier
     * @return Flux of TranscriptionSegment objects
     */
    Flux<TranscriptionSegment> getTranscriptionStream(UUID meetingId);
    
    /**
     * Gets all transcription segments for a meeting.
     * 
     * @param meetingId the meeting identifier
     * @return Flux of all TranscriptionSegment objects
     */
    Flux<TranscriptionSegment> getAllTranscriptionSegments(UUID meetingId);
    
    /**
     * Processes audio data for transcription.
     * 
     * @param meetingId the meeting identifier
     * @param audioData the audio data to transcribe
     * @return Flux of TranscriptionSegment objects
     */
    Flux<TranscriptionSegment> processAudio(UUID meetingId, byte[] audioData);
    
    /**
     * Sets the language for transcription.
     * 
     * @param meetingId the meeting identifier
     * @param language the language code (e.g., "en-US")
     * @return Mono that completes when language is set
     */
    Mono<Void> setLanguage(UUID meetingId, String language);
    
    /**
     * Gets the current language for a meeting.
     * 
     * @param meetingId the meeting identifier
     * @return Mono containing the current language code
     */
    Mono<String> getLanguage(UUID meetingId);
    
    /**
     * Enables or disables speaker diarization.
     * 
     * @param meetingId the meeting identifier
     * @param enabled true to enable speaker diarization
     * @return Mono that completes when setting is applied
     */
    Mono<Void> setSpeakerDiarization(UUID meetingId, boolean enabled);
    
    /**
     * Checks if speaker diarization is enabled for a meeting.
     * 
     * @param meetingId the meeting identifier
     * @return Mono containing true if enabled
     */
    Mono<Boolean> isSpeakerDiarizationEnabled(UUID meetingId);
    
    /**
     * Sets the confidence threshold for transcription.
     * 
     * @param meetingId the meeting identifier
     * @param threshold confidence threshold (0.0 to 1.0)
     * @return Mono that completes when threshold is set
     */
    Mono<Void> setConfidenceThreshold(UUID meetingId, double threshold);
    
    /**
     * Gets the current confidence threshold for a meeting.
     * 
     * @param meetingId the meeting identifier
     * @return Mono containing the current threshold
     */
    Mono<Double> getConfidenceThreshold(UUID meetingId);
    
    /**
     * Gets transcription statistics for a meeting.
     * 
     * @param meetingId the meeting identifier
     * @return Mono containing transcription statistics
     */
    Mono<TranscriptionStats> getTranscriptionStats(UUID meetingId);
    
    /**
     * Exports transcription for a meeting.
     * 
     * @param meetingId the meeting identifier
     * @param format export format
     * @return Mono containing the exported data
     */
    Mono<byte[]> exportTranscription(UUID meetingId, ExportFormat format);
    
    /**
     * Represents transcription status.
     */
    enum TranscriptionStatus {
        NOT_STARTED,
        INITIALIZING,
        ACTIVE,
        PAUSED,
        PROCESSING,
        COMPLETED,
        ERROR
    }
    
    /**
     * Represents transcription configuration.
     */
    record TranscriptionConfig(
        String language,
        boolean enableSpeakerDiarization,
        double confidenceThreshold,
        boolean enablePunctuation,
        boolean enableCapitalization,
        boolean enableTimestamps,
        int maxSpeakers,
        String model
    ) {
        /**
         * Creates a default transcription configuration.
         * 
         * @return default TranscriptionConfig
         */
        public static TranscriptionConfig defaultConfig() {
            return new TranscriptionConfig(
                "en-US",
                true,
                0.7,
                true,
                true,
                true,
                10,
                "whisper-1"
            );
        }
        
        /**
         * Creates a high-quality transcription configuration.
         * 
         * @return high-quality TranscriptionConfig
         */
        public static TranscriptionConfig highQuality() {
            return new TranscriptionConfig(
                "en-US",
                true,
                0.8,
                true,
                true,
                true,
                20,
                "whisper-1"
            );
        }
        
        /**
         * Creates a fast transcription configuration.
         * 
         * @return fast TranscriptionConfig
         */
        public static TranscriptionConfig fast() {
            return new TranscriptionConfig(
                "en-US",
                false,
                0.6,
                false,
                false,
                true,
                5,
                "whisper-tiny"
            );
        }
    }
    
    /**
     * Represents transcription statistics.
     */
    record TranscriptionStats(
        UUID meetingId,
        int totalSegments,
        int totalWords,
        double averageConfidence,
        LocalDateTime startTime,
        LocalDateTime endTime,
        java.time.Duration totalDuration,
        int speakerCount,
        List<String> detectedLanguages
    ) {
        /**
         * Calculates words per minute.
         * 
         * @return words per minute
         */
        public double getWordsPerMinute() {
            if (totalDuration == null || totalDuration.isZero()) return 0.0;
            var minutes = totalDuration.toMinutes();
            return minutes > 0 ? (double) totalWords / minutes : 0.0;
        }
        
        /**
         * Calculates average segment duration.
         * 
         * @return average segment duration
         */
        public java.time.Duration getAverageSegmentDuration() {
            if (totalSegments == 0) return java.time.Duration.ZERO;
            return totalDuration.dividedBy(totalSegments);
        }
    }
    
    /**
     * Represents export format options.
     */
    enum ExportFormat {
        TXT,
        SRT,
        VTT,
        JSON,
        CSV,
        DOCX
    }
}