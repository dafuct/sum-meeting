package com.zoomtranscriber.core.exceptions;

/**
 * Base exception for all audio-related errors in the Zoom Transcriber application.
 * This exception serves as the root for audio capture, processing, and format issues.
 */
public class AudioException extends RuntimeException {
    
    private final String errorCode;
    private final String component;
    
    /**
     * Constructs a new AudioException with the specified detail message.
     * 
     * @param message the detail message explaining the exception
     */
    public AudioException(String message) {
        super(message);
        this.errorCode = "AUDIO_001";
        this.component = "AUDIO";
    }
    
    /**
     * Constructs a new AudioException with the specified detail message and cause.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     */
    public AudioException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "AUDIO_001";
        this.component = "AUDIO";
    }
    
    /**
     * Constructs a new AudioException with the specified detail message, error code, and component.
     * 
     * @param message the detail message explaining the exception
     * @param errorCode the specific error code for this exception
     * @param component the component where the error occurred
     */
    public AudioException(String message, String errorCode, String component) {
        super(message);
        this.errorCode = errorCode;
        this.component = component;
    }
    
    /**
     * Constructs a new AudioException with the specified detail message, cause, error code, and component.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     * @param errorCode the specific error code for this exception
     * @param component the component where the error occurred
     */
    public AudioException(String message, Throwable cause, String errorCode, String component) {
        super(message, cause);
        this.errorCode = errorCode;
        this.component = component;
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
     * Gets the component where this exception occurred.
     * 
     * @return the component name
     */
    public String getComponent() {
        return component;
    }
}