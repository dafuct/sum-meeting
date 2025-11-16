package com.zoomtranscriber.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoomtranscriber.core.storage.MeetingSession;
import com.zoomtranscriber.core.storage.MeetingSessionRepository;
import com.zoomtranscriber.core.detection.ZoomDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for API MeetingController class.
 * Tests REST API endpoints for meeting management.
 */
@WebMvcTest(MeetingController.class)
@DisplayName("API MeetingController Tests")
class MeetingControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private ZoomDetectionService zoomDetectionService;
    
    @MockBean
    private MeetingSessionRepository meetingSessionRepository;
    
    private MeetingSession testMeeting;
    private List<MeetingSession> allMeetings;
    private ZoomDetectionService.MeetingState testMeetingState;
    private ZoomDetectionService.MeetingEvent testMeetingEvent;
    
    @BeforeEach
    void setUp() {
        // Create test data
        testMeeting = createTestMeeting();
        allMeetings = List.of(testMeeting);
        testMeetingState = createTestMeetingState();
        testMeetingEvent = createTestMeetingEvent();
        
        // Setup default mock behaviors
        when(zoomDetectionService.startMonitoring()).thenReturn(Mono.empty());
        when(zoomDetectionService.stopMonitoring()).thenReturn(Mono.empty());
        when(zoomDetectionService.isMonitoring()).thenReturn(false);
        when(zoomDetectionService.triggerDetectionScan()).thenReturn(Mono.empty());
        when(zoomDetectionService.getActiveMeetings()).thenReturn(Flux.empty());
        when(zoomDetectionService.getMeetingEvents()).thenReturn(Flux.empty());
        when(zoomDetectionService.getCurrentMeetingState(any()))
            .thenReturn(Mono.empty());
        
        when(meetingSessionRepository.findAll()).thenReturn(allMeetings);
        when(meetingSessionRepository.findById(any())).thenReturn(Optional.of(testMeeting));
        when(meetingSessionRepository.findByMeetingId(any())).thenReturn(Optional.of(testMeeting));
        when(meetingSessionRepository.save(any())).thenReturn(testMeeting);
        when(meetingSessionRepository.delete(any())).thenReturn(true);
    }
    
    @Test
    @DisplayName("Should get all meetings successfully")
    void shouldGetAllMeetingsSuccessfully() throws Exception {
        // Given
        when(meetingSessionRepository.findAll()).thenReturn(allMeetings);
        
        // When
        var result = mockMvc.perform(get("/api/v1/meetings"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(meetingSessionRepository, times(1)).findAll();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseMeetings = objectMapper.readValue(responseJson, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, MeetingSession.class));
        
        assertEquals(1, responseMeetings.size());
        assertEquals(testMeeting.getId(), responseMeetings.get(0).getId());
    }
    
    @Test
    @DisplayName("Should handle get all meetings error")
    void shouldHandleGetAllMeetingsError() throws Exception {
        // Given
        when(meetingSessionRepository.findAll()).thenThrow(new RuntimeException("Database error"));
        
        // When & Then
        mockMvc.perform(get("/api/v1/meetings"))
            .andExpect(status().isInternalServerError());
        
        verify(meetingSessionRepository, times(1)).findAll();
    }
    
    @Test
    @DisplayName("Should get meeting by ID successfully")
    void shouldGetMeetingByIdSuccessfully() throws Exception {
        // Given
        when(meetingSessionRepository.findById(testMeeting.getId()))
            .thenReturn(Optional.of(testMeeting));
        
        // When
        var result = mockMvc.perform(get("/api/v1/meetings/{id}", testMeeting.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(meetingSessionRepository, times(1)).findById(testMeeting.getId());
        
        var responseJson = result.getResponse().getContentAsString();
        var responseMeeting = objectMapper.readValue(responseJson, MeetingSession.class);
        
        assertEquals(testMeeting.getId(), responseMeeting.getId());
        assertEquals(testMeeting.getTitle(), responseMeeting.getTitle());
    }
    
    @Test
    @DisplayName("Should handle meeting not found")
    void shouldHandleMeetingNotFound() throws Exception {
        // Given
        var meetingId = UUID.randomUUID();
        when(meetingSessionRepository.findById(meetingId)).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(get("/api/v1/meetings/{id}", meetingId))
            .andExpect(status().isNotFound());
        
        verify(meetingSessionRepository, times(1)).findById(meetingId);
    }
    
    @Test
    @DisplayName("Should get meeting by meeting ID successfully")
    void shouldGetMeetingByMeetingIdSuccessfully() throws Exception {
        // Given
        when(meetingSessionRepository.findByMeetingId(testMeeting.getMeetingId()))
            .thenReturn(Optional.of(testMeeting));
        
        // When
        var result = mockMvc.perform(get("/api/v1/meetings/by-meeting-id/{meetingId}", testMeeting.getMeetingId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(meetingSessionRepository, times(1)).findByMeetingId(testMeeting.getMeetingId());
        
        var responseJson = result.getResponse().getContentAsString();
        var responseMeeting = objectMapper.readValue(responseJson, MeetingSession.class);
        
        assertEquals(testMeeting.getMeetingId(), responseMeeting.getMeetingId());
    }
    
    @Test
    @DisplayName("Should create meeting successfully")
    void shouldCreateMeetingSuccessfully() throws Exception {
        // Given
        var newMeeting = createTestMeeting();
        newMeeting.setId(null); // New meeting has no ID
        
        when(meetingSessionRepository.save(any())).thenAnswer(invocation -> {
            var meeting = (MeetingSession) invocation.getArgument(0);
            meeting.setId(UUID.randomUUID());
            return meeting;
        });
        
        // When
        var result = mockMvc.perform(post("/api/v1/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newMeeting)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(meetingSessionRepository, times(1)).save(any(MeetingSession.class));
        
        var responseJson = result.getResponse().getContentAsString();
        var responseMeeting = objectMapper.readValue(responseJson, MeetingSession.class);
        
        assertNotNull(responseMeeting.getId());
        assertEquals(newMeeting.getTitle(), responseMeeting.getTitle());
    }
    
    @Test
    @DisplayName("Should handle create meeting validation error")
    void shouldHandleCreateMeetingValidationError() throws Exception {
        // Given
        var invalidMeeting = new MeetingSession();
        invalidMeeting.setTitle(null); // Invalid: null title
        
        // When & Then
        mockMvc.perform(post("/api/v1/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidMeeting)))
            .andExpect(status().isBadRequest());
        
        verify(meetingSessionRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should update meeting successfully")
    void shouldUpdateMeetingSuccessfully() throws Exception {
        // Given
        var updatedMeeting = createTestMeeting();
        updatedMeeting.setTitle("Updated Meeting Title");
        
        when(meetingSessionRepository.save(updatedMeeting)).thenReturn(updatedMeeting);
        
        // When
        var result = mockMvc.perform(put("/api/v1/meetings/{id}", testMeeting.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedMeeting)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(meetingSessionRepository, times(1)).save(updatedMeeting);
        
        var responseJson = result.getResponse().getContentAsString();
        var responseMeeting = objectMapper.readValue(responseJson, MeetingSession.class);
        
        assertEquals("Updated Meeting Title", responseMeeting.getTitle());
    }
    
    @Test
    @DisplayName("Should handle update meeting not found")
    void shouldHandleUpdateMeetingNotFound() throws Exception {
        // Given
        var updatedMeeting = createTestMeeting();
        var meetingId = UUID.randomUUID();
        
        when(meetingSessionRepository.findById(meetingId)).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(put("/api/v1/meetings/{id}", meetingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedMeeting)))
            .andExpect(status().isNotFound());
        
        verify(meetingSessionRepository, times(1)).findById(meetingId);
        verify(meetingSessionRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should delete meeting successfully")
    void shouldDeleteMeetingSuccessfully() throws Exception {
        // Given
        when(meetingSessionRepository.findById(testMeeting.getId()))
            .thenReturn(Optional.of(testMeeting));
        when(meetingSessionRepository.delete(testMeeting)).thenReturn(true);
        
        // When
        mockMvc.perform(delete("/api/v1/meetings/{id}", testMeeting.getId()))
            .andExpect(status().isNoContent());
        
        // Then
        verify(meetingSessionRepository, times(1)).findById(testMeeting.getId());
        verify(meetingSessionRepository, times(1)).delete(testMeeting);
    }
    
    @Test
    @DisplayName("Should handle delete meeting not found")
    void shouldHandleDeleteMeetingNotFound() throws Exception {
        // Given
        var meetingId = UUID.randomUUID();
        when(meetingSessionRepository.findById(meetingId)).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(delete("/api/v1/meetings/{id}", meetingId))
            .andExpect(status().isNotFound());
        
        verify(meetingSessionRepository, times(1)).findById(meetingId);
        verify(meetingSessionRepository, never()).delete(any());
    }
    
    @Test
    @DisplayName("Should get active meetings successfully")
    void shouldGetActiveMeetingsSuccessfully() throws Exception {
        // Given
        when(zoomDetectionService.getActiveMeetings()).thenReturn(Flux.just(testMeetingState));
        
        // When
        var result = mockMvc.perform(get("/api/v1/meetings/active"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(zoomDetectionService, times(1)).getActiveMeetings();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseStates = objectMapper.readValue(responseJson, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, ZoomDetectionService.MeetingState.class));
        
        assertEquals(1, responseStates.size());
        assertEquals(testMeetingState.meetingId(), responseStates.get(0).meetingId());
    }
    
    @Test
    @DisplayName("Should start meeting detection successfully")
    void shouldStartMeetingDetectionSuccessfully() throws Exception {
        // Given
        when(zoomDetectionService.startMonitoring()).thenReturn(Mono.empty());
        
        // When
        mockMvc.perform(post("/api/v1/meetings/detection/start"))
            .andExpect(status().isOk());
        
        // Then
        verify(zoomDetectionService, times(1)).startMonitoring();
    }
    
    @Test
    @DisplayName("Should handle start meeting detection failure")
    void shouldHandleStartMeetingDetectionFailure() throws Exception {
        // Given
        var error = new RuntimeException("Failed to start detection");
        when(zoomDetectionService.startMonitoring()).thenReturn(Mono.error(error));
        
        // When & Then
        mockMvc.perform(post("/api/v1/meetings/detection/start"))
            .andExpect(status().isInternalServerError());
        
        verify(zoomDetectionService, times(1)).startMonitoring();
    }
    
    @Test
    @DisplayName("Should stop meeting detection successfully")
    void shouldStopMeetingDetectionSuccessfully() throws Exception {
        // Given
        when(zoomDetectionService.stopMonitoring()).thenReturn(Mono.empty());
        
        // When
        mockMvc.perform(post("/api/v1/meetings/detection/stop"))
            .andExpect(status().isOk());
        
        // Then
        verify(zoomDetectionService, times(1)).stopMonitoring();
    }
    
    @Test
    @DisplayName("Should handle stop meeting detection failure")
    void shouldHandleStopMeetingDetectionFailure() throws Exception {
        // Given
        var error = new RuntimeException("Failed to stop detection");
        when(zoomDetectionService.stopMonitoring()).thenReturn(Mono.error(error));
        
        // When & Then
        mockMvc.perform(post("/api/v1/meetings/detection/stop"))
            .andExpect(status().isInternalServerError());
        
        verify(zoomDetectionService, times(1)).stopMonitoring();
    }
    
    @Test
    @DisplayName("Should trigger detection scan successfully")
    void shouldTriggerDetectionScanSuccessfully() throws Exception {
        // Given
        when(zoomDetectionService.triggerDetectionScan()).thenReturn(Mono.empty());
        
        // When
        mockMvc.perform(post("/api/v1/meetings/detection/scan"))
            .andExpect(status().isOk());
        
        // Then
        verify(zoomDetectionService, times(1)).triggerDetectionScan();
    }
    
    @Test
    @DisplayName("Should get detection status successfully")
    void shouldGetDetectionStatusSuccessfully() throws Exception {
        // Given
        when(zoomDetectionService.isMonitoring()).thenReturn(true);
        
        // When
        var result = mockMvc.perform(get("/api/v1/meetings/detection/status"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(zoomDetectionService, times(1)).isMonitoring();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseMap = objectMapper.readValue(responseJson, Object.class);
        
        assertNotNull(responseMap);
        // Should contain monitoring status
    }
    
    @Test
    @DisplayName("Should get meeting events successfully")
    void shouldGetMeetingEventsSuccessfully() throws Exception {
        // Given
        when(zoomDetectionService.getMeetingEvents()).thenReturn(Flux.just(testMeetingEvent));
        
        // When
        var result = mockMvc.perform(get("/api/v1/meetings/events"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(zoomDetectionService, times(1)).getMeetingEvents();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseEvents = objectMapper.readValue(responseJson, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, ZoomDetectionService.MeetingEvent.class));
        
        assertEquals(1, responseEvents.size());
        assertEquals(testMeetingEvent.meetingId(), responseEvents.get(0).meetingId());
    }
    
    @Test
    @DisplayName("Should get meeting state by ID successfully")
    void shouldGetMeetingStateByIdSuccessfully() throws Exception {
        // Given
        when(zoomDetectionService.getCurrentMeetingState(testMeeting.getMeetingId()))
            .thenReturn(Mono.just(testMeetingState));
        
        // When
        var result = mockMvc.perform(get("/api/v1/meetings/{id}/state", testMeeting.getMeetingId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(zoomDetectionService, times(1)).getCurrentMeetingState(testMeeting.getMeetingId());
        
        var responseJson = result.getResponse().getContentAsString();
        var responseState = objectMapper.readValue(responseJson, ZoomDetectionService.MeetingState.class);
        
        assertEquals(testMeetingState.meetingId(), responseState.meetingId());
    }
    
    @Test
    @DisplayName("Should handle meeting state not found")
    void shouldHandleMeetingStateNotFound() throws Exception {
        // Given
        var meetingId = UUID.randomUUID();
        when(zoomDetectionService.getCurrentMeetingState(meetingId)).thenReturn(Mono.empty());
        
        // When & Then
        mockMvc.perform(get("/api/v1/meetings/{id}/state", meetingId))
            .andExpect(status().isNotFound());
        
        verify(zoomDetectionService, times(1)).getCurrentMeetingState(meetingId);
    }
    
    @Test
    @DisplayName("Should handle invalid UUID format")
    void shouldHandleInvalidUuidFormat() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/meetings/{id}", "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should handle empty request body")
    void shouldHandleEmptyRequestBody() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should handle invalid JSON")
    void shouldHandleInvalidJSON() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should handle CORS headers")
    void shouldHandleCorsHeaders() throws Exception {
        // When
        mockMvc.perform(options("/api/v1/meetings"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Access-Control-Allow-Origin"))
            .andExpect(header().exists("Access-Control-Allow-Methods"))
            .andExpect(header().exists("Access-Control-Allow-Headers"));
    }
    
    @Test
    @DisplayName("Should handle concurrent requests")
    void shouldHandleConcurrentRequests() throws Exception {
        // Given
        when(meetingSessionRepository.findAll()).thenReturn(allMeetings);
        when(zoomDetectionService.isMonitoring()).thenReturn(false);
        
        // When - make multiple requests
        var getResult = mockMvc.perform(get("/api/v1/meetings"));
        var statusResult = mockMvc.perform(get("/api/v1/meetings/detection/status"));
        
        // Then - both should complete successfully
        getResult.andExpect(status().isOk());
        statusResult.andExpect(status().isOk());
        
        verify(meetingSessionRepository, times(1)).findAll();
        verify(zoomDetectionService, times(1)).isMonitoring();
    }
    
    @Test
    @DisplayName("Should handle large response data")
    void shouldHandleLargeResponseData() throws Exception {
        // Given
        var manyMeetings = java.util.stream.IntStream.range(0, 100)
            .mapToObj(i -> {
                var meeting = createTestMeeting();
                meeting.setId(UUID.randomUUID());
                meeting.setTitle("Meeting " + i);
                return meeting;
            })
            .toList();
        
        when(meetingSessionRepository.findAll()).thenReturn(manyMeetings);
        
        // When
        mockMvc.perform(get("/api/v1/meetings"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(100));
        
        // Then
        verify(meetingSessionRepository, times(1)).findAll();
    }
    
    // Helper methods
    
    private MeetingSession createTestMeeting() {
        var meeting = new MeetingSession("Test Meeting");
        meeting.setId(UUID.randomUUID());
        meeting.setMeetingId(UUID.randomUUID());
        meeting.setStartTime(LocalDateTime.now().minusMinutes(30));
        meeting.setEndTime(LocalDateTime.now().minusMinutes(10));
        meeting.setStatus(MeetingSession.MeetingStatus.COMPLETED);
        meeting.setParticipantCount(5);
        meeting.setCreatedAt(LocalDateTime.now().minusHours(1));
        meeting.setUpdatedAt(LocalDateTime.now());
        return meeting;
    }
    
    private ZoomDetectionService.MeetingState createTestMeetingState() {
        return new ZoomDetectionService.MeetingState(
            UUID.randomUUID(),
            "Test Meeting",
            ZoomDetectionService.MeetingState.MeetingStatus.ACTIVE,
            LocalDateTime.now().minusMinutes(15),
            null,
            "zoom-123",
            3,
            LocalDateTime.now()
        );
    }
    
    private ZoomDetectionService.MeetingEvent createTestMeetingEvent() {
        return new ZoomDetectionService.MeetingEvent(
            UUID.randomUUID(),
            ZoomDetectionService.MeetingEvent.MeetingEventType.MEETING_STARTED,
            LocalDateTime.now().minusMinutes(30),
            "zoom-123",
            "Test Meeting"
        );
    }
}