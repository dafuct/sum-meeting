package com.zoomtranscriber.core.detection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ZoomDetectionService interface.
 * Tests meeting detection functionality and event handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ZoomDetectionService Tests")
class ZoomDetectionServiceTest {
    
    @Mock
    private ZoomDetectionService zoomDetectionService;
    
    private UUID testMeetingId;
    private LocalDateTime testTimestamp;
    private String testProcessId;
    private String testWindowTitle;
    
    @BeforeEach
    void setUp() {
        testMeetingId = UUID.randomUUID();
        testTimestamp = LocalDateTime.now();
        testProcessId = "zoom-process-123";
        testWindowTitle = "Zoom Meeting";
    }
    
    @Test
    @DisplayName("Should start monitoring successfully")
    void shouldStartMonitoringSuccessfully() {
        // Given
        when(zoomDetectionService.startMonitoring()).thenReturn(Mono.empty());
        
        // When
        var result = zoomDetectionService.startMonitoring();
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(zoomDetectionService, times(1)).startMonitoring();
    }
    
    @Test
    @DisplayName("Should handle monitoring start failure")
    void shouldHandleMonitoringStartFailure() {
        // Given
        var error = new RuntimeException("Failed to start monitoring");
        when(zoomDetectionService.startMonitoring()).thenReturn(Mono.error(error));
        
        // When
        var result = zoomDetectionService.startMonitoring();
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable -> throwable.getMessage().equals("Failed to start monitoring"))
            .verify();
        
