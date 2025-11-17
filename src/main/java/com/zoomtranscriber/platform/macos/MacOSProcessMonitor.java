package com.zoomtranscriber.platform.macos;

import com.zoomtranscriber.core.detection.ProcessMonitor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple mock implementation of ProcessMonitor for testing.
 * This simulates process detection without requiring any external dependencies.
 */
public class MacOSProcessMonitor extends ProcessMonitor {

    private volatile boolean isMonitoring = false;

    @Override
    protected Mono<List<ProcessInfo>> getCurrentProcesses() {
        // Return mock empty list
        return Mono.just(new ArrayList<>());
    }

    @Override
    protected Mono<String> getWindowTitle(String processId) {
        // Return mock window title
        return Mono.just("Mock Zoom Meeting Window");
    }

    @Override
    public Flux<ProcessEvent> startMonitoring() {
        return Flux.<ProcessEvent>create(emitter -> {
            isMonitoring = true;
            // Send just one mock event and complete
            var mockEvent = new ProcessEvent(
                "mock-zoom-process",
                "zoom.us",
                ProcessEvent.ProcessEventType.PROCESS_STARTED,
                LocalDateTime.now()
            );
            emitter.next(mockEvent);
            emitter.complete();
        });
    }

    @Override
    public void stopMonitoring() {
        isMonitoring = false;
    }

    @Override
    public boolean isMonitoring() {
        return isMonitoring;
    }
}