package com.zoomtranscriber.core.transcription;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a segment of transcribed speech.
 * Contains timing, speaker, and confidence information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TranscriptionSegment {
    
    private UUID id;
    private UUID meetingId;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    private String speakerId;
    private String text;
    private double confidence;
    private int segmentNumber;
    private boolean isFinal;
    private Duration duration;
    private String language;
    private String[] alternatives;
    private TranscriptionSegmentType type;
    private double startTime;
    private double endTime;
    private String[] keywords;
    private Sentiment sentiment;
    
    // Default constructor
    public TranscriptionSegment() {}
    
    // Full constructor
    public TranscriptionSegment(
        UUID id,
        UUID meetingId,
        LocalDateTime timestamp,
        String speakerId,
        String text,
        double confidence,
        int segmentNumber,
        boolean isFinal,
        Duration duration,
        String language
    ) {
        this.id = id;
        this.meetingId = meetingId;
        this.timestamp = timestamp;
        this.speakerId = speakerId;
        this.text = text;
        this.confidence = confidence;
        this.segmentNumber = segmentNumber;
        this.isFinal = isFinal;
        this.duration = duration;
        this.language = language;
        this.type = TranscriptionSegmentType.SPEECH;
        this.startTime = 0.0;
        this.endTime = duration != null ? duration.toMillis() / 1000.0 : 0.0;
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getMeetingId() {
        return meetingId;
    }
    
    public void setMeetingId(UUID meetingId) {
        this.meetingId = meetingId;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getSpeakerId() {
        return speakerId;
    }
    
    public void setSpeakerId(String speakerId) {
        this.speakerId = speakerId;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
    
    public int getSegmentNumber() {
        return segmentNumber;
    }
    
    public void setSegmentNumber(int segmentNumber) {
        this.segmentNumber = segmentNumber;
    }
    
    public boolean isFinal() {
        return isFinal;
    }
    
    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }
    
    public Duration getDuration() {
        return duration;
    }
    
    public void setDuration(Duration duration) {
        this.duration = duration;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String[] getAlternatives() {
        return alternatives;
    }
    
    public void setAlternatives(String[] alternatives) {
        this.alternatives = alternatives;
    }
    
    public TranscriptionSegmentType getType() {
        return type;
    }
    
    public void setType(TranscriptionSegmentType type) {
        this.type = type;
    }
    
    public double getStartTime() {
        return startTime;
    }
    
    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }
    
    public double getEndTime() {
        return endTime;
    }
    
    public void setEndTime(double endTime) {
        this.endTime = endTime;
    }
    
    public String[] getKeywords() {
        return keywords;
    }
    
    public void setKeywords(String[] keywords) {
        this.keywords = keywords;
    }
    
    public Sentiment getSentiment() {
        return sentiment;
    }
    
    public void setSentiment(Sentiment sentiment) {
        this.sentiment = sentiment;
    }
    
    // Utility methods
    
    /**
     * Gets the word count of the transcription text.
     * 
     * @return number of words
     */
    public int getWordCount() {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
    
    /**
     * Gets the character count of the transcription text.
     * 
     * @return number of characters
     */
    public int getCharacterCount() {
        return text != null ? text.length() : 0;
    }
    
    /**
     * Checks if the segment has high confidence.
     * 
     * @param threshold confidence threshold
     * @return true if confidence is above threshold
     */
    public boolean hasHighConfidence(double threshold) {
        return confidence >= threshold;
    }
    
    /**
     * Checks if the segment contains specific text.
     * 
     * @param searchText text to search for
     * @return true if text is found
     */
    public boolean containsText(String searchText) {
        return text != null && searchText != null && 
               text.toLowerCase().contains(searchText.toLowerCase());
    }
    
    /**
     * Gets a formatted representation of the timestamp.
     * 
     * @return formatted timestamp string
     */
    public String getFormattedTimestamp() {
        if (timestamp == null) {
            return "";
        }
        return timestamp.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    
    /**
     * Gets a formatted representation of the duration.
     * 
     * @return formatted duration string
     */
    public String getFormattedDuration() {
        if (duration == null) {
            return "";
        }
        var seconds = duration.toSecondsPart();
        var minutes = duration.toMinutesPart();
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    /**
     * Creates a copy of this segment with updated text.
     * 
     * @param newText the new text
     * @return new TranscriptionSegment with updated text
     */
    public TranscriptionSegment withText(String newText) {
        var copy = new TranscriptionSegment(
            this.id,
            this.meetingId,
            this.timestamp,
            this.speakerId,
            newText,
            this.confidence,
            this.segmentNumber,
            this.isFinal,
            this.duration,
            this.language
        );
        copy.setAlternatives(this.alternatives);
        copy.setType(this.type);
        copy.setStartTime(this.startTime);
        copy.setEndTime(this.endTime);
        copy.setKeywords(this.keywords);
        copy.setSentiment(this.sentiment);
        return copy;
    }
    
    /**
     * Creates a copy of this segment with updated confidence.
     * 
     * @param newConfidence the new confidence
     * @return new TranscriptionSegment with updated confidence
     */
    public TranscriptionSegment withConfidence(double newConfidence) {
        var copy = new TranscriptionSegment(
            this.id,
            this.meetingId,
            this.timestamp,
            this.speakerId,
            this.text,
            newConfidence,
            this.segmentNumber,
            this.isFinal,
            this.duration,
            this.language
        );
        copy.setAlternatives(this.alternatives);
        copy.setType(this.type);
        copy.setStartTime(this.startTime);
        copy.setEndTime(this.endTime);
        copy.setKeywords(this.keywords);
        copy.setSentiment(this.sentiment);
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format("TranscriptionSegment{id=%s, speaker='%s', text='%s', confidence=%.2f, segment=%d}",
            id != null ? id.toString().substring(0, 8) : "null",
            speakerId != null ? speakerId : "unknown",
            text != null && text.length() > 50 ? text.substring(0, 47) + "..." : text,
            confidence,
            segmentNumber
        );
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TranscriptionSegment that = (TranscriptionSegment) obj;
        return java.util.Objects.equals(id, that.id) &&
               java.util.Objects.equals(meetingId, that.meetingId) &&
               java.util.Objects.equals(timestamp, that.timestamp) &&
               java.util.Objects.equals(text, that.text);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, meetingId, timestamp, text);
    }
    
    /**
     * Represents the type of transcription segment.
     */
    public enum TranscriptionSegmentType {
        SPEECH,
        SILENCE,
        MUSIC,
        NOISE,
        PUNCTUATION,
        UNKNOWN
    }
    
    /**
     * Represents sentiment analysis result.
     */
    public enum Sentiment {
        POSITIVE("Positive", 1.0),
        NEUTRAL("Neutral", 0.0),
        NEGATIVE("Negative", -1.0),
        UNKNOWN("Unknown", 0.0);
        
        private final String displayName;
        private final double score;
        
        Sentiment(String displayName, double score) {
            this.displayName = displayName;
            this.score = score;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public double getScore() {
            return score;
        }
    }
}