package com.zoomtranscriber.core.exceptions;

/**
 * Base exception for all AI service-related errors in the Zoom Transcriber application.
 * This includes model inference failures, API communication issues, and resource constraints.
 */
public class AIServiceException extends RuntimeException {
    
    private final String errorCode;
    private final String service;
    private final String model;
    
    /**
     * Constructs a new AIServiceException with the specified detail message.
     * 
     * @param message the detail message explaining the exception
     */
    public AIServiceException(String message) {
        super(message);
        this.errorCode = "AI_SERVICE_001";
        this.service = "UNKNOWN";
        this.model = null;
    }
    
    /**
     * Constructs a new AIServiceException with the specified detail message and cause.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     */
    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "AI_SERVICE_001";
        this.service = "UNKNOWN";
        this.model = null;
    }
    
    /**
     * Constructs a new AIServiceException with the specified detail message, service, and model.
     * 
     * @param message the detail message explaining the exception
     * @param service the name of the AI service
     * @param model the name of the AI model
     */
    public AIServiceException(String message, String service, String model) {
        super(message);
        this.errorCode = "AI_SERVICE_001";
        this.service = service;
        this.model = model;
    }
    
    /**
     * Constructs a new AIServiceException with the specified detail message, cause, service, and model.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     * @param service the name of the AI service
     * @param model the name of the AI model
     */
    public AIServiceException(String message, Throwable cause, String service, String model) {
        super(message, cause);
        this.errorCode = "AI_SERVICE_001";
        this.service = service;
        this.model = model;
    }
    
    /**
     * Constructs a new AIServiceException with full context information.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     * @param errorCode the specific error code for this exception
     * @param service the name of the AI service
     * @param model the name of the AI model
     */
    public AIServiceException(String message, Throwable cause, String errorCode, String service, String model) {
        super(message, cause);
        this.errorCode = errorCode;
        this.service = service;
        this.model = model;
    }
    
    /**
     * Gets the error code associated with this exception.
     * 
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Gets the AI service where this exception occurred.
     * 
     * @return the service name
     */
    public String getService() {
        return service;
    }
    
    /**
     * Gets the AI model associated with this exception.
     * 
     * @return the model name or null if not applicable
     */
    public String getModel() {
        return model;
    }
    
    /**
     * Creates an AIServiceException for service unavailable errors.
     * 
     * @param service the name of the unavailable service
     * @return new AIServiceException instance
     */
    public static AIServiceException serviceUnavailable(String service) {
        return new AIServiceException(
            "AI service temporarily unavailable: " + service,
            null,
            "AI_SERVICE_002",
            service,
            null
        );
    }
    
    /**
     * Creates an AIServiceException for API rate limit exceeded.
     * 
     * @param service the name of the service
     * @return new AIServiceException instance
     */
    public static AIServiceException rateLimitExceeded(String service) {
        return new AIServiceException(
            "AI service rate limit exceeded: " + service,
            null,
            "AI_SERVICE_003",
            service,
            null
        );
    }
    
    /**
     * Creates an AIServiceException for model inference failures.
     * 
     * @param model the name of the model that failed
     * @param cause the underlying cause
     * @param service the service name
     * @return new AIServiceException instance
     */
    public static AIServiceException inferenceFailed(String model, Throwable cause, String service) {
        return new AIServiceException(
            "AI model inference failed: " + model,
            cause,
            "AI_SERVICE_004",
            service,
            model
        );
    }
    
    /**
     * Creates an AIServiceException for invalid API key or authentication.
     * 
     * @param service the name of the service
     * @return new AIServiceException instance
     */
    public static AIServiceException authenticationFailed(String service) {
        return new AIServiceException(
            "AI service authentication failed: " + service,
            null,
            "AI_SERVICE_005",
            service,
            null
        );
    }
}