package com.zoomtranscriber.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoomtranscriber.core.transcription.TranscriptionService;
import com.zoomtranscriber.core.transcription.TranscriptionSegment;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for API TranscriptionController class.
 * Tests REST API endpoints for transcription management.
 */
@WebMvcTest(TranscriptionController.class)
@DisplayName("API TranscriptionController Tests")
class TranscriptionControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private TranscriptionService transcriptionService;
    
    private UUID testMeetingId;
    private List<TranscriptionSegment> testSegments;
    private TranscriptionService.TranscriptionConfig testConfig;
    private TranscriptionService.TranscriptionStats testStats;
    
    @BeforeEach
    void setUp() {
        // Create test data
        testMeetingId = UUID.randomUUID();
        testSegments = List.of(
            createTestTranscriptionSegment(0, 5000, "Hello world", 0.95),
            createTestTranscriptionSegment(5100, 8500, "How are you?", 0.88),
            createTestTranscriptionSegment(8600, 12000, "I'm doing well", 0.92)
        );
        
        testConfig = TranscriptionService.TranscriptionConfig.defaultConfig();
        testStats = new TranscriptionService.TranscriptionStats(
            testMeetingId, 3, 7, 0.92,
            LocalDateTime.now().minusMinutes(10), LocalDateTime.now(),
            java.time.Duration.ofMinutes(10), 2, List.of("en-US")
        );
        
        // Setup default mock behaviors
        when(transcriptionService.startTranscription(eq(testMeetingId), any()))
            .thenReturn(Mono.empty());
        when(transcriptionService.stopTranscription(testMeetingId))
            .thenReturn(Mono.empty());
        when(transcriptionService.getTranscriptionStatus(testMeetingId))
            .thenReturn(Mono.just(TranscriptionService.TranscriptionStatus.NOT_STARTED));
        when(transcriptionService.getTranscriptionStream(testMeetingId))
            .thenReturn(Flux.empty());
        when(transcriptionService.getAllTranscriptionSegments(testMeetingId))
            .thenReturn(Flux.empty());
        when(transcriptionService.setLanguage(testMeetingId, anyString()))
            .thenReturn(Mono.empty());
        when(transcriptionService.getLanguage(testMeetingId))
            .thenReturn(Mono.just("en-US"));
        when(transcriptionService.setSpeakerDiarization(testMeetingId, anyBoolean()))
            .thenReturn(Mono.empty());
        when(transcriptionService.isSpeakerDiarizationEnabled(testMeetingId))
            .thenReturn(Mono.just(true));
        when(transcriptionService.setConfidenceThreshold(testMeetingId, anyDouble()))
            .thenReturn(Mono.empty());
        when(transcriptionService.getConfidenceThreshold(testMeetingId))
            .thenReturn(Mono.just(0.7));
        when(transcriptionService.getTranscriptionStats(testMeetingId))
            .thenReturn(Mono.empty());
        when(transcriptionService.exportTranscription(testMeetingId, any()))
            .thenReturn(Mono.just(new byte[]{1, 2, 3}));
    }
    
    @Test
    @DisplayName("Should start transcription successfully")
    void shouldStartTranscriptionSuccessfully() throws Exception {
        // Given
        when(transcriptionService.startTranscription(eq(testMeetingId), any()))
            .thenReturn(Mono.empty());
        
        // When
        mockMvc.perform(post("/api/v1/transcription/start/{meetingId}", testMeetingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testConfig)))
            .andExpect(status().isOk());
        
        // Then
        verify(transcriptionService, times(1)).startTranscription(eq(testMeetingId), any());
    }
    
    @Test
    @DisplayName("Should handle start transcription failure")
    void shouldHandleStartTranscriptionFailure() throws Exception {
        // Given
        var error = new RuntimeException("Failed to start transcription");
        when(transcriptionService.startTranscription(eq(testMeetingId), any()))
            .thenReturn(Mono.error(error));
        
        // When
        mockMvc.perform(post("/api/v1/transcription/start/{meetingId}", testMeetingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testConfig)))
            .andExpect(status().isInternalServerError());
        
        // Then
        verify(transcriptionService, times(1)).startTranscription(eq(testMeetingId), any());
    }
    
    @Test
    @DisplayName("Should stop transcription successfully")
    void shouldStopTranscriptionSuccessfully() throws Exception {
        // Given
        when(transcriptionService.stopTranscription(testMeetingId))
            .thenReturn(Mono.empty());
        
        // When
        mockMvc.perform(post("/api/v1/transcription/stop/{meetingId}", testMeetingId))
            .andExpect(status().isOk());
        
        // Then
        verify(transcriptionService, times(1)).stopTranscription(testMeetingId);
    }
    
    @Test
    @DisplayName("Should handle stop transcription failure")
    void shouldHandleStopTranscriptionFailure() throws Exception {
        // Given
        var error = new RuntimeException("Failed to stop transcription");
        when(transcriptionService.stopTranscription(testMeetingId))
            .thenReturn(Mono.error(error));
        
        // When
        mockMvc.perform(post("/api/v1/transcription/stop/{meetingId}", testMeetingId))
            .andExpect(status().isInternalServerError());
        
        // Then
        verify(transcriptionService, times(1)).stopTranscription(testMeetingId);
    }
    
    @Test
    @DisplayName("Should get transcription status successfully")
    void shouldGetTranscriptionStatusSuccessfully() throws Exception {
        // Given
        var expectedStatus = TranscriptionService.TranscriptionStatus.ACTIVE;
        when(transcriptionService.getTranscriptionStatus(testMeetingId))
            .thenReturn(Mono.just(expectedStatus));
        
        // When
        var result = mockMvc.perform(get("/api/v1/transcription/status/{meetingId}", testMeetingId))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(transcriptionService, times(1)).getTranscriptionStatus(testMeetingId);
        
        var responseJson = result.getResponse().getContentAsString();
        var responseStatus = objectMapper.readValue(responseJson, TranscriptionService.TranscriptionStatus.class);
        
        assertEquals(expectedStatus, responseStatus);
    }
    
    @Test
    @DisplayName("Should handle transcription status not found")
    void shouldHandleTranscriptionStatusNotFound() throws Exception {
        // Given
        when(transcriptionService.getTranscriptionStatus(testMeetingId))
            .thenReturn(Mono.empty());
        
        // When
        mockMvc.perform(get("/api/v1/transcription/status/{meetingId}", testMeetingId))
            .andExpect(status().isNotFound());
        
        // Then
        verify(transcriptionService, times(1)).getTranscriptionStatus(testMeetingId);
    }
    
    @Test
    @DisplayName("Should get transcription stream successfully")
    void shouldGetTranscriptionStreamSuccessfully() throws Exception {
        // Given
        when(transcriptionService.getTranscriptionStream(testMeetingId))
            .thenReturn(Flux.fromIterable(testSegments));
        
        // When
        var result = mockMvc.perform(get("/api/v1/transcription/stream/{meetingId}", testMeetingId))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(transcriptionService, times(1)).getTranscriptionStream(testMeetingId);
        
        var responseJson = result.getResponse().getContentAsString();
        var responseSegments = objectMapper.readValue(responseJson, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, TranscriptionSegment.class));
        
        assertEquals(testSegments.size(), responseSegments.size());
    }
    
    @Test
    @DisplayName("Should get all transcription segments successfully")
    void shouldGetAllTranscriptionSegmentsSuccessfully() throws Exception {
        // Given
        when(transcriptionService.getAllTranscriptionSegments(testMeetingId))
            .thenReturn(Flux.fromIterable(testSegments));
        
        // When
        var result = mockMvc.perform(get("/api/v1/transcription/segments/{meetingId}", testMeetingId))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(transcriptionService, times(1)).getAllTranscriptionSegments(testMeetingId);
        
        var responseJson = result.getResponse().getContentAsString();
        var responseSegments = objectMapper.readValue(responseJson, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, TranscriptionSegment.class));
        
        assertEquals(testSegments.size(), responseSegments.size());
    }
    
    @Test
    @DisplayName("Should set language successfully")
    void shouldSetLanguageSuccessfully() throws Exception {
        // Given
        var language = "en-US";
        when(transcriptionService.setLanguage(testMeetingId, language))
            .thenReturn(Mono.empty());
        
        // When
        mockMvc.perform(post("/api/v1/transcription/language/{meetingId}", testMeetingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"" + language + "\""))
            .andExpect(status().isOk());
        
        // Then
        verify(transcriptionService, times(1)).setLanguage(testMeetingId, language);
    }
    
    @Test
    @DisplayName("Should handle set language failure")
    void shouldHandleSetLanguageFailure() throws Exception {
        // Given
        var language = "en-US";
        var error = new RuntimeException("Failed to set language");
        when(transcriptionService.setLanguage(testMeetingId, language))
            .thenReturn(Mono.error(error));
        
        // When
        mockMvc.perform(post("/api/v1/transcription/language/{meetingId}", testMeetingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"" + language + "\""))
            .andExpect(status().isInternalServerError());
        
        // Then
        verify(transcriptionService, times(1)).setLanguage(testMeetingId, language);
    }
    
    @Test
    @DisplayName("Should get language successfully")
    void shouldGetLanguageSuccessfully() throws Exception {
        // Given
        var expectedLanguage = "en-US";
        when(transcriptionService.getLanguage(testMeetingId))
            .thenReturn(Mono.just(expectedLanguage));
        
        // When
        var result = mockMvc.perform(get("/api/v1/transcription/language/{meetingId}", testMeetingId))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(transcriptionService, times(1)).getLanguage(testMeetingId);
        
        var responseJson = result.getResponse().getContentAsString();
        var responseLanguage = objectMapper.readValue(responseJson, String.class);
        
        assertEquals(expectedLanguage, responseLanguage);
    }
    
    @Test
    @DisplayName("Should set speaker diarization successfully")
    void shouldSetSpeakerDiarizationSuccessfully() throws Exception {
        // Given
        var enabled = true;
        when(transcriptionService.setSpeakerDiarization(testMeetingId, enabled))
            .thenReturn(Mono.empty());
        
        // When
        mockMvc.perform(post("/api/v1/transcription/speaker-diarization/{meetingId}", testMeetingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(enabled)))
            .andExpect(status().isOk());
        
        // Then
        verify(transcriptionService, times(1)).setSpeakerDiarization(testMeetingId, enabled);
    }
    
    @Test
    @DisplayName("Should get speaker diarization status successfully")
    void shouldGetSpeakerDiarizationStatusSuccessfully() throws Exception {
        // Given
        var expectedEnabled = true;
        when(transcriptionService.isSpeakerDiarizationEnabled(testMeetingId))
            .thenReturn(Mono.just(expectedEnabled));
        
        // When
        var result = mockMvc.perform(get("/api/v1/transcription/speaker-diarization/{meetingId}", testMeetingId))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(transcriptionService, times(1)).isSpeakerDiarizationEnabled(testMeetingId);
        
        var responseJson = result.getResponse().getContentAsString();
        var responseEnabled = objectMapper.readValue(responseJson, Boolean.class);
        
        assertEquals(expectedEnabled, responseEnabled);
    }
    
    @Test
    @DisplayName("Should set confidence threshold successfully")
    void shouldSetConfidenceThresholdSuccessfully() throws Exception {
        // Given
        var threshold = 0.8;
        when(transcriptionService.setConfidenceThreshold(testMeetingId, threshold))
            .thenReturn(Mono.empty());
        
        // When
        mockMvc.perform(post("/api/v1/transcription/confidence-threshold/{meetingId}", testMeetingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(threshold)))
            .andExpect(status().isOk());
        
        // Then
        verify(transcriptionService, times(1)).setConfidenceThreshold(testMeetingId, threshold);
    }
    
    @Test
    @DisplayName("Should get confidence threshold successfully")
    void shouldGetConfidenceThresholdSuccessfully() throws Exception {
        // Given
        var expectedThreshold = 0.7;
        when(transcriptionService.getConfidenceThreshold(testMeetingId))
            .thenReturn(Mono.just(expectedThreshold));
        
        // When
        var result = mockMvc.perform(get("/api/v1/transcription/confidence-threshold/{meetingId}", testMeetingId))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(transcriptionService, times(1)).getConfidenceThreshold(testMeetingId);
        
        var responseJson = result.getResponse().getContentAsString();
        var responseThreshold = objectMapper.readValue(responseJson, Double.class);
        
        assertEquals(expectedThreshold, responseThreshold);
    }
    
    @Test
    @DisplayName("Should get transcription statistics successfully")
    void shouldGetTranscriptionStatisticsSuccessfully() throws Exception {
        // Given
        when(transcriptionService.getTranscriptionStats(testMeetingId))
            .thenReturn(Mono.just(testStats));
        
        // When
        var result = mockMvc.perform(get("/api/v1/transcription/stats/{meetingId}", testMeetingId))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(transcriptionService, times(1)).getTranscriptionStats(testMeetingId);
        
        var responseJson = result.getResponse().getContentAsString();
        var responseStats = objectMapper.readValue(responseJson, TranscriptionService.TranscriptionStats.class);
        
        assertEquals(testStats.meetingId(), responseStats.meetingId());
        assertEquals(testStats.totalSegments(), responseStats.totalSegments());
        assertEquals(testStats.totalWords(), responseStats.totalWords());
    }
    
    @Test
    @DisplayName("Should handle transcription statistics not found")
    void shouldHandleTranscriptionStatisticsNotFound() throws Exception {
        // Given
        when(transcriptionService.getTranscriptionStats(testMeetingId))
            .thenReturn(Mono.empty());
        
        // When
        mockMvc.perform(get("/api/v1/transcription/stats/{meetingId}", testMeetingId))
            .andExpect(status().isNotFound());
        
        // Then
        verify(transcriptionService, times(1)).getTranscriptionStats(testMeetingId);
    }
    
    @Test
    @DisplayName("Should export transcription successfully")
    void shouldExportTranscriptionSuccessfully() throws Exception {
        // Given
        var format = TranscriptionService.ExportFormat.TXT;
        var expectedData = new byte[]{1, 2, 3};
        when(transcriptionService.exportTranscription(testMeetingId, format))
            .thenReturn(Mono.just(expectedData));
        
        // When
        var result = mockMvc.perform(get("/api/v1/transcription/export/{meetingId}", testMeetingId)
                .param("format", format.name()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
            .andReturn();
        
        // Then
        verify(transcriptionService, times(1)).exportTranscription(testMeetingId, format);
        
        var responseData = result.getResponse().getContentAsByteArray();
        assertArrayEquals(expectedData, responseData);
    }
    
    @Test
    @DisplayName("Should handle export transcription failure")
    void shouldHandleExportTranscriptionFailure() throws Exception {
        // Given
        var format = TranscriptionService.ExportFormat.JSON;
        var error = new RuntimeException("Export failed");
        when(transcriptionService.exportTranscription(testMeetingId, format))
            .thenReturn(Mono.error(error));
        
        // When
        mockMvc.perform(get("/api/v1/transcription/export/{meetingId}", testMeetingId)
                .param("format", format.name()))
            .andExpect(status().isInternalServerError());
        
        // Then
        verify(transcriptionService, times(1)).exportTranscription(testMeetingId, format);
    }
    
    @Test
    @DisplayName("Should process audio data successfully")
    void shouldProcessAudioDataSuccessfully() throws Exception {
        // Given
        var audioData = new byte[]{1, 2, 3, 4, 5};
        when(transcriptionService.processAudio(eq(testMeetingId), any(byte[].class)))
            .thenReturn(Flux.fromIterable(testSegments));
        
        // When
        var result = mockMvc.perform(post("/api/v1/transcription/process/{meetingId}", testMeetingId)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(audioData))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(transcriptionService, times(1)).processAudio(eq(testMeetingId), eq(audioData));
        
        var responseJson = result.getResponse().getContentAsString();
        var responseSegments = objectMapper.readValue(responseJson, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, TranscriptionSegment.class));
        
        assertEquals(testSegments.size(), responseSegments.size());
    }
    
    @Test
    @DisplayName("Should handle invalid UUID format")
    void shouldHandleInvalidUuidFormat() throws Exception {
        // When
        mockMvc.perform(get("/api/v1/transcription/status/{meetingId}", "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should handle empty request body")
    void shouldHandleEmptyRequestBody() throws Exception {
        // When
        mockMvc.perform(post("/api/v1/transcription/start/{meetingId}", testMeetingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should handle invalid JSON")
    void shouldHandleInvalidJSON() throws Exception {
        // When
        mockMvc.perform(post("/api/v1/transcription/start/{meetingId}", testMeetingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should handle missing content type")
    void shouldHandleMissingContentType() throws Exception {
        // When
        mockMvc.perform(post("/api/v1/transcription/start/{meetingId}", testMeetingId)
                .content(objectMapper.writeValueAsString(testConfig)))
            .andExpect(status().isUnsupportedMediaType());
    }
    
    @Test
    @DisplayName("Should handle CORS headers")
    void shouldHandleCorsHeaders() throws Exception {
        // When
        mockMvc.perform(options("/api/v1/transcription/status/{meetingId}", testMeetingId))
            .andExpect(status().isOk())
            .andExpect(header().exists("Access-Control-Allow-Origin"))
            .andExpect(header().exists("Access-Control-Allow-Methods"))
            .andExpect(header().exists("Access-Control-Allow-Headers"));
    }
    
    @Test
    @DisplayName("Should handle concurrent requests")
    void shouldHandleConcurrentRequests() throws Exception {
        // Given
        when(transcriptionService.getTranscriptionStatus(testMeetingId))
            .thenReturn(Mono.just(TranscriptionService.TranscriptionStatus.ACTIVE));
        when(transcriptionService.getAllTranscriptionSegments(testMeetingId))
            .thenReturn(Flux.fromIterable(testSegments));
        
        // When - make multiple requests
        var statusResult = mockMvc.perform(get("/api/v1/transcription/status/{meetingId}", testMeetingId));
        var segmentsResult = mockMvc.perform(get("/api/v1/transcription/segments/{meetingId}", testMeetingId));
        
        // Then - both should complete successfully
        statusResult.andExpect(status().isOk());
        segmentsResult.andExpect(status().isOk());
        
        verify(transcriptionService, times(1)).getTranscriptionStatus(testMeetingId);
        verify(transcriptionService, times(1)).getAllTranscriptionSegments(testMeetingId);
    }
    
    @Test
    @DisplayName("Should handle large audio data")
    void shouldHandleLargeAudioData() throws Exception {
        // Given
        var largeAudioData = new byte[1024 * 1024]; // 1MB
        when(transcriptionService.processAudio(eq(testMeetingId), any(byte[].class)))
            .thenReturn(Flux.empty());
        
        // When
        mockMvc.perform(post("/api/v1/transcription/process/{meetingId}", testMeetingId)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(largeAudioData))
            .andExpect(status().isOk());
        
        // Then
        verify(transcriptionService, times(1)).processAudio(eq(testMeetingId), eq(largeAudioData));
    }
    
    // Helper methods
    
    private TranscriptionSegment createTestTranscriptionSegment(long startTime, long endTime, String text, double confidence) {
        return new TranscriptionSegment(
            UUID.randomUUID(),
            testMeetingId,
            startTime,
            endTime,
            text,
            confidence,
            0,
            LocalDateTime.now()
        );
    }
}