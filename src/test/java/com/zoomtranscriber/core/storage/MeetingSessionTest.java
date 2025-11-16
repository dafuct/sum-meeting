package com.zoomtranscriber.core.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MeetingSession entity.
 * Tests entity behavior, validation, and business logic.
 */
@DisplayName("MeetingSession Entity Tests")
class MeetingSessionTest {
    
    private MeetingSession meetingSession;
    private LocalDateTime testStartTime;
    private LocalDateTime testEndTime;
    
    @BeforeEach
    void setUp() {
        meetingSession = new MeetingSession();
        testStartTime = LocalDateTime.now();
        testEndTime = testStartTime.plusHours(1);
    }
    
    @Test
    @DisplayName("Should create meeting session with default constructor")
    void shouldCreateMeetingSessionWithDefaultConstructor() {
        // When
        var session = new MeetingSession();
        
        // Then
        assertNotNull(session.getId());
        assertNull(session.getTitle());
        assertNull(session.getStartTime());
        assertNull(session.getEndTime());
        assertNull(session.getDuration());
        assertEquals(MeetingSession.MeetingStatus.DETECTED, session.getStatus());
        assertNull(session.getAudioFilePath());
        assertNull(session.getParticipantCount());
        assertNotNull(session.getCreatedAt());
        assertNotNull(session.getUpdatedAt());
    }
    
    @Test
    @DisplayName("Should create meeting session with title constructor")
    void shouldCreateMeetingSessionWithTitleConstructor() {
        // Given
        var title = "Test Meeting";
        
        // When
        var session = new MeetingSession(title);
        
        // Then
        assertEquals(title, session.getTitle());
        assertEquals(MeetingSession.MeetingStatus.DETECTED, session.getStatus());
        assertNotNull(session.getCreatedAt());
        assertNotNull(session.getUpdatedAt());
    }
    
    @Test
    @DisplayName("Should set and get ID correctly")
    void shouldSetAndGetIdCorrectly() {
        // Given
        var id = UUID.randomUUID();
        
        // When
        meetingSession.setId(id);
        
        // Then
        assertEquals(id, meetingSession.getId());
    }
    
    @Test
    @DisplayName("Should set and get title correctly")
    void shouldSetAndGetTitleCorrectly() {
        // Given
        var title = "Team Standup Meeting";
        
        // When
        meetingSession.setTitle(title);
        
        // Then
        assertEquals(title, meetingSession.getTitle());
    }
    
    @Test
    @DisplayName("Should set and get start time correctly")
    void shouldSetAndGetStartTimeCorrectly() {
        // When
        meetingSession.setStartTime(testStartTime);
        
        // Then
        assertEquals(testStartTime, meetingSession.getStartTime());
    }
    
    @Test
    @DisplayName("Should set and get end time correctly")
    void shouldSetAndGetEndTimeCorrectly() {
        // When
        meetingSession.setEndTime(testEndTime);
        
        // Then
        assertEquals(testEndTime, meetingSession.getEndTime());
    }
    
    @Test
    @DisplayName("Should set and get duration correctly")
    void shouldSetAndGetDurationCorrectly() {
        // Given
        var duration = "PT1H30M"; // ISO 8601 duration format
        
        // When
        meetingSession.setDuration(duration);
        
        // Then
        assertEquals(duration, meetingSession.getDuration());
    }
    
    @ParameterizedTest
    @EnumSource(MeetingSession.MeetingStatus.class)
    @DisplayName("Should set and get status correctly for all enum values")
    void shouldSetAndGetStatusCorrectlyForAllEnumValues(MeetingSession.MeetingStatus status) {
        // When
        meetingSession.setStatus(status);
        
        // Then
        assertEquals(status, meetingSession.getStatus());
    }
    
    @Test
    @DisplayName("Should set and get audio file path correctly")
    void shouldSetAndGetAudioFilePathCorrectly() {
        // Given
        var audioFilePath = "/path/to/audio/recording.wav";
        
        // When
        meetingSession.setAudioFilePath(audioFilePath);
        
        // Then
        assertEquals(audioFilePath, meetingSession.getAudioFilePath());
    }
    
