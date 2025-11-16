package com.zoomtranscriber.core.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Service for component-specific health indicators.
 * Provides detailed health checking for individual components in the Zoom Transcriber system.
 * Supports both synchronous and asynchronous health checks with timeout management.
 */
@Component
public class HealthIndicatorService {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthIndicatorService.class);
    
    private final Map<String, ComponentHealthIndicator> indicators = new ConcurrentHashMap<>();
    private final Executor healthCheckExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "health-check-" + System.currentTimeMillis());
        t.setDaemon(true);
        return t;
    });
    
    /**
     * Registers a health indicator for a component.
     * 
     * @param componentName the component name
     * @param indicator the health indicator implementation
     */
    public void registerIndicator(String componentName, ComponentHealthIndicator indicator) {
        indicators.put(componentName, indicator);
        logger.debug("Registered health indicator for component: {}", componentName);
    }
    
    /**
     * Gets the health status of a specific component.
     * 
     * @param componentName the name of the component
     * @return health status of the component
     */
    public ComponentHealth getComponentHealth(String componentName) {
        ComponentHealthIndicator indicator = indicators.get(componentName);
        if (indicator == null) {
            return new ComponentHealth("DOWN", "Component not found: " + componentName, null);
        }
        
        try {
            return indicator.health();
        } catch (Exception e) {
            logger.error("Error checking health for component: {}", componentName, e);
            return new ComponentHealth("DOWN", "Health check failed: " + e.getMessage(), e.getClass().getSimpleName());
        }
    }
    
    /**
     * Gets the health status of all components asynchronously.
     * 
     * @return map of component names to their health status
     */
    public CompletableFuture<Map<String, ComponentHealth>> getAllComponentHealthAsync() {
        Map<String, CompletableFuture<ComponentHealth>> futures = new HashMap<>();
        
        for (Map.Entry<String, ComponentHealthIndicator> entry : indicators.entrySet()) {
            String componentName = entry.getKey();
            ComponentHealthIndicator indicator = entry.getValue();
            
            CompletableFuture<ComponentHealth> future = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        ComponentHealth health = indicator.health();
                        logger.debug("Health check completed for component: {} - {}", componentName, health.status());
                        return health;
                    } catch (Exception e) {
                        logger.error("Error in async health check for component: {}", componentName, e);
                        return new ComponentHealth("DOWN", "Health check failed: " + e.getMessage(), e.getClass().getSimpleName());
                    }
                }, healthCheckExecutor);
            
            futures.put(componentName, future);
        }
        
        // Combine all futures
        return CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                Map<String, ComponentHealth> results = new HashMap<>();
                for (Map.Entry<String, CompletableFuture<ComponentHealth>> entry : futures.entrySet()) {
                    try {
                        results.put(entry.getKey(), entry.getValue().get());
                    } catch (Exception e) {
                        logger.error("Error getting health check result for component: {}", entry.getKey(), e);
                        results.put(entry.getKey(), new ComponentHealth("DOWN", "Failed to get health check result: " + e.getMessage(), null));
                    }
                }
                return results;
            });
    }
    
    /**
     * Gets the health status of all components synchronously.
     * 
     * @return map of component names to their health status
     */
    public Map<String, ComponentHealth> getAllComponentHealth() {
        try {
            return getAllComponentHealthAsync().get();
        } catch (Exception e) {
            logger.error("Error getting all component health", e);
            Map<String, ComponentHealth> errorResults = new HashMap<>();
            for (String componentName : indicators.keySet()) {
                errorResults.put(componentName, new ComponentHealth("DOWN", "Health check system error: " + e.getMessage(), null));
            }
            return errorResults;
        }
    }
    
    /**
     * Gets the overall health status based on all component health indicators.
     * 
     * @return overall health status
     */
    public OverallHealth getOverallHealth() {
        Map<String, ComponentHealth> componentHealth = getAllComponentHealth();
        
        boolean allUp = componentHealth.values().stream()
            .allMatch(component -> component.status().equals("UP"));
        
        boolean anyDown = componentHealth.values().stream()
            .anyMatch(component -> component.status().equals("DOWN"));
        
        boolean anyWarning = componentHealth.values().stream()
            .anyMatch(component -> component.status().equals("WARNING"));
        
        String status = allUp ? "UP" : 
            anyDown ? "DOWN" : 
            anyWarning ? "WARNING" : "UNKNOWN";
        
        return new OverallHealth(
            status,
            "Overall system health: " + status,
            LocalDateTime.now(),
            componentHealth,
            componentHealth.size(),
            (int) componentHealth.values().stream()
                .filter(component -> component.status().equals("UP"))
                .count()
        );
    }
    
    /**
     * Gets component-specific health indicators for audio services.
     * 
     * @return audio component health indicator
     */
    public ComponentHealthIndicator getAudioHealthIndicator() {
        return new AudioComponentHealthIndicator();
    }
    
    /**
     * Gets component-specific health indicators for transcription services.
     * 
     * @return transcription component health indicator
     */
    public ComponentHealthIndicator getTranscriptionHealthIndicator() {
        return new TranscriptionComponentHealthIndicator();
    }
    
    /**
     * Gets component-specific health indicators for AI services.
     * 
     * @return AI service component health indicator
     */
    public ComponentHealthIndicator getAiServiceHealthIndicator() {
        return new AiServiceComponentHealthIndicator();
    }
    
    /**
     * Interface for component health indicators.
     */
    public interface ComponentHealthIndicator {
        
        /**
         * Checks the health of the component.
         * 
         * @return health status of the component
         */
        ComponentHealth health();
    }
    
    /**
     * Health indicator for audio components.
     */
    public static class AudioComponentHealthIndicator implements ComponentHealthIndicator {
        
        private volatile long lastAudioCheck = 0;
        private volatile boolean audioServiceUp = true;
        
        @Override
        public ComponentHealth health() {
            try {
                // Check audio service availability
                boolean isUp = checkAudioServiceHealth();
                
                if (isUp) {
                    audioServiceUp = true;
                    lastAudioCheck = System.currentTimeMillis();
                    
                    return new ComponentHealth("UP", "Audio capture and processing", Map.of(
                        "service", "Audio capture and processing",
                        "lastCheck", lastAudioCheck,
                        "status", "Operational"
                    ));
                } else {
                    audioServiceUp = false;
                    lastAudioCheck = System.currentTimeMillis();
                    
                    return new ComponentHealth("DOWN", "Audio service not responding", Map.of(
                        "service", "Audio capture and processing",
                        "lastCheck", lastAudioCheck,
                        "reason", "Audio service not responding"
                    ));
                }
            } catch (Exception e) {
                return new ComponentHealth("DOWN", "Health check failed: " + e.getMessage(), Map.of(
                    "service", "Audio capture and processing",
                    "reason", "Health check failed: " + e.getMessage(),
                    "error", e.getClass().getSimpleName()
                ));
            }
        }
        
        private boolean checkAudioServiceHealth() {
            // Implementation would check actual audio service health
            // For now, return a simple simulated check
            return System.currentTimeMillis() % 100 != 0; // 99% uptime simulation
        }
    }
    
    /**
     * Health indicator for transcription components.
     */
    public static class TranscriptionComponentHealthIndicator implements ComponentHealthIndicator {
        
        private volatile long lastTranscriptionCheck = 0;
        private volatile boolean transcriptionServiceUp = true;
        
        @Override
        public ComponentHealth health() {
            try {
                boolean isUp = checkTranscriptionServiceHealth();
                
                if (isUp) {
                    transcriptionServiceUp = true;
                    lastTranscriptionCheck = System.currentTimeMillis();
                    
                    return new ComponentHealth("UP", "Speech recognition and transcription", Map.of(
                        "service", "Speech recognition and transcription",
                        "lastCheck", lastTranscriptionCheck,
                        "status", "Operational"
                    ));
                } else {
                    transcriptionServiceUp = false;
                    lastTranscriptionCheck = System.currentTimeMillis();
                    
                    return new ComponentHealth("DOWN", "Transcription service not responding", Map.of(
                        "service", "Speech recognition and transcription",
                        "lastCheck", lastTranscriptionCheck,
                        "reason", "Transcription service not responding"
                    ));
                }
            } catch (Exception e) {
                return new ComponentHealth("DOWN", "Health check failed: " + e.getMessage(), Map.of(
                    "service", "Speech recognition and transcription",
                    "reason", "Health check failed: " + e.getMessage(),
                    "error", e.getClass().getSimpleName()
                ));
            }
        }
        
        private boolean checkTranscriptionServiceHealth() {
            // Implementation would check actual transcription service health
            // For now, return a simple simulated check
            return System.currentTimeMillis() % 100 != 0; // 99% uptime simulation
        }
    }
    
    /**
     * Health indicator for AI service components.
     */
    public static class AiServiceComponentHealthIndicator implements ComponentHealthIndicator {
        
        private volatile long lastAiCheck = 0;
        private volatile boolean aiServiceUp = true;
        
        @Override
        public ComponentHealth health() {
            try {
                boolean isUp = checkAiServiceHealth();
                
                if (isUp) {
                    aiServiceUp = true;
                    lastAiCheck = System.currentTimeMillis();
                    
                    return new ComponentHealth("UP", "AI and machine learning services", Map.of(
                        "service", "AI and machine learning services",
                        "lastCheck", lastAiCheck,
                        "status", "Operational"
                    ));
                } else {
                    aiServiceUp = false;
                    lastAiCheck = System.currentTimeMillis();
                    
                    return new ComponentHealth("DOWN", "AI service not responding", Map.of(
                        "service", "AI and machine learning services",
                        "lastCheck", lastAiCheck,
                        "reason", "AI service not responding"
                    ));
                }
            } catch (Exception e) {
                return new ComponentHealth("DOWN", "Health check failed: " + e.getMessage(), Map.of(
                    "service", "AI and machine learning services",
                    "reason", "Health check failed: " + e.getMessage(),
                    "error", e.getClass().getSimpleName()
                ));
            }
        }
        
        private boolean checkAiServiceHealth() {
            // Implementation would check actual AI service health
            // For now, return a simple simulated check
            return System.currentTimeMillis() % 100 != 0; // 99% uptime simulation
        }
    }
    
    /**
     * Component health information.
     */
    public record ComponentHealth(
        String status,
        String message,
        Object details
    ) {
        public String status() {
            return status;
        }
        
        public String message() {
            return message;
        }
        
        public Object details() {
            return details;
        }
    }
    
    /**
     * Overall health information.
     */
    public record OverallHealth(
        String status,
        String message,
        LocalDateTime timestamp,
        Map<String, ComponentHealth> components,
        int totalComponents,
        int healthyComponents
    ) {
        public String status() {
            return status;
        }
        
        public String message() {
            return message;
        }
        
        public LocalDateTime timestamp() {
            return timestamp;
        }
        
        public Map<String, ComponentHealth> components() {
            return components;
        }
        
        public int totalComponents() {
            return totalComponents;
        }
        
        public int healthyComponents() {
            return healthyComponents;
        }
    }
}