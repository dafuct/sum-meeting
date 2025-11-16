package com.zoomtranscriber.core.exceptions;

import java.util.UUID;

/**
 * Exception thrown when errors occur during speech recognition operations.
 * This includes model failures, audio format incompatibilities, and recognition timeouts.
 */
public class SpeechRecognitionException extends TranscriptionException {
    
    private final String model;
    private final double confidence;
    
    /**
     * Constructs a new SpeechRecognitionException with the specified detail message.
     * 
     * @param message the detail message explaining the exception
     */
    public SpeechRecognitionException(String message) {
        super(message, "SPEECH_RECOGNITION_001", "SPEECH_RECOGNITION", null);
        this.model = null;
        this.confidence = 0.0;
    }
    
    /**
     * Constructs a new SpeechRecognitionException with the specified detail message and cause.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     */
    public SpeechRecognitionException(String message, Throwable cause) {
        super(message, cause, null);
        this.model = null;
        this.confidence = 0.0;
    }
    
    /**
     * Constructs a new SpeechRecognitionException with the specified detail message and meeting ID.
     * 
     * @param message the detail message explaining the exception
     * @param meetingId the ID of the meeting where the error occurred
     */
    public SpeechRecognitionException(String message, UUID meetingId) {
        super(message, "SPEECH_RECOGNITION_001", "SPEECH_RECOGNITION", meetingId);
        this.model = null;
        this.confidence = 0.0;
    }
    
    /**
     * Constructs a new SpeechRecognitionException with the specified detail message, cause, and meeting ID.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     * @param meetingId the ID of the meeting where the error occurred
     */
    public SpeechRecognitionException(String message, Throwable cause, UUID meetingId) {
        super(message, cause, meetingId);
        this.model = null;
        this.confidence = 0.0;
    }
    
    /**
     * Constructs a new SpeechRecognitionException with full context information.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     * @param meetingId the ID of the meeting where the error occurred
     * @param model the speech recognition model being used
     * @param confidence the confidence level when the error occurred
     */
    public SpeechRecognitionException(String message, Throwable cause, UUID meetingId, String model, double confidence) {
        super(message, cause, meetingId);
        this.model = model;
        this.confidence = confidence;
    }
    
    /**
     * Gets the speech recognition model associated with this exception.
     * 
     * @return the model name or null if not applicable
     */
    public String getModel() {
        return model;
    }
    
    /**
     * Gets the confidence level associated with this exception.
     * 
     * @return the confidence level (0.0 to 1.0)
     */
    public double getConfidence() {
        return confidence;
    }
    
    /**
     * Creates a SpeechRecognitionException for model load failures.
     * 
     * @param model the name of the model that failed to load
     * @param cause the underlying cause
     * @param meetingId the meeting ID
     * @return new SpeechRecognitionException instance
     */
    public static SpeechRecognitionException modelLoadFailed(String model, Throwable cause, UUID meetingId) {
        return new SpeechRecognitionException(
            "Failed to load speech recognition model: " + model,
            cause,
            meetingId,
            model,
            0.0
        );
    }
    
    /**
     * Creates a SpeechRecognitionException for low confidence results.
     * 
     * @param confidence the low confidence level
     * @param model the model being used
     * @param meetingId the meeting ID
     * @return new SpeechRecognitionException instance
     */
    public static SpeechRecognitionException lowConfidence(double confidence, String model, UUID meetingId) {
        SpeechRecognitionException exception = new SpeechRecognitionException(
            "Speech recognition confidence too low: " + confidence,
            meetingId
        );
        return exception;
    }
    
    /**
     * Creates a SpeechRecognitionException for timeout errors.
     * 
     * @param timeoutMs the timeout duration in milliseconds
     * @param meetingId the meeting ID
     * @return new SpeechRecognitionException instance
     */
    public static SpeechRecognitionException timeout(long timeoutMs, UUID meetingId) {
        return new SpeechRecognitionException(
            "Speech recognition timeout after " + timeoutMs + "ms",
            meetingId
        );
    }
    
    /**
     * Creates a SpeechRecognitionException for unsupported audio format.
     * 
     * @param format the unsupported audio format
     * @param model the model being used
     * @param meetingId the meeting ID
     * @return new SpeechRecognitionException instance
     */
    public static SpeechRecognitionException unsupportedFormat(String format, String model, UUID meetingId) {
        return new SpeechRecognitionException(
            "Unsupported audio format for model " + model + ": " + format,
            meetingId
        );
    }
}