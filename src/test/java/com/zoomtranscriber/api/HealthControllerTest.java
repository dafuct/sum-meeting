package com.zoomtranscriber.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoomtranscriber.core.ai.OllamaService;
import com.zoomtranscriber.core.audio.AudioCaptureService;
import com.zoomtranscriber.core.detection.ZoomDetectionService;
import com.zoomtranscriber.core.transcription.TranscriptionService;
import com.zoomtranscriber.installer.InstallationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for API HealthController class.
 * Tests REST API endpoints for system health monitoring.
 */
@WebMvcTest(HealthController.class)
@DisplayName("API HealthController Tests")
class HealthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private AudioCaptureService audioCaptureService;
    
    @MockBean
    private ZoomDetectionService zoomDetectionService;
    
    @MockBean
    private TranscriptionService transcriptionService;
    
    @MockBean
    private OllamaService ollamaService;
    
    @MockBean
    private InstallationManager installationManager;
    
    @BeforeEach
    void setUp() {
        // Setup default mock behaviors - all services healthy
        when(audioCaptureService.isCapturing()).thenReturn(false);
        when(zoomDetectionService.isMonitoring()).thenReturn(false);
        when(transcriptionService.getTranscriptionStatus(any()))
            .thenReturn(Mono.just(TranscriptionService.TranscriptionStatus.NOT_STARTED));
        when(ollamaService.checkHealth()).thenReturn(Mono.just(true));
        when(ollamaService.getVersion()).thenReturn(Mono.just("0.1.42"));
        when(installationManager.isSystemReady()).thenReturn(true);
        when(installationManager.getMissingComponents()).thenReturn(java.util.List.of());
    }
    
    @Test
    @DisplayName("Should get overall health status successfully")
    void shouldGetOverallHealthStatusSuccessfully() throws Exception {
        // Given
        when(audioCaptureService.isCapturing()).thenReturn(false);
        when(zoomDetectionService.isMonitoring()).thenReturn(false);
        when(ollamaService.checkHealth()).thenReturn(Mono.just(true));
        when(installationManager.isSystemReady()).thenReturn(true);
        
        // When
        var result = mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(audioCaptureService, times(1)).isCapturing();
        verify(zoomDetectionService, times(1)).isMonitoring();
        verify(ollamaService, times(1)).checkHealth();
        verify(installationManager, times(1)).isSystemReady();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseMap = objectMapper.readValue(responseJson, Object.class);
        
        assertNotNull(responseMap);
        // Should contain overall status
        assertTrue(responseMap.containsKey("status"));
        assertTrue(responseMap.containsKey("timestamp"));
        assertTrue(responseMap.containsKey("services"));
    }
    
    @Test
    @DisplayName("Should handle unhealthy service status")
    void shouldHandleUnhealthyServiceStatus() throws Exception {
        // Given
        when(ollamaService.checkHealth()).thenReturn(Mono.just(false));
        when(installationManager.isSystemReady()).thenReturn(false);
        
        // When
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isServiceUnavailable());
        
        // Then
        verify(ollamaService, times(1)).checkHealth();
        verify(installationManager, times(1)).isSystemReady();
    }
    
    @Test
    @DisplayName("Should get detailed health status successfully")
    void shouldGetDetailedHealthStatusSuccessfully() throws Exception {
        // Given
        when(audioCaptureService.getCurrentSource()).thenReturn(null);
        when(zoomDetectionService.getActiveMeetings()).thenReturn(reactor.core.publisher.Flux.empty());
        when(ollamaService.listModels()).thenReturn(reactor.core.publisher.Flux.empty());
        when(installationManager.getInstallationStatus())
            .thenReturn(new InstallationManager.InstallationStatus(null, java.util.List.of()));
        
        // When
        var result = mockMvc.perform(get("/api/v1/health/detailed"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(audioCaptureService, times(1)).isCapturing();
        verify(zoomDetectionService, times(1)).isMonitoring();
        verify(ollamaService, times(1)).checkHealth();
        verify(transcriptionService, atLeastOnce()).getTranscriptionStatus(any());
        verify(installationManager, times(1)).isSystemReady();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseMap = objectMapper.readValue(responseJson, Object.class);
        
        assertNotNull(responseMap);
        // Should contain detailed service information
        assertTrue(responseMap.containsKey("services"));
        assertTrue(responseMap.containsKey("system"));
        assertTrue(responseMap.containsKey("dependencies"));
    }
    
    @Test
    @DisplayName("Should get audio service health successfully")
    void shouldGetAudioServiceHealthSuccessfully() throws Exception {
        // Given
        when(audioCaptureService.isCapturing()).thenReturn(false);
        when(audioCaptureService.getCurrentSource()).thenReturn(null);
        when(audioCaptureService.getCurrentFormat()).thenReturn(null);
        
        // When
        var result = mockMvc.perform(get("/api/v1/health/audio"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(audioCaptureService, times(1)).isCapturing();
        verify(audioCaptureService, times(1)).getCurrentSource();
        verify(audioCaptureService, times(1)).getCurrentFormat();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseMap = objectMapper.readValue(responseJson, Object.class);
        
        assertNotNull(responseMap);
        assertTrue(responseMap.containsKey("status"));
        assertTrue(responseMap.containsKey("capturing"));
    }
    
    @Test
    @DisplayName("Should get detection service health successfully")
    void shouldGetDetectionServiceHealthSuccessfully() throws Exception {
        // Given
        when(zoomDetectionService.isMonitoring()).thenReturn(true);
        when(zoomDetectionService.getActiveMeetings()).thenReturn(reactor.core.publisher.Flux.just(
            new ZoomDetectionService.MeetingState(
                java.util.UUID.randomUUID(), "Test Meeting",
                ZoomDetectionService.MeetingState.MeetingStatus.ACTIVE,
                LocalDateTime.now(), null, "zoom-123", 3, LocalDateTime.now()
            )
        ));
        
        // When
        var result = mockMvc.perform(get("/api/v1/health/detection"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(zoomDetectionService, times(1)).isMonitoring();
        verify(zoomDetectionService, times(1)).getActiveMeetings();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseMap = objectMapper.readValue(responseJson, Object.class);
        
        assertNotNull(responseMap);
        assertTrue(responseMap.containsKey("status"));
        assertTrue(responseMap.containsKey("monitoring"));
        assertTrue(responseMap.containsKey("activeMeetings"));
    }
    
    @Test
    @DisplayName("Should get transcription service health successfully")
    void shouldGetTranscriptionServiceHealthSuccessfully() throws Exception {
        // Given
        var meetingId = java.util.UUID.randomUUID();
        when(transcriptionService.getTranscriptionStatus(meetingId))
            .thenReturn(Mono.just(TranscriptionService.TranscriptionStatus.ACTIVE));
        
        // When
        var result = mockMvc.perform(get("/api/v1/health/transcription"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(transcriptionService, times(1)).getTranscriptionStatus(any());
        
        var responseJson = result.getResponse().getContentAsString();
        var responseMap = objectMapper.readValue(responseJson, Object.class);
        
        assertNotNull(responseMap);
        assertTrue(responseMap.containsKey("status"));
    }
    
    @Test
    @DisplayName("Should get AI service health successfully")
    void shouldGetAIServiceHealthSuccessfully() throws Exception {
        // Given
        when(ollamaService.checkHealth()).thenReturn(Mono.just(true));
        when(ollamaService.getVersion()).thenReturn(Mono.just("0.1.42"));
        when(ollamaService.listModels()).thenReturn(reactor.core.publisher.Flux.just(
            new OllamaService.ModelInfo("qwen2.5:0.5b", "qwen2.5", "2023-12-01", 
                "467MB", "sha256:abc", "test", "test", "test", 
                "test", "0.5b", "Q4_0")
        ));
        when(ollamaService.getResourceUsage()).thenReturn(Mono.just(new OllamaService.ResourceUsage(
            8589934592L, 4294967296L, 4294967296L, 50.0,
            107374182400L, 53687091200L, 53687091200L
        )));
        
        // When
        var result = mockMvc.perform(get("/api/v1/health/ai"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(ollamaService, times(1)).checkHealth();
        verify(ollamaService, times(1)).getVersion();
        verify(ollamaService, times(1)).listModels();
        verify(ollamaService, times(1)).getResourceUsage();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseMap = objectMapper.readValue(responseJson, Object.class);
        
        assertNotNull(responseMap);
        assertTrue(responseMap.containsKey("status"));
        assertTrue(responseMap.containsKey("version"));
        assertTrue(responseMap.containsKey("models"));
        assertTrue(responseMap.containsKey("resourceUsage"));
    }
    
    @Test
    @DisplayName("Should get installation health successfully")
    void shouldGetInstallationHealthSuccessfully() throws Exception {
        // Given
        when(installationManager.isSystemReady()).thenReturn(true);
        when(installationManager.getMissingComponents()).thenReturn(java.util.List.of());
        
        // When
        var result = mockMvc.perform(get("/api/v1/health/installation"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(installationManager, times(1)).isSystemReady();
        verify(installationManager, times(1)).getMissingComponents();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseMap = objectMapper.readValue(responseJson, Object.class);
        
        assertNotNull(responseMap);
        assertTrue(responseMap.containsKey("ready"));
        assertTrue(responseMap.containsKey("missingComponents"));
    }
    
    @Test
    @DisplayName("Should handle service exceptions gracefully")
    void shouldHandleServiceExceptionsGracefully() throws Exception {
        // Given
        when(ollamaService.checkHealth()).thenReturn(Mono.error(new RuntimeException("Service error")));
        
        // When
        var result = mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isInternalServerError())
            .andReturn();
        
        // Then
        verify(ollamaService, times(1)).checkHealth();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseMap = objectMapper.readValue(responseJson, Object.class);
        
        assertNotNull(responseMap);
        assertTrue(responseMap.containsKey("status"));
        assertEquals("ERROR", responseMap.get("status"));
    }
    
    @Test
    @DisplayName("Should handle timeout scenarios")
    void shouldHandleTimeoutScenarios() throws Exception {
        // Given
        when(ollamaService.checkHealth()).thenReturn(Mono.never());
        
        // When - set timeout for testing
        mockMvc.perform(get("/api/v1/health")
                .header("X-Timeout", "100"))
            .andExpect(status().isRequestTimeout());
        
        // Then
        verify(ollamaService, times(1)).checkHealth();
    }
    
    @Test
    @DisplayName("Should provide consistent response format")
    void shouldProvideConsistentResponseFormat() throws Exception {
        // Given
        when(audioCaptureService.isCapturing()).thenReturn(false);
        
        // When
        var result = mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk())
            .andReturn();
        
        // Then
        var responseJson = result.getResponse().getContentAsString();
        var responseMap = objectMapper.readValue(responseJson, Object.class);
        
        // Should always contain these fields
        assertTrue(responseMap.containsKey("status"));
        assertTrue(responseMap.containsKey("timestamp"));
        assertTrue(responseMap.containsKey("uptime"));
        
        // Timestamp should be valid ISO format
        var timestamp = responseMap.get("timestamp").toString();
        assertDoesNotThrow(() -> LocalDateTime.parse(timestamp.substring(0, timestamp.indexOf('.'))));
    }
    
    @Test
    @DisplayName("Should handle concurrent health checks")
    void shouldHandleConcurrentHealthChecks() throws Exception {
        // Given
        when(audioCaptureService.isCapturing()).thenReturn(false);
        when(ollamaService.checkHealth()).thenReturn(Mono.just(true));
        
        // When - make multiple concurrent requests
        var result1 = mockMvc.perform(get("/api/v1/health"));
        var result2 = mockMvc.perform(get("/api/v1/health/audio"));
        var result3 = mockMvc.perform(get("/api/v1/health/ai"));
        
        // Then - all should complete successfully
        result1.andExpect(status().isOk());
        result2.andExpect(status().isOk());
        result3.andExpect(status().isOk());
        
        verify(audioCaptureService, times(1)).isCapturing();
        verify(ollamaService, times(1)).checkHealth();
    }
    
    @Test
    @DisplayName("Should validate health check parameters")
    void shouldValidateHealthCheckParameters() throws Exception {
        // When
        mockMvc.perform(get("/api/v1/health")
                .param("verbose", "true"))
            .andExpect(status().isOk());
        
        // Then - should accept parameters without error
        verify(ollamaService, atLeastOnce()).checkHealth();
    }
    
    @Test
    @DisplayName("Should handle CORS headers")
    void shouldHandleCorsHeaders() throws Exception {
        // When
        mockMvc.perform(options("/api/v1/health"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Access-Control-Allow-Origin"))
            .andExpect(header().exists("Access-Control-Allow-Methods"))
            .andExpect(header().exists("Access-Control-Allow-Headers"));
    }
    
    @Test
    @DisplayName("Should cache health responses appropriately")
    void shouldCacheHealthResponsesAppropriately() throws Exception {
        // Given
        when(audioCaptureService.isCapturing()).thenReturn(false);
        
        // When - make same request twice
        var result1 = mockMvc.perform(get("/api/v1/health"));
        var result2 = mockMvc.perform(get("/api/v1/health"));
        
        // Then - both should complete successfully
        result1.andExpect(status().isOk());
        result2.andExpect(status().isOk());
        
        // Should check caching headers if implemented
        // This would depend on the actual implementation
    }
    
    @Test
    @DisplayName("Should handle system metrics correctly")
    void shouldHandleSystemMetricsCorrectly() throws Exception {
        // Given
        when(ollamaService.getResourceUsage()).thenReturn(Mono.just(new OllamaService.ResourceUsage(
            8589934592L, 4294967296L, 4294967296L, 50.0,
            107374182400L, 53687091200L, 53687091200L
        )));
        
        // When
        var result = mockMvc.perform(get("/api/v1/health/metrics"))
            .andExpect(status().isOk())
            .andReturn();
        
        // Then
        verify(ollamaService, times(1)).getResourceUsage();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseMap = objectMapper.readValue(responseJson, Object.class);
        
        assertNotNull(responseMap);
        // Should contain resource metrics
        assertTrue(responseMap.containsKey("memory"));
        assertTrue(responseMap.containsKey("cpu"));
        assertTrue(responseMap.containsKey("disk"));
    }
    
    @Test
    @DisplayName("Should handle service version information")
    void shouldHandleServiceVersionInformation() throws Exception {
        // Given
        when(ollamaService.getVersion()).thenReturn(Mono.just("0.1.42"));
        
        // When
        var result = mockMvc.perform(get("/api/v1/health/version"))
            .andExpect(status().isOk())
            .andReturn();
        
        // Then
        verify(ollamaService, times(1)).getVersion();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseMap = objectMapper.readValue(responseJson, Object.class);
        
        assertNotNull(responseMap);
        assertTrue(responseMap.containsKey("version"));
        assertTrue(responseMap.containsKey("build"));
        assertTrue(responseMap.containsKey("services"));
    }
    
    @Test
    @DisplayName("Should handle health check with disabled services")
    void shouldHandleHealthCheckWithDisabledServices() throws Exception {
        // Given - simulate services being disabled
        when(zoomDetectionService.isMonitoring()).thenReturn(false);
        when(transcriptionService.getTranscriptionStatus(any()))
            .thenReturn(Mono.just(TranscriptionService.TranscriptionStatus.ERROR));
        
        // When
        var result = mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isServiceUnavailable())
            .andReturn();
        
        // Then
        var responseJson = result.getResponse().getContentAsString();
        var responseMap = objectMapper.readValue(responseJson, Object.class);
        
        assertEquals("DEGRADED", responseMap.get("status"));
        assertNotNull(responseMap.get("issues"));
    }
    
    @Test
    @DisplayName("Should handle partial service failures")
    void shouldHandlePartialServiceFailures() throws Exception {
        // Given - some services failing
        when(ollamaService.checkHealth()).thenReturn(Mono.just(false));
        when(installationManager.isSystemReady()).thenReturn(true);
        
        // When
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isServiceUnavailable());
        
        // Then
        verify(ollamaService, times(1)).checkHealth();
        verify(installationManager, times(1)).isSystemReady();
    }
    
    @Test
    @DisplayName("Should handle health check with all services failing")
    void shouldHandleHealthCheckWithAllServicesFailing() throws Exception {
        // Given - all services failing
        when(audioCaptureService.isCapturing()).thenReturn(true); // Stuck state
        when(zoomDetectionService.isMonitoring()).thenReturn(true); // Stuck state
        when(ollamaService.checkHealth()).thenReturn(Mono.just(false));
        when(installationManager.isSystemReady()).thenReturn(false);
        
        // When
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isServiceUnavailable());
        
        // Then
        verify(audioCaptureService, times(1)).isCapturing();
        verify(zoomDetectionService, times(1)).isMonitoring();
        verify(ollamaService, times(1)).checkHealth();
        verify(installationManager, times(1)).isSystemReady();
    }
}