package com.zoomtranscriber.core.monitoring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * System resource monitoring service for the Zoom Transcriber application.
 * Tracks CPU usage, memory consumption, thread counts, and other system metrics.
 * Provides periodic updates and alerts for resource threshold violations.
 */
@Component
public class SystemResourceMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemResourceMonitor.class);
    
    private final PerformanceMonitor performanceMonitor;
    private final MeterRegistry meterRegistry;
    
    // JMX beans for system metrics
    private final MemoryMXBean memoryMXBean;
    private final OperatingSystemMXBean operatingSystemMXBean;
    private final ThreadMXBean threadMXBean;
    
    // Metric collectors
    private final DoubleAdder cpuUsage = new DoubleAdder();
    private final DoubleAdder heapMemoryUsage = new DoubleAdder();
    private final DoubleAdder nonHeapMemoryUsage = new DoubleAdder();
    private final AtomicLong threadCount = new AtomicLong(0);
    private final AtomicLong peakThreadCount = new AtomicLong(0);
    private final AtomicLong lastUpdate = new AtomicLong(0);
    
    // Thresholds for alerts
    private static final double CPU_WARNING_THRESHOLD = 70.0;
    private static final double CPU_CRITICAL_THRESHOLD = 85.0;
    private static final double MEMORY_WARNING_THRESHOLD = 75.0;
    private static final double MEMORY_CRITICAL_THRESHOLD = 90.0;
    
    // Alert tracking
    private volatile boolean cpuAlertActive = false;
    private volatile boolean memoryAlertActive = false;
    private volatile Instant lastCpuAlertTime = Instant.MIN;
    private volatile Instant lastMemoryAlertTime = Instant.MIN;
    private static final Duration ALERT_COOLDOWN = Duration.ofMinutes(5);
    
    /**
     * Constructs a new SystemResourceMonitor with the specified dependencies.
     * 
     * @param performanceMonitor performance monitor for metrics recording
     * @param meterRegistry micrometer meter registry for metrics collection
     */
    public SystemResourceMonitor(PerformanceMonitor performanceMonitor, MeterRegistry meterRegistry) {
        this.performanceMonitor = performanceMonitor;
        this.meterRegistry = meterRegistry;
        
        // Initialize JMX beans
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        
        // Register gauges for system metrics
        registerMetrics();
        
        logger.info("System resource monitor initialized");
    }
    
    /**
     * Scheduled method for periodic system resource monitoring.
     * Runs every 10 seconds to collect and update system metrics.
     */
    @Scheduled(fixedRate = 10000) // Every 10 seconds
    public void collectSystemMetrics() {
        try {
            collectCpuMetrics();
            collectMemoryMetrics();
            collectThreadMetrics();
            
            // Update performance monitor with system metrics
            performanceMonitor.updateSystemMetrics(
                heapMemoryUsage.sum(),
                cpuUsage.sum()
            );
            
            // Check for threshold violations
            checkThresholds();
            
            lastUpdate.set(System.currentTimeMillis());
            
            logger.debug("System metrics collected: cpu={}%, memory={}MB, threads={}", 
                String.format("%.2f", cpuUsage.sum()),
                String.format("%.1f", heapMemoryUsage.sum()),
                threadCount.get());
                
        } catch (Exception e) {
            logger.error("Error collecting system metrics", e);
            performanceMonitor.recordError("SYSTEM_MONITOR", e.getClass().getSimpleName());
        }
    }
    
    /**
     * Collects CPU usage metrics.
     */
    private void collectCpuMetrics() {
        try {
            double cpuLoad = getCpuUsage();
            if (cpuLoad >= 0) { // getCpuUsage() returns -1 on some systems
                cpuUsage.reset();
                cpuUsage.add(cpuLoad * 100.0); // Convert to percentage
            }
        } catch (Exception e) {
            logger.warn("Error collecting CPU metrics", e);
        }
    }
    
    /**
     * Gets CPU usage with fallback for different JVM implementations.
     * 
     * @return CPU usage as a percentage (0.0 to 1.0)
     */
    private double getCpuUsage() {
        try {
            // Try to get process CPU load
            if (operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean) {
                return ((com.sun.management.OperatingSystemMXBean) operatingSystemMXBean).getProcessCpuLoad();
            }
            return 0.0;
        } catch (Exception e) {
            logger.debug("Could not get CPU usage", e);
            return 0.0;
        }
    }
    
    /**
     * Collects memory usage metrics.
     */
    private void collectMemoryMetrics() {
        try {
            // Heap memory usage
            long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
            double heapUsagePercent = heapMax > 0 ? ((double) heapUsed / heapMax) * 100.0 : 0.0;
            
            // Convert to MB
            double heapUsedMB = heapUsed / (1024.0 * 1024.0);
            
            heapMemoryUsage.reset();
            heapMemoryUsage.add(heapUsedMB);
            
            // Non-heap memory usage
            long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
            double nonHeapUsedMB = nonHeapUsed / (1024.0 * 1024.0);
            
            nonHeapMemoryUsage.reset();
            nonHeapMemoryUsage.add(nonHeapUsedMB);
            
        } catch (Exception e) {
            logger.warn("Error collecting memory metrics", e);
        }
    }
    
    /**
     * Collects thread-related metrics.
     */
    private void collectThreadMetrics() {
        try {
            threadCount.set(threadMXBean.getThreadCount());
            peakThreadCount.set(threadMXBean.getPeakThreadCount());
        } catch (Exception e) {
            logger.warn("Error collecting thread metrics", e);
        }
    }
    
    /**
     * Registers Micrometer gauges for system metrics.
     */
    private void registerMetrics() {
        // CPU usage gauge
        Gauge.builder("zoom.system.cpu.usage", this, monitor -> monitor.cpuUsage.sum())
            .description("CPU usage percentage")
            .register(meterRegistry);
        
        // Heap memory usage gauge
        Gauge.builder("zoom.system.memory.heap.usage", this, monitor -> monitor.heapMemoryUsage.sum())
            .description("Heap memory usage in MB")
            .register(meterRegistry);
        
        // Non-heap memory usage gauge
        Gauge.builder("zoom.system.memory.nonheap.usage", this, monitor -> monitor.nonHeapMemoryUsage.sum())
            .description("Non-heap memory usage in MB")
            .register(meterRegistry);
        
        // Thread count gauge
        Gauge.builder("zoom.system.threads.count", this, monitor -> (double) monitor.threadCount.get())
            .description("Current thread count")
            .register(meterRegistry);
        
        // Peak thread count gauge
        Gauge.builder("zoom.system.threads.peak", this, monitor -> (double) monitor.peakThreadCount.get())
            .description("Peak thread count")
            .register(meterRegistry);
        
        // JVM uptime gauge
        Gauge.builder("zoom.jvm.uptime", this, monitor -> {
                try {
                    return (double) ManagementFactory.getRuntimeMXBean().getUptime();
                } catch (Exception e) {
                    return 0.0;
                }
            })
            .description("JVM uptime in milliseconds")
            .register(meterRegistry);
    }
    
    /**
     * Checks for threshold violations and triggers alerts if necessary.
     */
    private void checkThresholds() {
        checkCpuThresholds();
        checkMemoryThresholds();
    }
    
    /**
     * Checks CPU usage thresholds and triggers alerts.
     */
    private void checkCpuThresholds() {
        double currentCpu = cpuUsage.sum();
        Instant now = Instant.now();
        
        if (currentCpu >= CPU_CRITICAL_THRESHOLD) {
            if (!cpuAlertActive || Duration.between(lastCpuAlertTime, now).compareTo(ALERT_COOLDOWN) > 0) {
                logger.error("CRITICAL: CPU usage exceeded threshold: {}% >= {}%", 
                    String.format("%.2f", currentCpu), CPU_CRITICAL_THRESHOLD);
                performanceMonitor.recordError("SYSTEM_CPU", "CRITICAL_THRESHOLD_EXCEEDED");
                lastCpuAlertTime = now;
                cpuAlertActive = true;
            }
        } else if (currentCpu >= CPU_WARNING_THRESHOLD) {
            if (!cpuAlertActive || Duration.between(lastCpuAlertTime, now).compareTo(ALERT_COOLDOWN) > 0) {
                logger.warn("WARNING: CPU usage exceeded threshold: {}% >= {}%", 
                    String.format("%.2f", currentCpu), CPU_WARNING_THRESHOLD);
                performanceMonitor.recordError("SYSTEM_CPU", "WARNING_THRESHOLD_EXCEEDED");
                lastCpuAlertTime = now;
            }
        } else {
            cpuAlertActive = false;
        }
    }
    
    /**
     * Checks memory usage thresholds and triggers alerts.
     */
    private void checkMemoryThresholds() {
        try {
            long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
            double memoryUsagePercent = heapMax > 0 ? ((double) heapUsed / heapMax) * 100.0 : 0.0;
            
            Instant now = Instant.now();
            
            if (memoryUsagePercent >= MEMORY_CRITICAL_THRESHOLD) {
                if (!memoryAlertActive || Duration.between(lastMemoryAlertTime, now).compareTo(ALERT_COOLDOWN) > 0) {
                    logger.error("CRITICAL: Memory usage exceeded threshold: {}% >= {}%", 
                        String.format("%.2f", memoryUsagePercent), MEMORY_CRITICAL_THRESHOLD);
                    performanceMonitor.recordError("SYSTEM_MEMORY", "CRITICAL_THRESHOLD_EXCEEDED");
                    lastMemoryAlertTime = now;
                    memoryAlertActive = true;
                }
            } else if (memoryUsagePercent >= MEMORY_WARNING_THRESHOLD) {
                if (!memoryAlertActive || Duration.between(lastMemoryAlertTime, now).compareTo(ALERT_COOLDOWN) > 0) {
                    logger.warn("WARNING: Memory usage exceeded threshold: {}% >= {}%", 
                        String.format("%.2f", memoryUsagePercent), MEMORY_WARNING_THRESHOLD);
                    performanceMonitor.recordError("SYSTEM_MEMORY", "WARNING_THRESHOLD_EXCEEDED");
                    lastMemoryAlertTime = now;
                }
            } else {
                memoryAlertActive = false;
            }
        } catch (Exception e) {
            logger.error("Error checking memory thresholds", e);
        }
    }
    
    /**
     * Gets current system resource metrics.
     * 
     * @return system resource metrics
     */
    public SystemMetrics getCurrentMetrics() {
        return new SystemMetrics(
            cpuUsage.sum(),
            heapMemoryUsage.sum(),
            nonHeapMemoryUsage.sum(),
            threadCount.get(),
            peakThreadCount.get(),
            getHeapMemoryUsagePercent(),
            ManagementFactory.getRuntimeMXBean().getUptime(),
            System.currentTimeMillis() - lastUpdate.get() // Age of metrics
        );
    }
    
    /**
     * Calculates heap memory usage percentage.
     * 
     * @return heap memory usage percentage
     */
    private double getHeapMemoryUsagePercent() {
        try {
            long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
            return heapMax > 0 ? ((double) heapUsed / heapMax) * 100.0 : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    /**
     * Records system resource metrics.
     */
    public record SystemMetrics(
        double cpuUsagePercent,
        double heapMemoryUsageMB,
        double nonHeapMemoryUsageMB,
        long threadCount,
        long peakThreadCount,
        double heapMemoryUsagePercent,
        long jvmUptimeMs,
        long metricsAgeMs
    ) {
        /**
         * Gets the overall health status based on metrics.
         * 
         * @return health status
         */
        public String getHealthStatus() {
            if (cpuUsagePercent >= 85.0 || heapMemoryUsagePercent >= 90.0) {
                return "CRITICAL";
            } else if (cpuUsagePercent >= 70.0 || heapMemoryUsagePercent >= 75.0) {
                return "WARNING";
            } else {
                return "HEALTHY";
            }
        }
        
        /**
         * Checks if metrics are stale.
         * 
         * @param maxAgeMs maximum acceptable age in milliseconds
         * @return true if metrics are stale
         */
        public boolean isStale(long maxAgeMs) {
            return metricsAgeMs > maxAgeMs;
        }
    }
}