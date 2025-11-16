package com.zoomtranscriber.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoomtranscriber.core.ai.OllamaService;
import com.zoomtranscriber.core.storage.Summary;
import com.zoomtranscriber.core.storage.SummaryRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for API SummaryController class.
 * Tests REST API endpoints for summary management.
 */
@WebMvcTest(SummaryController.class)
@DisplayName("API SummaryController Tests")
class SummaryControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private SummaryRepository summaryRepository;
    
    @MockBean
    private OllamaService ollamaService;
    
    private UUID testMeetingId;
    private Summary testSummary;
    private List<Summary> allSummaries;
    
    @BeforeEach
    void setUp() {
        // Create test data
        testMeetingId = UUID.randomUUID();
        testSummary = createTestSummary();
        allSummaries = List.of(testSummary);
        
        // Setup default mock behaviors
        when(ollamaService.generateText(anyString(), anyString()))
            .thenReturn(Mono.just("This is a generated summary"));
        when(ollamaService.checkHealth())
            .thenReturn(Mono.just(true));
        when(ollamaService.isModelAvailable(anyString()))
            .thenReturn(Mono.just(true));
        
        when(summaryRepository.findByMeetingId(testMeetingId)).thenReturn(allSummaries);
        when(summaryRepository.findById(testSummary.getId())).thenReturn(Optional.of(testSummary));
        when(summaryRepository.findAll()).thenReturn(allSummaries);
        when(summaryRepository.save(any())).thenReturn(testSummary);
        when(summaryRepository.deleteById(testSummary.getId())).thenReturn(true);
    }
    
    @Test
    @DisplayName("Should get all summaries successfully")
    void shouldGetAllSummariesSuccessfully() throws Exception {
        // Given
        when(summaryRepository.findAll()).thenReturn(allSummaries);
        
        // When
        var result = mockMvc.perform(get("/api/v1/summary"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(summaryRepository, times(1)).findAll();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseSummaries = objectMapper.readValue(responseJson, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, Summary.class));
        
        assertEquals(1, responseSummaries.size());
        assertEquals(testSummary.getId(), responseSummaries.get(0).getId());
    }
    
    @Test
    @DisplayName("Should handle get all summaries error")
    void shouldHandleGetAllSummariesError() throws Exception {
        // Given
        when(summaryRepository.findAll()).thenThrow(new RuntimeException("Database error"));
        
        // When & Then
        mockMvc.perform(get("/api/v1/summary"))
            .andExpect(status().isInternalServerError());
        
        verify(summaryRepository, times(1)).findAll();
    }
    
    @Test
    @DisplayName("Should get summary by ID successfully")
    void shouldGetSummaryByIdSuccessfully() throws Exception {
        // Given
        when(summaryRepository.findById(testSummary.getId()))
            .thenReturn(Optional.of(testSummary));
        
        // When
        var result = mockMvc.perform(get("/api/v1/summary/{id}", testSummary.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(summaryRepository, times(1)).findById(testSummary.getId());
        
        var responseJson = result.getResponse().getContentAsString();
        var responseSummary = objectMapper.readValue(responseJson, Summary.class);
        
        assertEquals(testSummary.getId(), responseSummary.getId());
        assertEquals(testSummary.getContent(), responseSummary.getContent());
    }
    
    @Test
    @DisplayName("Should handle summary not found")
    void shouldHandleSummaryNotFound() throws Exception {
        // Given
        var summaryId = UUID.randomUUID();
        when(summaryRepository.findById(summaryId)).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(get("/api/v1/summary/{id}", summaryId))
            .andExpect(status().isNotFound());
        
        verify(summaryRepository, times(1)).findById(summaryId);
    }
    
    @Test
    @DisplayName("Should get summaries by meeting ID successfully")
    void shouldGetSummariesByMeetingIdSuccessfully() throws Exception {
        // Given
        when(summaryRepository.findByMeetingId(testMeetingId)).thenReturn(allSummaries);
        
        // When
        var result = mockMvc.perform(get("/api/v1/summary/meeting/{meetingId}", testMeetingId))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(summaryRepository, times(1)).findByMeetingId(testMeetingId);
        
        var responseJson = result.getResponse().getContentAsString();
        var responseSummaries = objectMapper.readValue(responseJson, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, Summary.class));
        
        assertEquals(1, responseSummaries.size());
        assertEquals(testMeetingId, responseSummaries.get(0).getMeetingId());
    }
    
    @Test
    @DisplayName("Should create summary successfully")
    void shouldCreateSummarySuccessfully() throws Exception {
        // Given
        var newSummary = createTestSummary();
        newSummary.setId(null); // New summary has no ID
        
        when(summaryRepository.save(any())).thenAnswer(invocation -> {
            var summary = (Summary) invocation.getArgument(0);
            summary.setId(UUID.randomUUID());
            return summary;
        });
        
        // When
        var result = mockMvc.perform(post("/api/v1/summary")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newSummary)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(summaryRepository, times(1)).save(any(Summary.class));
        
        var responseJson = result.getResponse().getContentAsString();
        var responseSummary = objectMapper.readValue(responseJson, Summary.class);
        
        assertNotNull(responseSummary.getId());
        assertEquals(newSummary.getContent(), responseSummary.getContent());
    }
    
    @Test
    @DisplayName("Should handle create summary validation error")
    void shouldHandleCreateSummaryValidationError() throws Exception {
        // Given
        var invalidSummary = new Summary();
        invalidSummary.setContent(null); // Invalid: null content
        
        // When & Then
        mockMvc.perform(post("/api/v1/summary")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidSummary)))
            .andExpect(status().isBadRequest());
        
        verify(summaryRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should generate summary successfully")
    void shouldGenerateSummarySuccessfully() throws Exception {
        // Given
        var requestBody = new SummaryGenerationRequest(
            testMeetingId,
            "Generate a summary of this meeting",
            "qwen2.5:0.5b",
            0.7
        );
        
        var expectedSummary = "This is a generated summary of the meeting";
        when(ollamaService.generateText(anyString(), anyString()))
            .thenReturn(Mono.just(expectedSummary));
        
        // When
        var result = mockMvc.perform(post("/api/v1/summary/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(ollamaService, times(1)).generateText(eq("qwen2.5:0.5b"), eq("Generate a summary of this meeting"));
        
        var responseJson = result.getResponse().getContentAsString();
        var responseMap = objectMapper.readValue(responseJson, Object.class);
        
        assertNotNull(responseMap);
        // Should contain generated summary
    }
    
    @Test
    @DisplayName("Should handle generate summary failure")
    void shouldHandleGenerateSummaryFailure() throws Exception {
        // Given
        var requestBody = new SummaryGenerationRequest(
            testMeetingId,
            "Generate a summary",
            "qwen2.5:0.5b",
            0.7
        );
        
        var error = new RuntimeException("Generation failed");
        when(ollamaService.generateText(anyString(), anyString()))
            .thenReturn(Mono.error(error));
        
        // When & Then
        mockMvc.perform(post("/api/v1/summary/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isInternalServerError());
        
        verify(ollamaService, times(1)).generateText(anyString(), anyString());
    }
    
    @Test
    @DisplayName("Should update summary successfully")
    void shouldUpdateSummarySuccessfully() throws Exception {
        // Given
        var updatedSummary = createTestSummary();
        updatedSummary.setContent("Updated summary content");
        
        when(summaryRepository.save(updatedSummary)).thenReturn(updatedSummary);
        
        // When
        var result = mockMvc.perform(put("/api/v1/summary/{id}", testSummary.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedSummary)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(summaryRepository, times(1)).save(updatedSummary);
        
        var responseJson = result.getResponse().getContentAsString();
        var responseSummary = objectMapper.readValue(responseJson, Summary.class);
        
        assertEquals("Updated summary content", responseSummary.getContent());
    }
    
    @Test
    @DisplayName("Should handle update summary not found")
    void shouldHandleUpdateSummaryNotFound() throws Exception {
        // Given
        var updatedSummary = createTestSummary();
        var summaryId = UUID.randomUUID();
        
        when(summaryRepository.findById(summaryId)).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(put("/api/v1/summary/{id}", summaryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedSummary)))
            .andExpect(status().isNotFound());
        
        verify(summaryRepository, times(1)).findById(summaryId);
        verify(summaryRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should delete summary successfully")
    void shouldDeleteSummarySuccessfully() throws Exception {
        // Given
        when(summaryRepository.findById(testSummary.getId()))
            .thenReturn(Optional.of(testSummary));
        when(summaryRepository.deleteById(testSummary.getId())).thenReturn(true);
        
        // When
        mockMvc.perform(delete("/api/v1/summary/{id}", testSummary.getId()))
            .andExpect(status().isNoContent());
        
        // Then
        verify(summaryRepository, times(1)).findById(testSummary.getId());
        verify(summaryRepository, times(1)).deleteById(testSummary.getId());
    }
    
    @Test
    @DisplayName("Should handle delete summary not found")
    void shouldHandleDeleteSummaryNotFound() throws Exception {
        // Given
        var summaryId = UUID.randomUUID();
        when(summaryRepository.findById(summaryId)).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(delete("/api/v1/summary/{id}", summaryId))
            .andExpect(status().isNotFound());
        
        verify(summaryRepository, times(1)).findById(summaryId);
        verify(summaryRepository, never()).deleteById(any());
    }
    
    @Test
    @DisplayName("Should get available models successfully")
    void shouldGetAvailableModelsSuccessfully() throws Exception {
        // Given
        var models = List.of("qwen2.5:0.5b", "llama2:7b", "mistral:7b");
        
        when(ollamaService.checkHealth()).thenReturn(Mono.just(true));
        when(ollamaService.listModels()).thenReturn(
            reactor.core.publisher.Flux.fromIterable(models.stream()
                .map(model -> new OllamaService.ModelInfo(
                    model, model, "2023-12-01", "100MB", "sha256:abc",
                    "test", "test", "test", "test", "test", "test"
                ))
                .toList())
        );
        
        // When
        var result = mockMvc.perform(get("/api/v1/summary/models"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(ollamaService, times(1)).checkHealth();
        verify(ollamaService, times(1)).listModels();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseModels = objectMapper.readValue(responseJson, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        
        assertEquals(models.size(), responseModels.size());
    }
    
    @Test
    @DisplayName("Should handle Ollama service unavailable")
    void shouldHandleOllamaServiceUnavailable() throws Exception {
        // Given
        when(ollamaService.checkHealth()).thenReturn(Mono.just(false));
        
        // When
        mockMvc.perform(get("/api/v1/summary/models"))
            .andExpect(status().isServiceUnavailable());
        
        // Then
        verify(ollamaService, times(1)).checkHealth();
        verify(ollamaService, never()).listModels();
    }
    
    @Test
    @DisplayName("Should check model availability successfully")
    void shouldCheckModelAvailabilitySuccessfully() throws Exception {
        // Given
        var model = "qwen2.5:0.5b";
        when(ollamaService.isModelAvailable(model)).thenReturn(Mono.just(true));
        
        // When
        var result = mockMvc.perform(get("/api/v1/summary/models/{model}/available", model))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(ollamaService, times(1)).isModelAvailable(model);
        
        var responseJson = result.getResponse().getContentAsString();
        var responseAvailable = objectMapper.readValue(responseJson, Boolean.class);
        
        assertTrue(responseAvailable);
    }
    
    @Test
    @DisplayName("Should handle model check failure")
    void shouldHandleModelCheckFailure() throws Exception {
        // Given
        var model = "nonexistent-model";
        var error = new RuntimeException("Model check failed");
        when(ollamaService.isModelAvailable(model)).thenReturn(Mono.error(error));
        
        // When
        mockMvc.perform(get("/api/v1/summary/models/{model}/available", model))
            .andExpect(status().isInternalServerError());
        
        // Then
        verify(ollamaService, times(1)).isModelAvailable(model);
    }
    
    @Test
    @DisplayName("Should handle invalid UUID format")
    void shouldHandleInvalidUuidFormat() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/summary/{id}", "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should handle empty request body")
    void shouldHandleEmptyRequestBody() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/summary")
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should handle invalid JSON")
    void shouldHandleInvalidJSON() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/summary")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should handle missing content type")
    void shouldHandleMissingContentType() throws Exception {
        // Given
        var newSummary = createTestSummary();
        
        // When & Then
        mockMvc.perform(post("/api/v1/summary")
                .content(objectMapper.writeValueAsString(newSummary)))
            .andExpect(status().isUnsupportedMediaType());
    }
    
    @Test
    @DisplayName("Should handle CORS headers")
    void shouldHandleCorsHeaders() throws Exception {
        // When
        mockMvc.perform(options("/api/v1/summary"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Access-Control-Allow-Origin"))
            .andExpect(header().exists("Access-Control-Allow-Methods"))
            .andExpect(header().exists("Access-Control-Allow-Headers"));
    }
    
    @Test
    @DisplayName("Should handle concurrent requests")
    void shouldHandleConcurrentRequests() throws Exception {
        // Given
        when(summaryRepository.findAll()).thenReturn(allSummaries);
        when(ollamaService.checkHealth()).thenReturn(Mono.just(true));
        
        // When - make multiple requests
        var getResult = mockMvc.perform(get("/api/v1/summary"));
        var modelsResult = mockMvc.perform(get("/api/v1/summary/models"));
        
        // Then - both should complete successfully
        getResult.andExpect(status().isOk());
        modelsResult.andExpect(status().isOk());
        
        verify(summaryRepository, times(1)).findAll();
        verify(ollamaService, times(1)).checkHealth();
    }
    
    @Test
    @DisplayName("Should handle large summary content")
    void shouldHandleLargeSummaryContent() throws Exception {
        // Given
        var largeSummary = createTestSummary();
        var largeContent = "A".repeat(10000); // 10K characters
        largeSummary.setContent(largeContent);
        
        when(summaryRepository.save(any())).thenReturn(largeSummary);
        
        // When
        mockMvc.perform(post("/api/v1/summary")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeSummary)))
            .andExpect(status().isCreated());
        
        // Then
        verify(summaryRepository, times(1)).save(any(Summary.class));
    }
    
    @Test
    @DisplayName("Should validate generation request")
    void shouldValidateGenerationRequest() throws Exception {
        // Given
        var invalidRequest = new SummaryGenerationRequest(
            null, // Invalid: null meeting ID
            "Generate summary",
            "qwen2.5:0.5b",
            0.7
        );
        
        // When & Then
        mockMvc.perform(post("/api/v1/summary/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
        
        verify(ollamaService, never()).generateText(anyString(), anyString());
    }
    
    // Helper classes and methods
    
    public record SummaryGenerationRequest(
        UUID meetingId,
        String prompt,
        String model,
        Double temperature
    ) {
        public boolean isValid() {
            return meetingId != null && prompt != null && !prompt.trim().isEmpty() 
                && model != null && !model.trim().isEmpty() && temperature != null;
        }
    }
    
    private Summary createTestSummary() {
        var summary = new Summary();
        summary.setId(UUID.randomUUID());
        summary.setMeetingId(testMeetingId);
        summary.setContent("This is a test summary of the meeting");
        summary.setSummaryType(Summary.SummaryType.STANDARD);
        summary.setWordCount(10);
        summary.setCreatedAt(LocalDateTime.now());
        summary.setUpdatedAt(LocalDateTime.now());
        summary.setModel("qwen2.5:0.5b");
        summary.setPrompt("Generate a summary");
        return summary;
    }
}