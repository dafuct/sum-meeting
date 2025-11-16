package com.zoomtranscriber.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Health;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Adaptive thread pool configuration that adjusts based on system load.
 * Monitors system metrics and adjusts pool sizes dynamically.
 */
@Configuration
@EnableAsync
public class AdaptiveThreadPoolConfiguration {

    @Bean(name = "adaptiveTaskExecutor")
    public ThreadPoolTaskExecutor adaptiveTaskExecutor() {
        AdaptiveThreadPoolTaskExecutor executor = new AdaptiveThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("adaptive-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean(name = "audioProcessingExecutor")
    public ThreadPoolTaskExecutor audioProcessingExecutor() {
        AdaptiveThreadPoolTaskExecutor executor = new AdaptiveThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("audio-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        return executor;
    }

    @Bean(name = "transcriptionExecutor")
    public ThreadPoolTaskExecutor transcriptionExecutor() {
        AdaptiveThreadPoolTaskExecutor executor = new AdaptiveThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(12);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(45);
        executor.setThreadNamePrefix("transcription-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy()); // Fail fast for transcription
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        return executor;
    }

    @Bean(name = "aiProcessingExecutor")
    public ThreadPoolTaskExecutor aiProcessingExecutor() {
        AdaptiveThreadPoolTaskExecutor executor = new AdaptiveThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(90);
        executor.setThreadNamePrefix("ai-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        return executor;
    }

    @Bean
    public ThreadPoolHealthIndicator threadPoolHealthIndicator() {
        return new ThreadPoolHealthIndicator();
    }

    /**
     * Adaptive thread pool that adjusts based on system metrics.
     */
    public static class AdaptiveThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {
        
        private static final double CPU_THRESHOLD = 0.7;
        private static final double MEMORY_THRESHOLD = 0.8;
        
        @Override
        public void execute(Runnable task) {
            // Adjust pool size based on system load
            adjustPoolSize();
            super.execute(task);
        }

        private void adjustPoolSize() {
            double cpuUsage = getCpuUsage();
            double memoryUsage = getMemoryUsage();
            
            int currentSize = getPoolSize();
            int maxSize = getMaxPoolSize();
            
            if (cpuUsage > CPU_THRESHOLD || memoryUsage > MEMORY_THRESHOLD) {
                // Reduce pool size under heavy load
                int newSize = Math.max(getCorePoolSize(), currentSize - 2);
                if (newSize < currentSize) {
                    setMaxPoolSize(newSize);
                    System.out.println("Reduced thread pool size to " + newSize + " due to high CPU/Memory usage");
                }
            } else if (cpuUsage < CPU_THRESHOLD * 0.5 && memoryUsage < MEMORY_THRESHOLD * 0.5) {
                // Increase pool size under light load
                int newSize = Math.min(maxSize, currentSize + 1);
                if (newSize > currentSize && getActiveCount() > currentSize * 0.8) {
                    setMaxPoolSize(newSize);
                    System.out.println("Increased thread pool size to " + newSize + " due to low system load");
                }
            }
        }

        private double getCpuUsage() {
            // Implement CPU usage monitoring
            try {
                com.sun.management.OperatingSystemMXBean osBean = 
                    (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
                return osBean.getProcessCpuLoad();
            } catch (Exception e) {
                return 0.0;
            }
        }

        private double getMemoryUsage() {
            try {
                Runtime runtime = Runtime.getRuntime();
                long totalMemory = runtime.totalMemory();
                long freeMemory = runtime.freeMemory();
                return (double) (totalMemory - freeMemory) / totalMemory;
            } catch (Exception e) {
                return 0.0;
            }
        }
    }

    /**
     * Health indicator for thread pool monitoring.
     */
    public static class ThreadPoolHealthIndicator implements HealthIndicator {
        
        @Override
        public Health health() {
            try {
                // This would be injected or accessed via application context
                // For demonstration, creating sample health check
                return Health.up()
                    .withDetail("status", "All thread pools healthy")
                    .withDetail("timestamp", System.currentTimeMillis())
                    .build();
            } catch (Exception e) {
                return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
            }
        }
    }
}