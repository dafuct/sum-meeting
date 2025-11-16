package com.zoomtranscriber.core.exceptions;

import java.util.Map;
import java.util.HashMap;

/**
 * Exception thrown when validation errors occur in the Zoom Transcriber application.
 * This includes invalid input parameters, constraint violations, and data format errors.
 */
public class ValidationException extends RuntimeException {
    
    private final String errorCode;
    private final String component;
    private final Map<String, String> validationErrors;
    
    /**
     * Constructs a new ValidationException with the specified detail message.
     * 
     * @param message the detail message explaining the exception
     */
    public ValidationException(String message) {
        super(message);
        this.errorCode = "VALIDATION_001";
        this.component = "VALIDATION";
        this.validationErrors = new HashMap<>();
    }
    
    /**
     * Constructs a new ValidationException with the specified detail message and cause.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "VALIDATION_001";
        this.component = "VALIDATION";
        this.validationErrors = new HashMap<>();
    }
    
    /**
     * Constructs a new ValidationException with the specified detail message and validation errors.
     * 
     * @param message the detail message explaining the exception
     * @param validationErrors map of field names to error messages
     */
    public ValidationException(String message, Map<String, String> validationErrors) {
        super(message);
        this.errorCode = "VALIDATION_001";
        this.component = "VALIDATION";
        this.validationErrors = new HashMap<>(validationErrors);
    }
    
    /**
     * Constructs a new ValidationException with full context information.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     * @param errorCode the specific error code for this exception
     * @param component the component where the error occurred
     * @param validationErrors map of field names to error messages
     */
    public ValidationException(String message, Throwable cause, String errorCode, String component, Map<String, String> validationErrors) {
        super(message, cause);
        this.errorCode = errorCode;
        this.component = component;
        this.validationErrors = validationErrors != null ? new HashMap<>(validationErrors) : new HashMap<>();
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
     * Gets the validation errors associated with this exception.
     * 
     * @return immutable map of validation errors
     */
    public Map<String, String> getValidationErrors() {
        return new HashMap<>(validationErrors);
    }
    
    /**
     * Adds a validation error to this exception.
     * 
     * @param field the field name
     * @param error the error message
     */
    public void addValidationError(String field, String error) {
        validationErrors.put(field, error);
    }
    
    /**
     * Checks if this exception has any validation errors.
     * 
     * @return true if there are validation errors
     */
    public boolean hasValidationErrors() {
        return !validationErrors.isEmpty();
    }
    
    /**
     * Gets the number of validation errors.
     * 
     * @return the number of errors
     */
    public int getErrorCount() {
        return validationErrors.size();
    }
    
    /**
     * Creates a ValidationException for missing required field.
     * 
     * @param fieldName the name of the missing field
     * @param component the component where the validation failed
     * @return new ValidationException instance
     */
    public static ValidationException requiredFieldMissing(String fieldName, String component) {
        Map<String, String> errors = new HashMap<>();
        errors.put(fieldName, "Field is required");
        return new ValidationException(
            "Validation failed: required field missing",
            null,
            "VALIDATION_002",
            component,
            errors
        );
    }
    
    /**
     * Creates a ValidationException for invalid field value.
     * 
     * @param fieldName the name of the invalid field
     * @param value the invalid value
     * @param expectedType the expected type
     * @param component the component where the validation failed
     * @return new ValidationException instance
     */
    public static ValidationException invalidFieldValue(String fieldName, Object value, String expectedType, String component) {
        Map<String, String> errors = new HashMap<>();
        errors.put(fieldName, "Invalid value: " + value + ". Expected: " + expectedType);
        return new ValidationException(
            "Validation failed: invalid field value",
            null,
            "VALIDATION_003",
            component,
            errors
        );
    }
    
    /**
     * Creates a ValidationException for value out of range.
     * 
     * @param fieldName the name of the field
     * @param value the out-of-range value
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     * @param component the component where the validation failed
     * @return new ValidationException instance
     */
    public static ValidationException valueOutOfRange(String fieldName, Number value, Number min, Number max, String component) {
        Map<String, String> errors = new HashMap<>();
        errors.put(fieldName, "Value " + value + " is out of range. Expected: " + min + " to " + max);
        return new ValidationException(
            "Validation failed: value out of range",
            null,
            "VALIDATION_004",
            component,
            errors
        );
    }
    
    /**
     * Creates a ValidationException for invalid configuration.
     * 
     * @param configName the name of the invalid configuration
     * @param reason the reason for invalidity
     * @param component the component where the validation failed
     * @return new ValidationException instance
     */
    public static ValidationException invalidConfiguration(String configName, String reason, String component) {
        Map<String, String> errors = new HashMap<>();
        errors.put(configName, reason);
        return new ValidationException(
            "Validation failed: invalid configuration",
            null,
            "VALIDATION_005",
            component,
            errors
        );
    }
}