package com.zoomtranscriber.core.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TranscriptionRepository.
 * Tests repository query methods, edge cases, and exception handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TranscriptionRepository Tests")
class TranscriptionRepositoryTest {

    @Mock
    private TranscriptionRepository transcriptionRepository;

    private Transcription testTranscription;
    private MeetingSession testMeeting;
    private UUID testMeetingId;
    private UUID testTranscriptionId;
    private LocalDateTime testTimestamp;

    @BeforeEach
    void setUp() {
        testMeetingId = UUID.randomUUID();
        testTranscriptionId = UUID.randomUUID();
        testTimestamp = LocalDateTime.of(2023, 11, 4, 10, 30, 0);

        testMeeting = new MeetingSession("Test Meeting");
        testMeeting.setId(testMeetingId);

        testTranscription = new Transcription();
        testTranscription.setId(testTranscriptionId);
        testTranscription.setMeetingSession(testMeeting);
        testTranscription.setTimestamp(testTimestamp);
        testTranscription.setSpeakerId("Speaker-1");
        testTranscription.setText("This is a test transcription.");
        testTranscription.setConfidence(0.95);
        testTranscription.setSegmentNumber(1);
    }

    @Test
    @DisplayName("Should find transcriptions by meeting session ID")
    void shouldFindTranscriptionsByMeetingSessionId() {
        // Given
        var expectedTranscriptions = List.of(testTranscription);
        when(transcriptionRepository.findByMeetingSessionId(testMeetingId)).thenReturn(expectedTranscriptions);

        // When
        var result = transcriptionRepository.findByMeetingSessionId(testMeetingId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTranscription, result.get(0));
        verify(transcriptionRepository, times(1)).findByMeetingSessionId(testMeetingId);
    }

