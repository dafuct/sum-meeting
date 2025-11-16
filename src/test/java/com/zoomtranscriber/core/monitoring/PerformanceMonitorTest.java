package com.zoomtranscriber.core.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PerformanceMonitor class.
 * Tests performance tracking, metrics collection, and context management.
 */
class PerformanceMonitorTest {
    
    private MeterRegistry meterRegistry;
    private PerformanceMonitor performanceMonitor;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        performanceMonitor = new PerformanceMonitor(meterRegistry);
    }
    
    @Test
    @DisplayName("Should create and close audio processing context")
    void shouldCreateAndCloseAudioProcessingContext() {
        String operationName = "test-audio-operation";
        
        try (PerformanceMonitor.PerformanceContext context = 
                performanceMonitor.startAudioProcessing(operationName)) {
            
            assertNotNull(context);
            assertEquals(PerformanceMonitor.PerformanceContext.ComponentType.AUDIO.toString(), 
                context.componentType());
            assertEquals(operationName, context.operationName());
            assertNotNull(context.startTime());
            
            // Simulate some processing time
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Verify metrics were recorded
        Counter audioCounter = meterRegistry.find("zoom.audio.processing.count").counter();
        assertNotNull(audioCounter);
        assertEquals(1.0, audioCounter.count());
        
        Timer audioTimer = meterRegistry.find("zoom.audio.processing.time").timer();
        assertNotNull(audioTimer);
        assertEquals(1.0, audioTimer.count());
        assertTrue(audioTimer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) >= 0);
    }
    
    @Test
    @DisplayName("Should create and close transcription context")
    void shouldCreateAndCloseTranscriptionContext() {
        String operationName = "test-transcription-operation";
        
        try (PerformanceMonitor.PerformanceContext context = 
                performanceMonitor.startTranscription(operationName)) {
            
            assertNotNull(context);
            assertEquals(PerformanceMonitor.PerformanceContext.ComponentType.TRANSCRIPTION.toString(), 
                context.componentType());
            assertEquals(operationName, context.operationName());
            
            // Simulate processing time
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Verify metrics were recorded
        Counter transcriptionCounter = meterRegistry.find("zoom.transcription.count").counter();
        assertNotNull(transcriptionCounter);
        assertEquals(1.0, transcriptionCounter.count());
        
        Timer transcriptionTimer = meterRegistry.find("zoom.transcription.time").timer();
        assertNotNull(transcriptionTimer);
        assertEquals(1.0, transcriptionTimer.count());
    }
    
    @Test
    @DisplayName("Should create and close AI service context")
    void shouldCreateAndCloseAiServiceContext() {
        String operationName = "test-ai-operation";
        
        try (PerformanceMonitor.PerformanceContext context = 
                performanceMonitor.startAiService(operationName)) {
            
            assertNotNull(context);
            assertEquals(PerformanceMonitor.PerformanceContext.ComponentType.AI_SERVICE.toString(), 
                context.componentType());
            assertEquals(operationName, context.operationName());
            
            // Simulate processing time
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Verify metrics were recorded
        Counter aiServiceCounter = meterRegistry.find("zoom.ai.service.count").counter();
        assertNotNull(aiServiceCounter);
        assertEquals(1.0, aiServiceCounter.count());
        
        Timer aiServiceTimer = meterRegistry.find("zoom.ai.service.time").timer();
        assertNotNull(aiServiceTimer);
        assertEquals(1.0, aiServiceTimer.count());
    }
    
    @Test
    @DisplayName("Should record errors correctly")
    void shouldRecordErrorsCorrectly() {
        String component = "TEST_COMPONENT";
        String errorType = "TEST_ERROR";
        
        performanceMonitor.recordError(component, errorType);
        
        Counter errorCounter = meterRegistry.find("zoom.errors.count")
            .tag("component", component)
            .tag("errorType", errorType)
            .counter();
        
        assertNotNull(errorCounter);
        assertEquals(1.0, errorCounter.count());
    }
    
    @Test
    @DisplayName("Should update system metrics correctly")
    void shouldUpdateSystemMetricsCorrectly() {
        double memoryUsageMB = 512.5;
        double cpuUsagePercent = 65.2;
        
        performanceMonitor.updateSystemMetrics(memoryUsageMB, cpuUsagePercent);
        
        // Check memory gauge
        var memoryGauge = meterRegistry.find("zoom.system.memory.usage").gauge();
        assertNotNull(memoryGauge);
        assertEquals(memoryUsageMB, memoryGauge.value());
        
        // Check CPU gauge
        var cpuGauge = meterRegistry.find("zoom.system.cpu.usage").gauge();
        assertNotNull(cpuGauge);
        assertEquals(cpuUsagePercent, cpuGauge.value());
    }
    
    @Test
    @DisplayName("Should calculate average times correctly")
    void shouldCalculateAverageTimesCorrectly() {
        // Record multiple operations
        try (PerformanceMonitor.PerformanceContext context = 
                performanceMonitor.startAudioProcessing("test-1")) {
            try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        
        try (PerformanceMonitor.PerformanceContext context = 
                performanceMonitor.startAudioProcessing("test-2")) {
            try { Thread.sleep(20); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        
        PerformanceMonitor.PerformanceSummary summary = performanceMonitor.getPerformanceSummary();
        
        // Verify averages are calculated correctly
        assertTrue(summary.averageAudioProcessingTime() >= 10.0);
        assertTrue(summary.averageAudioProcessingTime() <= 30.0); // Allow some overhead
        assertEquals(2.0, summary.totalOperations(), 0.01);
    }
    
    @Test
    @DisplayName("Should handle multiple contexts correctly")
    void shouldHandleMultipleContextsCorrectly() {
        // Simulate concurrent operations
        try (var audioContext = performanceMonitor.startAudioProcessing("audio")) {
            try (var transcriptionContext = performanceMonitor.startTranscription("transcription")) {
                try (var aiContext = performanceMonitor.startAiService("ai")) {
                    // All contexts should be active
                    assertNotNull(audioContext);
                    assertNotNull(transcriptionContext);
                    assertNotNull(aiContext);
                }
            }
        }
        
        // Verify all metrics were recorded
        assertEquals(1.0, meterRegistry.find("zoom.audio.processing.count").counter().count());
        assertEquals(1.0, meterRegistry.find("zoom.transcription.count").counter().count());
        assertEquals(1.0, meterRegistry.find("zoom.ai.service.count").counter().count());
    }
    
    @Test
    @DisplayName("Should get elapsed time correctly")
    void shouldGetElapsedTimeCorrectly() throws InterruptedException {
        try (PerformanceMonitor.PerformanceContext context = 
                performanceMonitor.startAudioProcessing("test")) {
            
            Thread.sleep(10);
            Duration elapsed1 = context.getElapsedTime();
            assertTrue(elapsed1.toMillis() >= 10);
            
            Thread.sleep(10);
            Duration elapsed2 = context.getElapsedTime();
            assertTrue(elapsed2.toMillis() >= 20);
            assertTrue(elapsed2.toMillis() >= elapsed1.toMillis());
        }
    }
    
    @Test
    @DisplayName("Should prevent double closing of context")
    void shouldPreventDoubleClosingOfContext() {
        PerformanceMonitor.PerformanceContext context = 
            performanceMonitor.startAudioProcessing("test");
        
        context.close();
        
        // Second close should not record additional metrics
        context.close();
        
        Counter audioCounter = meterRegistry.find("zoom.audio.processing.count").counter();
        assertEquals(1.0, audioCounter.count(), "Should only record once despite double close");
    }
    
    @Test
    @DisplayName("Should provide comprehensive performance summary")
    void shouldProvideComprehensivePerformanceSummary() {
        // Record various operations
        performanceMonitor.recordError("TEST_COMPONENT", "TEST_ERROR");
        performanceMonitor.updateSystemMetrics(512.0, 75.0);
        
        try (var context = performanceMonitor.startAudioProcessing("test")) {
            try { Thread.sleep(5); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        
        PerformanceMonitor.PerformanceSummary summary = performanceMonitor.getPerformanceSummary();
        
        assertNotNull(summary);
        assertTrue(summary.totalOperations() >= 1);
        assertTrue(summary.averageAudioProcessingTime() >= 0);
        assertEquals(512.0, summary.currentMemoryUsageMB(), 0.01);
        assertEquals(75.0, summary.currentCpuUsagePercent(), 0.01);
        assertNotNull(summary.componentMetrics());
    }
    
    @Test
    @DisplayName("Should handle zero operations gracefully")
    void shouldHandleZeroOperationsGracefully() {
        PerformanceMonitor.PerformanceSummary summary = performanceMonitor.getPerformanceSummary();
        
        assertEquals(0.0, summary.averageAudioProcessingTime());
        assertEquals(0.0, summary.averageTranscriptionTime());
        assertEquals(0.0, summary.averageAiServiceTime());
        assertEquals(0, summary.totalOperations());
    }
}