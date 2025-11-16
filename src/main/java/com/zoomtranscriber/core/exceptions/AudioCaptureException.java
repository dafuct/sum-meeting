package com.zoomtranscriber.core.exceptions;

/**
 * Exception thrown when errors occur during audio capture operations.
 * This includes issues with audio device access, initialization, and recording failures.
 */
public class AudioCaptureException extends AudioException {
    
    private final String deviceId;
    
    /**
     * Constructs a new AudioCaptureException with the specified detail message.
     * 
     * @param message the detail message explaining the exception
     */
    public AudioCaptureException(String message) {
        super(message, "AUDIO_CAPTURE_001", "AUDIO_CAPTURE");
        this.deviceId = null;
    }
    
    /**
     * Constructs a new AudioCaptureException with the specified detail message and cause.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     */
    public AudioCaptureException(String message, Throwable cause) {
        super(message, cause, "AUDIO_CAPTURE_001", "AUDIO_CAPTURE");
        this.deviceId = null;
    }
    
    /**
     * Constructs a new AudioCaptureException with the specified detail message and device ID.
     * 
     * @param message the detail message explaining the exception
     * @param deviceId the ID of the audio device that caused the error
     */
    public AudioCaptureException(String message, String deviceId) {
        super(message, "AUDIO_CAPTURE_002", "AUDIO_CAPTURE");
        this.deviceId = deviceId;
    }
    
    /**
     * Constructs a new AudioCaptureException with the specified detail message, cause, and device ID.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     * @param deviceId the ID of the audio device that caused the error
     */
    public AudioCaptureException(String message, Throwable cause, String deviceId) {
        super(message, cause, "AUDIO_CAPTURE_002", "AUDIO_CAPTURE");
        this.deviceId = deviceId;
    }
    
    /**
     * Gets the device ID associated with this capture exception.
     * 
     * @return the device ID or null if not applicable
     */
    public String getDeviceId() {
        return deviceId;
    }
    
    /**
     * Creates an AudioCaptureException for device not found errors.
     * 
     * @param deviceId the ID of the device that was not found
     * @return new AudioCaptureException instance
     */
    public static AudioCaptureException deviceNotFound(String deviceId) {
        return new AudioCaptureException(
            "Audio device not found: " + deviceId,
            deviceId
        );
    }
    
    /**
     * Creates an AudioCaptureException for device access denied errors.
     * 
     * @param deviceId the ID of the device that access was denied to
     * @return new AudioCaptureException instance
     */
    public static AudioCaptureException accessDenied(String deviceId) {
        return new AudioCaptureException(
            "Access denied to audio device: " + deviceId,
            deviceId
        );
    }
    
    /**
     * Creates an AudioCaptureException for device initialization errors.
     * 
     * @param deviceId the ID of the device that failed to initialize
     * @param cause the underlying cause
     * @return new AudioCaptureException instance
     */
    public static AudioCaptureException initializationFailed(String deviceId, Throwable cause) {
        return new AudioCaptureException(
            "Failed to initialize audio device: " + deviceId,
            cause,
            deviceId
        );
    }
}