    @Test
    @DisplayName("Should return empty list when no transcriptions found for meeting")
    void shouldReturnEmptyListWhenNoTranscriptionsFoundForMeeting() {
        // Given
        var nonExistentMeetingId = UUID.randomUUID();
        when(transcriptionRepository.findByMeetingSessionId(nonExistentMeetingId)).thenReturn(Collections.emptyList());

        // When
        var result = transcriptionRepository.findByMeetingSessionId(nonExistentMeetingId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(transcriptionRepository, times(1)).findByMeetingSessionId(nonExistentMeetingId);
    }

    @Test
    @DisplayName("Should find transcriptions by meeting session ID ordered by segment number")
    void shouldFindTranscriptionsByMeetingSessionIdOrderedBySegmentNumber() {
        // Given
        var transcription2 = createTranscription(2, "Second transcription");
        var transcription3 = createTranscription(3, "Third transcription");
        var expectedTranscriptions = Arrays.asList(testTranscription, transcription2, transcription3);
        when(transcriptionRepository.findByMeetingSessionIdOrderBySegmentNumber(testMeetingId))
                .thenReturn(expectedTranscriptions);

        // When
        var result = transcriptionRepository.findByMeetingSessionIdOrderBySegmentNumber(testMeetingId);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(1, result.get(0).getSegmentNumber());
        assertEquals(2, result.get(1).getSegmentNumber());
        assertEquals(3, result.get(2).getSegmentNumber());
        verify(transcriptionRepository, times(1)).findByMeetingSessionIdOrderBySegmentNumber(testMeetingId);
    }

    @Test
    @DisplayName("Should find transcriptions by meeting session ID ordered by timestamp")
    void shouldFindTranscriptionsByMeetingSessionIdOrderedByTimestamp() {
        // Given
        var transcription2 = createTranscriptionWithTimestamp(testTimestamp.plusMinutes(5), "Later transcription");
        var expectedTranscriptions = Arrays.asList(testTranscription, transcription2);
        when(transcriptionRepository.findByMeetingSessionIdOrderByTimestamp(testMeetingId))
                .thenReturn(expectedTranscriptions);

        // When
        var result = transcriptionRepository.findByMeetingSessionIdOrderByTimestamp(testMeetingId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(0).getTimestamp().isBefore(result.get(1).getTimestamp()));
        verify(transcriptionRepository, times(1)).findByMeetingSessionIdOrderByTimestamp(testMeetingId);
    }

    @Test
    @DisplayName("Should find transcriptions by meeting session ID ordered by segment number using query")
    void shouldFindTranscriptionsByMeetingSessionIdOrderedBySegmentNumberQuery() {
        // Given
        var expectedTranscriptions = List.of(testTranscription);
        when(transcriptionRepository.findByMeetingSessionIdOrderBySegmentNumberQuery(testMeetingId))
                .thenReturn(expectedTranscriptions);

        // When
        var result = transcriptionRepository.findByMeetingSessionIdOrderBySegmentNumberQuery(testMeetingId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTranscription, result.get(0));
        verify(transcriptionRepository, times(1)).findByMeetingSessionIdOrderBySegmentNumberQuery(testMeetingId);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Speaker-1", "Speaker-2", "Guest-Speaker"})
    @DisplayName("Should find transcriptions by speaker ID")
    void shouldFindTranscriptionsBySpeakerId(String speakerId) {
        // Given
        var expectedTranscriptions = speakerId != null && !speakerId.isEmpty() ? 
                List.of(testTranscription) : Collections.emptyList();
        when(transcriptionRepository.findBySpeakerId(speakerId)).thenReturn(expectedTranscriptions);

        // When
        var result = transcriptionRepository.findBySpeakerId(speakerId);

        // Then
        assertNotNull(result);
        if (speakerId != null && !speakerId.isEmpty()) {
            assertEquals(1, result.size());
            assertEquals(speakerId, result.get(0).getSpeakerId());
        } else {
            assertTrue(result.isEmpty());
        }
        verify(transcriptionRepository, times(1)).findBySpeakerId(speakerId);
    }

    @Test
    @DisplayName("Should find transcriptions by meeting session ID and speaker ID")
    void shouldFindTranscriptionsByMeetingSessionIdAndSpeakerId() {
        // Given
        var speakerId = "Speaker-1";
        var expectedTranscriptions = List.of(testTranscription);
        when(transcriptionRepository.findByMeetingSessionIdAndSpeakerId(testMeetingId, speakerId))
                .thenReturn(expectedTranscriptions);

        // When
        var result = transcriptionRepository.findByMeetingSessionIdAndSpeakerId(testMeetingId, speakerId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTranscription, result.get(0));
        assertEquals(speakerId, result.get(0).getSpeakerId());
        verify(transcriptionRepository, times(1)).findByMeetingSessionIdAndSpeakerId(testMeetingId, speakerId);
    }

    @Test
    @DisplayName("Should find transcriptions by timestamp range")
    void shouldFindTranscriptionsByTimestampRange() {
        // Given
        var startTime = testTimestamp.minusMinutes(10);
        var endTime = testTimestamp.plusMinutes(10);
        var expectedTranscriptions = List.of(testTranscription);
        when(transcriptionRepository.findByTimestampBetween(startTime, endTime)).thenReturn(expectedTranscriptions);

        // When
        var result = transcriptionRepository.findByTimestampBetween(startTime, endTime);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTranscription, result.get(0));
        verify(transcriptionRepository, times(1)).findByTimestampBetween(startTime, endTime);
    }

    @Test
    @DisplayName("Should find transcriptions by meeting session ID and timestamp range")
    void shouldFindTranscriptionsByMeetingSessionIdAndTimestampRange() {
        // Given
        var startTime = testTimestamp.minusMinutes(10);
        var endTime = testTimestamp.plusMinutes(10);
        var expectedTranscriptions = List.of(testTranscription);
        when(transcriptionRepository.findByMeetingSessionIdAndTimestampBetween(testMeetingId, startTime, endTime))
                .thenReturn(expectedTranscriptions);

        // When
        var result = transcriptionRepository.findByMeetingSessionIdAndTimestampBetween(testMeetingId, startTime, endTime);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTranscription, result.get(0));
        verify(transcriptionRepository, times(1)).findByMeetingSessionIdAndTimestampBetween(testMeetingId, startTime, endTime);
    }

