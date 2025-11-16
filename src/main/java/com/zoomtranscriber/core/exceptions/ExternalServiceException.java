package com.zoomtranscriber.core.exceptions;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Exception thrown when errors occur during communication with external services.
 * This includes API failures, network timeouts, and service unavailability issues.
 */
public class ExternalServiceException extends RuntimeException {
    
    private final String errorCode;
    private final String serviceName;
    private final String endpoint;
    private final int httpStatusCode;
    private final LocalDateTime timestamp;
    private final Map<String, Object> context;
    
    /**
     * Constructs a new ExternalServiceException with the specified detail message.
     * 
     * @param message the detail message explaining the exception
     */
    public ExternalServiceException(String message) {
        super(message);
        this.errorCode = "EXTERNAL_SERVICE_001";
        this.serviceName = "UNKNOWN";
        this.endpoint = null;
        this.httpStatusCode = 0;
        this.timestamp = LocalDateTime.now();
        this.context = new HashMap<>();
    }
    
    /**
     * Constructs a new ExternalServiceException with the specified detail message and cause.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     */
    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "EXTERNAL_SERVICE_001";
        this.serviceName = "UNKNOWN";
        this.endpoint = null;
        this.httpStatusCode = 0;
        this.timestamp = LocalDateTime.now();
        this.context = new HashMap<>();
    }
    
    /**
     * Constructs a new ExternalServiceException with the specified detail message and service information.
     * 
     * @param message the detail message explaining the exception
     * @param serviceName the name of the external service
     * @param endpoint the endpoint that was being called
     * @param httpStatusCode the HTTP status code received
     */
    public ExternalServiceException(String message, String serviceName, String endpoint, int httpStatusCode) {
        super(message);
        this.errorCode = "EXTERNAL_SERVICE_001";
        this.serviceName = serviceName;
        this.endpoint = endpoint;
        this.httpStatusCode = httpStatusCode;
        this.timestamp = LocalDateTime.now();
        this.context = new HashMap<>();
    }
    
    /**
     * Constructs a new ExternalServiceException with full context information.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     * @param serviceName the name of the external service
     * @param endpoint the endpoint that was being called
     * @param httpStatusCode the HTTP status code received
     * @param context additional context information
     */
    public ExternalServiceException(String message, Throwable cause, String serviceName, String endpoint, int httpStatusCode, Map<String, Object> context) {
        super(message, cause);
        this.errorCode = "EXTERNAL_SERVICE_001";
        this.serviceName = serviceName;
        this.endpoint = endpoint;
        this.httpStatusCode = httpStatusCode;
        this.timestamp = LocalDateTime.now();
        this.context = context != null ? new HashMap<>(context) : new HashMap<>();
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
     * Gets the name of the external service where this exception occurred.
     * 
     * @return the service name
     */
    public String getServiceName() {
        return serviceName;
    }
    
    /**
     * Gets the endpoint that was being called when this exception occurred.
     * 
     * @return the endpoint URL or null if not applicable
     */
    public String getEndpoint() {
        return endpoint;
    }
    
    /**
     * Gets the HTTP status code received when this exception occurred.
     * 
     * @return the HTTP status code or 0 if not applicable
     */
    public int getHttpStatusCode() {
        return httpStatusCode;
    }
    
    /**
     * Gets the timestamp when this exception occurred.
     * 
     * @return the timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Gets the additional context information for this exception.
     * 
     * @return immutable map of context information
     */
    public Map<String, Object> getContext() {
        return new HashMap<>(context);
    }
    
    /**
     * Adds context information to this exception.
     * 
     * @param key the context key
     * @param value the context value
     */
    public void addContext(String key, Object value) {
        context.put(key, value);
    }
    
    /**
     * Creates an ExternalServiceException for network timeout errors.
     * 
     * @param serviceName the name of the service
     * @param endpoint the endpoint that timed out
     * @param timeoutMs the timeout duration in milliseconds
     * @return new ExternalServiceException instance
     */
    public static ExternalServiceException networkTimeout(String serviceName, String endpoint, long timeoutMs) {
        Map<String, Object> context = new HashMap<>();
        context.put("timeoutMs", timeoutMs);
        
        return new ExternalServiceException(
            "Network timeout while calling " + serviceName + " at " + endpoint + " after " + timeoutMs + "ms",
            null,
            serviceName,
            endpoint,
            0,
            context
        );
    }
    
    /**
     * Creates an ExternalServiceException for service unavailable errors.
     * 
     * @param serviceName the name of the unavailable service
     * @param endpoint the endpoint that was being called
     * @return new ExternalServiceException instance
     */
    public static ExternalServiceException serviceUnavailable(String serviceName, String endpoint) {
        return new ExternalServiceException(
            "External service unavailable: " + serviceName + " at " + endpoint,
            null,
            serviceName,
            endpoint,
            503,
            null
        );
    }
    
    /**
     * Creates an ExternalServiceException for authentication failures.
     * 
     * @param serviceName the name of the service
     * @param endpoint the endpoint that was being called
     * @return new ExternalServiceException instance
     */
    public static ExternalServiceException authenticationFailed(String serviceName, String endpoint) {
        return new ExternalServiceException(
            "Authentication failed for service: " + serviceName + " at " + endpoint,
            null,
            serviceName,
            endpoint,
            401,
            null
        );
    }
    
    /**
     * Creates an ExternalServiceException for rate limit exceeded errors.
     * 
     * @param serviceName the name of the service
     * @param endpoint the endpoint that was being called
     * @param retryAfter the retry-after delay in seconds
     * @return new ExternalServiceException instance
     */
    public static ExternalServiceException rateLimitExceeded(String serviceName, String endpoint, int retryAfter) {
        Map<String, Object> context = new HashMap<>();
        context.put("retryAfterSeconds", retryAfter);
        
        return new ExternalServiceException(
            "Rate limit exceeded for service: " + serviceName + " at " + endpoint + ". Retry after " + retryAfter + " seconds",
            null,
            serviceName,
            endpoint,
            429,
            context
        );
    }
    
    /**
     * Creates an ExternalServiceException for bad request errors.
     * 
     * @param serviceName the name of the service
     * @param endpoint the endpoint that was being called
     * @param errorMessage the error message from the service
     * @return new ExternalServiceException instance
     */
    public static ExternalServiceException badRequest(String serviceName, String endpoint, String errorMessage) {
        Map<String, Object> context = new HashMap<>();
        context.put("serviceError", errorMessage);
        
        return new ExternalServiceException(
            "Bad request to service: " + serviceName + " at " + endpoint + ". Error: " + errorMessage,
            null,
            serviceName,
            endpoint,
            400,
            context
        );
    }
}