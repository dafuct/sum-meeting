package com.zoomtranscriber.core.storage;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "summaries")
public class Summary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_session_id", nullable = false)
    private MeetingSession meetingSession;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "summary_type", nullable = false)
    private SummaryType summaryType;
    
    @Lob
    @Column(nullable = false, length = 50000)
    private String content;
    
    @Column(name = "model_used")
    private String modelUsed;
    
    @Column(name = "processing_time")
    private String processingTime;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public enum SummaryType {
        FULL,
        KEY_POINTS,
        DECISIONS,
        ACTION_ITEMS
    }
    
    public Summary() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Summary(MeetingSession meetingSession, SummaryType summaryType, String content) {
        this();
        this.meetingSession = meetingSession;
        this.summaryType = summaryType;
        this.content = content;
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
    
    public SummaryType getSummaryType() {
        return summaryType;
    }
    
    public void setSummaryType(SummaryType summaryType) {
        this.summaryType = summaryType;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getModelUsed() {
        return modelUsed;
    }
    
    public void setModelUsed(String modelUsed) {
        this.modelUsed = modelUsed;
    }
    
    public String getProcessingTime() {
        return processingTime;
    }
    
    public void setProcessingTime(String processingTime) {
        this.processingTime = processingTime;
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
        
        var that = (Summary) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "Summary{" +
                "id=" + id +
                ", meetingSessionId=" + (meetingSession != null ? meetingSession.getId() : null) +
                ", summaryType=" + summaryType +
                ", modelUsed='" + modelUsed + '\'' +
                ", processingTime='" + processingTime + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}