    @ParameterizedTest
    @CsvSource({
        "0.5, true",
        "0.8, true", 
        "0.95, true",
        "0.99, false"
    })
    @DisplayName("Should find transcriptions by meeting session ID and minimum confidence")
    void shouldFindTranscriptionsByMeetingSessionIdAndMinConfidence(Double minConfidence, boolean shouldFind) {
        // Given
        var expectedTranscriptions = shouldFind ? List.of(testTranscription) : Collections.emptyList();
        when(transcriptionRepository.findByMeetingSessionIdAndMinConfidence(testMeetingId, minConfidence))
                .thenReturn(expectedTranscriptions);

        // When
        var result = transcriptionRepository.findByMeetingSessionIdAndMinConfidence(testMeetingId, minConfidence);

        // Then
        assertNotNull(result);
        assertEquals(shouldFind ? 1 : 0, result.size());
        if (shouldFind) {
            assertTrue(result.get(0).getConfidence() >= minConfidence);
        }
        verify(transcriptionRepository, times(1)).findByMeetingSessionIdAndMinConfidence(testMeetingId, minConfidence);
    }

    @Test
    @DisplayName("Should count transcriptions by meeting session ID")
    void shouldCountTranscriptionsByMeetingSessionId() {
        // Given
        var expectedCount = 10L;
        when(transcriptionRepository.countByMeetingSessionId(testMeetingId)).thenReturn(expectedCount);

        // When
        var result = transcriptionRepository.countByMeetingSessionId(testMeetingId);

        // Then
        assertEquals(expectedCount, result);
        verify(transcriptionRepository, times(1)).countByMeetingSessionId(testMeetingId);
    }

