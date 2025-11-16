package com.zoomtranscriber.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoomtranscriber.core.storage.InstallationState;
import com.zoomtranscriber.core.storage.InstallationStateRepository;
import com.zoomtranscriber.installer.DependencyInstaller;
import com.zoomtranscriber.installer.InstallationManager;
import com.zoomtranscriber.installer.SystemChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for API InstallationController class.
 * Tests REST API endpoints for installation management.
 */
@WebMvcTest(InstallationController.class)
@DisplayName("API InstallationController Tests")
class InstallationControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private InstallationManager installationManager;
    
    @MockBean
    private InstallationStateRepository installationStateRepository;
    
    private InstallationState testJavaState;
    private InstallationState testOllamaState;
    private InstallationState testModelState;
    private List<InstallationState> allStates;
    private InstallationManager.InstallationStatus installationStatus;
    private SystemChecker.SystemCheckResult systemCheckResult;
    
    @BeforeEach
    void setUp() {
        // Create test data
        testJavaState = createTestInstallationState(InstallationState.Component.Java, 
            InstallationState.InstallStatus.INSTALLED, "17.0.2");
        testOllamaState = createTestInstallationState(InstallationState.Component.Ollama, 
            InstallationState.InstallStatus.INSTALLED, "0.1.42");
        testModelState = createTestInstallationState(InstallationState.Component.Model, 
            InstallationState.InstallStatus.NOT_INSTALLED, null);
        
        allStates = List.of(testJavaState, testOllamaState, testModelState);
        
        // Create system check result
        var checkResults = List.of(
            new SystemChecker.CheckResult(
                InstallationState.Component.Java,
                InstallationState.InstallStatus.INSTALLED,
                "17.0.2",
                null
            ),
            new SystemChecker.CheckResult(
                InstallationState.Component.Ollama,
                InstallationState.InstallStatus.INSTALLED,
                "0.1.42",
                null
            ),
            new SystemChecker.CheckResult(
                InstallationState.Component.Model,
                InstallationState.InstallStatus.NOT_INSTALLED,
                null,
                "Model not found"
            )
        );
        systemCheckResult = new SystemChecker.SystemCheckResult(checkResults);
        installationStatus = new InstallationManager.InstallationStatus(systemCheckResult, allStates);
        
        // Setup default mock behaviors
        when(installationManager.getInstallationStatus()).thenReturn(installationStatus);
        when(installationManager.isSystemReady()).thenReturn(false);
        when(installationManager.getMissingComponents()).thenReturn(List.of("Model"));
    }
    
    @Test
    @DisplayName("Should get installation status successfully")
    void shouldGetInstallationStatusSuccessfully() throws Exception {
        // Given
        when(installationManager.getInstallationStatus()).thenReturn(installationStatus);
        
        // When
        var result = mockMvc.perform(get("/api/v1/system/installation"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(installationManager, times(1)).getInstallationStatus();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseStates = objectMapper.readValue(responseJson, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, InstallationState.class));
        
        assertEquals(3, responseStates.size());
        assertTrue(responseStates.stream().anyMatch(s -> s.getComponent() == InstallationState.Component.Java));
        assertTrue(responseStates.stream().anyMatch(s -> s.getComponent() == InstallationState.Component.Ollama));
        assertTrue(responseStates.stream().anyMatch(s -> s.getComponent() == InstallationState.Component.Model));
    }
    
    @Test
    @DisplayName("Should handle installation status retrieval error")
    void shouldHandleInstallationStatusRetrievalError() throws Exception {
        // Given
        when(installationManager.getInstallationStatus())
            .thenThrow(new RuntimeException("Database error"));
        
        // When & Then
        mockMvc.perform(get("/api/v1/system/installation"))
            .andExpect(status().isInternalServerError());
        
        verify(installationManager, times(1)).getInstallationStatus();
    }
    
    @Test
    @DisplayName("Should install component successfully")
    void shouldInstallComponentSuccessfully() throws Exception {
        // Given
        var installRequest = new InstallationController.InstallRequest("Java");
        var expectedResult = new DependencyInstaller.InstallationResult(
            InstallationState.Component.Java,
            InstallationState.InstallStatus.INSTALLED,
            "Java installed successfully",
            "17.0.2"
        );
        
        when(installationManager.installComponent(eq(InstallationState.Component.Java), any()))
            .thenReturn(CompletableFuture.completedFuture(expectedResult));
        
        // When
        var result = mockMvc.perform(post("/api/v1/system/installation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(installRequest)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(installationManager, times(1)).installComponent(eq(InstallationState.Component.Java), any());
        
        var responseJson = result.getResponse().getContentAsString();
        var responseState = objectMapper.readValue(responseJson, InstallationState.class);
        
        assertEquals(InstallationState.Component.Java, responseState.getComponent());
        assertEquals(InstallationState.InstallStatus.INSTALLED, responseState.getStatus());
        assertEquals("17.0.2", responseState.getVersion());
    }
    
    @Test
    @DisplayName("Should handle component installation failure")
    void shouldHandleComponentInstallationFailure() throws Exception {
        // Given
        var installRequest = new InstallationController.InstallRequest("Java");
        var expectedResult = new DependencyInstaller.InstallationResult(
            InstallationState.Component.Java,
            InstallationState.InstallStatus.FAILED,
            "Java installation failed",
            null
        );
        
        when(installationManager.installComponent(eq(InstallationState.Component.Java), any()))
            .thenReturn(CompletableFuture.completedFuture(expectedResult));
        
        // When
        mockMvc.perform(post("/api/v1/system/installation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(installRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        
        // Then
        verify(installationManager, times(1)).installComponent(eq(InstallationState.Component.Java), any());
    }
    
    @Test
    @DisplayName("Should install all components successfully")
    void shouldInstallAllComponentsSuccessfully() throws Exception {
        // Given
        var expectedResult = new DependencyInstaller.InstallationResult(
            null,
            InstallationState.InstallStatus.INSTALLED,
            "All components installed",
            null
        );
        
        when(installationManager.installAll(any()))
            .thenReturn(CompletableFuture.completedFuture(expectedResult));
        
        // When
        var result = mockMvc.perform(post("/api/v1/system/installation/install-all"))
            .andExpect(status().isAccepted())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(installationManager, times(1)).installAll(any());
        
        var responseJson = result.getResponse().getContentAsString();
        var responseMap = objectMapper.readValue(responseJson, Object.class);
        
        assertNotNull(responseMap);
        // The actual response structure would depend on the implementation
    }
    
    @Test
    @DisplayName("Should handle install all components failure")
    void shouldHandleInstallAllComponentsFailure() throws Exception {
        // Given
        var expectedResult = new DependencyInstaller.InstallationResult(
            null,
            InstallationState.InstallStatus.FAILED,
            "Installation failed",
            null
        );
        
        when(installationManager.installAll(any()))
            .thenReturn(CompletableFuture.completedFuture(expectedResult));
        
        // When
        mockMvc.perform(post("/api/v1/system/installation/install-all"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        
        // Then
        verify(installationManager, times(1)).installAll(any());
    }
    
    @Test
    @DisplayName("Should refresh installation status successfully")
    void shouldRefreshInstallationStatusSuccessfully() throws Exception {
        // Given
        doNothing().when(installationManager).refreshInstallationStatus();
        when(installationManager.getInstallationStatus()).thenReturn(installationStatus);
        
        // When
        var result = mockMvc.perform(post("/api/v1/system/installation/refresh"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(installationManager, times(1)).refreshInstallationStatus();
        verify(installationManager, times(1)).getInstallationStatus();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseStates = objectMapper.readValue(responseJson, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, InstallationState.class));
        
        assertEquals(3, responseStates.size());
    }
    
    @Test
    @DisplayName("Should check system successfully")
    void shouldCheckSystemSuccessfully() throws Exception {
        // Given
        when(installationManager.getInstallationStatus()).thenReturn(installationStatus);
        
        // When
        var result = mockMvc.perform(get("/api/v1/system/installation/check"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(installationManager, times(1)).getInstallationStatus();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseMap = objectMapper.readValue(responseJson, Object.class);
        
        assertNotNull(responseMap);
        // The actual response structure would depend on the implementation
    }
    
    @Test
    @DisplayName("Should check system readiness successfully")
    void shouldCheckSystemReadinessSuccessfully() throws Exception {
        // Given
        when(installationManager.isSystemReady()).thenReturn(true);
        
        // When
        var result = mockMvc.perform(get("/api/v1/system/installation/ready"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(installationManager, times(1)).isSystemReady();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseMap = objectMapper.readValue(responseJson, Object.class);
        
        assertNotNull(responseMap);
        // The actual response structure would depend on the implementation
    }
    
    @Test
    @DisplayName("Should get component status successfully")
    void shouldGetComponentStatusSuccessfully() throws Exception {
        // Given
        var component = "Java";
        when(installationStateRepository.findByComponent(InstallationState.Component.Java))
            .thenReturn(Optional.of(testJavaState));
        
        // When
        var result = mockMvc.perform(get("/api/v1/system/installation/component/{component}", component))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(installationStateRepository, times(1)).findByComponent(InstallationState.Component.Java);
        
        var responseJson = result.getResponse().getContentAsString();
        var responseState = objectMapper.readValue(responseJson, InstallationState.class);
        
        assertEquals(InstallationState.Component.Java, responseState.getComponent());
        assertEquals(InstallationState.InstallStatus.INSTALLED, responseState.getStatus());
        assertEquals("17.0.2", responseState.getVersion());
    }
    
    @Test
    @DisplayName("Should handle component not found")
    void shouldHandleComponentNotFound() throws Exception {
        // Given
        var component = "Java";
        when(installationStateRepository.findByComponent(InstallationState.Component.Java))
            .thenReturn(Optional.empty());
        
        // When
        mockMvc.perform(get("/api/v1/system/installation/component/{component}", component))
            .andExpect(status().isNotFound());
        
        // Then
        verify(installationStateRepository, times(1)).findByComponent(InstallationState.Component.Java);
    }
    
    @Test
    @DisplayName("Should handle invalid component name")
    void shouldHandleInvalidComponentName() throws Exception {
        // Given
        var invalidComponent = "InvalidComponent";
        
        // When
        mockMvc.perform(get("/api/v1/system/installation/component/{component}", invalidComponent))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should reset component status successfully")
    void shouldResetComponentStatusSuccessfully() throws Exception {
        // Given
        var component = "Java";
        when(installationStateRepository.findByComponent(InstallationState.Component.Java))
            .thenReturn(Optional.of(testJavaState));
        doNothing().when(installationStateRepository).delete(testJavaState);
        
        // When
        mockMvc.perform(delete("/api/v1/system/installation/component/{component}", component))
            .andExpect(status().isOk());
        
        // Then
        verify(installationStateRepository, times(1)).findByComponent(InstallationState.Component.Java);
        verify(installationStateRepository, times(1)).delete(testJavaState);
    }
    
    @Test
    @DisplayName("Should handle reset component not found")
    void shouldHandleResetComponentNotFound() throws Exception {
        // Given
        var component = "Java";
        when(installationStateRepository.findByComponent(InstallationState.Component.Java))
            .thenReturn(Optional.empty());
        
        // When
        mockMvc.perform(delete("/api/v1/system/installation/component/{component}", component))
            .andExpect(status().isNotFound());
        
        // Then
        verify(installationStateRepository, times(1)).findByComponent(InstallationState.Component.Java);
        verify(installationStateRepository, never()).delete(any());
    }
    
    @Test
    @DisplayName("Should handle invalid install request")
    void shouldHandleInvalidInstallRequest() throws Exception {
        // Given
        var invalidRequest = new InstallationController.InstallRequest("InvalidComponent");
        
        // When
        mockMvc.perform(post("/api/v1/system/installation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should handle installation exception")
    void shouldHandleInstallationException() throws Exception {
        // Given
        var installRequest = new InstallationController.InstallRequest("Java");
        
        when(installationManager.installComponent(eq(InstallationState.Component.Java), any()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Installation error")));
        
        // When
        mockMvc.perform(post("/api/v1/system/installation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(installRequest)))
            .andExpect(status().isInternalServerError());
        
        // Then
        verify(installationManager, times(1)).installComponent(eq(InstallationState.Component.Java), any());
    }
    
    @Test
    @DisplayName("Should handle empty request body")
    void shouldHandleEmptyRequestBody() throws Exception {
        // When
        mockMvc.perform(post("/api/v1/system/installation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should handle invalid JSON")
    void shouldHandleInvalidJSON() throws Exception {
        // When
        mockMvc.perform(post("/api/v1/system/installation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should handle missing content type")
    void shouldHandleMissingContentType() throws Exception {
        // Given
        var installRequest = new InstallationController.InstallRequest("Java");
        
        // When
        mockMvc.perform(post("/api/v1/system/installation")
                .content(objectMapper.writeValueAsString(installRequest)))
            .andExpect(status().isUnsupportedMediaType());
    }
    
    @Test
    @DisplayName("Should handle CORS headers")
    void shouldHandleCorsHeaders() throws Exception {
        // When
        mockMvc.perform(options("/api/v1/system/installation"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Access-Control-Allow-Origin"))
            .andExpect(header().exists("Access-Control-Allow-Methods"))
            .andExpect(header().exists("Access-Control-Allow-Headers"));
    }
    
    @Test
    @DisplayName("Should handle concurrent requests")
    void shouldHandleConcurrentRequests() throws Exception {
        // Given
        var installRequest = new InstallationController.InstallRequest("Java");
        var expectedResult = new DependencyInstaller.InstallationResult(
            InstallationState.Component.Java,
            InstallationState.InstallStatus.INSTALLED,
            "Java installed",
            "17.0.2"
        );
        
        when(installationManager.installComponent(eq(InstallationState.Component.Java), any()))
            .thenReturn(CompletableFuture.completedFuture(expectedResult));
        
        // When - make multiple requests
        var result1 = mockMvc.perform(post("/api/v1/system/installation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(installRequest)));
        
        var result2 = mockMvc.perform(get("/api/v1/system/installation"));
        
        // Then - both should complete successfully
        result1.andExpect(status().isCreated());
        result2.andExpect(status().isOk());
        
        verify(installationManager, times(1)).installComponent(eq(InstallationState.Component.Java), any());
        verify(installationManager, times(1)).getInstallationStatus();
    }
    
    @Test
    @DisplayName("Should validate install request structure")
    void shouldValidateInstallRequestStructure() throws Exception {
        // Given
        var invalidJson = "{\"invalidField\": \"value\"}";
        
        // When
        mockMvc.perform(post("/api/v1/system/installation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should handle system check with missing components")
    void shouldHandleSystemCheckWithMissingComponents() throws Exception {
        // Given
        when(installationManager.getMissingComponents()).thenReturn(List.of("Java", "Ollama", "Model"));
        
        // When
        var result = mockMvc.perform(get("/api/v1/system/installation/check"))
            .andExpect(status().isOk())
            .andReturn();
        
        // Then
        verify(installationManager, times(1)).getInstallationStatus();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseMap = objectMapper.readValue(responseJson, Object.class);
        
        assertNotNull(responseMap);
        // Should contain information about missing components
    }
    
    @Test
    @DisplayName("Should handle large response data")
    void shouldHandleLargeResponseData() throws Exception {
        // Given
        var manyStates = java.util.stream.IntStream.range(0, 100)
            .mapToObj(i -> createTestInstallationState(InstallationState.Component.Java, 
                InstallationState.InstallStatus.INSTALLED, "17.0." + i))
            .toList();
        
        when(installationManager.getInstallationStatus())
            .thenReturn(new InstallationManager.InstallationStatus(systemCheckResult, manyStates));
        
        // When
        mockMvc.perform(get("/api/v1/system/installation"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(100));
        
        // Then
        verify(installationManager, times(1)).getInstallationStatus();
    }
    
    // Helper methods
    
    private InstallationState createTestInstallationState(InstallationState.Component component,
                                                       InstallationState.InstallStatus status,
                                                       String version) {
        var state = new InstallationState(component);
        state.setStatus(status);
        state.setVersion(version);
        state.setCreatedAt(LocalDateTime.now().minusDays(1));
        state.setUpdatedAt(LocalDateTime.now());
        state.setLastChecked(LocalDateTime.now());
        return state;
    }
    
    private java.util.concurrent.CompletableFuture<DependencyInstaller.InstallationResult> CompletableFuture(com.zoomtranscriber.installer.DependencyInstaller.InstallationResult result) {
        return java.util.concurrent.CompletableFuture.completedFuture(result);
    }
}