        verify(zoomDetectionService, times(1)).startMonitoring();
    }
    
    @Test
    @DisplayName("Should stop monitoring successfully")
    void shouldStopMonitoringSuccessfully() {
        // Given
        when(zoomDetectionService.stopMonitoring()).thenReturn(Mono.empty());
        
        // When
        var result = zoomDetectionService.stopMonitoring();
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(zoomDetectionService, times(1)).stopMonitoring();
    }
    
    @Test
    @DisplayName("Should handle monitoring stop failure")
    void shouldHandleMonitoringStopFailure() {
        // Given
        var error = new RuntimeException("Failed to stop monitoring");
        when(zoomDetectionService.stopMonitoring()).thenReturn(Mono.error(error));
        
        // When
        var result = zoomDetectionService.stopMonitoring();
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable -> throwable.getMessage().equals("Failed to stop monitoring"))
            .verify();
        
        verify(zoomDetectionService, times(1)).stopMonitoring();
    }
    
    @Test
    @DisplayName("Should return monitoring status correctly")
    void shouldReturnMonitoringStatusCorrectly() {
        // Given
        when(zoomDetectionService.isMonitoring()).thenReturn(true);
        
        // When
        var result = zoomDetectionService.isMonitoring();
        
        // Then
        assertTrue(result);
        verify(zoomDetectionService, times(1)).isMonitoring();
    }
    
    @Test
    @DisplayName("Should return false monitoring status when not monitoring")
    void shouldReturnFalseMonitoringStatusWhenNotMonitoring() {
        // Given
        when(zoomDetectionService.isMonitoring()).thenReturn(false);
        
        // When
        var result = zoomDetectionService.isMonitoring();
        
        // Then
        assertFalse(result);
        verify(zoomDetectionService, times(1)).isMonitoring();
    }
    
    @Test
    @DisplayName("Should get meeting events stream")
    void shouldGetMeetingEventsStream() {
        // Given
        var event1 = new ZoomDetectionService.MeetingEvent(
            testMeetingId, ZoomDetectionService.MeetingEvent.MeetingEventType.MEETING_STARTED,
            testTimestamp, testProcessId, testWindowTitle);
        var event2 = new ZoomDetectionService.MeetingEvent(
            UUID.randomUUID(), ZoomDetectionService.MeetingEvent.MeetingEventType.MEETING_ENDED,
            testTimestamp.plusMinutes(30), testProcessId, testWindowTitle);
        var expectedEvents = List.of(event1, event2);
        
        when(zoomDetectionService.getMeetingEvents()).thenReturn(Flux.fromIterable(expectedEvents));
        
        // When
        var result = zoomDetectionService.getMeetingEvents();
        
        // Then
        StepVerifier.create(result)
            .expectNext(event1)
            .expectNext(event2)
            .verifyComplete();
        
        verify(zoomDetectionService, times(1)).getMeetingEvents();
    }
    
    @Test
    @DisplayName("Should handle empty meeting events stream")
    void shouldHandleEmptyMeetingEventsStream() {
        // Given
        when(zoomDetectionService.getMeetingEvents()).thenReturn(Flux.empty());
        
        // When
        var result = zoomDetectionService.getMeetingEvents();
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(zoomDetectionService, times(1)).getMeetingEvents();
    }
    
    @Test
    @DisplayName("Should get current meeting state successfully")
    void shouldGetCurrentMeetingStateSuccessfully() {
        // Given
        var expectedState = new ZoomDetectionService.MeetingState(
            testMeetingId, "Test Meeting", ZoomDetectionService.MeetingState.MeetingStatus.ACTIVE,
            testTimestamp, null, testProcessId, 5, testTimestamp);
        when(zoomDetectionService.getCurrentMeetingState(testMeetingId)).thenReturn(Mono.just(expectedState));
        
        // When
        var result = zoomDetectionService.getCurrentMeetingState(testMeetingId);
        
        // Then
        StepVerifier.create(result)
            .expectNext(expectedState)
            .verifyComplete();
        
        verify(zoomDetectionService, times(1)).getCurrentMeetingState(testMeetingId);
    }
    
    @Test
    @DisplayName("Should handle meeting state not found")
    void shouldHandleMeetingStateNotFound() {
        // Given
        when(zoomDetectionService.getCurrentMeetingState(testMeetingId)).thenReturn(Mono.empty());
        
        // When
        var result = zoomDetectionService.getCurrentMeetingState(testMeetingId);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(zoomDetectionService, times(1)).getCurrentMeetingState(testMeetingId);
    }
    
    @Test
    @DisplayName("Should handle meeting state retrieval error")
    void shouldHandleMeetingStateRetrievalError() {
        // Given
        var error = new RuntimeException("Failed to get meeting state");
        when(zoomDetectionService.getCurrentMeetingState(testMeetingId)).thenReturn(Mono.error(error));
        
        // When
        var result = zoomDetectionService.getCurrentMeetingState(testMeetingId);
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable -> throwable.getMessage().equals("Failed to get meeting state"))
            .verify();
        
        verify(zoomDetectionService, times(1)).getCurrentMeetingState(testMeetingId);
    }
    
    @Test
    @DisplayName("Should get active meetings stream")
    void shouldGetActiveMeetingsStream() {
        // Given
        var meeting1 = new ZoomDetectionService.MeetingState(
            testMeetingId, "Meeting 1", ZoomDetectionService.MeetingState.MeetingStatus.ACTIVE,
            testTimestamp, null, testProcessId, 3, testTimestamp);
        var meeting2 = new ZoomDetectionService.MeetingState(
            UUID.randomUUID(), "Meeting 2", ZoomDetectionService.MeetingState.MeetingStatus.ACTIVE,
            testTimestamp.plusMinutes(10), null, "zoom-456", 2, testTimestamp.plusMinutes(10));
        var expectedMeetings = List.of(meeting1, meeting2);
        
        when(zoomDetectionService.getActiveMeetings()).thenReturn(Flux.fromIterable(expectedMeetings));
        
        // When
        var result = zoomDetectionService.getActiveMeetings();
        
        // Then
        StepVerifier.create(result)
            .expectNext(meeting1)
            .expectNext(meeting2)
            .verifyComplete();
        
        verify(zoomDetectionService, times(1)).getActiveMeetings();
    }
    
    @Test
    @DisplayName("Should handle empty active meetings stream")
    void shouldHandleEmptyActiveMeetingsStream() {
        // Given
        when(zoomDetectionService.getActiveMeetings()).thenReturn(Flux.empty());
        
        // When
        var result = zoomDetectionService.getActiveMeetings();
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(zoomDetectionService, times(1)).getActiveMeetings();
    }
    
    @Test
    @DisplayName("Should trigger detection scan successfully")
    void shouldTriggerDetectionScanSuccessfully() {
        // Given
        when(zoomDetectionService.triggerDetectionScan()).thenReturn(Mono.empty());
        
        // When
        var result = zoomDetectionService.triggerDetectionScan();
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(zoomDetectionService, times(1)).triggerDetectionScan();
    }
    
    @Test
    @DisplayName("Should handle detection scan failure")
    void shouldHandleDetectionScanFailure() {
        // Given
        var error = new RuntimeException("Scan failed");
        when(zoomDetectionService.triggerDetectionScan()).thenReturn(Mono.error(error));
        
        // When
        var result = zoomDetectionService.triggerDetectionScan();
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable -> throwable.getMessage().equals("Scan failed"))
            .verify();
        
        verify(zoomDetectionService, times(1)).triggerDetectionScan();
    }
    
    @Test
    @DisplayName("Should create meeting event record correctly")
    void shouldCreateMeetingEventRecordCorrectly() {
        // Given
        var eventType = ZoomDetectionService.MeetingEvent.MeetingEventType.MEETING_STARTED;
        
        // When
        var event = new ZoomDetectionService.MeetingEvent(
            testMeetingId, eventType, testTimestamp, testProcessId, testWindowTitle);
        
        // Then
        assertEquals(testMeetingId, event.meetingId());
        assertEquals(eventType, event.eventType());
        assertEquals(testTimestamp, event.timestamp());
        assertEquals(testProcessId, event.processId());
        assertEquals(testWindowTitle, event.windowTitle());
    }
    
    @ParameterizedTest
    @EnumSource(ZoomDetectionService.MeetingEvent.MeetingEventType.class)
    @DisplayName("Should handle all meeting event types")
    void shouldHandleAllMeetingEventTypes(ZoomDetectionService.MeetingEvent.MeetingEventType eventType) {
        // When
        var event = new ZoomDetectionService.MeetingEvent(
            testMeetingId, eventType, testTimestamp, testProcessId, testWindowTitle);
        
        // Then
        assertEquals(eventType, event.eventType());
    }
    
    @Test
    @DisplayName("Should create meeting state record correctly")
    void shouldCreateMeetingStateRecordCorrectly() {
        // Given
        var status = ZoomDetectionService.MeetingState.MeetingStatus.ACTIVE;
        var startTime = testTimestamp;
        var endTime = testTimestamp.plusHours(1);
        var participantCount = 5;
        var lastUpdated = testTimestamp.plusMinutes(30);
        
        // When
        var state = new ZoomDetectionService.MeetingState(
            testMeetingId, "Test Meeting", status, startTime, endTime,
            testProcessId, participantCount, lastUpdated);
        
        // Then
        assertEquals(testMeetingId, state.meetingId());
        assertEquals("Test Meeting", state.title());
        assertEquals(status, state.status());
        assertEquals(startTime, state.startTime());
        assertEquals(endTime, state.endTime());
        assertEquals(testProcessId, state.processId());
        assertEquals(participantCount, state.participantCount());
        assertEquals(lastUpdated, state.lastUpdated());
    }
    
    @ParameterizedTest
    @EnumSource(ZoomDetectionService.MeetingState.MeetingStatus.class)
    @DisplayName("Should handle all meeting status types")
    void shouldHandleAllMeetingStatusTypes(ZoomDetectionService.MeetingState.MeetingStatus status) {
        // When
        var state = new ZoomDetectionService.MeetingState(
            testMeetingId, "Test Meeting", status, testTimestamp, null,
            testProcessId, 0, testTimestamp);
        
        // Then
        assertEquals(status, state.status());
    }
    
    @Test
    @DisplayName("Should handle meeting state with null end time")
    void shouldHandleMeetingStateWithNullEndTime() {
        // Given
        var status = ZoomDetectionService.MeetingState.MeetingStatus.ACTIVE;
        
        // When
        var state = new ZoomDetectionService.MeetingState(
            testMeetingId, "Test Meeting", status, testTimestamp, null,
            testProcessId, 0, testTimestamp);
        
        // Then
        assertNull(state.endTime());
    }
    
    @Test
    @DisplayName("Should handle meeting state with zero participants")
    void shouldHandleMeetingStateWithZeroParticipants() {
        // Given
        var status = ZoomDetectionService.MeetingState.MeetingStatus.DETECTED;
        
        // When
        var state = new ZoomDetectionService.MeetingState(
            testMeetingId, "Test Meeting", status, testTimestamp, null,
            testProcessId, 0, testTimestamp);
        
        // Then
        assertEquals(0, state.participantCount());
    }
    
    @Test
    @DisplayName("Should handle meeting state with negative participants")
    void shouldHandleMeetingStateWithNegativeParticipants() {
        // Given
        var status = ZoomDetectionService.MeetingState.MeetingStatus.ERROR;
        
        // When
        var state = new ZoomDetectionService.MeetingState(
            testMeetingId, "Test Meeting", status, testTimestamp, null,
            testProcessId, -5, testTimestamp);
        
        // Then
        assertEquals(-5, state.participantCount());
    }
    
    @Test
    @DisplayName("Should handle meeting event with null window title")
    void shouldHandleMeetingEventWithNullWindowTitle() {
        // Given
        var eventType = ZoomDetectionService.MeetingEvent.MeetingEventType.PARTICIPANT_JOINED;
        
        // When
        var event = new ZoomDetectionService.MeetingEvent(
            testMeetingId, eventType, testTimestamp, testProcessId, null);
        
        // Then
        assertNull(event.windowTitle());
    }
    
    @Test
    @DisplayName("Should handle meeting state with null title")
    void shouldHandleMeetingStateWithNullTitle() {
        // Given
        var status = ZoomDetectionService.MeetingState.MeetingStatus.DETECTED;
        
        // When
        var state = new ZoomDetectionService.MeetingState(
            testMeetingId, null, status, testTimestamp, null,
            testProcessId, 0, testTimestamp);
        
        // Then
        assertNull(state.title());
    }
    
    @Test
    @DisplayName("Should handle meeting state with null process ID")
    void shouldHandleMeetingStateWithNullProcessId() {
        // Given
        var status = ZoomDetectionService.MeetingState.MeetingStatus.ERROR;
        
        // When
        var state = new ZoomDetectionService.MeetingState(
            testMeetingId, "Test Meeting", status, testTimestamp, null,
            null, 0, testTimestamp);
        
        // Then
        assertNull(state.processId());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
    @DisplayName("Should handle meeting state with various title formats")
    void shouldHandleMeetingStateWithVariousTitleFormats(String title) {
        // Given
        var status = ZoomDetectionService.MeetingState.MeetingStatus.ACTIVE;
        
        // When
        var state = new ZoomDetectionService.MeetingState(
            testMeetingId, title, status, testTimestamp, null,
            testProcessId, 0, testTimestamp);
        
        // Then
        assertEquals(title, state.title());
    }
    
    @Test
    @DisplayName("Should handle meeting state with future timestamps")
    void shouldHandleMeetingStateWithFutureTimestamps() {
        // Given
        var futureTime = LocalDateTime.now().plusDays(1);
        var status = ZoomDetectionService.MeetingState.MeetingStatus.DETECTED;
        
        // When
        var state = new ZoomDetectionService.MeetingState(
            testMeetingId, "Future Meeting", status, futureTime, null,
            testProcessId, 0, futureTime);
        
        // Then
        assertEquals(futureTime, state.startTime());
        assertEquals(futureTime, state.lastUpdated());
    }
    
    @Test
    @DisplayName("Should handle meeting state with past timestamps")
    void shouldHandleMeetingStateWithPastTimestamps() {
        // Given
        var pastTime = LocalDateTime.now().minusDays(1);
        var status = ZoomDetectionService.MeetingState.MeetingStatus.ENDED;
        
        // When
        var state = new ZoomDetectionService.MeetingState(
            testMeetingId, "Past Meeting", status, pastTime.minusHours(1), pastTime,
            testProcessId, 0, pastTime);
        
        // Then
        assertEquals(pastTime.minusHours(1), state.startTime());
        assertEquals(pastTime, state.endTime());
        assertEquals(pastTime, state.lastUpdated());
    }
    
    @Test
    @DisplayName("Should handle meeting event with special characters in window title")
    void shouldHandleMeetingEventWithSpecialCharactersInWindowTitle() {
        // Given
        var eventType = ZoomDetectionService.MeetingEvent.MeetingEventType.MEETING_STARTED;
        var specialTitle = "Meeting with @#$%^&*()_+-={}[]|\\:;\"'<>?,./";
        
        // When
        var event = new ZoomDetectionService.MeetingEvent(
            testMeetingId, eventType, testTimestamp, testProcessId, specialTitle);
        
        // Then
        assertEquals(specialTitle, event.windowTitle());
    }
    
    @Test
    @DisplayName("Should handle meeting state with unicode characters in title")
    void shouldHandleMeetingStateWithUnicodeCharactersInTitle() {
        // Given
        var status = ZoomDetectionService.MeetingState.MeetingStatus.ACTIVE;
        var unicodeTitle = "Meeting with 世界 🌍 ñáéíóú";
        
        // When
        var state = new ZoomDetectionService.MeetingState(
            testMeetingId, unicodeTitle, status, testTimestamp, null,
            testProcessId, 0, testTimestamp);
        
        // Then
        assertEquals(unicodeTitle, state.title());
    }
    
    @Test
    @DisplayName("Should handle meeting event with very long window title")
    void shouldHandleMeetingEventWithVeryLongWindowTitle() {
        // Given
        var eventType = ZoomDetectionService.MeetingEvent.MeetingEventType.MEETING_STARTED;
        var longTitle = "A".repeat(1000); // Very long title
        
        // When
        var event = new ZoomDetectionService.MeetingEvent(
            testMeetingId, eventType, testTimestamp, testProcessId, longTitle);
        
        // Then
        assertEquals(longTitle, event.windowTitle());
    }
    
    @Test
    @DisplayName("Should handle meeting state with very long title")
    void shouldHandleMeetingStateWithVeryLongTitle() {
        // Given
        var status = ZoomDetectionService.MeetingState.MeetingStatus.ACTIVE;
        var longTitle = "A".repeat(1000); // Very long title
        
        // When
        var state = new ZoomDetectionService.MeetingState(
            testMeetingId, longTitle, status, testTimestamp, null,
            testProcessId, 0, testTimestamp);
        
        // Then
        assertEquals(longTitle, state.title());
    }
}