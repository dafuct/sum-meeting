package com.zoomtranscriber.core.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TranscriptionRepository extends JpaRepository<Transcription, UUID> {
    
    List<Transcription> findByMeetingSessionId(UUID meetingSessionId);
    
    List<Transcription> findByMeetingSessionIdOrderBySegmentNumber(UUID meetingSessionId);
    
    List<Transcription> findByMeetingSessionIdOrderByTimestamp(UUID meetingSessionId);
    
    @Query("SELECT t FROM Transcription t WHERE t.meetingSession.id = :meetingSessionId ORDER BY t.segmentNumber")
    List<Transcription> findByMeetingSessionIdOrderBySegmentNumberQuery(@Param("meetingSessionId") UUID meetingSessionId);
    
    List<Transcription> findBySpeakerId(String speakerId);
    
    List<Transcription> findByMeetingSessionIdAndSpeakerId(UUID meetingSessionId, String speakerId);
    
    List<Transcription> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    List<Transcription> findByMeetingSessionIdAndTimestampBetween(UUID meetingSessionId, LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT t FROM Transcription t WHERE t.meetingSession.id = :meetingSessionId AND t.confidence >= :minConfidence ORDER BY t.segmentNumber")
    List<Transcription> findByMeetingSessionIdAndMinConfidence(@Param("meetingSessionId") UUID meetingSessionId, 
                                                              @Param("minConfidence") Double minConfidence);
    
    @Query("SELECT COUNT(t) FROM Transcription t WHERE t.meetingSession.id = :meetingSessionId")
    long countByMeetingSessionId(@Param("meetingSessionId") UUID meetingSessionId);
    
    @Query("SELECT DISTINCT t.speakerId FROM Transcription t WHERE t.meetingSession.id = :meetingSessionId AND t.speakerId IS NOT NULL")
    List<String> findDistinctSpeakersByMeetingSessionId(@Param("meetingSessionId") UUID meetingSessionId);
    
    @Query("SELECT t FROM Transcription t WHERE t.text LIKE %:keyword% ORDER BY t.timestamp")
    List<Transcription> findByTextContaining(@Param("keyword") String keyword);
    
    @Query("SELECT t FROM Transcription t WHERE t.meetingSession.id = :meetingSessionId AND t.text LIKE %:keyword% ORDER BY t.segmentNumber")
    List<Transcription> findByMeetingSessionIdAndTextContaining(@Param("meetingSessionId") UUID meetingSessionId, 
                                                               @Param("keyword") String keyword);
    
    @Query("SELECT MAX(t.segmentNumber) FROM Transcription t WHERE t.meetingSession.id = :meetingSessionId")
    Integer findMaxSegmentNumberByMeetingSessionId(@Param("meetingSessionId") UUID meetingSessionId);
    
    void deleteByMeetingSessionId(UUID meetingSessionId);
    
    boolean existsByMeetingSessionId(UUID meetingSessionId);
}