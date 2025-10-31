package com.zoomtranscriber.core.storage;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transcriptions")
public class Transcription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_session_id", nullable = false)
    private MeetingSession meetingSession;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "speaker_id")
    private String speakerId;
    
    @Lob
    @Column(nullable = false, length = 10000)
    private String text;
    
    @Column(nullable = false)
    private Double confidence;
    
    @Column(name = "segment_number", nullable = false)
    private Integer segmentNumber;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public Transcription() {
        this.createdAt = LocalDateTime.now();
        this.confidence = 0.0;
    }
    
    public Transcription(MeetingSession meetingSession, String text, LocalDateTime timestamp, Integer segmentNumber) {
        this();
        this.meetingSession = meetingSession;
        this.text = text;
        this.timestamp = timestamp;
        this.segmentNumber = segmentNumber;
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public MeetingSession getMeetingSession() {
        return meetingSession;
    }
    
    public void setMeetingSession(MeetingSession meetingSession) {
        this.meetingSession = meetingSession;
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
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    public Integer getSegmentNumber() {
        return segmentNumber;
    }
    
    public void setSegmentNumber(Integer segmentNumber) {
        this.segmentNumber = segmentNumber;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        var that = (Transcription) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "Transcription{" +
                "id=" + id +
                ", meetingSessionId=" + (meetingSession != null ? meetingSession.getId() : null) +
                ", timestamp=" + timestamp +
                ", speakerId='" + speakerId + '\'' +
                ", text='" + text + '\'' +
                ", confidence=" + confidence +
                ", segmentNumber=" + segmentNumber +
                '}';
    }
}