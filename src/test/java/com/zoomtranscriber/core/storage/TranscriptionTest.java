package com.zoomtranscriber.core.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Transcription entity.
 * Tests entity behavior, validation, and business logic.
 */
@DisplayName("Transcription Entity Tests")
class TranscriptionTest {
    
    private Transcription transcription;
    private MeetingSession meetingSession;
    private LocalDateTime testTimestamp;
    
    @BeforeEach
    void setUp() {
        transcription = new Transcription();
        meetingSession = new MeetingSession("Test Meeting");
        meetingSession.setId(UUID.randomUUID());
        testTimestamp = LocalDateTime.now();
    }
    
    @Test
    @DisplayName("Should create transcription with default constructor")
    void shouldCreateTranscriptionWithDefaultConstructor() {
        // When
        var transcription = new Transcription();
        
        // Then
        assertNotNull(transcription.getId());
        assertNull(transcription.getMeetingSession());
        assertNull(transcription.getTimestamp());
        assertNull(transcription.getSpeakerId());
        assertNull(transcription.getText());
        assertNull(transcription.getConfidence());
        assertNull(transcription.getSegmentNumber());
        assertNotNull(transcription.getCreatedAt());
    }
    
    @Test
    @DisplayName("Should set and get ID correctly")
    void shouldSetAndGetIdCorrectly() {
        // Given
        var id = UUID.randomUUID();
        
        // When
        transcription.setId(id);
        
        // Then
        assertEquals(id, transcription.getId());
    }
    
    @Test
    @DisplayName("Should set and get meeting session correctly")
    void shouldSetAndGetMeetingSessionCorrectly() {
        // When
        transcription.setMeetingSession(meetingSession);
        
        // Then
        assertEquals(meetingSession, transcription.getMeetingSession());
    }
    
    @Test
    @DisplayName("Should set and get timestamp correctly")
    void shouldSetAndGetTimestampCorrectly() {
        // When
        transcription.setTimestamp(testTimestamp);
        
        // Then
        assertEquals(testTimestamp, transcription.getTimestamp());
    }
    
    @Test
    @DisplayName("Should set and get speaker ID correctly")
    void shouldSetAndGetSpeakerIdCorrectly() {
        // Given
        var speakerId = "speaker-123";
        
        // When
        transcription.setSpeakerId(speakerId);
        
        // Then
        assertEquals(speakerId, transcription.getSpeakerId());
    }
    
