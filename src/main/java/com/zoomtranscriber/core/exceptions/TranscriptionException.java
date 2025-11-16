package com.zoomtranscriber.core.exceptions;

import java.util.UUID;

/**
 * Base exception for all transcription-related errors in the Zoom Transcriber application.
 * This includes speech recognition failures, model issues, and processing errors.
 */
public class TranscriptionException extends RuntimeException {
    
    private final String errorCode;
    private final String component;
    private final UUID meetingId;
    
    /**
     * Constructs a new TranscriptionException with the specified detail message.
     * 
     * @param message the detail message explaining the exception
     */
    public TranscriptionException(String message) {
        super(message);
        this.errorCode = "TRANSCRIPTION_001";
        this.component = "TRANSCRIPTION";
        this.meetingId = null;
    }
    
    /**
     * Constructs a new TranscriptionException with the specified detail message and cause.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     */
    public TranscriptionException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "TRANSCRIPTION_001";
        this.component = "TRANSCRIPTION";
        this.meetingId = null;
    }
    
    /**
     * Constructs a new TranscriptionException with the specified detail message and meeting ID.
     * 
     * @param message the detail message explaining the exception
     * @param meetingId the ID of the meeting where the error occurred
     */
    public TranscriptionException(String message, UUID meetingId) {
        super(message);
        this.errorCode = "TRANSCRIPTION_001";
        this.component = "TRANSCRIPTION";
        this.meetingId = meetingId;
    }
    
    /**
     * Constructs a new TranscriptionException with the specified detail message, cause, and meeting ID.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     * @param meetingId the ID of the meeting where the error occurred
     */
    public TranscriptionException(String message, Throwable cause, UUID meetingId) {
        super(message, cause);
        this.errorCode = "TRANSCRIPTION_001";
        this.component = "TRANSCRIPTION";
        this.meetingId = meetingId;
    }
    
    /**
     * Constructs a new TranscriptionException with the specified detail message, error code, component, and meeting ID.
     * 
     * @param message the detail message explaining the exception
     * @param errorCode the specific error code for this exception
     * @param component the component where the error occurred
     * @param meetingId the ID of the meeting where the error occurred
     */
    public TranscriptionException(String message, String errorCode, String component, UUID meetingId) {
        super(message);
        this.errorCode = errorCode;
        this.component = component;
        this.meetingId = meetingId;
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
    
    /**
     * Gets the meeting ID associated with this exception.
     * 
     * @return the meeting ID or null if not applicable
     */
    public UUID getMeetingId() {
        return meetingId;
    }
    
    /**
     * Creates a TranscriptionException for model not available errors.
     * 
     * @param model the name of the unavailable model
     * @param meetingId the meeting ID
     * @return new TranscriptionException instance
     */
    public static TranscriptionException modelNotAvailable(String model, UUID meetingId) {
        return new TranscriptionException(
            "Transcription model not available: " + model,
            "TRANSCRIPTION_002",
            "TRANSCRIPTION",
            meetingId
        );
    }
    
    /**
     * Creates a TranscriptionException for service unavailable errors.
     * 
     * @param meetingId the meeting ID
     * @return new TranscriptionException instance
     */
    public static TranscriptionException serviceUnavailable(UUID meetingId) {
        return new TranscriptionException(
            "Transcription service temporarily unavailable",
            "TRANSCRIPTION_003",
            "TRANSCRIPTION",
            meetingId
        );
    }
    
    /**
     * Creates a TranscriptionException for invalid language settings.
     * 
     * @param language the invalid language code
     * @param meetingId the meeting ID
     * @return new TranscriptionException instance
     */
    public static TranscriptionException invalidLanguage(String language, UUID meetingId) {
        return new TranscriptionException(
            "Invalid transcription language: " + language,
            "TRANSCRIPTION_004",
            "TRANSCRIPTION",
            meetingId
        );
    }
}