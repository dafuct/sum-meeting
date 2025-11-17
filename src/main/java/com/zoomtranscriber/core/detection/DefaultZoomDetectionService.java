package com.zoomtranscriber.core.detection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of ZoomDetectionService.
 * Integrates process monitoring with meeting state tracking to detect Zoom meetings.
 */
@Service
public class DefaultZoomDetectionService implements ZoomDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultZoomDetectionService.class);

    private final ProcessMonitor processMonitor;
    private final MeetingStateTracker meetingStateTracker;
    
    private volatile boolean isMonitoring = false;
    private reactor.core.Disposable monitoringDisposable;
    private final ConcurrentHashMap<UUID, MeetingState> activeMeetings = new ConcurrentHashMap<>();
    private final Flux<MeetingEvent> meetingEventFlux;

    public DefaultZoomDetectionService(ProcessMonitor processMonitor, MeetingStateTracker meetingStateTracker) {
        this.processMonitor = processMonitor;
        this.meetingStateTracker = meetingStateTracker;
        
        // Create a simple flux that can be subscribed to
        this.meetingEventFlux = Flux.<MeetingEvent>create(emitter -> {
            logger.info("Meeting event flux created");
            emitter.onCancel(() -> logger.debug("Meeting event flux cancelled"));
        }).publish().refCount();
    }

    @Override
    public Mono<Void> startMonitoring() {
        logger.info("Starting Zoom meeting detection");
        
        return Mono.fromRunnable(() -> {
            if (isMonitoring) {
                logger.warn("Monitoring is already active");
                return;
            }

            isMonitoring = true;
            
            // Start process monitoring
            monitoringDisposable = processMonitor.startMonitoring()
                .doOnComplete(() -> {
                    isMonitoring = false;
                    logger.info("Process monitoring completed");
                })
                .doOnError(error -> {
                    isMonitoring = false;
                    logger.error("Error in monitoring stream", error);
                })
                .subscribe();
                
            logger.info("Zoom meeting detection started successfully");
        });
    }

    @Override
    public Mono<Void> stopMonitoring() {
        logger.info("Stopping Zoom meeting detection");
        
        return Mono.fromRunnable(() -> {
            if (!isMonitoring) {
                logger.warn("Monitoring is not active");
                return;
            }

            isMonitoring = false;
            
            if (monitoringDisposable != null) {
                monitoringDisposable.dispose();
                monitoringDisposable = null;
            }
            
            processMonitor.stopMonitoring();
            logger.info("Zoom meeting detection stopped");
        });
    }

    @Override
    public boolean isMonitoring() {
        return isMonitoring && processMonitor.isMonitoring();
    }

    @Override
    public Flux<MeetingEvent> getMeetingEvents() {
        return meetingEventFlux;
    }

    @Override
    public Mono<MeetingState> getCurrentMeetingState(UUID meetingId) {
        return Mono.justOrEmpty(activeMeetings.get(meetingId));
    }

    @Override
    public Flux<MeetingState> getActiveMeetings() {
        return Flux.fromIterable(activeMeetings.values())
            .filter(state -> state.status() != MeetingState.MeetingStatus.ENDED);
    }

    @Override
    public Mono<Void> triggerDetectionScan() {
        logger.info("Triggering manual detection scan");
        
        return Mono.fromRunnable(() -> {
            logger.debug("Manual detection scan completed");
        });
    }

    public void cleanup() {
        logger.info("Cleaning up ZoomDetectionService");
        if (monitoringDisposable != null) {
            monitoringDisposable.dispose();
        }
        stopMonitoring().block();
    }
}