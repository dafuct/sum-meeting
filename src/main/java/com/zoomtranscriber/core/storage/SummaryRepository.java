package com.zoomtranscriber.core.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, UUID> {
    
    List<Summary> findByMeetingSessionId(UUID meetingSessionId);
    
    List<Summary> findByMeetingSessionIdOrderByCreatedAt(UUID meetingSessionId);
    
    List<Summary> findBySummaryType(Summary.SummaryType summaryType);
    
    List<Summary> findByMeetingSessionIdAndSummaryType(UUID meetingSessionId, Summary.SummaryType summaryType);
    
    List<Summary> findByModelUsed(String modelUsed);
    
    List<Summary> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    List<Summary> findByMeetingSessionIdAndCreatedAtBetween(UUID meetingSessionId, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT s FROM Summary s WHERE s.meetingSession.id = :meetingSessionId AND s.summaryType IN :summaryTypes ORDER BY s.createdAt")
    List<Summary> findByMeetingSessionIdAndSummaryTypes(@Param("meetingSessionId") UUID meetingSessionId, 
                                                      @Param("summaryTypes") List<Summary.SummaryType> summaryTypes);
    
    @Query("SELECT COUNT(s) FROM Summary s WHERE s.meetingSession.id = :meetingSessionId")
    long countByMeetingSessionId(@Param("meetingSessionId") UUID meetingSessionId);
    
    @Query("SELECT COUNT(s) FROM Summary s WHERE s.summaryType = :summaryType")
    long countBySummaryType(@Param("summaryType") Summary.SummaryType summaryType);
    
    @Query("SELECT DISTINCT s.modelUsed FROM Summary s WHERE s.modelUsed IS NOT NULL")
    List<String> findAllDistinctModels();
    
    @Query("SELECT s FROM Summary s WHERE s.content LIKE %:keyword% ORDER BY s.createdAt DESC")
    List<Summary> findByContentContaining(@Param("keyword") String keyword);
    
    @Query("SELECT s FROM Summary s WHERE s.meetingSession.id = :meetingSessionId AND s.content LIKE %:keyword% ORDER BY s.createdAt DESC")
    List<Summary> findByMeetingSessionIdAndContentContaining(@Param("meetingSessionId") UUID meetingSessionId, 
                                                           @Param("keyword") String keyword);
    
    @Query("SELECT s.summaryType, COUNT(s) FROM Summary s GROUP BY s.summaryType")
    List<Object[]> countBySummaryTypeGrouped();
    
    @Query("SELECT s.modelUsed, COUNT(s) FROM Summary s WHERE s.modelUsed IS NOT NULL GROUP BY s.modelUsed")
    List<Object[]> countByModelUsedGrouped();
    
    void deleteByMeetingSessionId(UUID meetingSessionId);
    
    boolean existsByMeetingSessionIdAndSummaryType(UUID meetingSessionId, Summary.SummaryType summaryType);
    
    @Query("SELECT s FROM Summary s WHERE s.meetingSession.id = :meetingSessionId ORDER BY s.createdAt DESC")
    Summary findLatestByMeetingSessionId(@Param("meetingSessionId") UUID meetingSessionId);
}