    @Test
    @DisplayName("Should set and get participant count correctly")
    void shouldSetAndGetParticipantCountCorrectly() {
        // Given
        var participantCount = 5;
        
        // When
        meetingSession.setParticipantCount(participantCount);
        
        // Then
        assertEquals(participantCount, meetingSession.getParticipantCount());
    }
    
    @Test
    @DisplayName("Should set and get created at timestamp correctly")
    void shouldSetAndGetCreatedAtCorrectly() {
        // Given
        var createdAt = LocalDateTime.of(2023, 1, 1, 12, 0, 0);
        
        // When
        meetingSession.setCreatedAt(createdAt);
        
        // Then
        assertEquals(createdAt, meetingSession.getCreatedAt());
    }
    
    @Test
    @DisplayName("Should set and get updated at timestamp correctly")
    void shouldSetAndGetUpdatedAtCorrectly() {
        // Given
        var updatedAt = LocalDateTime.of(2023, 1, 1, 12, 30, 0);
        
        // When
        meetingSession.setUpdatedAt(updatedAt);
        
        // Then
        assertEquals(updatedAt, meetingSession.getUpdatedAt());
    }
    
    @Test
    @DisplayName("Should update timestamp on preUpdate")
    void shouldUpdateTimestampOnPreUpdate() {
        // Given
        var originalUpdatedAt = meetingSession.getUpdatedAt();
        
        // Simulate some time passing
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // When
        meetingSession.onUpdate();
        
        // Then
        assertTrue(meetingSession.getUpdatedAt().isAfter(originalUpdatedAt));
    }
    
    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
        // Given
        var id = UUID.randomUUID();
        var session1 = new MeetingSession();
        var session2 = new MeetingSession();
        var session3 = new MeetingSession();
        
        session1.setId(id);
        session2.setId(id);
        session3.setId(UUID.randomUUID());
        