    @Test
    @DisplayName("Should set and get text correctly")
    void shouldSetAndGetTextCorrectly() {
        // Given
        var text = "This is a test transcription segment.";
        
        // When
        transcription.setText(text);
        
        // Then
        assertEquals(text, transcription.getText());
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.5, 0.85, 0.95, 1.0})
    @DisplayName("Should set and get confidence correctly for valid values")
    void shouldSetAndGetConfidenceCorrectlyForValidValues(Double confidence) {
        // When
        transcription.setConfidence(confidence);
        
        // Then
        assertEquals(confidence, transcription.getConfidence());
    }
    
    @Test
    @DisplayName("Should set and get segment number correctly")
    void shouldSetAndGetSegmentNumberCorrectly() {
        // Given
        var segmentNumber = 5;
        
        // When
        transcription.setSegmentNumber(segmentNumber);
        
        // Then
        assertEquals(segmentNumber, transcription.getSegmentNumber());
    }
    
    @Test
    @DisplayName("Should set and get created at timestamp correctly")
    void shouldSetAndGetCreatedAtCorrectly() {
        // Given
        var createdAt = LocalDateTime.of(2023, 1, 1, 12, 0, 0);
        
        // When
        transcription.setCreatedAt(createdAt);
        
        // Then
        assertEquals(createdAt, transcription.getCreatedAt());
    }
    
    @Test
    @DisplayName("Should handle null meeting session")
    void shouldHandleNullMeetingSession() {
        // When
        transcription.setMeetingSession(null);
        
        // Then
        assertNull(transcription.getMeetingSession());
    }
    
    @Test
    @DisplayName("Should handle null speaker ID")
    void shouldHandleNullSpeakerId() {
        // When
        transcription.setSpeakerId(null);
        
        // Then
        assertNull(transcription.getSpeakerId());
    }
    
    @Test
    @DisplayName("Should handle null text")
    void shouldHandleNullText() {
        // When
        transcription.setText(null);
        
        // Then
        assertNull(transcription.getText());
    }
    
    @Test
    @DisplayName("Should handle null confidence")
    void shouldHandleNullConfidence() {
        // When
        transcription.setConfidence(null);
        
        // Then
        assertNull(transcription.getConfidence());
    }
    
    @Test
    @DisplayName("Should handle null segment number")
    void shouldHandleNullSegmentNumber() {
        // When
        transcription.setSegmentNumber(null);
        
        // Then
        assertNull(transcription.getSegmentNumber());
    }
    
    @Test
    @DisplayName("Should handle empty text")
    void shouldHandleEmptyText() {
        // When
        transcription.setText("");
        
        // Then
        assertEquals("", transcription.getText());
    }
    
    @Test
    @DisplayName("Should handle empty speaker ID")
    void shouldHandleEmptySpeakerId() {
        // When
        transcription.setSpeakerId("");
        
        // Then
        assertEquals("", transcription.getSpeakerId());
    }
    
    @Test
    @DisplayName("Should handle long text content")
    void shouldHandleLongTextContent() {
        // Given
        var longText = "A".repeat(5000); // 5000 characters
        
        // When
        transcription.setText(longText);
        
        // Then
        assertEquals(longText, transcription.getText());
    }
    
    @Test
    @DisplayName("Should handle maximum allowed text length")
    void shouldHandleMaximumAllowedTextLength() {
        // Given
        var maxText = "A".repeat(10000); // Maximum allowed length
        
        // When
        transcription.setText(maxText);
        
        // Then
        assertEquals(maxText, transcription.getText());
    }
    
    @Test
    @DisplayName("Should handle negative segment numbers")
    void shouldHandleNegativeSegmentNumbers() {
        // Given
        var negativeSegmentNumber = -1;
        
        // When
        transcription.setSegmentNumber(negativeSegmentNumber);
        
        // Then
        assertEquals(negativeSegmentNumber, transcription.getSegmentNumber());
    }
    
    @Test
    @DisplayName("Should handle zero segment number")
    void shouldHandleZeroSegmentNumber() {
        // When
        transcription.setSegmentNumber(0);
        
        // Then
        assertEquals(0, transcription.getSegmentNumber());
    }
    
    @Test
    @DisplayName("Should handle large segment numbers")
    void shouldHandleLargeSegmentNumbers() {
        // Given
        var largeSegmentNumber = 10000;
        
        // When
        transcription.setSegmentNumber(largeSegmentNumber);
        
        // Then
        assertEquals(largeSegmentNumber, transcription.getSegmentNumber());
    }
    
    @Test
    @DisplayName("Should handle confidence values outside 0-1 range")
    void shouldHandleConfidenceValuesOutside01Range() {
        // Given
        var negativeConfidence = -0.5;
        var highConfidence = 1.5;
        
        // When
        transcription.setConfidence(negativeConfidence);
        
        // Then
        assertEquals(negativeConfidence, transcription.getConfidence());
        
        // When
        transcription.setConfidence(highConfidence);
        
        // Then
        assertEquals(highConfidence, transcription.getConfidence());
    }
    
    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
        // Given
        var id = UUID.randomUUID();
        var transcription1 = new Transcription();
        var transcription2 = new Transcription();
        var transcription3 = new Transcription();
        
        transcription1.setId(id);
        transcription2.setId(id);
        transcription3.setId(UUID.randomUUID());
        
        // Set same properties for transcription1 and transcription2
        transcription1.setMeetingSession(meetingSession);
        transcription2.setMeetingSession(meetingSession);
        transcription1.setTimestamp(testTimestamp);
        transcription2.setTimestamp(testTimestamp);
        transcription1.setText("Test text");
        transcription2.setText("Test text");
        transcription1.setConfidence(0.95);
        transcription2.setConfidence(0.95);
        transcription1.setSegmentNumber(1);
        transcription2.setSegmentNumber(1);
        
        // Then
        assertEquals(transcription1, transcription2);
        assertNotEquals(transcription1, transcription3);
        assertEquals(transcription1, transcription1); // Same object
        assertNotEquals(transcription1, null);
        assertNotEquals(transcription1, "string");
    }
    
    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
        // Given
        var transcription1 = new Transcription();
        var transcription2 = new Transcription();
        
        // Set same properties
        transcription1.setId(UUID.randomUUID());
        transcription2.setId(transcription1.getId());
        transcription1.setMeetingSession(meetingSession);
        transcription2.setMeetingSession(meetingSession);
        transcription1.setTimestamp(testTimestamp);
        transcription2.setTimestamp(testTimestamp);
        transcription1.setText("Test text");
        transcription2.setText("Test text");
        transcription1.setConfidence(0.95);
        transcription2.setConfidence(0.95);
        transcription1.setSegmentNumber(1);
        transcription2.setSegmentNumber(1);
        
        // Then
        assertEquals(transcription1.hashCode(), transcription2.hashCode());
    }
    
    @Test
    @DisplayName("Should implement toString correctly")
    void shouldImplementToStringCorrectly() {
        // Given
        var id = UUID.randomUUID();
        var text = "Test transcription";
        var speakerId = "speaker-1";
        var confidence = 0.95;
        var segmentNumber = 1;
        
        transcription.setId(id);
        transcription.setMeetingSession(meetingSession);
        transcription.setTimestamp(testTimestamp);
        transcription.setSpeakerId(speakerId);
        transcription.setText(text);
        transcription.setConfidence(confidence);
        transcription.setSegmentNumber(segmentNumber);
        
        // When
        var result = transcription.toString();
        
        // Then
        assertTrue(result.contains("id=" + id));
        assertTrue(result.contains("meetingSessionId=" + meetingSession.getId()));
        assertTrue(result.contains("timestamp=" + testTimestamp));
        assertTrue(result.contains("speakerId='" + speakerId + "'"));
        assertTrue(result.contains("text='" + text + "'"));
        assertTrue(result.contains("confidence=" + confidence));
        assertTrue(result.contains("segmentNumber=" + segmentNumber));
    }
    
    @Test
    @DisplayName("Should handle transcription with all fields set")
    void shouldHandleTranscriptionWithAllFieldsSet() {
        // Given
        var id = UUID.randomUUID();
        var speakerId = "speaker-123";
        var text = "Complete transcription text";
        var confidence = 0.87;
        var segmentNumber = 5;
        var createdAt = LocalDateTime.of(2023, 6, 15, 14, 30, 0);
        
        // When
        transcription.setId(id);
        transcription.setMeetingSession(meetingSession);
        transcription.setTimestamp(testTimestamp);
        transcription.setSpeakerId(speakerId);
        transcription.setText(text);
        transcription.setConfidence(confidence);
        transcription.setSegmentNumber(segmentNumber);
        transcription.setCreatedAt(createdAt);
        
        // Then
        assertEquals(id, transcription.getId());
        assertEquals(meetingSession, transcription.getMeetingSession());
        assertEquals(testTimestamp, transcription.getTimestamp());
        assertEquals(speakerId, transcription.getSpeakerId());
        assertEquals(text, transcription.getText());
        assertEquals(confidence, transcription.getConfidence());
        assertEquals(segmentNumber, transcription.getSegmentNumber());
        assertEquals(createdAt, transcription.getCreatedAt());
    }
    
    @Test
    @DisplayName("Should handle special characters in text")
    void shouldHandleSpecialCharactersInText() {
        // Given
        var textWithSpecialChars = "Hello! @#$%^&*()_+-={}[]|\\:;\"'<>?,./";
        
        // When
        transcription.setText(textWithSpecialChars);
        
        // Then
        assertEquals(textWithSpecialChars, transcription.getText());
    }
    
    @Test
    @DisplayName("Should handle unicode characters in text")
    void shouldHandleUnicodeCharactersInText() {
        // Given
        var unicodeText = "Hello 世界 🌍 ñáéíóú";
        
        // When
        transcription.setText(unicodeText);
        
        // Then
        assertEquals(unicodeText, transcription.getText());
    }
    
    @Test
    @DisplayName("Should handle time-based operations correctly")
    void shouldHandleTimeBasedOperationsCorrectly() {
        // Given
        var before = LocalDateTime.now();
        
        // When
        var transcription = new Transcription();
        var after = LocalDateTime.now();
        
        // Then
        assertTrue(transcription.getCreatedAt().isAfter(before) || transcription.getCreatedAt().isEqual(before));
        assertTrue(transcription.getCreatedAt().isBefore(after) || transcription.getCreatedAt().isEqual(after));
    }
    
    @Test
    @DisplayName("Should handle different speaker ID formats")
    void shouldHandleDifferentSpeakerIdFormats() {
        // Given
        var numericSpeakerId = "123";
        var alphanumericSpeakerId = "speaker-abc-123";
        var emailSpeakerId = "user@example.com";
        var uuidSpeakerId = UUID.randomUUID().toString();
        
        // When & Then
        transcription.setSpeakerId(numericSpeakerId);
        assertEquals(numericSpeakerId, transcription.getSpeakerId());
        
        transcription.setSpeakerId(alphanumericSpeakerId);
        assertEquals(alphanumericSpeakerId, transcription.getSpeakerId());
        
        transcription.setSpeakerId(emailSpeakerId);
        assertEquals(emailSpeakerId, transcription.getSpeakerId());
        
        transcription.setSpeakerId(uuidSpeakerId);
        assertEquals(uuidSpeakerId, transcription.getSpeakerId());
    }
}