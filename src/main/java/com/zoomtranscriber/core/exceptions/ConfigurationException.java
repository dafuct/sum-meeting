package com.zoomtranscriber.core.exceptions;

/**
 * Exception thrown when configuration errors occur in the Zoom Transcriber application.
 * This includes missing configuration files, invalid settings, and environment issues.
 */
public class ConfigurationException extends RuntimeException {
    
    private final String errorCode;
    private final String component;
    private final String configKey;
    
    /**
     * Constructs a new ConfigurationException with the specified detail message.
     * 
     * @param message the detail message explaining the exception
     */
    public ConfigurationException(String message) {
        super(message);
        this.errorCode = "CONFIG_001";
        this.component = "CONFIGURATION";
        this.configKey = null;
    }
    
    /**
     * Constructs a new ConfigurationException with the specified detail message and cause.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "CONFIG_001";
        this.component = "CONFIGURATION";
        this.configKey = null;
    }
    
    /**
     * Constructs a new ConfigurationException with the specified detail message and config key.
     * 
     * @param message the detail message explaining the exception
     * @param configKey the configuration key that caused the error
     */
    public ConfigurationException(String message, String configKey) {
        super(message);
        this.errorCode = "CONFIG_001";
        this.component = "CONFIGURATION";
        this.configKey = configKey;
    }
    
    /**
     * Constructs a new ConfigurationException with the specified detail message, cause, and config key.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     * @param configKey the configuration key that caused the error
     */
    public ConfigurationException(String message, Throwable cause, String configKey) {
        super(message, cause);
        this.errorCode = "CONFIG_001";
        this.component = "CONFIGURATION";
        this.configKey = configKey;
    }
    
    /**
     * Constructs a new ConfigurationException with full context information.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     * @param errorCode the specific error code for this exception
     * @param component the component where the error occurred
     * @param configKey the configuration key that caused the error
     */
    public ConfigurationException(String message, Throwable cause, String errorCode, String component, String configKey) {
        super(message, cause);
        this.errorCode = errorCode;
        this.component = component;
        this.configKey = configKey;
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
     * Gets the configuration key associated with this exception.
     * 
     * @return the configuration key or null if not applicable
     */
    public String getConfigKey() {
        return configKey;
    }
    
    /**
     * Creates a ConfigurationException for missing configuration key.
     * 
     * @param configKey the missing configuration key
     * @param component the component that requires the configuration
     * @return new ConfigurationException instance
     */
    public static ConfigurationException missingConfiguration(String configKey, String component) {
        return new ConfigurationException(
            "Missing required configuration: " + configKey,
            null,
            "CONFIG_002",
            component,
            configKey
        );
    }
    
    /**
     * Creates a ConfigurationException for invalid configuration value.
     * 
     * @param configKey the configuration key with invalid value
     * @param value the invalid value
     * @param expectedType the expected type or format
     * @param component the component that uses the configuration
     * @return new ConfigurationException instance
     */
    public static ConfigurationException invalidValue(String configKey, String value, String expectedType, String component) {
        return new ConfigurationException(
            "Invalid configuration value for " + configKey + ": '" + value + "'. Expected: " + expectedType,
            null,
            "CONFIG_003",
            component,
            configKey
        );
    }
    
    /**
     * Creates a ConfigurationException for missing configuration file.
     * 
     * @param filePath the path to the missing configuration file
     * @param component the component that requires the file
     * @return new ConfigurationException instance
     */
    public static ConfigurationException missingFile(String filePath, String component) {
        return new ConfigurationException(
            "Missing configuration file: " + filePath,
            null,
            "CONFIG_004",
            component,
            "config.file"
        );
    }
    
    /**
     * Creates a ConfigurationException for invalid configuration file format.
     * 
     * @param filePath the path to the invalid configuration file
     * @param format the detected format
     * @param expectedFormat the expected format
     * @param component the component that uses the file
     * @return new ConfigurationException instance
     */
    public static ConfigurationException invalidFileFormat(String filePath, String format, String expectedFormat, String component) {
        return new ConfigurationException(
            "Invalid configuration file format: " + filePath + ". Detected: " + format + ", Expected: " + expectedFormat,
            null,
            "CONFIG_005",
            component,
            "config.file.format"
        );
    }
    
    /**
     * Creates a ConfigurationException for environment variable issues.
     * 
     * @param envVar the environment variable name
     * @param issue the specific issue (missing, invalid, etc.)
     * @param component the component that requires the environment variable
     * @return new ConfigurationException instance
     */
    public static ConfigurationException environmentVariableIssue(String envVar, String issue, String component) {
        return new ConfigurationException(
            "Environment variable issue: " + envVar + " - " + issue,
            null,
            "CONFIG_006",
            component,
            "env." + envVar
        );
    }
}