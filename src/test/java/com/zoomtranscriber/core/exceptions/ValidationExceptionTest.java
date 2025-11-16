package com.zoomtranscriber.core.exceptions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ValidationException class.
 * Tests validation error creation, context management, and utility methods.
 */
class ValidationExceptionTest {
    
    @Test
    @DisplayName("Should create ValidationException with default values")
    void shouldCreateValidationExceptionWithDefaults() {
        String message = "Validation failed";
        ValidationException exception = new ValidationException(message);
        
        assertEquals(message, exception.getMessage());
        assertEquals("VALIDATION_001", exception.getErrorCode());
        assertEquals("VALIDATION", exception.getComponent());
        assertTrue(exception.getValidationErrors().isEmpty());
        assertFalse(exception.hasValidationErrors());
        assertEquals(0, exception.getErrorCount());
    }
    
    @Test
    @DisplayName("Should create ValidationException with validation errors")
    void shouldCreateValidationExceptionWithValidationErrors() {
        Map<String, String> errors = new HashMap<>();
        errors.put("field1", "Field is required");
        errors.put("field2", "Invalid format");
        
        String message = "Validation failed with errors";
        ValidationException exception = new ValidationException(message, errors);
        
        assertEquals(message, exception.getMessage());
        assertEquals("VALIDATION_001", exception.getErrorCode());
        assertEquals("VALIDATION", exception.getComponent());
        assertEquals(2, exception.getErrorCount());
        assertTrue(exception.hasValidationErrors());
        assertEquals("Field is required", exception.getValidationErrors().get("field1"));
        assertEquals("Invalid format", exception.getValidationErrors().get("field2"));
    }
    
    @Test
    @DisplayName("Should create ValidationException with full context")
    void shouldCreateValidationExceptionWithFullContext() {
        Map<String, String> errors = new HashMap<>();
        errors.put("name", "Name is required");
        
        String message = "Validation failed";
        String errorCode = "VALIDATION_002";
        String component = "USER_SERVICE";
        
        ValidationException exception = new ValidationException(message, null, errorCode, component, errors);
        
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(component, exception.getComponent());
        assertEquals(1, exception.getErrorCount());
        assertTrue(exception.hasValidationErrors());
    }
    
    @Test
    @DisplayName("Should add validation errors dynamically")
    void shouldAddValidationErrorsDynamically() {
        ValidationException exception = new ValidationException("Validation failed");
        
        assertFalse(exception.hasValidationErrors());
        assertEquals(0, exception.getErrorCount());
        
        exception.addValidationError("field1", "Field is required");
        exception.addValidationError("field2", "Invalid format");
        
        assertTrue(exception.hasValidationErrors());
        assertEquals(2, exception.getErrorCount());
        assertEquals("Field is required", exception.getValidationErrors().get("field1"));
        assertEquals("Invalid format", exception.getValidationErrors().get("field2"));
    }
    
    @Test
    @DisplayName("Should create required field missing exception")
    void shouldCreateRequiredFieldMissingException() {
        String fieldName = "username";
        String component = "AUTH_SERVICE";
        
        ValidationException exception = ValidationException.requiredFieldMissing(fieldName, component);
        
        assertTrue(exception.getMessage().contains("required field missing"));
        assertEquals("VALIDATION_002", exception.getErrorCode());
        assertEquals(component, exception.getComponent());
        assertTrue(exception.hasValidationErrors());
        assertEquals(1, exception.getErrorCount());
        assertEquals("Field is required", exception.getValidationErrors().get(fieldName));
    }
    
    @Test
    @DisplayName("Should create invalid field value exception")
    void shouldCreateInvalidFieldValueException() {
        String fieldName = "age";
        String value = "invalid";
        String expectedType = "Integer";
        String component = "USER_SERVICE";
        
        ValidationException exception = ValidationException.invalidFieldValue(fieldName, value, expectedType, component);
        
        assertTrue(exception.getMessage().contains("invalid value"));
        assertEquals("VALIDATION_003", exception.getErrorCode());
        assertEquals(component, exception.getComponent());
        assertTrue(exception.hasValidationErrors());
        assertEquals(1, exception.getErrorCount());
        assertTrue(exception.getValidationErrors().get(fieldName).contains(value));
        assertTrue(exception.getValidationErrors().get(fieldName).contains(expectedType));
    }
    
    @Test
    @DisplayName("Should create value out of range exception")
    void shouldCreateValueOutOfRangeException() {
        String fieldName = "age";
        Number value = 150;
        Number min = 0;
        Number max = 120;
        String component = "USER_SERVICE";
        
        ValidationException exception = ValidationException.valueOutOfRange(fieldName, value, min, max, component);
        
        assertTrue(exception.getMessage().contains("out of range"));
        assertEquals("VALIDATION_004", exception.getErrorCode());
        assertEquals(component, exception.getComponent());
        assertTrue(exception.hasValidationErrors());
        assertEquals(1, exception.getErrorCount());
        assertTrue(exception.getValidationErrors().get(fieldName).contains(value.toString()));
        assertTrue(exception.getValidationErrors().get(fieldName).contains(min.toString()));
        assertTrue(exception.getValidationErrors().get(fieldName).contains(max.toString()));
    }
    
    @Test
    @DisplayName("Should create invalid configuration exception")
    void shouldCreateInvalidConfigurationException() {
        String configName = "timeout";
        String reason = "Value must be positive";
        String component = "CONFIG_SERVICE";
        
        ValidationException exception = ValidationException.invalidConfiguration(configName, reason, component);
        
        assertTrue(exception.getMessage().contains("invalid configuration"));
        assertEquals("VALIDATION_005", exception.getErrorCode());
        assertEquals(component, exception.getComponent());
        assertTrue(exception.hasValidationErrors());
        assertEquals(1, exception.getErrorCount());
        assertEquals(reason, exception.getValidationErrors().get(configName));
    }
    
    @Test
    @DisplayName("Should handle null validation errors gracefully")
    void shouldHandleNullValidationErrorsGracefully() {
        ValidationException exception = new ValidationException("Test message", null);
        
        assertFalse(exception.hasValidationErrors());
        assertEquals(0, exception.getErrorCount());
        assertTrue(exception.getValidationErrors().isEmpty());
    }
    
    @Test
    @DisplayName("Should return copy of validation errors")
    void shouldReturnCopyOfValidationErrors() {
        ValidationException exception = new ValidationException("Test message");
        exception.addValidationError("field1", "Error 1");
        
        Map<String, String> errors = exception.getValidationErrors();
        errors.put("field2", "Error 2"); // Modify returned map
        
        assertEquals(1, exception.getErrorCount(), "Original exception should not be modified");
        assertEquals(1, errors.size(), "Returned map should be modifiable but doesn't affect original");
    }
}