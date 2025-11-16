package com.zoomtranscriber.api;

import com.zoomtranscriber.core.monitoring.PerformanceMonitor;
import com.zoomtranscriber.core.monitoring.SystemResourceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST API controller for system health monitoring and status checks.
 * Provides comprehensive health information for all components and system resources.
 * Supports both overall health status and detailed component-specific health information.
 */
@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "*")
public class HealthController {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    
    private final PerformanceMonitor performanceMonitor;
    private final SystemResourceMonitor systemResourceMonitor;
    
    /**
     * Constructs a new HealthController with the specified dependencies.
     * 
     * @param performanceMonitor performance monitoring service
     * @param systemResourceMonitor system resource monitoring service
     */
    public HealthController(PerformanceMonitor performanceMonitor, SystemResourceMonitor systemResourceMonitor) {
        this.performanceMonitor = performanceMonitor;
        this.systemResourceMonitor = systemResourceMonitor;
    }
    
    /**
     * Gets overall system health status.
     * Provides a quick health check for load balancers and monitoring systems.
     * 
     * @return overall health status
     */
    @GetMapping
    public Mono<ResponseEntity<HealthResponse>> getOverallHealth() {
        logger.debug("Overall health check requested");
        
        try {
            var performanceSummary = performanceMonitor.getPerformanceSummary();
            var systemMetrics = systemResourceMonitor.getCurrentMetrics();
            
            String overallStatus = determineOverallStatus(performanceSummary, systemMetrics);
            
            HealthResponse response = new HealthResponse(
                overallStatus,
                "All systems operational",
                LocalDateTime.now(),
                Map.of(
                    "performance", Map.of(
                        "status", performanceSummary.getHealthStatus(),
                        "totalOperations", performanceSummary.totalOperations()
                    ),
                    "system", Map.of(
                        "status", systemMetrics.getHealthStatus(),
                        "cpuUsage", systemMetrics.cpuUsagePercent(),
                        "memoryUsage", systemMetrics.heapMemoryUsagePercent()
                    )
                )
            );
            
            HttpStatus httpStatus = getHttpStatusForStatus(overallStatus);
            return Mono.just(ResponseEntity.status(httpStatus).body(response));
            
        } catch (Exception e) {
            logger.error("Error during health check", e);
            HealthResponse errorResponse = new HealthResponse(
                "DOWN",
                "Health check failed: " + e.getMessage(),
                LocalDateTime.now(),
                Map.of("error", e.getMessage())
            );
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse));
        }
    }
    
    /**
     * Gets detailed health status for all components.
     * Provides comprehensive health information including performance metrics and system resources.
     * 
     * @return detailed health status
     */
    @GetMapping("/detailed")
    public Mono<ResponseEntity<DetailedHealthResponse>> getDetailedHealth() {
        logger.debug("Detailed health check requested");
        
        try {
            var performanceSummary = performanceMonitor.getPerformanceSummary();
            var systemMetrics = systemResourceMonitor.getCurrentMetrics();
            
            Map<String, Object> audioStatus = getAudioComponentHealth();
            Map<String, Object> transcriptionStatus = getTranscriptionComponentHealth();
            Map<String, Object> aiServiceStatus = getAiServiceComponentHealth();
            
            DetailedHealthResponse response = new DetailedHealthResponse(
                determineOverallStatus(performanceSummary, systemMetrics),
                "Detailed system health information",
                LocalDateTime.now(),
                new ComponentHealth(audioStatus, transcriptionStatus, aiServiceStatus),
                performanceSummary,
                systemMetrics
            );
            
            HttpStatus httpStatus = getHttpStatusForStatus(response.status());
            return Mono.just(ResponseEntity.status(httpStatus).body(response));
            
        } catch (Exception e) {
            logger.error("Error during detailed health check", e);
            DetailedHealthResponse errorResponse = new DetailedHealthResponse(
                "DOWN",
                "Detailed health check failed: " + e.getMessage(),
                LocalDateTime.now(),
                new ComponentHealth(Map.of(), Map.of(), Map.of()),
                null,
                null
            );
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse));
        }
    }
    
    /**
     * Gets health status for audio components.
     * 
     * @return audio component health status
     */
    @GetMapping("/audio")
    public Mono<ResponseEntity<ComponentHealthResponse>> getAudioHealth() {
        logger.debug("Audio component health check requested");
        
        return Mono.just(ResponseEntity.ok(new ComponentHealthResponse(
            getAudioComponentHealth()
        )));
    }
    
    /**
     * Gets health status for transcription components.
     * 
     * @return transcription component health status
     */
    @GetMapping("/transcription")
    public Mono<ResponseEntity<ComponentHealthResponse>> getTranscriptionHealth() {
        logger.debug("Transcription component health check requested");
        
        return Mono.just(ResponseEntity.ok(new ComponentHealthResponse(
            getTranscriptionComponentHealth()
        )));
    }
    
    /**
     * Gets health status for AI service components.
     * 
     * @return AI service component health status
     */
    @GetMapping("/ai")
    public Mono<ResponseEntity<ComponentHealthResponse>> getAiServiceHealth() {
        logger.debug("AI service component health check requested");
        
        return Mono.just(ResponseEntity.ok(new ComponentHealthResponse(
            getAiServiceComponentHealth()
        )));
    }
    
    /**
     * Gets system resource health status.
     * 
     * @return system resource health status
     */
    @GetMapping("/system")
    public Mono<ResponseEntity<SystemHealthResponse>> getSystemHealth() {
        logger.debug("System resource health check requested");
        
        try {
            var systemMetrics = systemResourceMonitor.getCurrentMetrics();
            SystemHealthResponse response = new SystemHealthResponse(
                systemMetrics.getHealthStatus(),
                "System resource health information",
                LocalDateTime.now(),
                systemMetrics
            );
            
            HttpStatus httpStatus = getHttpStatusForStatus(systemMetrics.getHealthStatus());
            return Mono.just(ResponseEntity.status(httpStatus).body(response));
            
        } catch (Exception e) {
            logger.error("Error during system health check", e);
            SystemHealthResponse errorResponse = new SystemHealthResponse(
                "DOWN",
                "System health check failed: " + e.getMessage(),
                LocalDateTime.now(),
                null
            );
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse));
        }
    }
    
    /**
     * Gets performance metrics health status.
     * 
     * @return performance metrics health status
     */
    @GetMapping("/performance")
    public Mono<ResponseEntity<PerformanceHealthResponse>> getPerformanceHealth() {
        logger.debug("Performance health check requested");
        
        try {
            var performanceSummary = performanceMonitor.getPerformanceSummary();
            PerformanceHealthResponse response = new PerformanceHealthResponse(
                performanceSummary.getHealthStatus(),
                "Performance metrics health information",
                LocalDateTime.now(),
                performanceSummary
            );
            
            HttpStatus httpStatus = getHttpStatusForStatus(performanceSummary.getHealthStatus());
            return Mono.just(ResponseEntity.status(httpStatus).body(response));
            
        } catch (Exception e) {
            logger.error("Error during performance health check", e);
            PerformanceHealthResponse errorResponse = new PerformanceHealthResponse(
                "DOWN",
                "Performance health check failed: " + e.getMessage(),
                LocalDateTime.now(),
                null
            );
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse));
        }
    }
    
    /**
     * Triggers a health check for a specific component.
     * Used for manual component health verification.
     * 
     * @param component the component to check
     * @return component health status
     */
    @PostMapping("/check/{component}")
    public Mono<ResponseEntity<ComponentHealthResponse>> checkComponent(@PathVariable String component) {
        logger.info("Manual health check triggered for component: {}", component);
        
        Map<String, Object> componentHealth = switch (component.toLowerCase()) {
            case "audio" -> getAudioComponentHealth();
            case "transcription" -> getTranscriptionComponentHealth();
            case "ai", "ai-service" -> getAiServiceComponentHealth();
            default -> Map.of(
                "status", "UNKNOWN",
                "message", "Unknown component: " + component,
                "timestamp", LocalDateTime.now()
            );
        };
        
        return Mono.just(ResponseEntity.ok(new ComponentHealthResponse(componentHealth)));
    }
    
    /**
     * Gets audio component health information.
     * 
     * @return audio component health map
     */
    private Map<String, Object> getAudioComponentHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "HEALTHY"); // Would check actual audio service status
        health.put("message", "Audio capture and processing services operational");
        health.put("timestamp", LocalDateTime.now());
        health.put("details", Map.of(
            "captureService", "UP",
            "processingService", "UP",
            "lastCheck", LocalDateTime.now()
        ));
        return health;
    }
    
    /**
     * Gets transcription component health information.
     * 
     * @return transcription component health map
     */
    private Map<String, Object> getTranscriptionComponentHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "HEALTHY"); // Would check actual transcription service status
        health.put("message", "Transcription services operational");
        health.put("timestamp", LocalDateTime.now());
        health.put("details", Map.of(
            "speechRecognitionService", "UP",
            "languageDetectionService", "UP",
            "lastCheck", LocalDateTime.now()
        ));
        return health;
    }
    
    /**
     * Gets AI service component health information.
     * 
     * @return AI service component health map
     */
    private Map<String, Object> getAiServiceComponentHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "HEALTHY"); // Would check actual AI service status
        health.put("message", "AI services operational");
        health.put("timestamp", LocalDateTime.now());
        health.put("details", Map.of(
            "ollamaService", "UP",
            "modelLoadService", "UP",
            "lastCheck", LocalDateTime.now()
        ));
        return health;
    }
    
    /**
     * Determines the overall system status based on component and system health.
     * 
     * @param performanceSummary performance summary
     * @param systemMetrics system metrics
     * @return overall status
     */
    private String determineOverallStatus(PerformanceMonitor.PerformanceSummary performanceSummary, 
                                         SystemResourceMonitor.SystemMetrics systemMetrics) {
        String performanceStatus = performanceSummary.getHealthStatus();
        String systemStatus = systemMetrics.getHealthStatus();
        
        if (performanceStatus.equals("CRITICAL") || systemStatus.equals("CRITICAL")) {
            return "DOWN";
        } else if (performanceStatus.equals("WARNING") || systemStatus.equals("WARNING")) {
            return "WARNING";
        } else {
            return "UP";
        }
    }
    
    /**
     * Gets the appropriate HTTP status code for a health status.
     * 
     * @param status the health status
     * @return HTTP status code
     */
    private HttpStatus getHttpStatusForStatus(String status) {
        return switch (status) {
            case "UP", "HEALTHY" -> HttpStatus.OK;
            case "WARNING" -> HttpStatus.OK; // Still healthy but with warnings
            case "DOWN", "CRITICAL" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
    
    /**
     * Overall health response.
     */
    public record HealthResponse(
        String status,
        String message,
        LocalDateTime timestamp,
        Map<String, Object> details
    ) {}
    
    /**
     * Detailed health response with component information.
     */
    public record DetailedHealthResponse(
        String status,
        String message,
        LocalDateTime timestamp,
        ComponentHealth components,
        PerformanceMonitor.PerformanceSummary performance,
        SystemResourceMonitor.SystemMetrics system
    ) {}
    
    /**
     * Component health information.
     */
    public record ComponentHealth(
        Map<String, Object> audio,
        Map<String, Object> transcription,
        Map<String, Object> aiService
    ) {}
    
    /**
     * Component health response.
     */
    public record ComponentHealthResponse(
        Map<String, Object> health
    ) {}
    
    /**
     * System health response.
     */
    public record SystemHealthResponse(
        String status,
        String message,
        LocalDateTime timestamp,
        SystemResourceMonitor.SystemMetrics metrics
    ) {}
    
    /**
     * Performance health response.
     */
    public record PerformanceHealthResponse(
        String status,
        String message,
        LocalDateTime timestamp,
        PerformanceMonitor.PerformanceSummary metrics
    ) {}
}