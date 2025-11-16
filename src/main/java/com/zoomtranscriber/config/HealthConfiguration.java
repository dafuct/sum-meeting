package com.zoomtranscriber.config;

import com.zoomtranscriber.core.monitoring.HealthIndicatorService;
import com.zoomtranscriber.core.monitoring.SystemResourceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

/**
 * Configuration class for health monitoring and custom health setup.
 * Configures custom health monitoring beans for the Zoom Transcriber application.
 * Enables scheduling for periodic health checks and system monitoring.
 */
@Configuration
@EnableScheduling
public class HealthConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthConfiguration.class);
    
    /**
     * Bean for overall health monitoring service.
     * Provides a comprehensive view of application health including performance and system metrics.
     * 
     * @param healthIndicatorService service for component health checking
     * @param systemResourceMonitor system resource monitoring service
     * @return overall health bean
     */
    @Bean
    public OverallHealthService overallHealthService(
            HealthIndicatorService healthIndicatorService,
            SystemResourceMonitor systemResourceMonitor) {
        
        return new OverallHealthService(healthIndicatorService, systemResourceMonitor);
    }
    
    /**
     * Overall health service bean.
     * Combines all health monitoring into a single service.
     */
    public static class OverallHealthService {
        
        private final HealthIndicatorService healthIndicatorService;
        private final SystemResourceMonitor systemResourceMonitor;
        
        public OverallHealthService(HealthIndicatorService healthIndicatorService, 
                               SystemResourceMonitor systemResourceMonitor) {
            this.healthIndicatorService = healthIndicatorService;
            this.systemResourceMonitor = systemResourceMonitor;
        }
        
        /**
         * Gets the overall health status including all components and system metrics.
         * 
         * @return overall health information
         */
        public HealthIndicatorService.OverallHealth getOverallHealth() {
            try {
                // Get component health
                var componentHealth = healthIndicatorService.getAllComponentHealth();
                
                // Get system metrics
                SystemResourceMonitor.SystemMetrics systemMetrics = systemResourceMonitor.getCurrentMetrics();
                
                // Determine overall status
                boolean allComponentsUp = componentHealth.values().stream()
                    .allMatch(component -> component.status().equals("UP"));
                
                boolean systemHealthy = !systemMetrics.getHealthStatus().equals("CRITICAL");
                
                String status = (allComponentsUp && systemHealthy) ? "UP" : "DOWN";
                
                return new HealthIndicatorService.OverallHealth(
                    status,
                    "Overall system health: " + status,
                    java.time.LocalDateTime.now(),
                    componentHealth,
                    componentHealth.size(),
                    (int) componentHealth.values().stream()
                        .filter(component -> component.status().equals("UP"))
                        .count()
                );
                        
            } catch (Exception e) {
                logger.error("Error getting overall health", e);
                Map<String, HealthIndicatorService.ComponentHealth> errorComponents = Map.of();
                return new HealthIndicatorService.OverallHealth(
                    "DOWN",
                    "Health check system error: " + e.getMessage(),
                    java.time.LocalDateTime.now(),
                    errorComponents,
                    0,
                    0
                );
            }
        }
        
        /**
         * Gets the system resource health status.
         * 
         * @return system resource health information
         */
        public SystemResourceMonitor.SystemMetrics getSystemHealth() {
            return systemResourceMonitor.getCurrentMetrics();
        }
    }
}