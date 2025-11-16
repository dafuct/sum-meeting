package com.zoomtranscriber.core.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages Ollama model lifecycle including download, verification, updates, and caching.
 * Provides progress tracking and performance optimization for model operations.
 */
@Service
public class ModelManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelManager.class);
    
    private final OllamaService ollamaService;
    private final ModelManagerConfig config;
    private final Map<String, ModelStatus> modelStatusCache;
    private final Map<String, OllamaService.DownloadProgress> downloadProgressCache;
    private final AtomicBoolean healthCheckRunning;
    private final AtomicLong lastHealthCheck;
    
    @Autowired
    public ModelManager(OllamaService ollamaService, ModelManagerConfig config) {
        this.ollamaService = ollamaService;
        this.config = config;
        this.modelStatusCache = new ConcurrentHashMap<>();
        this.downloadProgressCache = new ConcurrentHashMap<>();
        this.healthCheckRunning = new AtomicBoolean(false);
        this.lastHealthCheck = new AtomicLong(0);
        
        // Initialize with default models
        initializeDefaultModels();
    }
    
    /**
     * Checks if a model is available locally.
     * 
     * @param modelName the model name
     * @return Mono containing true if model is available
     */
    public Mono<Boolean> isModelAvailable(String modelName) {
        return Mono.fromCallable(() -> {
            ModelStatus cachedStatus = modelStatusCache.get(modelName);
            if (cachedStatus != null && 
                cachedStatus.status() == ModelLifecycle.AVAILABLE &&
                !isStatusExpired(cachedStatus)) {
                return true;
            }
            return null;
        })
        .flatMap(cachedResult -> {
            if (cachedResult != null) {
                return Mono.just(cachedResult);
            }
            
            return ollamaService.isModelAvailable(modelName)
                .doOnNext(available -> {
                    ModelStatus status = new ModelStatus(
                        modelName,
                        available ? ModelLifecycle.AVAILABLE : ModelLifecycle.NOT_FOUND,
                        LocalDateTime.now(),
                        null
                    );
                    modelStatusCache.put(modelName, status);
                });
        })
        .doOnNext(available -> {
            if (available) {
                logger.debug("Model {} is available", modelName);
            } else {
                logger.debug("Model {} is not available", modelName);
            }
        })
        .onErrorResume(error -> {
            logger.error("Error checking model availability for: {}", modelName, error);
            return Mono.just(false);
        });
    }
    
    /**
     * Downloads a model with progress tracking.
     * 
     * @param modelName the model name to download
     * @return Flux of download progress updates
     */
    public Flux<OllamaService.DownloadProgress> downloadModel(String modelName) {
        logger.info("Starting download for model: {}", modelName);
        
        return Mono.fromCallable(() -> {
            ModelStatus status = modelStatusCache.get(modelName);
            if (status != null && status.status() == ModelLifecycle.DOWNLOADING) {
                throw new IllegalStateException("Model is already being downloaded: " + modelName);
            }
            return true;
        })
        .flatMapMany(ignored -> {
            // Update status to downloading
            updateModelStatus(modelName, ModelLifecycle.DOWNLOADING);
            
            return ollamaService.pullModel(modelName)
                .doOnNext(progress -> {
                    downloadProgressCache.put(modelName, progress);
                    logger.debug("Download progress for {}: {}%", 
                        modelName, progress.getProgressPercentage());
                })
                .doOnComplete(() -> {
                    updateModelStatus(modelName, ModelLifecycle.AVAILABLE);
                    downloadProgressCache.remove(modelName);
                    logger.info("Successfully downloaded model: {}", modelName);
                })
                .doOnError(error -> {
                    updateModelStatus(modelName, ModelLifecycle.ERROR);
                    downloadProgressCache.remove(modelName);
                    logger.error("Failed to download model: {}", modelName, error);
                })
                .onErrorResume(error -> {
                    logger.error("Error during model download for: {}", modelName, error);
                    return Flux.empty();
                });
        });
    }
    
    /**
     * Gets the current download progress for a model.
     * 
     * @param modelName the model name
     * @return Mono containing current progress or empty if not downloading
     */
    public Mono<OllamaService.DownloadProgress> getDownloadProgress(String modelName) {
        return Mono.fromCallable(() -> downloadProgressCache.get(modelName))
            .filter(progress -> progress != null)
            .switchIfEmpty(Mono.empty());
    }
    
    /**
     * Deletes a model from local storage.
     * 
     * @param modelName the model name
     * @return Mono that completes when model is deleted
     */
    public Mono<Void> deleteModel(String modelName) {
        logger.info("Deleting model: {}", modelName);
        
        return ollamaService.deleteModel(modelName)
            .doOnSuccess(ignored -> {
                modelStatusCache.remove(modelName);
                downloadProgressCache.remove(modelName);
                logger.info("Successfully deleted model: {}", modelName);
            })
            .doOnError(error -> logger.error("Failed to delete model: {}", modelName, error))
            .onErrorResume(error -> {
                logger.error("Error during model deletion for: {}", modelName, error);
                return Mono.empty();
            });
    }
    
    /**
     * Lists all available models with their status.
     * 
     * @return Flux of ModelStatus objects
     */
    public Flux<ModelStatus> listModelStatuses() {
        return ollamaService.listModels()
            .map(modelInfo -> new ModelStatus(
                modelInfo.name(),
                ModelLifecycle.AVAILABLE,
                LocalDateTime.now(),
                modelInfo
            ))
            .mergeWith(getCachedModelStatuses())
            .distinct(ModelStatus::modelName)
            .doOnNext(status -> modelStatusCache.put(status.modelName(), status));
    }
    
    /**
     * Verifies model integrity and availability.
     * 
     * @param modelName the model name
     * @return Mono containing true if model is valid
     */
    public Mono<Boolean> verifyModel(String modelName) {
        logger.info("Verifying model: {}", modelName);
        
        return ollamaService.getModelInfo(modelName)
            .map(modelInfo -> {
                // Basic verification checks
                boolean isValid = modelInfo != null && 
                    modelInfo.name() != null && 
                    !modelInfo.name().isEmpty() &&
                    modelInfo.getSizeInBytes() > 0;
                
                updateModelStatus(modelName, isValid ? ModelLifecycle.AVAILABLE : ModelLifecycle.CORRUPTED);
                return isValid;
            })
            .doOnNext(isValid -> {
                if (isValid) {
                    logger.info("Model {} verification passed", modelName);
                } else {
                    logger.warn("Model {} verification failed", modelName);
                }
            })
            .onErrorResume(error -> {
                logger.error("Error verifying model: {}", modelName, error);
                updateModelStatus(modelName, ModelLifecycle.ERROR);
                return Mono.just(false);
            });
    }
    
    /**
     * Updates a model to the latest version.
     * 
     * @param modelName the model name
     * @return Flux of update progress
     */
    public Flux<OllamaService.DownloadProgress> updateModel(String modelName) {
        logger.info("Updating model: {}", modelName);
        
        return deleteModel(modelName)
            .thenMany(downloadModel(modelName))
            .doOnComplete(() -> logger.info("Successfully updated model: {}", modelName))
            .doOnError(error -> logger.error("Failed to update model: {}", modelName, error));
    }
    
    /**
     * Gets model statistics and usage information.
     * 
     * @return Mono containing ModelStatistics
     */
    public Mono<ModelStatistics> getModelStatistics() {
        return ollamaService.listModels()
            .collectList()
            .zipWith(ollamaService.getResourceUsage())
            .map(tuple -> {
                List<OllamaService.ModelInfo> models = tuple.getT1();
                OllamaService.ResourceUsage resources = tuple.getT2();
                
                long totalSize = models.stream()
                    .mapToLong(OllamaService.ModelInfo::getSizeInBytes)
                    .sum();
                
                int downloadingCount = (int) modelStatusCache.values().stream()
                    .filter(status -> status.status() == ModelLifecycle.DOWNLOADING)
                    .count();
                
                int errorCount = (int) modelStatusCache.values().stream()
                    .filter(status -> status.status() == ModelLifecycle.ERROR)
                    .count();
                
                return new ModelStatistics(
                    models.size(),
                    totalSize,
                    downloadingCount,
                    errorCount,
                    resources.memoryUsed(),
                    resources.memoryAvailable(),
                    LocalDateTime.now()
                );
            });
    }
    
    /**
     * Preloads essential models for optimal performance.
     * 
     * @param modelNames list of model names to preload
     * @return Flux of preload results
     */
    public Flux<ModelPreloadResult> preloadModels(List<String> modelNames) {
        logger.info("Preloading {} models", modelNames.size());
        
        return Flux.fromIterable(modelNames)
            .parallel()
            .runOn(Schedulers.parallel())
            .flatMap(modelName -> 
                isModelAvailable(modelName)
                    .flatMap(available -> {
                        if (available) {
                            return Mono.just(new ModelPreloadResult(
                                modelName, 
                                ModelPreloadStatus.ALREADY_LOADED, 
                                null
                            ));
                        } else {
                            return downloadModel(modelName)
                                .then(Mono.just(new ModelPreloadResult(
                                    modelName,
                                    ModelPreloadStatus.SUCCESS,
                                    null
                                )))
                                .onErrorResume(error -> Mono.just(new ModelPreloadResult(
                                    modelName,
                                    ModelPreloadStatus.FAILED,
                                    error.getMessage()
                                )));
                        }
                    })
            )
            .sequential()
            .doOnComplete(() -> logger.info("Completed model preloading"));
    }
    
    /**
     * Performs health check on model manager and Ollama service.
     * 
     * @return Mono containing health status
     */
    public Mono<ModelManagerHealth> performHealthCheck() {
        if (healthCheckRunning.compareAndSet(false, true)) {
            return ollamaService.checkHealth()
                .zipWith(ollamaService.getResourceUsage())
                .map(tuple -> {
                    boolean ollamaHealthy = tuple.getT1();
                    OllamaService.ResourceUsage resources = tuple.getT2();
                    
                    lastHealthCheck.set(System.currentTimeMillis());
                    
                    return new ModelManagerHealth(
                        ollamaHealthy,
                        resources.getMemoryUsagePercentage(),
                        resources.getDiskUsagePercentage(),
                        modelStatusCache.size(),
                        downloadProgressCache.size(),
                        LocalDateTime.now()
                    );
                })
                .doFinally(signalType -> healthCheckRunning.set(false))
                .onErrorResume(error -> {
                    logger.error("Health check failed", error);
                    return Mono.just(new ModelManagerHealth(
                        false,
                        0.0,
                        0.0,
                        modelStatusCache.size(),
                        downloadProgressCache.size(),
                        LocalDateTime.now()
                    ));
                });
        } else {
            return Mono.just(ModelManagerHealth.fromCache(lastHealthCheck.get()));
        }
    }
    
    /**
     * Clears the model cache.
     */
    public void clearCache() {
        logger.info("Clearing model manager cache");
        modelStatusCache.clear();
        downloadProgressCache.clear();
    }
    
    /**
     * Updates model status in cache.
     * 
     * @param modelName the model name
     * @param status the model lifecycle status
     */
    private void updateModelStatus(String modelName, ModelLifecycle status) {
        ModelStatus modelStatus = new ModelStatus(
            modelName,
            status,
            LocalDateTime.now(),
            null
        );
        modelStatusCache.put(modelName, modelStatus);
    }
    
    /**
     * Checks if model status is expired.
     * 
     * @param status the model status
     * @return true if status is expired
     */
    private boolean isStatusExpired(ModelStatus status) {
        return status.lastChecked().plusMinutes(config.getCacheExpirationMinutes())
            .isBefore(LocalDateTime.now());
    }
    
    /**
     * Gets cached model statuses that are not expired.
     * 
     * @return Flux of cached ModelStatus objects
     */
    private Flux<ModelStatus> getCachedModelStatuses() {
        return Flux.fromIterable(modelStatusCache.values())
            .filter(status -> !isStatusExpired(status));
    }
    
    /**
     * Initializes default models in the cache.
     */
    private void initializeDefaultModels() {
        for (String model : config.getDefaultModels()) {
            modelStatusCache.put(model, new ModelStatus(
                model,
                ModelLifecycle.UNKNOWN,
                LocalDateTime.now(),
                null
            ));
        }
    }
    
    /**
     * Represents the lifecycle status of a model.
     */
    public enum ModelLifecycle {
        UNKNOWN,
        NOT_FOUND,
        DOWNLOADING,
        AVAILABLE,
        CORRUPTED,
        ERROR,
        DELETING
    }
    
    /**
     * Represents the status of a model.
     */
    public record ModelStatus(
        String modelName,
        ModelLifecycle status,
        LocalDateTime lastChecked,
        OllamaService.ModelInfo modelInfo
    ) {
        /**
         * Checks if the model is ready for use.
         * 
         * @return true if model is available
         */
        public boolean isReady() {
            return status == ModelLifecycle.AVAILABLE;
        }
        
        /**
         * Checks if the model is in a transitional state.
         * 
         * @return true if model is being processed
         */
        public boolean isInTransit() {
            return status == ModelLifecycle.DOWNLOADING || status == ModelLifecycle.DELETING;
        }
        
        /**
         * Checks if the model has an error.
         * 
         * @return true if model has error
         */
        public boolean hasError() {
            return status == ModelLifecycle.ERROR || status == ModelLifecycle.CORRUPTED;
        }
    }
    
    /**
     * Represents download progress with additional metadata.
     */
    public record DownloadProgress(
        String modelName,
        double percentage,
        long bytesDownloaded,
        long totalBytes,
        double downloadSpeed,
        LocalDateTime startTime,
        LocalDateTime estimatedCompletion
    ) {
        /**
         * Checks if download is complete.
         * 
         * @return true if download is complete
         */
        public boolean isComplete() {
            return percentage >= 100.0;
        }
        
        /**
         * Gets remaining time in seconds.
         * 
         * @return remaining time in seconds
         */
        public long getRemainingSeconds() {
            if (startTime == null || downloadSpeed <= 0 || isComplete()) {
                return 0;
            }
            long remainingBytes = totalBytes - bytesDownloaded;
            return (long) (remainingBytes / downloadSpeed);
        }
    }
    
    /**
     * Represents model statistics.
     */
    public record ModelStatistics(
        int totalModels,
        long totalSizeBytes,
        int downloadingModels,
        int errorModels,
        long memoryUsed,
        long memoryAvailable,
        LocalDateTime lastUpdated
    ) {
        /**
         * Gets total size in human readable format.
         * 
         * @return formatted size string
         */
        public String getFormattedSize() {
            double gb = totalSizeBytes / (1024.0 * 1024.0 * 1024.0);
            if (gb >= 1.0) {
                return String.format("%.2f GB", gb);
            } else {
                double mb = totalSizeBytes / (1024.0 * 1024.0);
                return String.format("%.2f MB", mb);
            }
        }
        
        /**
         * Gets memory usage percentage.
         * 
         * @return memory usage percentage
         */
        public double getMemoryUsagePercentage() {
            long totalMemory = memoryUsed + memoryAvailable;
            return totalMemory > 0 ? (double) memoryUsed / totalMemory * 100.0 : 0.0;
        }
    }
    
    /**
     * Represents model preload results.
     */
    public record ModelPreloadResult(
        String modelName,
        ModelPreloadStatus status,
        String errorMessage
    ) {
        /**
         * Checks if preload was successful.
         * 
         * @return true if successful
         */
        public boolean isSuccess() {
            return status == ModelPreloadStatus.SUCCESS || 
                   status == ModelPreloadStatus.ALREADY_LOADED;
        }
    }
    
    /**
     * Enum for preload status.
     */
    public enum ModelPreloadStatus {
        SUCCESS,
        ALREADY_LOADED,
        FAILED
    }
    
    /**
     * Represents model manager health status.
     */
    public record ModelManagerHealth(
        boolean ollamaHealthy,
        double memoryUsagePercentage,
        double diskUsagePercentage,
        int cachedModels,
        int activeDownloads,
        LocalDateTime lastCheck
    ) {
        /**
         * Checks if system is healthy.
         * 
         * @return true if healthy
         */
        public boolean isHealthy() {
            return ollamaHealthy && 
                   memoryUsagePercentage < 90.0 && 
                   diskUsagePercentage < 95.0;
        }
        
        /**
         * Creates health status from cache.
         * 
         * @param timestamp the cached timestamp
         * @return ModelManagerHealth
         */
        public static ModelManagerHealth fromCache(long timestamp) {
            return new ModelManagerHealth(
                false,
                0.0,
                0.0,
                0,
                0,
                LocalDateTime.now()
            );
        }
    }
    
    /**
     * Configuration class for ModelManager.
     */
    public static class ModelManagerConfig {
        private List<String> defaultModels = List.of("qwen2.5:0.5b");
        private int cacheExpirationMinutes = 30;
        private int maxConcurrentDownloads = 2;
        private long maxModelSizeBytes = 10L * 1024 * 1024 * 1024; // 10GB
        private boolean enableAutoUpdate = false;
        private String autoUpdateSchedule = "0 2 * * *"; // 2 AM daily
        
        public ModelManagerConfig() {}
        
        // Getters and setters
        public List<String> getDefaultModels() { return defaultModels; }
        public void setDefaultModels(List<String> defaultModels) { this.defaultModels = defaultModels; }
        
        public int getCacheExpirationMinutes() { return cacheExpirationMinutes; }
        public void setCacheExpirationMinutes(int cacheExpirationMinutes) { this.cacheExpirationMinutes = cacheExpirationMinutes; }
        
        public int getMaxConcurrentDownloads() { return maxConcurrentDownloads; }
        public void setMaxConcurrentDownloads(int maxConcurrentDownloads) { this.maxConcurrentDownloads = maxConcurrentDownloads; }
        
        public long getMaxModelSizeBytes() { return maxModelSizeBytes; }
        public void setMaxModelSizeBytes(long maxModelSizeBytes) { this.maxModelSizeBytes = maxModelSizeBytes; }
        
        public boolean isEnableAutoUpdate() { return enableAutoUpdate; }
        public void setEnableAutoUpdate(boolean enableAutoUpdate) { this.enableAutoUpdate = enableAutoUpdate; }
        
        public String getAutoUpdateSchedule() { return autoUpdateSchedule; }
        public void setAutoUpdateSchedule(String autoUpdateSchedule) { this.autoUpdateSchedule = autoUpdateSchedule; }
    }
}