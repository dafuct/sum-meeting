package com.zoomtranscriber.core.detection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base class for monitoring system processes to detect Zoom meetings.
 * Provides common functionality for process monitoring with platform-specific implementations.
 */
@Component
public abstract class ProcessMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessMonitor.class);
    private static final Duration POLLING_INTERVAL = Duration.ofSeconds(2);
    private static final String ZOOM_PROCESS_NAME = "zoom";
    
    protected final Map<String, ProcessInfo> activeProcesses = new ConcurrentHashMap<>();
    protected final Map<String, UUID> processToMeetingMap = new ConcurrentHashMap<>();
    protected boolean isMonitoring = false;
    
    /**
     * Platform-specific implementation to get current process list.
     * 
     * @return Mono containing list of ProcessInfo objects
     */
    protected abstract Mono<List<ProcessInfo>> getCurrentProcesses();
    
    /**
     * Platform-specific implementation to get window title for a process.
     * 
     * @param processId the process ID
     * @return Mono containing the window title or empty if not found
     */
    protected abstract Mono<String> getWindowTitle(String processId);
    
    /**
     * Starts monitoring for Zoom processes.
     * 
     * @return Flux of ProcessEvent objects representing process changes
     */
    public Flux<ProcessEvent> startMonitoring() {
        logger.info("Starting process monitoring for Zoom meetings");
        isMonitoring = true;
        
        return Flux.interval(POLLING_INTERVAL)
            .flatMap(tick -> detectProcessChanges())
            .doOnComplete(() -> {
                isMonitoring = false;
                logger.info("Process monitoring stopped");
            })
            .doOnError(error -> {
                isMonitoring = false;
                logger.error("Process monitoring error", error);
            });
    }
    
    /**
     * Stops monitoring for Zoom processes.
     */
    public void stopMonitoring() {
        logger.info("Stopping process monitoring");
        isMonitoring = false;
    }
    
    /**
     * Gets the current monitoring status.
     * 
     * @return true if monitoring is active, false otherwise
     */
    public boolean isMonitoring() {
        return isMonitoring;
    }
    
    /**
     * Detects changes in Zoom processes since the last check.
     * 
     * @return Flux of ProcessEvent objects representing detected changes
     */
    private Flux<ProcessEvent> detectProcessChanges() {
        return getCurrentProcesses()
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(currentProcesses -> {
                var currentProcessMap = new ConcurrentHashMap<String, ProcessInfo>();
                currentProcesses.forEach(process -> 
                    currentProcessMap.put(process.processId(), process));
                
                // Find new processes
                var newProcesses = currentProcesses.stream()
                    .filter(process -> !activeProcesses.containsKey(process.processId()))
                    .toList();
                
                // Find ended processes
                var endedProcesses = activeProcesses.entrySet().stream()
                    .filter(entry -> !currentProcessMap.containsKey(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .toList();
                
                // Update active processes
                activeProcesses.clear();
                activeProcesses.putAll(currentProcessMap);
                
                // Create events for new and ended processes
                var events = new java.util.ArrayList<ProcessEvent>();
                
                newProcesses.forEach(process -> {
                    var event = new ProcessEvent(
                        process.processId(),
                        process.processName(),
                        ProcessEvent.ProcessEventType.PROCESS_STARTED,
                        LocalDateTime.now()
                    );
                    events.add(event);
                    logger.debug("Detected new Zoom process: {}", process);
                });
                
                endedProcesses.forEach(process -> {
                    var event = new ProcessEvent(
                        process.processId(),
                        process.processName(),
                        ProcessEvent.ProcessEventType.PROCESS_ENDED,
                        LocalDateTime.now()
                    );
                    events.add(event);
                    logger.debug("Detected ended Zoom process: {}", process);
                    
                    // Clean up meeting mapping
                    processToMeetingMap.remove(process.processId());
                });
                
                return Flux.fromIterable(events);
            })
            .onErrorResume(error -> {
                logger.error("Error detecting process changes", error);
                return Flux.empty();
            });
    }
    
    /**
     * Associates a process ID with a meeting ID.
     * 
     * @param processId the process ID
     * @param meetingId the meeting ID
     */
    public void associateProcessWithMeeting(String processId, UUID meetingId) {
        processToMeetingMap.put(processId, meetingId);
        logger.debug("Associated process {} with meeting {}", processId, meetingId);
    }
    
    /**
     * Gets the meeting ID associated with a process.
     * 
     * @param processId the process ID
     * @return the meeting ID or null if not associated
     */
    public UUID getMeetingForProcess(String processId) {
        return processToMeetingMap.get(processId);
    }
    
    /**
     * Gets all currently active Zoom processes.
     * 
     * @return list of ProcessInfo objects
     */
    public List<ProcessInfo> getActiveZoomProcesses() {
        return List.copyOf(activeProcesses.values());
    }
    
    /**
     * Represents information about a system process.
     */
    public record ProcessInfo(
        String processId,
        String processName,
        String commandLine,
        LocalDateTime startTime,
        double cpuUsage,
        long memoryUsage
    ) {}
    
    /**
     * Represents a process-related event.
     */
    public record ProcessEvent(
        String processId,
        String processName,
        ProcessEventType eventType,
        LocalDateTime timestamp
    ) {
        public enum ProcessEventType {
            PROCESS_STARTED,
            PROCESS_ENDED,
            PROCESS_MODIFIED
        }
    }
}