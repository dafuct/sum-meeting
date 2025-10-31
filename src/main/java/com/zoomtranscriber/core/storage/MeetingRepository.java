package com.zoomtranscriber.core.storage;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingRepository extends JpaRepository<MeetingSession, UUID> {
    
    List<MeetingSession> findByStatus(MeetingSession.MeetingStatus status);
    
    Page<MeetingSession> findByStatus(MeetingSession.MeetingStatus status, Pageable pageable);
    
    List<MeetingSession> findByStartTimeBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    Page<MeetingSession> findByStartTimeBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    @Query("SELECT m FROM MeetingSession m WHERE " +
           "(:status IS NULL OR m.status = :status) AND " +
           "(:startDate IS NULL OR m.startTime >= :startDate) AND " +
           "(:endDate IS NULL OR m.startTime <= :endDate)")
    Page<MeetingSession> findByFilters(@Param("status") MeetingSession.MeetingStatus status,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate,
                                     Pageable pageable);
    
    Optional<MeetingSession> findByTitleContainingIgnoreCase(String title);
    
    List<MeetingSession> findByAudioFilePathIsNotNull();
    
    @Query("SELECT COUNT(m) FROM MeetingSession m WHERE m.status = :status")
    long countByStatus(@Param("status") MeetingSession.MeetingStatus status);
    
    @Query("SELECT m FROM MeetingSession m WHERE m.status IN :statuses")
    List<MeetingSession> findByStatusIn(@Param("statuses") List<MeetingSession.MeetingStatus> statuses);
    
    @Query("SELECT m FROM MeetingSession m WHERE m.startTime >= :since ORDER BY m.startTime DESC")
    List<MeetingSession> findRecentMeetings(@Param("since") LocalDateTime since);
    
    boolean existsByTitle(String title);
    
    @Query("SELECT DISTINCT m.status FROM MeetingSession m")
    List<MeetingSession.MeetingStatus> findAllStatuses();
}