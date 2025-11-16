package com.zoomtranscriber.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoomtranscriber.config.AudioConfig;
import com.zoomtranscriber.config.OllamaConfig;
import com.zoomtranscriber.core.storage.Configuration;
import com.zoomtranscriber.core.storage.ConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for API ConfigurationController class.
 * Tests REST API endpoints for configuration management.
 */
@WebMvcTest(ConfigurationController.class)
@DisplayName("API ConfigurationController Tests")
class ConfigurationControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private ConfigurationRepository configurationRepository;
    
    @MockBean
    private AudioConfig audioConfig;
    
    @MockBean
    private OllamaConfig ollamaConfig;
    
    private List<Configuration> allConfigurations;
    private Configuration testAudioConfig;
    private Configuration testOllamaConfig;
    private Map<String, Object> audioSettings;
    private Map<String, Object> ollamaSettings;
    
    @BeforeEach
    void setUp() {
        // Create test data
        testAudioConfig = new Configuration("audio.sampleRate", "44100");
        testOllamaConfig = new Configuration("ollama.model", "qwen2.5:0.5b");
        allConfigurations = List.of(testAudioConfig, testOllamaConfig);
        
        // Create settings maps
        audioSettings = Map.of(
            "deviceId", "Default",
            "sampleRate", 44100,
            "bufferSize", 4096,
            "noiseReduction", true,
            "autoGainControl", true
        );
        
        ollamaSettings = Map.of(
            "url", "http://localhost:11434",
            "model", "qwen2.5:0.5b",
            "temperature", 0.7,
            "maxTokens", 2048
        );
        
        // Setup default mock behaviors
        when(configurationRepository.findAll()).thenReturn(allConfigurations);
        when(configurationRepository.findByKey(any())).thenReturn(Optional.empty());
        when(configurationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(configurationRepository.deleteByKey(any())).thenReturn(true);
        
        when(audioConfig.getDeviceId()).thenReturn("Default");
        when(audioConfig.getSampleRate()).thenReturn(44100);
        when(audioConfig.getBufferSize()).thenReturn(4096);
        when(audioConfig.isNoiseReduction()).thenReturn(true);
        when(audioConfig.isAutoGainControl()).thenReturn(true);
        
        when(ollamaConfig.getUrl()).thenReturn("http://localhost:11434");
        when(ollamaConfig.getModel()).thenReturn("qwen2.5:0.5b");
        when(ollamaConfig.getTemperature()).thenReturn(0.7);
        when(ollamaConfig.getMaxTokens()).thenReturn(2048);
    }
    
    @Test
    @DisplayName("Should get all configurations successfully")
    void shouldGetAllConfigurationsSuccessfully() throws Exception {
        // Given
        when(configurationRepository.findAll()).thenReturn(allConfigurations);
        
        // When
        var result = mockMvc.perform(get("/api/v1/configuration"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(configurationRepository, times(1)).findAll();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseConfigs = objectMapper.readValue(responseJson, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, Configuration.class));
        
        assertEquals(2, responseConfigs.size());
        assertTrue(responseConfigs.stream().anyMatch(c -> "audio.sampleRate".equals(c.getKey())));
        assertTrue(responseConfigs.stream().anyMatch(c -> "ollama.model".equals(c.getKey())));
    }
    
    @Test
    @DisplayName("Should handle get all configurations error")
    void shouldHandleGetAllConfigurationsError() throws Exception {
        // Given
        when(configurationRepository.findAll()).thenThrow(new RuntimeException("Database error"));
        
        // When & Then
        mockMvc.perform(get("/api/v1/configuration"))
            .andExpect(status().isInternalServerError());
        
        verify(configurationRepository, times(1)).findAll();
    }
    
    @Test
    @DisplayName("Should get configuration by key successfully")
    void shouldGetConfigurationByKeySuccessfully() throws Exception {
        // Given
        var key = "audio.sampleRate";
        when(configurationRepository.findByKey(key)).thenReturn(Optional.of(testAudioConfig));
        
        // When
        var result = mockMvc.perform(get("/api/v1/configuration/{key}", key))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(configurationRepository, times(1)).findByKey(key);
        
        var responseJson = result.getResponse().getContentAsString();
        var responseConfig = objectMapper.readValue(responseJson, Configuration.class);
        
        assertEquals(key, responseConfig.getKey());
        assertEquals("44100", responseConfig.getValue());
    }
    
    @Test
    @DisplayName("Should handle configuration not found")
    void shouldHandleConfigurationNotFound() throws Exception {
        // Given
        var key = "nonexistent.key";
        when(configurationRepository.findByKey(key)).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(get("/api/v1/configuration/{key}", key))
            .andExpect(status().isNotFound());
        
        verify(configurationRepository, times(1)).findByKey(key);
    }
    
    @Test
    @DisplayName("Should save configuration successfully")
    void shouldSaveConfigurationSuccessfully() throws Exception {
        // Given
        var newConfig = new Configuration("test.key", "test.value");
        when(configurationRepository.save(any())).thenReturn(newConfig);
        
        // When
        var result = mockMvc.perform(post("/api/v1/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newConfig)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(configurationRepository, times(1)).save(newConfig);
        
        var responseJson = result.getResponse().getContentAsString();
        var responseConfig = objectMapper.readValue(responseJson, Configuration.class);
        
        assertEquals("test.key", responseConfig.getKey());
        assertEquals("test.value", responseConfig.getValue());
    }
    
    @Test
    @DisplayName("Should handle save configuration validation error")
    void shouldHandleSaveConfigurationValidationError() throws Exception {
        // Given
        var invalidConfig = new Configuration(null, "test.value"); // Invalid: null key
        
        // When & Then
        mockMvc.perform(post("/api/v1/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidConfig)))
            .andExpect(status().isBadRequest());
        
        verify(configurationRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should update configuration successfully")
    void shouldUpdateConfigurationSuccessfully() throws Exception {
        // Given
        var key = "audio.sampleRate";
        var updatedConfig = new Configuration(key, "48000");
        when(configurationRepository.findByKey(key)).thenReturn(Optional.of(testAudioConfig));
        when(configurationRepository.save(updatedConfig)).thenReturn(updatedConfig);
        
        // When
        var result = mockMvc.perform(put("/api/v1/configuration/{key}", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedConfig)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(configurationRepository, times(1)).findByKey(key);
        verify(configurationRepository, times(1)).save(updatedConfig);
        
        var responseJson = result.getResponse().getContentAsString();
        var responseConfig = objectMapper.readValue(responseJson, Configuration.class);
        
        assertEquals("48000", responseConfig.getValue());
    }
    
    @Test
    @DisplayName("Should handle update configuration not found")
    void shouldHandleUpdateConfigurationNotFound() throws Exception {
        // Given
        var key = "nonexistent.key";
        var updatedConfig = new Configuration(key, "updated.value");
        when(configurationRepository.findByKey(key)).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(put("/api/v1/configuration/{key}", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedConfig)))
            .andExpect(status().isNotFound());
        
        verify(configurationRepository, times(1)).findByKey(key);
        verify(configurationRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should delete configuration successfully")
    void shouldDeleteConfigurationSuccessfully() throws Exception {
        // Given
        var key = "audio.sampleRate";
        when(configurationRepository.findByKey(key)).thenReturn(Optional.of(testAudioConfig));
        when(configurationRepository.deleteByKey(key)).thenReturn(true);
        
        // When
        mockMvc.perform(delete("/api/v1/configuration/{key}", key))
            .andExpect(status().isNoContent());
        
        // Then
        verify(configurationRepository, times(1)).findByKey(key);
        verify(configurationRepository, times(1)).deleteByKey(key);
    }
    
    @Test
    @DisplayName("Should handle delete configuration not found")
    void shouldHandleDeleteConfigurationNotFound() throws Exception {
        // Given
        var key = "nonexistent.key";
        when(configurationRepository.findByKey(key)).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(delete("/api/v1/configuration/{key}", key))
            .andExpect(status().isNotFound());
        
        verify(configurationRepository, times(1)).findByKey(key);
        verify(configurationRepository, never()).deleteByKey(any());
    }
    
    @Test
    @DisplayName("Should get audio configuration successfully")
    void shouldGetAudioConfigurationSuccessfully() throws Exception {
        // When
        var result = mockMvc.perform(get("/api/v1/configuration/audio"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(audioConfig, times(1)).getDeviceId();
        verify(audioConfig, times(1)).getSampleRate();
        verify(audioConfig, times(1)).getBufferSize();
        verify(audioConfig, times(1)).isNoiseReduction();
        verify(audioConfig, times(1)).isAutoGainControl();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseSettings = objectMapper.readValue(responseJson, Object.class);
        
        assertNotNull(responseSettings);
    }
    
    @Test
    @DisplayName("Should update audio configuration successfully")
    void shouldUpdateAudioConfigurationSuccessfully() throws Exception {
        // Given
        var updatedSettings = Map.of(
            "deviceId", "Microphone 1",
            "sampleRate", 48000,
            "bufferSize", 8192,
            "noiseReduction", false,
            "autoGainControl", false
        );
        
        // When
        mockMvc.perform(put("/api/v1/configuration/audio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedSettings)))
            .andExpect(status().isOk());
        
        // Then
        verify(audioConfig, times(1)).setDeviceId("Microphone 1");
        verify(audioConfig, times(1)).setSampleRate(48000);
        verify(audioConfig, times(1)).setBufferSize(8192);
        verify(audioConfig, times(1)).setNoiseReduction(false);
        verify(audioConfig, times(1)).setAutoGainControl(false);
    }
    
    @Test
    @DisplayName("Should get Ollama configuration successfully")
    void shouldGetOllamaConfigurationSuccessfully() throws Exception {
        // When
        var result = mockMvc.perform(get("/api/v1/configuration/ollama"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        // Then
        verify(ollamaConfig, times(1)).getUrl();
        verify(ollamaConfig, times(1)).getModel();
        verify(ollamaConfig, times(1)).getTemperature();
        verify(ollamaConfig, times(1)).getMaxTokens();
        
        var responseJson = result.getResponse().getContentAsString();
        var responseSettings = objectMapper.readValue(responseJson, Object.class);
        
        assertNotNull(responseSettings);
    }
    
    @Test
    @DisplayName("Should update Ollama configuration successfully")
    void shouldUpdateOllamaConfigurationSuccessfully() throws Exception {
        // Given
        var updatedSettings = Map.of(
            "url", "http://localhost:11435",
            "model", "llama2:7b",
            "temperature", 0.8,
            "maxTokens", 4096
        );
        
        // When
        mockMvc.perform(put("/api/v1/configuration/ollama")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedSettings)))
            .andExpect(status().isOk());
        
        // Then
        verify(ollamaConfig, times(1)).setUrl("http://localhost:11435");
        verify(ollamaConfig, times(1)).setModel("llama2:7b");
        verify(ollamaConfig, times(1)).setTemperature(0.8);
        verify(ollamaConfig, times(1)).setMaxTokens(4096);
    }
    
    @Test
    @DisplayName("Should reset to defaults successfully")
    void shouldResetToDefaultsSuccessfully() throws Exception {
        // When
        mockMvc.perform(post("/api/v1/configuration/reset"))
            .andExpect(status().isOk());
        
        // Then
        verify(audioConfig, times(1)).resetToDefaults();
        verify(ollamaConfig, times(1)).resetToDefaults();
    }
    
    @Test
    @DisplayName("Should export configuration successfully")
    void shouldExportConfigurationSuccessfully() throws Exception {
        // When
        var result = mockMvc.perform(get("/api/v1/configuration/export"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
            .andReturn();
        
        // Then
        assertNotNull(result.getResponse().getContentAsByteArray());
        // Should be a valid JSON/YAML export
    }
    
    @Test
    @DisplayName("Should handle export configuration error")
    void shouldHandleExportConfigurationError() throws Exception {
        // Given
        when(configurationRepository.findAll()).thenThrow(new RuntimeException("Export failed"));
        
        // When & Then
        mockMvc.perform(get("/api/v1/configuration/export"))
            .andExpect(status().isInternalServerError());
        
        verify(configurationRepository, times(1)).findAll();
    }
    
    @Test
    @DisplayName("Should import configuration successfully")
    void shouldImportConfigurationSuccessfully() throws Exception {
        // Given
        var importData = Map.of(
            "audio.sampleRate", "48000",
            "ollama.model", "llama2:7b"
        );
        
        // When
        mockMvc.perform(post("/api/v1/configuration/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(importData)))
            .andExpect(status().isOk());
        
        // Then
        verify(configurationRepository, atLeastOnce()).save(any(Configuration.class));
    }
    
    @Test
    @DisplayName("Should handle import configuration error")
    void shouldHandleImportConfigurationError() throws Exception {
        // Given
        var importData = Map.of("key", "value");
        when(configurationRepository.save(any())).thenThrow(new RuntimeException("Import failed"));
        
        // When & Then
        mockMvc.perform(post("/api/v1/configuration/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(importData)))
            .andExpect(status().isInternalServerError());
        
        verify(configurationRepository, times(1)).save(any(Configuration.class));
    }
    
    @Test
    @DisplayName("Should validate configuration values")
    void shouldValidateConfigurationValues() throws Exception {
        // Given
        var invalidConfig = new Configuration("audio.sampleRate", "invalid-rate");
        
        // When & Then
        mockMvc.perform(post("/api/v1/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidConfig)))
            .andExpect(status().isBadRequest());
        
        verify(configurationRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should handle empty request body")
    void shouldHandleEmptyRequestBody() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should handle invalid JSON")
    void shouldHandleInvalidJSON() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should handle missing content type")
    void shouldHandleMissingContentType() throws Exception {
        // Given
        var newConfig = new Configuration("test.key", "test.value");
        
        // When & Then
        mockMvc.perform(post("/api/v1/configuration")
                .content(objectMapper.writeValueAsString(newConfig)))
            .andExpect(status().isUnsupportedMediaType());
    }
    
    @Test
    @DisplayName("Should handle CORS headers")
    void shouldHandleCorsHeaders() throws Exception {
        // When
        mockMvc.perform(options("/api/v1/configuration"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Access-Control-Allow-Origin"))
            .andExpect(header().exists("Access-Control-Allow-Methods"))
            .andExpect(header().exists("Access-Control-Allow-Headers"));
    }
    
    @Test
    @DisplayName("Should handle concurrent requests")
    void shouldHandleConcurrentRequests() throws Exception {
        // Given
        when(configurationRepository.findAll()).thenReturn(allConfigurations);
        
        // When - make multiple requests
        var getResult = mockMvc.perform(get("/api/v1/configuration"));
        var audioResult = mockMvc.perform(get("/api/v1/configuration/audio"));
        
        // Then - both should complete successfully
        getResult.andExpect(status().isOk());
        audioResult.andExpect(status().isOk());
        
        verify(configurationRepository, times(1)).findAll();
        verify(audioConfig, times(1)).getDeviceId();
    }
    
    @Test
    @DisplayName("Should handle configuration with special characters")
    void shouldHandleConfigurationWithSpecialCharacters() throws Exception {
        // Given
        var specialConfig = new Configuration("special.key", "value with 中文 and émoji 🎵");
        when(configurationRepository.save(any())).thenReturn(specialConfig);
        
        // When
        mockMvc.perform(post("/api/v1/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(specialConfig)))
            .andExpect(status().isCreated());
        
        // Then
        verify(configurationRepository, times(1)).save(specialConfig);
    }
    
    @Test
    @DisplayName("Should handle large configuration values")
    void shouldHandleLargeConfigurationValues() throws Exception {
        // Given
        var largeConfig = new Configuration("large.key", "A".repeat(10000)); // 10K characters
        when(configurationRepository.save(any())).thenReturn(largeConfig);
        
        // When
        mockMvc.perform(post("/api/v1/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeConfig)))
            .andExpect(status().isCreated());
        
        // Then
        verify(configurationRepository, times(1)).save(largeConfig);
    }
    
    @Test
    @DisplayName("Should handle configuration value type validation")
    void shouldHandleConfigurationValueTypeValidation() throws Exception {
        // Given
        var configSettings = Map.of(
            "audio.sampleRate", 44100,
            "audio.noiseReduction", true,
            "ollama.temperature", 0.7
        );
        
        // When
        mockMvc.perform(post("/api/v1/configuration/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(configSettings)))
            .andExpect(status().isOk());
        
        // Then
        verify(configurationRepository, atLeastOnce()).save(any(Configuration.class));
    }
}