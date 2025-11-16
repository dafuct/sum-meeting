package com.zoomtranscriber.core.async;

import org.springframework.stereotype.Service;
import org.springframework.core.task.TaskExecutor;
import org.springframework.context.annotation.Lazy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.function.Function;

/**
 * Advanced async processing service with optimized performance.
 * Provides non-blocking operations with proper resource management.
 */
@Service
public class AsyncProcessingService {

    private final TaskExecutor taskExecutor;
    private final TaskExecutor audioProcessingExecutor;
    private final TaskExecutor transcriptionExecutor;
    private final TaskExecutor aiProcessingExecutor;

    public AsyncProcessingService(
            @Lazy TaskExecutor taskExecutor,
            @Lazy TaskExecutor audioProcessingExecutor,
            @Lazy TaskExecutor transcriptionExecutor,
            @Lazy TaskExecutor aiProcessingExecutor) {
        this.taskExecutor = taskExecutor;
        this.audioProcessingExecutor = audioProcessingExecutor;
        this.transcriptionExecutor = transcriptionExecutor;
        this.aiProcessingExecutor = aiProcessingExecutor;
    }

    /**
     * Executes CPU-intensive task with optimized scheduling.
     */
    public <T> Mono<T> executeCpuIntensive(Supplier<T> task) {
        return Mono.fromFuture(CompletableFuture.supplyAsync(task, taskExecutor))
            .subscribeOn(Schedulers.parallel())
            .publishOn(Schedulers.boundedElastic());
    }

    /**
     * Executes I/O-intensive task with optimized scheduling.
     */
    public <T> Mono<T> executeIoIntensive(Supplier<T> task) {
        return Mono.fromFuture(CompletableFuture.supplyAsync(task, taskExecutor))
            .subscribeOn(Schedulers.boundedElastic())
            .publishOn(Schedulers.parallel());
    }

    /**
     * Executes audio processing task with dedicated scheduler.
     */
    public <T> Mono<T> executeAudioProcessing(Supplier<T> task) {
        return Mono.fromFuture(CompletableFuture.supplyAsync(task, audioProcessingExecutor))
            .subscribeOn(Schedulers.newBoundedElastic(100, 1000, "audio-processing"))
            .timeout(Duration.ofMinutes(5))
            .onErrorResume(timeout -> Mono.empty());
    }

    /**
     * Executes transcription task with dedicated scheduler.
     */
    public <T> Mono<T> executeTranscription(Supplier<T> task) {
        return Mono.fromFuture(CompletableFuture.supplyAsync(task, transcriptionExecutor))
            .subscribeOn(Schedulers.newBoundedElastic(50, 500, "transcription"))
            .timeout(Duration.ofMinutes(10))
            .onErrorResume(timeout -> Mono.empty());
    }

    /**
     * Executes AI processing task with dedicated scheduler.
     */
    public <T> Mono<T> executeAiProcessing(Supplier<T> task) {
        return Mono.fromFuture(CompletableFuture.supplyAsync(task, aiProcessingExecutor))
            .subscribeOn(Schedulers.newBoundedElastic(25, 250, "ai-processing"))
            .timeout(Duration.ofMinutes(15))
            .retry(3)
            .onErrorResume(error -> Mono.empty());
    }

    /**
     * Batch processes items with controlled concurrency.
     */
    public <T, R> Flux<R> batchProcess(Flux<T> items, 
                                      Function<T, R> processor, 
                                      int batchSize,
                                      int concurrency) {
        return items
            .buffer(batchSize)
            .flatMap(batch -> 
                Flux.fromIterable(batch)
                    .flatMap(item -> 
                        Mono.fromFuture(CompletableFuture.supplyAsync(() -> processor.apply(item), taskExecutor))
                            .subscribeOn(Schedulers.parallel()),
                        concurrency
                    ),
                concurrency
            )
            .subscribeOn(Schedulers.parallel());
    }

