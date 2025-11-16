package com.zoomtranscriber.config;

import com.zoomtranscriber.core.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GlobalExceptionHandler class.
 * Tests exception handling for various exception types and error responses.
 */
class GlobalExceptionHandlerTest {
    
    private GlobalExceptionHandler globalExceptionHandler;
    private WebTestClient webTestClient;
    
    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        
        // Setup WebTestClient for testing REST endpoints
        webTestClient = WebTestClient.bindToController(
            new TestController(globalExceptionHandler)
        ).build();
    }
    
    @Test
    @DisplayName("Should handle AudioException correctly")
    void shouldHandleAudioExceptionCorrectly() {
        AudioException exception = new AudioException("Audio processing failed");
        
        Mono<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleAudioException(exception);
        
        GlobalExceptionHandler.ErrorResponse errorResponse = response.block();
        
        assertNotNull(errorResponse);
        assertEquals("AUDIO_ERROR", errorResponse.errorType());
        assertEquals("Audio processing failed", errorResponse.message());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorResponse.statusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), errorResponse.statusReason());
        assertNotNull(errorResponse.timestamp());
        assertTrue(errorResponse.details().containsKey("errorCode"));
        assertTrue(errorResponse.details().containsKey("component"));
    }
    
    @Test
    @DisplayName("Should handle TranscriptionException correctly")
    void shouldHandleTranscriptionExceptionCorrectly() {
        TranscriptionException exception = new TranscriptionException(
            "Transcription failed",
            java.util.UUID.randomUUID()
        );
        
        Mono<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleTranscriptionException(exception);
        
        GlobalExceptionHandler.ErrorResponse errorResponse = response.block();
        
        assertNotNull(errorResponse);
        assertEquals("TRANSCRIPTION_ERROR", errorResponse.errorType());
        assertEquals("Transcription failed", errorResponse.message());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorResponse.statusCode());
        assertTrue(errorResponse.details().containsKey("meetingId"));
    }
    
    @Test
    @DisplayName("Should handle ValidationException correctly")
    void shouldHandleValidationExceptionCorrectly() {
        java.util.Map<String, String> validationErrors = java.util.Map.of(
            "field1", "Field is required",
            "field2", "Invalid format"
        );
        ValidationException exception = new ValidationException("Validation failed", validationErrors);
        
        Mono<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleValidationException(exception);
        
        GlobalExceptionHandler.ErrorResponse errorResponse = response.block();
        
        assertNotNull(errorResponse);
        assertEquals("VALIDATION_ERROR", errorResponse.errorType());
        assertEquals("Validation failed", errorResponse.message());
        assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.statusCode());
        assertTrue(errorResponse.details().containsKey("validationErrors"));
        assertTrue(errorResponse.details().containsKey("errorCount"));
        assertEquals(2, errorResponse.details().get("errorCount"));
    }
    
    @Test
    @DisplayName("Should handle NetworkException correctly")
    void shouldHandleNetworkExceptionCorrectly() {
        NetworkException exception = new NetworkException(
            "Network connection failed",
            "example.com",
            8080,
            5000
        );
        
        Mono<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleNetworkException(exception);
        
        GlobalExceptionHandler.ErrorResponse errorResponse = response.block();
        
        assertNotNull(errorResponse);
        assertEquals("NETWORK_ERROR", errorResponse.errorType());
        assertEquals("Network connection failed", errorResponse.message());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), errorResponse.statusCode());
        assertTrue(errorResponse.details().containsKey("host"));
        assertTrue(errorResponse.details().containsKey("port"));
        assertTrue(errorResponse.details().containsKey("timeoutMs"));
    }
    
    @Test
    @DisplayName("Should handle AIServiceException correctly")
    void shouldHandleAIServiceExceptionCorrectly() {
        AIServiceException exception = new AIServiceException(
            "AI service failed",
            "AI_SERVICE_002",
            "OLLAMA",
            "whisper-model"
        );
        
        Mono<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleAIServiceException(exception);
        
        GlobalExceptionHandler.ErrorResponse errorResponse = response.block();
        
        assertNotNull(errorResponse);
        assertEquals("AI_SERVICE_ERROR", errorResponse.errorType());
        assertEquals("AI service failed", errorResponse.message());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), errorResponse.statusCode());
        assertTrue(errorResponse.details().containsKey("service"));
        assertTrue(errorResponse.details().containsKey("model"));
    }
    
    @Test
    @DisplayName("Should handle ExternalServiceException correctly")
    void shouldHandleExternalServiceExceptionCorrectly() {
        Map<String, Object> context = Map.of("attempt", 3);
        ExternalServiceException exception = new ExternalServiceException(
            "External service call failed",
            "EXTERNAL_SERVICE_003",
            "EXTERNAL_API",
            "external-api-endpoint",
            503,
            context
        );
        
        Mono<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleExternalServiceException(exception);
        
        GlobalExceptionHandler.ErrorResponse errorResponse = response.block();
        
        assertNotNull(errorResponse);
        assertEquals("EXTERNAL_SERVICE_ERROR", errorResponse.errorType());
        assertEquals("External service call failed", errorResponse.message());
        assertEquals(HttpStatus.BAD_GATEWAY.value(), errorResponse.statusCode());
        assertTrue(errorResponse.details().containsKey("serviceName"));
        assertTrue(errorResponse.details().containsKey("endpoint"));
        assertTrue(errorResponse.details().containsKey("httpStatusCode"));
        assertTrue(errorResponse.details().containsKey("context"));
    }
    
    @Test
    @DisplayName("Should handle ConfigurationException correctly")
    void shouldHandleConfigurationExceptionCorrectly() {
        ConfigurationException exception = new ConfigurationException(
            "Configuration invalid",
            "CONFIGURATION_002",
            "CONFIG_SERVICE",
            "timeout.setting"
        );
        
        Mono<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleConfigurationException(exception);
        
        GlobalExceptionHandler.ErrorResponse errorResponse = response.block();
        
        assertNotNull(errorResponse);
        assertEquals("CONFIGURATION_ERROR", errorResponse.errorType());
        assertEquals("Configuration invalid", errorResponse.message());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorResponse.statusCode());
        assertTrue(errorResponse.details().containsKey("configKey"));
    }
    
    @Test
    @DisplayName("Should handle generic RuntimeException correctly")
    void shouldHandleRuntimeExceptionCorrectly() {
        RuntimeException exception = new RuntimeException("Unexpected error");
        
        Mono<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleRuntimeException(exception);
        
        GlobalExceptionHandler.ErrorResponse errorResponse = response.block();
        
        assertNotNull(errorResponse);
        assertEquals("INTERNAL_ERROR", errorResponse.errorType());
        assertEquals("An unexpected error occurred while processing your request", errorResponse.message());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorResponse.statusCode());
        assertTrue(errorResponse.details().containsKey("errorCode"));
        assertTrue(errorResponse.details().containsKey("component"));
        assertTrue(errorResponse.details().containsKey("details"));
    }
    
    @Test
    @DisplayName("Should create error response with all fields")
    void shouldCreateErrorResponseWithAllFields() {
        String errorType = "TEST_ERROR";
        String message = "Test message";
        Map<String, Object> details = Map.of("detail1", "value1", "detail2", "value2");
        
        GlobalExceptionHandler.ErrorResponse response = 
            new GlobalExceptionHandler.ErrorResponse(
                errorType,
                message,
                500,
                "Internal Server Error",
                java.time.LocalDateTime.now(),
                details
            );
        
        assertEquals(errorType, response.errorType());
        assertEquals(message, response.message());
        assertEquals(500, response.statusCode());
        assertEquals("Internal Server Error", response.statusReason());
        assertNotNull(response.timestamp());
        assertEquals(details, response.details());
    }
    
    @Test
    @DisplayName("Should convert error response to log message")
    void shouldConvertErrorResponseToLogMessage() {
        GlobalExceptionHandler.ErrorResponse response = 
            new GlobalExceptionHandler.ErrorResponse(
                "TEST_ERROR",
                "Test message",
                400,
                "Bad Request",
                java.time.LocalDateTime.now(),
                Map.of()
            );
        
        String logMessage = response.toLogMessage();
        
        assertTrue(logMessage.contains("[TEST_ERROR]"));
        assertTrue(logMessage.contains("400"));
        assertTrue(logMessage.contains("Test message"));
    }
    
    /**
     * Test controller for exception handling integration tests.
     */
    private static class TestController {
        
        private final GlobalExceptionHandler globalExceptionHandler;
        
        public TestController(GlobalExceptionHandler globalExceptionHandler) {
            this.globalExceptionHandler = globalExceptionHandler;
        }
        
        @org.springframework.web.bind.annotation.GetMapping("/test/audio-error")
        public Mono<org.springframework.http.ResponseEntity<GlobalExceptionHandler.ErrorResponse>> testAudioError() {
            return globalExceptionHandler.handleAudioException(new AudioException("Test audio error"));
        }
        
        @org.springframework.web.bind.annotation.GetMapping("/test/validation-error")
        public Mono<org.springframework.http.ResponseEntity<GlobalExceptionHandler.ErrorResponse>> testValidationError() {
            return globalExceptionHandler.handleValidationException(
                new ValidationException("Test validation error")
            );
        }
        
        @org.springframework.web.bind.annotation.GetMapping("/test/runtime-error")
        public Mono<org.springframework.http.ResponseEntity<GlobalExceptionHandler.ErrorResponse>> testRuntimeError() {
            return globalExceptionHandler.handleRuntimeException(new RuntimeException("Test runtime error"));
        }
    }
}