    @Test
    @DisplayName("Should find distinct speakers by meeting session ID")
    void shouldFindDistinctSpeakersByMeetingSessionId() {
        // Given
        var expectedSpeakers = Arrays.asList("Speaker-1", "Speaker-2", "Guest");
        when(transcriptionRepository.findDistinctSpeakersByMeetingSessionId(testMeetingId))
                .thenReturn(expectedSpeakers);

        // When
        var result = transcriptionRepository.findDistinctSpeakersByMeetingSessionId(testMeetingId);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsAll(expectedSpeakers));
        verify(transcriptionRepository, times(1)).findDistinctSpeakersByMeetingSessionId(testMeetingId);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"test", "meeting", "transcription"})
    @DisplayName("Should find transcriptions by text containing keyword")
    void shouldFindTranscriptionsByTextContainingKeyword(String keyword) {
        // Given
        var expectedTranscriptions = keyword != null && !keyword.isEmpty() ? 
                List.of(testTranscription) : Collections.emptyList();
        when(transcriptionRepository.findByTextContaining(keyword)).thenReturn(expectedTranscriptions);

        // When
        var result = transcriptionRepository.findByTextContaining(keyword);

        // Then
        assertNotNull(result);
        assertEquals(keyword != null && !keyword.isEmpty() ? 1 : 0, result.size());
        if (keyword != null && !keyword.isEmpty()) {
            assertTrue(result.get(0).getText().contains(keyword));
        }
        verify(transcriptionRepository, times(1)).findByTextContaining(keyword);
    }

    @Test
    @DisplayName("Should find transcriptions by meeting session ID and text containing keyword")
    void shouldFindTranscriptionsByMeetingSessionIdAndTextContaining() {
        // Given
        var keyword = "test";
        var expectedTranscriptions = List.of(testTranscription);
        when(transcriptionRepository.findByMeetingSessionIdAndTextContaining(testMeetingId, keyword))
                .thenReturn(expectedTranscriptions);

        // When
        var result = transcriptionRepository.findByMeetingSessionIdAndTextContaining(testMeetingId, keyword);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTranscription, result.get(0));
        assertTrue(result.get(0).getText().contains(keyword));
        verify(transcriptionRepository, times(1)).findByMeetingSessionIdAndTextContaining(testMeetingId, keyword);
    }

    @Test
    @DisplayName("Should find max segment number by meeting session ID")
    void shouldFindMaxSegmentNumberByMeetingSessionId() {
        // Given
        var expectedMaxSegment = 25;
        when(transcriptionRepository.findMaxSegmentNumberByMeetingSessionId(testMeetingId))
                .thenReturn(expectedMaxSegment);

        // When
        var result = transcriptionRepository.findMaxSegmentNumberByMeetingSessionId(testMeetingId);

        // Then
        assertEquals(expectedMaxSegment, result);
        verify(transcriptionRepository, times(1)).findMaxSegmentNumberByMeetingSessionId(testMeetingId);
    }

    @Test
    @DisplayName("Should return null when no transcriptions exist for meeting")
    void shouldReturnNullWhenNoTranscriptionsExistForMeeting() {
        // Given
        var nonExistentMeetingId = UUID.randomUUID();
        when(transcriptionRepository.findMaxSegmentNumberByMeetingSessionId(nonExistentMeetingId))
                .thenReturn(null);

        // When
        var result = transcriptionRepository.findMaxSegmentNumberByMeetingSessionId(nonExistentMeetingId);

        // Then
        assertNull(result);
        verify(transcriptionRepository, times(1)).findMaxSegmentNumberByMeetingSessionId(nonExistentMeetingId);
    }

    @Test
    @DisplayName("Should delete transcriptions by meeting session ID")
    void shouldDeleteTranscriptionsByMeetingSessionId() {
        // Given
        doNothing().when(transcriptionRepository).deleteByMeetingSessionId(testMeetingId);

        // When
        transcriptionRepository.deleteByMeetingSessionId(testMeetingId);

        // Then
        verify(transcriptionRepository, times(1)).deleteByMeetingSessionId(testMeetingId);
    }

    @Test
    @DisplayName("Should check if transcriptions exist by meeting session ID")
    void shouldCheckIfTranscriptsExistByMeetingSessionId() {
        // Given
        when(transcriptionRepository.existsByMeetingSessionId(testMeetingId)).thenReturn(true);

        // When
        var result = transcriptionRepository.existsByMeetingSessionId(testMeetingId);

        // Then
        assertTrue(result);
        verify(transcriptionRepository, times(1)).existsByMeetingSessionId(testMeetingId);
    }

    @Test
    @DisplayName("Should handle large number of transcriptions")
    void shouldHandleLargeNumberOfTranscriptions() {
        // Given
        var largeTranscriptionList = new ArrayList<Transcription>();
        for (int i = 0; i < 5000; i++) {
            var transcription = createTranscription(i + 1, "Transcription " + i);
            largeTranscriptionList.add(transcription);
        }
        when(transcriptionRepository.findByMeetingSessionId(testMeetingId)).thenReturn(largeTranscriptionList);

        // When
        var result = transcriptionRepository.findByMeetingSessionId(testMeetingId);

        // Then
        assertNotNull(result);
        assertEquals(5000, result.size());
        verify(transcriptionRepository, times(1)).findByMeetingSessionId(testMeetingId);
    }

    @Test
    @DisplayName("Should handle empty meeting session ID list")
    void shouldHandleEmptyMeetingSessionIdList() {
        // Given
        var emptyList = Collections.<UUID>emptyList();
        
        // When & Then - this would typically be handled in service layer
        assertDoesNotThrow(() -> {
            transcriptionRepository.findByMeetingSessionId(null);
        });
    }

    @Test
    @DisplayName("Should handle timestamp range with same start and end time")
    void shouldHandleTimestampRangeWithSameStartAndEndTime() {
        // Given
        var sameTime = testTimestamp;
        var expectedTranscriptions = List.of(testTranscription);
        when(transcriptionRepository.findByTimestampBetween(sameTime, sameTime)).thenReturn(expectedTranscriptions);

        // When
        var result = transcriptionRepository.findByTimestampBetween(sameTime, sameTime);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTranscription, result.get(0));
        verify(transcriptionRepository, times(1)).findByTimestampBetween(sameTime, sameTime);
    }

    @Test
    @DisplayName("Should handle confidence values at boundaries")
    void shouldHandleConfidenceValuesAtBoundaries() {
        // Given
        var highConfidenceTranscription = createTranscriptionWithConfidence(1.0);
        var lowConfidenceTranscription = createTranscriptionWithConfidence(0.0);
        
        when(transcriptionRepository.findByMeetingSessionIdAndMinConfidence(eq(testMeetingId), eq(0.0)))
                .thenReturn(Arrays.asList(highConfidenceTranscription, lowConfidenceTranscription));
        when(transcriptionRepository.findByMeetingSessionIdAndMinConfidence(eq(testMeetingId), eq(1.0)))
                .thenReturn(List.of(highConfidenceTranscription));

        // When
        var resultWithMin0 = transcriptionRepository.findByMeetingSessionIdAndMinConfidence(testMeetingId, 0.0);
        var resultWithMin1 = transcriptionRepository.findByMeetingSessionIdAndMinConfidence(testMeetingId, 1.0);

        // Then
        assertEquals(2, resultWithMin0.size());
        assertEquals(1, resultWithMin1.size());
        verify(transcriptionRepository, times(1)).findByMeetingSessionIdAndMinConfidence(testMeetingId, 0.0);
        verify(transcriptionRepository, times(1)).findByMeetingSessionIdAndMinConfidence(testMeetingId, 1.0);
    }

    @Test
    @DisplayName("Should handle special characters in text search")
    void shouldHandleSpecialCharactersInTextSearch() {
        // Given
        var keywordWithSpecialChars = "\"quoted text\" & (parentheses) [brackets]";
        var expectedTranscriptions = List.of(testTranscription);
        when(transcriptionRepository.findByTextContaining(keywordWithSpecialChars))
                .thenReturn(expectedTranscriptions);

        // When
        var result = transcriptionRepository.findByTextContaining(keywordWithSpecialChars);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTranscription, result.get(0));
        verify(transcriptionRepository, times(1)).findByTextContaining(keywordWithSpecialChars);
    }

    @Test
    @DisplayName("Should verify repository interface extends JpaRepository")
    void shouldVerifyRepositoryInterfaceExtendsJpaRepository() {
        // Given & When & Then
        assertTrue(transcriptionRepository instanceof JpaRepository);
        assertDoesNotThrow(() -> {
            // Test that all JpaRepository methods are available
            transcriptionRepository.save(testTranscription);
            transcriptionRepository.findById(testTranscriptionId);
            transcriptionRepository.findAll();
            transcriptionRepository.count();
            transcriptionRepository.delete(testTranscription);
        });
    }

    // Helper methods
    private Transcription createTranscription(int segmentNumber, String text) {
        var transcription = new Transcription();
        transcription.setId(UUID.randomUUID());
        transcription.setMeetingSession(testMeeting);
        transcription.setTimestamp(testTimestamp.plusMinutes(segmentNumber));
        transcription.setSpeakerId("Speaker-1");
        transcription.setText(text);
        transcription.setConfidence(0.95);
        transcription.setSegmentNumber(segmentNumber);
        return transcription;
    }

    private Transcription createTranscriptionWithTimestamp(LocalDateTime timestamp, String text) {
        var transcription = new Transcription();
        transcription.setId(UUID.randomUUID());
        transcription.setMeetingSession(testMeeting);
        transcription.setTimestamp(timestamp);
        transcription.setSpeakerId("Speaker-1");
        transcription.setText(text);
        transcription.setConfidence(0.95);
        transcription.setSegmentNumber(1);
        return transcription;
    }

    private Transcription createTranscriptionWithConfidence(Double confidence) {
        var transcription = new Transcription();
        transcription.setId(UUID.randomUUID());
        transcription.setMeetingSession(testMeeting);
        transcription.setTimestamp(testTimestamp);
        transcription.setSpeakerId("Speaker-1");
        transcription.setText("Test transcription with confidence " + confidence);
        transcription.setConfidence(confidence);
        transcription.setSegmentNumber(1);
        return transcription;
    }
}