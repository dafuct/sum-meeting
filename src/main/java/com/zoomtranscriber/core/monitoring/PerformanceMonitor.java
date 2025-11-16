package com.zoomtranscriber.core.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Performance monitoring service for the Zoom Transcriber application.
 * Provides metrics collection for audio processing, transcription, AI services,
 * and system resource monitoring with comprehensive performance tracking.
 */
@Component
public class PerformanceMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitor.class);
    
    private final MeterRegistry meterRegistry;
    
    // Component-specific metrics
    private final ConcurrentHashMap<String, ComponentMetrics> componentMetrics = new ConcurrentHashMap<>();
    
    // System resource metrics
    private final AtomicLong audioProcessingTime = new AtomicLong(0);
    private final AtomicLong transcriptionTime = new AtomicLong(0);
    private final AtomicLong aiServiceTime = new AtomicLong(0);
    private final DoubleAdder memoryUsage = new DoubleAdder();
    private final DoubleAdder cpuUsage = new DoubleAdder();
    
    // Counters for various operations
    private final Counter audioProcessingCounter;
    private final Counter transcriptionCounter;
    private final Counter aiServiceCounter;
    private final Counter errorCounter;
    
    // Timers for performance measurement
    private final Timer audioProcessingTimer;
    private final Timer transcriptionTimer;
    private final Timer aiServiceTimer;
    
    /**
     * Constructs a new PerformanceMonitor with the specified MeterRegistry.
     * 
     * @param meterRegistry the micrometer meter registry for metrics collection
     */
    public PerformanceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.audioProcessingCounter = Counter.builder("zoom.audio.processing.count")
            .description("Total number of audio processing operations")
            .register(meterRegistry);
            
        this.transcriptionCounter = Counter.builder("zoom.transcription.count")
            .description("Total number of transcription operations")
            .register(meterRegistry);
            
        this.aiServiceCounter = Counter.builder("zoom.ai.service.count")
            .description("Total number of AI service operations")
            .register(meterRegistry);
            
        this.errorCounter = Counter.builder("zoom.errors.count")
            .description("Total number of errors")
            .tag("component", "unknown")
            .register(meterRegistry);
        
        // Initialize timers
        this.audioProcessingTimer = Timer.builder("zoom.audio.processing.time")
            .description("Audio processing execution time")
            .register(meterRegistry);
            
        this.transcriptionTimer = Timer.builder("zoom.transcription.time")
            .description("Transcription execution time")
            .register(meterRegistry);
            
        this.aiServiceTimer = Timer.builder("zoom.ai.service.time")
            .description("AI service execution time")
            .register(meterRegistry);
        
        // Initialize gauges for system metrics
        Gauge.builder("zoom.system.memory.usage", this, PerformanceMonitor::getMemoryUsage)
            .description("Current memory usage in MB")
            .register(meterRegistry);
            
        Gauge.builder("zoom.system.cpu.usage", this, PerformanceMonitor::getCpuUsage)
            .description("Current CPU usage percentage")
            .register(meterRegistry);
            
        Gauge.builder("zoom.audio.processing.average.time", this, PerformanceMonitor::getAverageAudioProcessingTime)
            .description("Average audio processing time in milliseconds")
            .register(meterRegistry);
            
        Gauge.builder("zoom.transcription.average.time", this, PerformanceMonitor::getAverageTranscriptionTime)
            .description("Average transcription time in milliseconds")
            .register(meterRegistry);
            
        Gauge.builder("zoom.ai.service.average.time", this, PerformanceMonitor::getAverageAiServiceTime)
            .description("Average AI service time in milliseconds")
            .register(meterRegistry);
        
        logger.info("Performance monitoring initialized with Micrometer metrics registry");
    }
    
    /**
     * Records the start of an audio processing operation.
     * 
     * @param operationName the name of the audio processing operation
     * @return performance context for tracking the operation
     */
    public PerformanceContext startAudioProcessing(String operationName) {
        return new PerformanceContext(this, "AUDIO_PROCESSING", operationName);
    }
    
    /**
     * Records the start of a transcription operation.
     * 
     * @param operationName the name of the transcription operation
     * @return performance context for tracking the operation
     */
    public PerformanceContext startTranscription(String operationName) {
        return new PerformanceContext(this, "TRANSCRIPTION", operationName);
    }
    
    /**
     * Records the start of an AI service operation.
     * 
     * @param operationName the name of the AI service operation
     * @return performance context for tracking the operation
     */
    public PerformanceContext startAiService(String operationName) {
        return new PerformanceContext(this, "AI_SERVICE", operationName);
    }
    
    /**
     * Records the completion of an audio processing operation.
     * 
     * @param context the performance context
     * @param duration the operation duration
     */
    void recordAudioProcessingComplete(PerformanceContext context, Duration duration) {
        audioProcessingCounter.increment();
        audioProcessingTimer.record(duration);
        audioProcessingTime.addAndGet(duration.toMillis());
        updateComponentMetrics(context, duration);
        
        logger.debug("Audio processing completed: {} in {}ms", context.operationName(), duration.toMillis());
    }
    
    /**
     * Records the completion of a transcription operation.
     * 
     * @param context the performance context
     * @param duration the operation duration
     */
    void recordTranscriptionComplete(PerformanceContext context, Duration duration) {
        transcriptionCounter.increment();
        transcriptionTimer.record(duration);
        transcriptionTime.addAndGet(duration.toMillis());
        updateComponentMetrics(context, duration);
        
        logger.debug("Transcription completed: {} in {}ms", context.operationName(), duration.toMillis());
    }
    
    /**
     * Records the completion of an AI service operation.
     * 
     * @param context the performance context
     * @param duration the operation duration
     */
    void recordAiServiceComplete(PerformanceContext context, Duration duration) {
        aiServiceCounter.increment();
        aiServiceTimer.record(duration);
        aiServiceTime.addAndGet(duration.toMillis());
        updateComponentMetrics(context, duration);
        
        logger.debug("AI service completed: {} in {}ms", context.operationName(), duration.toMillis());
    }
    
    /**
     * Records an error occurrence.
     * 
     * @param component the component where the error occurred
     * @param errorType the type of error
     */
    public void recordError(String component, String errorType) {
        Counter.builder("zoom.errors.count")
            .description("Total number of errors")
            .tag("component", component)
            .tag("errorType", errorType)
            .register(meterRegistry)
            .increment();
        
        logger.warn("Error recorded: component={}, errorType={}", component, errorType);
    }
    
    /**
     * Updates system resource metrics.
     * 
     * @param memoryUsageMB current memory usage in MB
     * @param cpuUsagePercent current CPU usage percentage
     */
    public void updateSystemMetrics(double memoryUsageMB, double cpuUsagePercent) {
        this.memoryUsage.reset();
        this.memoryUsage.add(memoryUsageMB);
        this.cpuUsage.reset();
        this.cpuUsage.add(cpuUsagePercent);
        
        logger.debug("System metrics updated: memory={}MB, cpu={}%", memoryUsageMB, cpuUsagePercent);
    }
    
    /**
     * Gets performance summary for all components.
     * 
     * @return performance summary
     */
    public PerformanceSummary getPerformanceSummary() {
        return new PerformanceSummary(
            getComponentMetrics(),
            getAverageAudioProcessingTime(),
            getAverageTranscriptionTime(),
            getAverageAiServiceTime(),
            getMemoryUsage(),
            getCpuUsage(),
            getTotalOperations()
        );
    }
    
    private void updateComponentMetrics(PerformanceContext context, Duration duration) {
        ComponentMetrics metrics = componentMetrics.computeIfAbsent(
            context.componentType(), 
            k -> new ComponentMetrics(context.componentType())
        );
        metrics.recordOperation(context.operationName(), duration);
    }
    
    private double getAverageAudioProcessingTime() {
        double count = audioProcessingCounter.count();
        return count > 0 ? (double) audioProcessingTime.get() / count : 0.0;
    }
    
    private double getAverageTranscriptionTime() {
        double count = transcriptionCounter.count();
        return count > 0 ? (double) transcriptionTime.get() / count : 0.0;
    }
    
    private double getAverageAiServiceTime() {
        double count = aiServiceCounter.count();
        return count > 0 ? (double) aiServiceTime.get() / count : 0.0;
    }
    
    private double getMemoryUsage() {
        return memoryUsage.sum();
    }
    
    private double getCpuUsage() {
        return cpuUsage.sum();
    }
    
    private long getTotalOperations() {
        return (long) (audioProcessingCounter.count() + transcriptionCounter.count() + aiServiceCounter.count());
    }
    
    private java.util.Map<String, ComponentMetrics> getComponentMetrics() {
        return new java.util.HashMap<>(componentMetrics);
    }
    
    /**
     * Performance context for tracking operation execution.
     */
    public static class PerformanceContext implements AutoCloseable {
        private final PerformanceMonitor monitor;
        private final String componentType;
        private final String operationName;
        private final Instant startTime;
        private volatile boolean completed = false;
        
        PerformanceContext(PerformanceMonitor monitor, String componentType, String operationName) {
            this.monitor = monitor;
            this.componentType = componentType;
            this.operationName = operationName;
            this.startTime = Instant.now();
        }
        
        public String componentType() {
            return componentType;
        }
        
        public String operationName() {
            return operationName;
        }
        
        public Instant startTime() {
            return startTime;
        }
        
        public Duration getElapsedTime() {
            return Duration.between(startTime, Instant.now());
        }
        
        @Override
        public void close() {
            if (!completed) {
                Duration duration = getElapsedTime();
                
                switch (componentType) {
                    case "AUDIO_PROCESSING" -> monitor.recordAudioProcessingComplete(this, duration);
                    case "TRANSCRIPTION" -> monitor.recordTranscriptionComplete(this, duration);
                    case "AI_SERVICE" -> monitor.recordAiServiceComplete(this, duration);
                    default -> logger.warn("Unknown component type: {}", componentType);
                }
                
                completed = true;
            }
        }
    }
    
    /**
     * Component-specific metrics tracking.
     */
    public static class ComponentMetrics {
        private final String componentType;
        private final ConcurrentHashMap<String, OperationMetrics> operations = new ConcurrentHashMap<>();
        private final AtomicLong totalOperations = new AtomicLong(0);
        private final AtomicLong totalTime = new AtomicLong(0);
        
        ComponentMetrics(String componentType) {
            this.componentType = componentType;
        }
        
        void recordOperation(String operationName, Duration duration) {
            operations.computeIfAbsent(operationName, k -> new OperationMetrics(operationName))
                .recordExecution(duration);
            totalOperations.incrementAndGet();
            totalTime.addAndGet(duration.toMillis());
        }
        
        public String componentType() {
            return componentType;
        }
        
        public long totalOperations() {
            return totalOperations.get();
        }
        
        public double averageTime() {
            long total = totalOperations.get();
            return total > 0 ? (double) totalTime.get() / total : 0.0;
        }
        
        public java.util.Map<String, OperationMetrics> operations() {
            return new java.util.HashMap<>(operations);
        }
    }
    
    /**
     * Operation-specific metrics tracking.
     */
    public static class OperationMetrics {
        private final String operationName;
        private final AtomicLong executionCount = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private volatile long lastExecutionTime = 0;
        
        OperationMetrics(String operationName) {
            this.operationName = operationName;
        }
        
        void recordExecution(Duration duration) {
            executionCount.incrementAndGet();
            totalExecutionTime.addAndGet(duration.toMillis());
            lastExecutionTime = System.currentTimeMillis();
        }
        
        public String operationName() {
            return operationName;
        }
        
        public long executionCount() {
            return executionCount.get();
        }
        
        public double averageExecutionTime() {
            long count = executionCount.get();
            return count > 0 ? (double) totalExecutionTime.get() / count : 0.0;
        }
        
        public long lastExecutionTime() {
            return lastExecutionTime;
        }
    }
    
    /**
     * Overall performance summary.
     */
    public record PerformanceSummary(
        java.util.Map<String, ComponentMetrics> componentMetrics,
        double averageAudioProcessingTime,
        double averageTranscriptionTime,
        double averageAiServiceTime,
        double currentMemoryUsageMB,
        double currentCpuUsagePercent,
        long totalOperations
    ) {
        /**
         * Gets performance health status based on metrics.
         * 
         * @return health status
         */
        public String getHealthStatus() {
            if (currentMemoryUsageMB > 1000 || currentCpuUsagePercent > 80) {
                return "CRITICAL";
            } else if (currentMemoryUsageMB > 500 || currentCpuUsagePercent > 60) {
                return "WARNING";
            } else {
                return "HEALTHY";
            }
        }
    }
}