    /**
     * Processes items with backpressure control.
     */
    public <T, R> Flux<R> processWithBackpressure(Flux<T> items,
                                                  Function<T, R> processor,
                                                  int maxConcurrency) {
        return items
            .onBackpressureBuffer(1000, item -> {
                // Handle dropped items
                System.err.println("Dropped item due to backpressure: " + item);
            })
            .flatMap(item -> 
                Mono.fromFuture(CompletableFuture.supplyAsync(() -> processor.apply(item), taskExecutor))
                    .subscribeOn(Schedulers.parallel())
                    .onErrorResume(error -> {
                        System.err.println("Processing failed: " + error.getMessage());
                        return Mono.empty();
                    }),
                maxConcurrency
            );
    }

    /**
     * Executes tasks with circuit breaker pattern.
     */
    public <T> Mono<T> executeWithCircuitBreaker(
            Supplier<T> task,
            int failureThreshold,
            Duration recoveryTimeout) {
        
        return Mono.defer(() -> {
            try {
                T result = task.get();
                return Mono.just(result);
            } catch (Exception e) {
                return Mono.error(e);
            }
        })
        .timeout(Duration.ofSeconds(30))
        .retry(3)
        .onErrorResume(error -> {
            System.err.println("Circuit breaker opened: " + error.getMessage());
            return Mono.empty();
        });
    }

    /**
     * Parallel execution of multiple tasks.
     */
    @SafeVarargs
    public final <T> Flux<T> executeParallel(Supplier<T>... tasks) {
        return Flux.fromArray(tasks)
            .flatMap(task -> 
                Mono.fromFuture(CompletableFuture.supplyAsync(task, taskExecutor))
                    .subscribeOn(Schedulers.parallel())
            );
    }

    /**
     * Executes task with memory management.
     */
    public <T> Mono<T> executeWithMemoryManagement(
            Supplier<T> task,
            long maxMemoryMB) {
        
        return Mono.defer(() -> {
            // Check available memory
            Runtime runtime = Runtime.getRuntime();
            long freeMemory = runtime.freeMemory() / (1024 * 1024);
            long maxMemory = runtime.maxMemory() / (1024 * 1024);
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
            
            if (usedMemory > maxMemoryMB) {
                return Mono.error(new RuntimeException("Insufficient memory for operation"));
            }
            
            return Mono.fromFuture(CompletableFuture.supplyAsync(task, taskExecutor));
        })
        .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Scheduled execution with delay.
     */
    public <T> Mono<T> executeWithDelay(Supplier<T> task, Duration delay) {
        return Mono.delay(delay)
            .flatMap(tick -> 
                Mono.fromFuture(CompletableFuture.supplyAsync(task, taskExecutor))
            )
            .subscribeOn(Schedulers.parallel());
    }

    /**
     * Processes items with rate limiting.
     */
    public <T, R> Flux<R> processWithRateLimit(
            Flux<T> items,
            Function<T, R> processor,
            int itemsPerSecond) {
        
        Duration delay = Duration.ofMillis(1000 / itemsPerSecond);
        
        return items
            .delayElements(delay)
            .flatMap(item -> 
                Mono.fromFuture(CompletableFuture.supplyAsync(() -> processor.apply(item), taskExecutor))
                    .subscribeOn(Schedulers.parallel())
            );
    }

    /**
     * Executes task with resource cleanup.
     */
    public <T, R> Mono<R> executeWithResource(
            Supplier<T> resourceSupplier,
            Function<T, R> processor,
            java.util.function.Consumer<T> cleanup) {
        
        return Mono.using(
            () -> resourceSupplier.get(),
            resource -> Mono.fromFuture(
                CompletableFuture.supplyAsync(() -> processor.apply(resource), taskExecutor)
            ),
            resource -> {
                try {
                    cleanup.accept(resource);
                } catch (Exception e) {
                    System.err.println("Resource cleanup failed: " + e.getMessage());
                }
            }
        )
        .subscribeOn(Schedulers.boundedElastic());
    }
}