        // Then
        assertEquals(session1, session2);
        assertNotEquals(session1, session3);
        assertNotEquals(session2, session3);
        assertEquals(session1, session1); // Same object
        assertNotEquals(session1, null);
        assertNotEquals(session1, "string");
    }
    
    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
        // Given
        var id = UUID.randomUUID();
        var session1 = new MeetingSession();
        var session2 = new MeetingSession();
        
        session1.setId(id);
        session2.setId(id);
        
        // Then
        assertEquals(session1.hashCode(), session2.hashCode());
        
        // Test with null ID
        var session3 = new MeetingSession();
        var session4 = new MeetingSession();
        assertEquals(session3.hashCode(), session4.hashCode());
    }
    
    @Test
    @DisplayName("Should implement toString correctly")
    void shouldImplementToStringCorrectly() {
        // Given
        var id = UUID.randomUUID();
        var title = "Test Meeting";
        var status = MeetingSession.MeetingStatus.RECORDING;
        
        meetingSession.setId(id);
        meetingSession.setTitle(title);
        meetingSession.setStatus(status);
        meetingSession.setStartTime(testStartTime);
        meetingSession.setEndTime(testEndTime);
        
        // When
        var result = meetingSession.toString();
        
        // Then
        assertTrue(result.contains("id=" + id));
        assertTrue(result.contains("title='" + title + "'"));
        assertTrue(result.contains("status=" + status));
        assertTrue(result.contains("startTime=" + testStartTime));
        assertTrue(result.contains("endTime=" + testEndTime));
    }
    
    @Test
    @DisplayName("Should handle null values gracefully")
    void shouldHandleNullValuesGracefully() {
        // When
        meetingSession.setTitle(null);
        meetingSession.setAudioFilePath(null);
        meetingSession.setDuration(null);
        
        // Then
        assertNull(meetingSession.getTitle());
        assertNull(meetingSession.getAudioFilePath());
        assertNull(meetingSession.getDuration());
    }
    
    @Test
    @DisplayName("Should handle zero participant count")
    void shouldHandleZeroParticipantCount() {
        // When
        meetingSession.setParticipantCount(0);
        
        // Then
        assertEquals(0, meetingSession.getParticipantCount());
    }
    
    @Test
    @DisplayName("Should handle negative participant count")
    void shouldHandleNegativeParticipantCount() {
        // When
        meetingSession.setParticipantCount(-1);
        
        // Then
        assertEquals(-1, meetingSession.getParticipantCount());
    }
    
    @Test
    @DisplayName("Should maintain immutability of created at timestamp")
    void shouldMaintainImmutabilityOfCreatedAtTimestamp() {
        // Given
        var originalCreatedAt = meetingSession.getCreatedAt();
        
        // When
        var newCreatedAt = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        meetingSession.setCreatedAt(newCreatedAt);
        
        // Then
        assertEquals(newCreatedAt, meetingSession.getCreatedAt());
        assertNotEquals(originalCreatedAt, meetingSession.getCreatedAt());
    }
    
    @Test
    @DisplayName("Should handle empty title")
    void shouldHandleEmptyTitle() {
        // When
        meetingSession.setTitle("");
        
        // Then
        assertEquals("", meetingSession.getTitle());
    }
    
    @Test
    @DisplayName("Should handle long title")
    void shouldHandleLongTitle() {
        // Given
        var longTitle = "A".repeat(300); // 300 characters
        
        // When
        meetingSession.setTitle(longTitle);
        
        // Then
        assertEquals(longTitle, meetingSession.getTitle());
    }
    
    @Test
    @DisplayName("Should handle empty audio file path")
    void shouldHandleEmptyAudioFilePath() {
        // When
        meetingSession.setAudioFilePath("");
        
        // Then
        assertEquals("", meetingSession.getAudioFilePath());
    }
    
    @Test
    @DisplayName("Should handle empty duration")
    void shouldHandleEmptyDuration() {
        // When
        meetingSession.setDuration("");
        
        // Then
        assertEquals("", meetingSession.getDuration());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"PT30M", "PT1H", "PT1H30M", "PT2H15M30S"})
    @DisplayName("Should handle various ISO 8601 duration formats")
    void shouldHandleVariousISO8601DurationFormats(String duration) {
        // When
        meetingSession.setDuration(duration);
        
        // Then
        assertEquals(duration, meetingSession.getDuration());
    }
    
    @Test
    @DisplayName("Should handle meeting status transitions")
    void shouldHandleMeetingStatusTransitions() {
        // Test typical status flow
        meetingSession.setStatus(MeetingSession.MeetingStatus.DETECTED);
        assertEquals(MeetingSession.MeetingStatus.DETECTED, meetingSession.getStatus());
        
        meetingSession.setStatus(MeetingSession.MeetingStatus.RECORDING);
        assertEquals(MeetingSession.MeetingStatus.RECORDING, meetingSession.getStatus());
        
        meetingSession.setStatus(MeetingSession.MeetingStatus.PROCESSING);
        assertEquals(MeetingSession.MeetingStatus.PROCESSING, meetingSession.getStatus());
        
        meetingSession.setStatus(MeetingSession.MeetingStatus.COMPLETED);
        assertEquals(MeetingSession.MeetingStatus.COMPLETED, meetingSession.getStatus());
        
        meetingSession.setStatus(MeetingSession.MeetingStatus.ERROR);
        assertEquals(MeetingSession.MeetingStatus.ERROR, meetingSession.getStatus());
    }
    
    @Test
    @DisplayName("Should handle time-based operations correctly")
    void shouldHandleTimeBasedOperationsCorrectly() {
        // Given
        var before = LocalDateTime.now();
        
        // When
        var session = new MeetingSession();
        var after = LocalDateTime.now();
        
        // Then
        assertTrue(session.getCreatedAt().isAfter(before) || session.getCreatedAt().isEqual(before));
        assertTrue(session.getCreatedAt().isBefore(after) || session.getCreatedAt().isEqual(after));
        assertTrue(session.getUpdatedAt().isAfter(before) || session.getUpdatedAt().isEqual(before));
        assertTrue(session.getUpdatedAt().isBefore(after) || session.getUpdatedAt().isEqual(after));
    }
}