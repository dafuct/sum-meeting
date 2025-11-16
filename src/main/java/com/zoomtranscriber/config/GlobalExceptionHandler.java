package com.zoomtranscriber.config;

import com.zoomtranscriber.core.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Zoom Transcriber application.
 * Provides centralized error handling with proper HTTP status codes and structured error responses.
 * Integrates with logging and monitoring systems for comprehensive error tracking.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handles audio-related exceptions.
     * 
     * @param ex the audio exception
     * @return structured error response
     */
    @ExceptionHandler(AudioException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ErrorResponse> handleAudioException(AudioException ex) {
        logError(ex, "Audio processing error occurred");
        return Mono.just(createErrorResponse(
            "AUDIO_ERROR",
            ex.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR,
            Map.of(
                "errorCode", ex.getErrorCode(),
                "component", ex.getComponent()
            )
        ));
    }
    
    /**
     * Handles audio capture exceptions specifically.
     * 
     * @param ex the audio capture exception
     * @return structured error response
     */
    @ExceptionHandler(AudioCaptureException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Mono<ErrorResponse> handleAudioCaptureException(AudioCaptureException ex) {
        logError(ex, "Audio capture error occurred");
        
        Map<String, Object> details = new HashMap<>();
        details.put("errorCode", ex.getErrorCode());
        details.put("component", ex.getComponent());
        if (ex.getDeviceId() != null) {
            details.put("deviceId", ex.getDeviceId());
        }
        
        return Mono.just(createErrorResponse(
            "AUDIO_CAPTURE_ERROR",
            ex.getMessage(),
            HttpStatus.SERVICE_UNAVAILABLE,
            details
        ));
    }
    
    /**
     * Handles transcription-related exceptions.
     * 
     * @param ex the transcription exception
     * @return structured error response
     */
    @ExceptionHandler(TranscriptionException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ErrorResponse> handleTranscriptionException(TranscriptionException ex) {
        logError(ex, "Transcription error occurred");
        
        Map<String, Object> details = new HashMap<>();
        details.put("errorCode", ex.getErrorCode());
        details.put("component", ex.getComponent());
        if (ex.getMeetingId() != null) {
            details.put("meetingId", ex.getMeetingId());
        }
        
        return Mono.just(createErrorResponse(
            "TRANSCRIPTION_ERROR",
            ex.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR,
            details
        ));
    }
    
    /**
     * Handles speech recognition exceptions specifically.
     * 
     * @param ex the speech recognition exception
     * @return structured error response
     */
    @ExceptionHandler(SpeechRecognitionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorResponse> handleSpeechRecognitionException(SpeechRecognitionException ex) {
        logError(ex, "Speech recognition error occurred");
        
        Map<String, Object> details = new HashMap<>();
        details.put("errorCode", ex.getErrorCode());
        details.put("component", ex.getComponent());
        details.put("model", ex.getModel());
        details.put("confidence", ex.getConfidence());
        if (ex.getMeetingId() != null) {
            details.put("meetingId", ex.getMeetingId());
        }
        
        return Mono.just(createErrorResponse(
            "SPEECH_RECOGNITION_ERROR",
            ex.getMessage(),
            HttpStatus.BAD_REQUEST,
            details
        ));
    }
    
    /**
     * Handles AI service exceptions.
     * 
     * @param ex the AI service exception
     * @return structured error response
     */
    @ExceptionHandler(AIServiceException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Mono<ErrorResponse> handleAIServiceException(AIServiceException ex) {
        logError(ex, "AI service error occurred");
        
        Map<String, Object> details = new HashMap<>();
        details.put("errorCode", ex.getErrorCode());
        details.put("service", ex.getService());
        details.put("model", ex.getModel());
        
        return Mono.just(createErrorResponse(
            "AI_SERVICE_ERROR",
            ex.getMessage(),
            HttpStatus.SERVICE_UNAVAILABLE,
            details
        ));
    }
    
    /**
     * Handles model load exceptions specifically.
     * 
     * @param ex the model load exception
     * @return structured error response
     */
    @ExceptionHandler(ModelLoadException.class)
    @ResponseStatus(HttpStatus.INSUFFICIENT_STORAGE)
    public Mono<ErrorResponse> handleModelLoadException(ModelLoadException ex) {
        logError(ex, "Model loading error occurred");
        
        Map<String, Object> details = new HashMap<>();
        details.put("errorCode", ex.getErrorCode());
        details.put("service", ex.getService());
        details.put("model", ex.getModel());
        details.put("modelPath", ex.getModelPath());
        details.put("requiredMemory", ex.getRequiredMemory());
        details.put("availableMemory", ex.getAvailableMemory());
        
        return Mono.just(createErrorResponse(
            "MODEL_LOAD_ERROR",
            ex.getMessage(),
            HttpStatus.INSUFFICIENT_STORAGE,
            details
        ));
    }
    
    /**
     * Handles validation exceptions.
     * 
     * @param ex the validation exception
     * @return structured error response
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorResponse> handleValidationException(ValidationException ex) {
        logError(ex, "Validation error occurred");
        
        Map<String, Object> details = new HashMap<>();
        details.put("errorCode", ex.getErrorCode());
        details.put("component", ex.getComponent());
        details.put("validationErrors", ex.getValidationErrors());
        details.put("errorCount", ex.getErrorCount());
        
        return Mono.just(createErrorResponse(
            "VALIDATION_ERROR",
            ex.getMessage(),
            HttpStatus.BAD_REQUEST,
            details
        ));
    }
    
    /**
     * Handles configuration exceptions.
     * 
     * @param ex the configuration exception
     * @return structured error response
     */
    @ExceptionHandler(ConfigurationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ErrorResponse> handleConfigurationException(ConfigurationException ex) {
        logError(ex, "Configuration error occurred");
        
        Map<String, Object> details = new HashMap<>();
        details.put("errorCode", ex.getErrorCode());
        details.put("component", ex.getComponent());
        details.put("configKey", ex.getConfigKey());
        
        return Mono.just(createErrorResponse(
            "CONFIGURATION_ERROR",
            ex.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR,
            details
        ));
    }
    
    /**
     * Handles external service exceptions.
     * 
     * @param ex the external service exception
     * @return structured error response
     */
    @ExceptionHandler(ExternalServiceException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Mono<ErrorResponse> handleExternalServiceException(ExternalServiceException ex) {
        logError(ex, "External service error occurred");
        
        Map<String, Object> details = new HashMap<>();
        details.put("errorCode", ex.getErrorCode());
        details.put("serviceName", ex.getServiceName());
        details.put("endpoint", ex.getEndpoint());
        details.put("httpStatusCode", ex.getHttpStatusCode());
        details.put("timestamp", ex.getTimestamp());
        details.put("context", ex.getContext());
        
        return Mono.just(createErrorResponse(
            "EXTERNAL_SERVICE_ERROR",
            ex.getMessage(),
            HttpStatus.BAD_GATEWAY,
            details
        ));
    }
    
    /**
     * Handles network exceptions.
     * 
     * @param ex the network exception
     * @return structured error response
     */
    @ExceptionHandler(NetworkException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Mono<ErrorResponse> handleNetworkException(NetworkException ex) {
        logError(ex, "Network error occurred");
        
        Map<String, Object> details = new HashMap<>();
        details.put("errorCode", ex.getErrorCode());
        details.put("host", ex.getHost());
        details.put("port", ex.getPort());
        details.put("timestamp", ex.getTimestamp());
        details.put("timeoutMs", ex.getTimeoutMs());
        
        return Mono.just(createErrorResponse(
            "NETWORK_ERROR",
            ex.getMessage(),
            HttpStatus.SERVICE_UNAVAILABLE,
            details
        ));
    }
    
    /**
     * Handles Spring validation exceptions for method arguments.
     * 
     * @param ex the method argument not valid exception
     * @return structured error response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        logError(ex, "Method argument validation failed");
        
        Map<String, String> validationErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                fieldError -> fieldError.getField(),
                fieldError -> fieldError.getDefaultMessage()
            ));
        
        return Mono.just(createErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed",
            HttpStatus.BAD_REQUEST,
            Map.of(
                "errorCode", "SPRING_VALIDATION_001",
                "component", "VALIDATION",
                "validationErrors", validationErrors,
                "errorCount", validationErrors.size()
            )
        ));
    }
    
    /**
     * Handles constraint violation exceptions.
     * 
     * @param ex the constraint violation exception
     * @return structured error response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        logError(ex, "Constraint validation failed");
        
        Map<String, String> validationErrors = ex.getConstraintViolations()
            .stream()
            .collect(Collectors.toMap(
                violation -> violation.getPropertyPath().toString(),
                violation -> violation.getMessage()
            ));
        
        return Mono.just(createErrorResponse(
            "VALIDATION_ERROR",
            "Constraint validation failed",
            HttpStatus.BAD_REQUEST,
            Map.of(
                "errorCode", "CONSTRAINT_VALIDATION_001",
                "component", "VALIDATION",
                "validationErrors", validationErrors,
                "errorCount", validationErrors.size()
            )
        ));
    }
    
    /**
     * Handles HTTP message not readable exceptions.
     * 
     * @param ex the HTTP message not readable exception
     * @return structured error response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        logError(ex, "HTTP message not readable");
        
        return Mono.just(createErrorResponse(
            "INVALID_REQUEST",
            "Invalid JSON format in request body",
            HttpStatus.BAD_REQUEST,
            Map.of(
                "errorCode", "HTTP_MESSAGE_001",
                "component", "API",
                "details", ex.getMostSpecificCause().getMessage()
            )
        ));
    }
    
    /**
     * Handles server web input exceptions.
     * 
     * @param ex the server web input exception
     * @return structured error response
     */
    @ExceptionHandler(ServerWebInputException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorResponse> handleServerWebInputException(ServerWebInputException ex) {
        logError(ex, "Server web input error");
        
        return Mono.just(createErrorResponse(
            "INVALID_REQUEST",
            "Invalid request input",
            HttpStatus.BAD_REQUEST,
            Map.of(
                "errorCode", "WEB_INPUT_001",
                "component", "API",
                "details", ex.getReason()
            )
        ));
    }
    
    /**
     * Handles generic runtime exceptions as a fallback.
     * 
     * @param ex the runtime exception
     * @return structured error response
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        logError(ex, "Unexpected runtime error occurred");
        
        return Mono.just(createErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred while processing your request",
            HttpStatus.INTERNAL_SERVER_ERROR,
            Map.of(
                "errorCode", "RUNTIME_001",
                "component", "SYSTEM",
                "details", "Internal server error"
            )
        ));
    }
    
    /**
     * Handles all other exceptions as the final fallback.
     * 
     * @param ex the exception
     * @return structured error response
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ErrorResponse> handleGenericException(Exception ex) {
        logError(ex, "Unexpected error occurred");
        
        return Mono.just(createErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred while processing your request",
            HttpStatus.INTERNAL_SERVER_ERROR,
            Map.of(
                "errorCode", "GENERIC_001",
                "component", "SYSTEM",
                "details", "Internal server error"
            )
        ));
    }
    
    /**
     * Logs the error with appropriate context information.
     * 
     * @param ex the exception to log
     * @param message the log message
     */
    private void logError(Exception ex, String message) {
        logger.error(message, ex);
    }
    
    /**
     * Creates a structured error response.
     * 
     * @param errorType the type of error
     * @param message the error message
     * @param status the HTTP status
     * @param details additional error details
     * @return structured error response
     */
    private ErrorResponse createErrorResponse(String errorType, String message, HttpStatus status, Map<String, Object> details) {
        return new ErrorResponse(
            errorType,
            message,
            status.value(),
            status.getReasonPhrase(),
            LocalDateTime.now(),
            details
        );
    }
    
    /**
     * Standard error response structure.
     */
    public record ErrorResponse(
        String errorType,
        String message,
        int statusCode,
        String statusReason,
        LocalDateTime timestamp,
        Map<String, Object> details
    ) {
        /**
         * Gets a simplified version of the error response for logging.
         * 
         * @return simplified error message
         */
        public String toLogMessage() {
            return String.format("[%s] %s: %s", errorType, statusCode, message);
        }
    }
}