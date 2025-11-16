package com.zoomtranscriber.core.exceptions;

/**
 * Exception thrown when errors occur during AI model loading operations.
 * This includes model file not found, memory constraints, and model corruption issues.
 */
public class ModelLoadException extends AIServiceException {
    
    private final String modelPath;
    private final long requiredMemory;
    private final long availableMemory;
    
    /**
     * Constructs a new ModelLoadException with the specified detail message.
     * 
     * @param message the detail message explaining the exception
     */
    public ModelLoadException(String message) {
        super(message, "MODEL_LOAD", null);
        this.modelPath = null;
        this.requiredMemory = 0;
        this.availableMemory = 0;
    }
    
    /**
     * Constructs a new ModelLoadException with the specified detail message and cause.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     */
    public ModelLoadException(String message, Throwable cause) {
        super(message, cause, "MODEL_LOAD", null);
        this.modelPath = null;
        this.requiredMemory = 0;
        this.availableMemory = 0;
    }
    
    /**
     * Constructs a new ModelLoadException with the specified detail message, model, and model path.
     * 
     * @param message the detail message explaining the exception
     * @param model the name of the model that failed to load
     * @param modelPath the path to the model file
     */
    public ModelLoadException(String message, String model, String modelPath) {
        super(message, "MODEL_LOAD", model);
        this.modelPath = modelPath;
        this.requiredMemory = 0;
        this.availableMemory = 0;
    }
    
    /**
     * Constructs a new ModelLoadException with full context information.
     * 
     * @param message the detail message explaining the exception
     * @param cause the cause of this exception
     * @param model the name of the model that failed to load
     * @param modelPath the path to the model file
     * @param requiredMemory the memory required to load the model
     * @param availableMemory the memory currently available
     */
    public ModelLoadException(String message, Throwable cause, String model, String modelPath, long requiredMemory, long availableMemory) {
        super(message, cause, "MODEL_LOAD_001", "MODEL_LOAD", model);
        this.modelPath = modelPath;
        this.requiredMemory = requiredMemory;
        this.availableMemory = availableMemory;
    }
    
    /**
     * Gets the model path associated with this exception.
     * 
     * @return the model path or null if not applicable
     */
    public String getModelPath() {
        return modelPath;
    }
    
    /**
     * Gets the required memory for loading the model.
     * 
     * @return the required memory in bytes
     */
    public long getRequiredMemory() {
        return requiredMemory;
    }
    
    /**
     * Gets the available memory when the exception occurred.
     * 
     * @return the available memory in bytes
     */
    public long getAvailableMemory() {
        return availableMemory;
    }
    
    /**
     * Creates a ModelLoadException for model file not found errors.
     * 
     * @param modelPath the path to the missing model file
     * @param model the name of the model
     * @return new ModelLoadException instance
     */
    public static ModelLoadException modelFileNotFound(String modelPath, String model) {
        return new ModelLoadException(
            "Model file not found: " + modelPath,
            model,
            modelPath
        );
    }
    
    /**
     * Creates a ModelLoadException for insufficient memory errors.
     * 
     * @param model the name of the model
     * @param requiredMemory the memory required in bytes
     * @param availableMemory the memory available in bytes
     * @return new ModelLoadException instance
     */
    public static ModelLoadException insufficientMemory(String model, long requiredMemory, long availableMemory) {
        return new ModelLoadException(
            "Insufficient memory to load model: " + model + 
            ". Required: " + requiredMemory + " bytes, Available: " + availableMemory + " bytes",
            null,
            model,
            null,
            requiredMemory,
            availableMemory
        );
    }
    
    /**
     * Creates a ModelLoadException for model corruption errors.
     * 
     * @param modelPath the path to the corrupted model file
     * @param model the name of the model
     * @param cause the underlying cause
     * @return new ModelLoadException instance
     */
    public static ModelLoadException modelCorrupted(String modelPath, String model, Throwable cause) {
        return new ModelLoadException(
            "Model file is corrupted or invalid: " + modelPath,
            cause,
            model,
            modelPath,
            0,
            0
        );
    }
    
    /**
     * Creates a ModelLoadException for unsupported model format errors.
     * 
     * @param modelPath the path to the model file
     * @param model the name of the model
     * @param format the unsupported format
     * @return new ModelLoadException instance
     */
    public static ModelLoadException unsupportedFormat(String modelPath, String model, String format) {
        return new ModelLoadException(
            "Unsupported model format: " + format + " for model " + model + " at " + modelPath,
            model,
            modelPath
        );
    }
}