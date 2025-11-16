package com.zoomtranscriber.core.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for comprehensive metrics collection and export in the Zoom Transcriber application.
 * Collects performance, system, and application metrics for monitoring and alerting.
 * Provides scheduled collection, aggregation, and export of metrics data.
 */
@Service
public class MetricsCollectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsCollectionService.class);
    
    private final MeterRegistry meterRegistry;
    private final PerformanceMonitor performanceMonitor;
    private final SystemResourceMonitor systemResourceMonitor;
    
    // Metrics storage
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final Map<String, Double> gauges = new ConcurrentHashMap<>();
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();
    
    // Business metrics
    private final AtomicLong totalMeetingsProcessed = new AtomicLong(0);
    private final AtomicLong totalAudioChunksProcessed = new AtomicLong(0);
    private final AtomicLong totalTranscriptionRequests = new AtomicLong(0);
    private final AtomicLong totalAiServiceRequests = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    
    // Micrometer metrics
    private final Counter meetingsProcessedCounter;
    private final Counter audioChunksCounter;
    private final Counter transcriptionRequestsCounter;
    private final Counter aiServiceRequestsCounter;
    private final Counter errorsCounter;
    private final Timer meetingProcessingTimer;
    private final Timer audioProcessingTimer;
    private final Timer transcriptionTimer;
    private final Timer aiServiceTimer;
    
    /**
     * Constructs a new MetricsCollectionService with the specified dependencies.
     * 
     * @param meterRegistry micrometer meter registry
     * @param performanceMonitor performance monitoring service
     * @param systemResourceMonitor system resource monitoring service
     */
    public MetricsCollectionService(MeterRegistry meterRegistry,
                                  PerformanceMonitor performanceMonitor,
                                  SystemResourceMonitor systemResourceMonitor) {
        this.meterRegistry = meterRegistry;
        this.performanceMonitor = performanceMonitor;
        this.systemResourceMonitor = systemResourceMonitor;
        
        // Initialize Micrometer metrics
        this.meetingsProcessedCounter = Counter.builder("zoom.meetings.processed")
            .description("Total number of meetings processed")
            .register(meterRegistry);
            
        this.audioChunksCounter = Counter.builder("zoom.audio.chunks.processed")
            .description("Total number of audio chunks processed")
            .register(meterRegistry);
            
        this.transcriptionRequestsCounter = Counter.builder("zoom.transcription.requests")
            .description("Total number of transcription requests")
            .register(meterRegistry);
            
        this.aiServiceRequestsCounter = Counter.builder("zoom.ai.service.requests")
            .description("Total number of AI service requests")
            .register(meterRegistry);
            
        this.errorsCounter = Counter.builder("zoom.errors.total")
            .description("Total number of errors")
            .register(meterRegistry);
        
        // Initialize timers
        this.meetingProcessingTimer = Timer.builder("zoom.meetings.processing.time")
            .description("Meeting processing time")
            .register(meterRegistry);
            
        this.audioProcessingTimer = Timer.builder("zoom.audio.processing.time")
            .description("Audio processing time")
            .register(meterRegistry);
            
        this.transcriptionTimer = Timer.builder("zoom.transcription.time")
            .description("Transcription processing time")
            .register(meterRegistry);
            
        this.aiServiceTimer = Timer.builder("zoom.ai.service.time")
            .description("AI service processing time")
            .register(meterRegistry);
        
        // Register custom gauges
        registerCustomGauges();
        
        logger.info("Metrics collection service initialized");
    }
    
    /**
     * Records a meeting processing event.
     * 
     * @param meetingId the meeting ID
     * @param duration processing duration in milliseconds
     */
    public void recordMeetingProcessed(String meetingId, long duration) {
        totalMeetingsProcessed.incrementAndGet();
        meetingsProcessedCounter.increment();
        meetingProcessingTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        MDC.put("meetingId", meetingId);
        MDC.put("duration", String.valueOf(duration));
        logger.info("Meeting processed: {} in {}ms", meetingId, duration);
        MDC.clear();
        
        logger.debug("Metrics recorded - Meeting processed: {}", meetingId);
    }
    
    /**
     * Records an audio chunk processing event.
     * 
     * @param chunkId the chunk ID
     * @param duration processing duration in milliseconds
     */
    public void recordAudioChunkProcessed(String chunkId, long duration) {
        totalAudioChunksProcessed.incrementAndGet();
        audioChunksCounter.increment();
        audioProcessingTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        logger.debug("Audio chunk processed: {} in {}ms", chunkId, duration);
    }
    
    /**
     * Records a transcription request event.
     * 
     * @param meetingId the meeting ID
     * @param duration processing duration in milliseconds
     */
    public void recordTranscriptionRequest(String meetingId, long duration) {
        totalTranscriptionRequests.incrementAndGet();
        transcriptionRequestsCounter.increment();
        transcriptionTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        logger.debug("Transcription request processed: {} in {}ms", meetingId, duration);
    }
    
    /**
     * Records an AI service request event.
     * 
     * @param service the AI service name
     * @param model the AI model name
     * @param duration processing duration in milliseconds
     */
    public void recordAiServiceRequest(String service, String model, long duration) {
        totalAiServiceRequests.incrementAndGet();
        
        Counter.builder("zoom.ai.service.requests")
            .description("AI service requests by service and model")
            .tag("service", service)
            .tag("model", model)
            .register(meterRegistry)
            .increment();
            
        aiServiceRequestsCounter.increment();
        aiServiceTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        logger.debug("AI service request processed: {} {} in {}ms", service, model, duration);
    }
    
    /**
     * Records an error event.
     * 
     * @param component the component where the error occurred
     * @param errorType the type of error
     * @param error the error details
     */
    public void recordError(String component, String errorType, String error) {
        totalErrors.incrementAndGet();
        errorsCounter.increment();
        
        Counter.builder("zoom.errors.count")
            .description("Error count by component and type")
            .tag("component", component)
            .tag("errorType", errorType)
            .register(meterRegistry)
            .increment();
        
        MDC.put("component", component);
        MDC.put("errorType", errorType);
        logger.error("Error recorded: {}", error);
        MDC.clear();
        
        logger.debug("Error metrics recorded - Component: {}, Type: {}", component, errorType);
    }
    
    /**
     * Records a custom counter metric.
     * 
     * @param metricName the metric name
     * @param tags metric tags
     * @param delta the increment value
     */
    public void recordCounter(String metricName, Map<String, String> tags, long delta) {
        counters.computeIfAbsent(metricName, k -> new AtomicLong(0)).addAndGet(delta);
        
        Counter.Builder counterBuilder = Counter.builder("zoom.custom." + metricName)
            .description("Custom counter metric: " + metricName);
            
        if (tags != null) {
            tags.forEach(counterBuilder::tag);
        }
        
        counterBuilder.register(meterRegistry).increment(delta);
        
        logger.debug("Custom counter recorded: {} (+{})", metricName, delta);
    }
    
    /**
     * Records a custom gauge metric.
     * 
     * @param metricName the metric name
     * @param value the gauge value
     */
    public void recordGauge(String metricName, double value) {
        gauges.put(metricName, value);
        
        Gauge.builder("zoom.custom." + metricName, gauges, g -> g.getOrDefault(metricName, 0.0))
            .description("Custom gauge metric: " + metricName)
            .register(meterRegistry);
        
        logger.debug("Custom gauge recorded: {} = {}", metricName, value);
    }
    
    /**
     * Records a custom timer metric.
     * 
     * @param metricName the metric name
     * @param duration the duration in milliseconds
     * @param tags metric tags
     */
    public void recordTimer(String metricName, long duration, Map<String, String> tags) {
        Timer.Builder timerBuilder = Timer.builder("zoom.custom." + metricName)
            .description("Custom timer metric: " + metricName);
            
        if (tags != null) {
            tags.forEach(timerBuilder::tag);
        }
        
        timerBuilder.register(meterRegistry)
            .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        logger.debug("Custom timer recorded: {} = {}ms", metricName, duration);
    }
    
    /**
     * Gets current metrics snapshot.
     * 
     * @return metrics snapshot
     */
    public MetricsSnapshot getMetricsSnapshot() {
        try {
            var performanceSummary = performanceMonitor.getPerformanceSummary();
            var systemMetrics = systemResourceMonitor.getCurrentMetrics();
            
            Map<String, Object> performance = new HashMap<>();
            performance.put("totalOperations", performanceSummary.totalOperations());
            performance.put("averageAudioProcessingTime", performanceSummary.averageAudioProcessingTime());
            performance.put("averageTranscriptionTime", performanceSummary.averageTranscriptionTime());
            performance.put("averageAiServiceTime", performanceSummary.averageAiServiceTime());
            performance.put("componentMetrics", performanceSummary.componentMetrics());
            
            Map<String, Object> system = new HashMap<>();
            system.put("cpuUsage", systemMetrics.cpuUsagePercent());
            system.put("memoryUsage", systemMetrics.heapMemoryUsagePercent());
            system.put("threadCount", systemMetrics.threadCount());
            system.put("jvmUptime", systemMetrics.jvmUptimeMs());
            system.put("healthStatus", systemMetrics.getHealthStatus());
            
            Map<String, Object> application = new HashMap<>();
            application.put("totalMeetingsProcessed", totalMeetingsProcessed.get());
            application.put("totalAudioChunksProcessed", totalAudioChunksProcessed.get());
            application.put("totalTranscriptionRequests", totalTranscriptionRequests.get());
            application.put("totalAiServiceRequests", totalAiServiceRequests.get());
            application.put("totalErrors", totalErrors.get());
            application.put("customCounters", new HashMap<>(counters));
            application.put("customGauges", new HashMap<>(gauges));
            
            return new MetricsSnapshot(
                Instant.now(),
                performance,
                system,
                application
            );
            
        } catch (Exception e) {
            logger.error("Error collecting metrics snapshot", e);
            return new MetricsSnapshot(
                Instant.now(),
                Map.of("error", e.getMessage()),
                Map.of("error", e.getMessage()),
                Map.of("error", e.getMessage())
            );
        }
    }
    
    /**
     * Scheduled method for periodic metrics collection and export.
     * Runs every minute to collect and log metrics.
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void collectAndExportMetrics() {
        try {
            MetricsSnapshot snapshot = getMetricsSnapshot();
            
            // Log metrics for monitoring systems
            logger.info("Metrics snapshot collected at {}", snapshot.timestamp());
            
            // Export metrics to external systems if needed
            exportMetricsToExternalSystems(snapshot);
            
            // Check for alerts and thresholds
            checkMetricThresholds(snapshot);
            
            logger.debug("Periodic metrics collection completed");
            
        } catch (Exception e) {
            logger.error("Error during periodic metrics collection", e);
        }
    }
    
    /**
     * Registers custom gauges with Micrometer.
     */
    private void registerCustomGauges() {
        Gauge.builder("zoom.application.meetings.processed", this, service -> (double) service.totalMeetingsProcessed.get())
            .description("Total meetings processed")
            .register(meterRegistry);
            
        Gauge.builder("zoom.application.audio.chunks.processed", this, service -> (double) service.totalAudioChunksProcessed.get())
            .description("Total audio chunks processed")
            .register(meterRegistry);
            
        Gauge.builder("zoom.application.transcription.requests", this, service -> (double) service.totalTranscriptionRequests.get())
            .description("Total transcription requests")
            .register(meterRegistry);
            
        Gauge.builder("zoom.application.ai.service.requests", this, service -> (double) service.totalAiServiceRequests.get())
            .description("Total AI service requests")
            .register(meterRegistry);
            
        Gauge.builder("zoom.application.errors.total", this, service -> (double) service.totalErrors.get())
            .description("Total errors")
            .register(meterRegistry);
    }
    
    /**
     * Exports metrics to external monitoring systems.
     * 
     * @param snapshot the metrics snapshot to export
     */
    private void exportMetricsToExternalSystems(MetricsSnapshot snapshot) {
        // Implementation would export to external systems like Prometheus, InfluxDB, etc.
        // For now, just log the export event
        logger.debug("Exporting metrics to external systems");
    }
    
    /**
     * Checks metric thresholds and triggers alerts if necessary.
     * 
     * @param snapshot the metrics snapshot to check
     */
    private void checkMetricThresholds(MetricsSnapshot snapshot) {
        // Check error rate threshold
        long totalRequests = totalTranscriptionRequests.get() + totalAiServiceRequests.get();
        if (totalRequests > 0) {
            double errorRate = (double) totalErrors.get() / totalRequests;
            if (errorRate > 0.05) { // 5% error rate threshold
                logger.warn("High error rate detected: {}%", String.format("%.2f", errorRate * 100));
                recordError("METRICS_ALERT", "HIGH_ERROR_RATE", "Error rate: " + String.format("%.2f", errorRate * 100) + "%");
            }
        }
        
        // Check system resource thresholds
        Object cpuUsage = snapshot.system().get("cpuUsage");
        Object memoryUsage = snapshot.system().get("memoryUsage");
        
        if (cpuUsage instanceof Double && (Double) cpuUsage > 80.0) {
            logger.warn("High CPU usage detected: {}%", String.format("%.2f", cpuUsage));
        }
        
        if (memoryUsage instanceof Double && (Double) memoryUsage > 85.0) {
            logger.warn("High memory usage detected: {}%", String.format("%.2f", memoryUsage));
        }
    }
    
    /**
     * Metrics snapshot record.
     */
    public record MetricsSnapshot(
        Instant timestamp,
        Map<String, Object> performance,
        Map<String, Object> system,
        Map<String, Object> application
    ) {
        /**
         * Gets the overall health status based on metrics.
         * 
         * @return health status
         */
        public String getOverallHealth() {
            if (system.containsKey("healthStatus")) {
                return system.get("healthStatus").toString();
            }
            return "UNKNOWN";
        }
        
        /**
         * Checks if metrics are healthy.
         * 
         * @return true if metrics indicate healthy status
         */
        public boolean isHealthy() {
            return "HEALTHY".equals(getOverallHealth()) || "WARNING".equals(getOverallHealth());
        }
    }
}