package com.zoomtranscriber.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.zoomtranscriber.core.detection.ZoomDetectionService.MeetingEvent;
import com.zoomtranscriber.core.detection.ZoomDetectionService.MeetingState;
import com.zoomtranscriber.core.ai.OllamaService;

/**
 * Web configuration for the Zoom Transcriber application.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public com.zoomtranscriber.core.detection.ZoomDetectionService mockDetectionService() {
        return new com.zoomtranscriber.core.detection.ZoomDetectionService() {
            @Override
            public reactor.core.publisher.Mono<Void> startMonitoring() {
                return reactor.core.publisher.Mono.empty();
            }

            @Override
            public reactor.core.publisher.Mono<Void> stopMonitoring() {
                return reactor.core.publisher.Mono.empty();
            }

            @Override
            public boolean isMonitoring() {
                return false;
            }

            @Override
            public reactor.core.publisher.Flux<MeetingEvent> getMeetingEvents() {
                return reactor.core.publisher.Flux.empty();
            }

            @Override
            public reactor.core.publisher.Mono<MeetingState> getCurrentMeetingState(java.util.UUID meetingId) {
                return reactor.core.publisher.Mono.empty();
            }

            @Override
            public reactor.core.publisher.Flux<MeetingState> getActiveMeetings() {
                return reactor.core.publisher.Flux.empty();
            }

            @Override
            public reactor.core.publisher.Mono<Void> triggerDetectionScan() {
                return reactor.core.publisher.Mono.empty();
            }
        };
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public OllamaService mockOllamaService() {
        return new OllamaService() {
            @Override
            public reactor.core.publisher.Mono<String> generateText(String model, String prompt) {
                return reactor.core.publisher.Mono.just("Mock response for: " + prompt);
            }

            @Override
            public reactor.core.publisher.Flux<String> generateTextStream(String model, String prompt) {
                return reactor.core.publisher.Flux.just("Mock stream response");
            }

            @Override
            public reactor.core.publisher.Mono<String> generateTextWithOptions(String model, String prompt, OllamaService.GenerationOptions options) {
                return reactor.core.publisher.Mono.just("Mock response with options for: " + prompt);
            }

            @Override
            public reactor.core.publisher.Flux<String> generateTextStreamWithOptions(String model, String prompt, OllamaService.GenerationOptions options) {
                return reactor.core.publisher.Flux.just("Mock stream response with options");
            }

            @Override
            public reactor.core.publisher.Mono<Boolean> checkHealth() {
                return reactor.core.publisher.Mono.just(true);
            }

            @Override
            public reactor.core.publisher.Mono<String> getVersion() {
                return reactor.core.publisher.Mono.just("mock-version-1.0.0");
            }

            @Override
            public reactor.core.publisher.Flux<OllamaService.ModelInfo> listModels() {
                OllamaService.ModelInfo mockModel = new OllamaService.ModelInfo(
                    "mock-model", "mock-model", "2024-01-01", "1MB",
                    "mock-digest", "mock-details", "mock-family", "1",
                    "gguf", "1M", "Q4_0"
                );
                return reactor.core.publisher.Flux.just(mockModel);
            }

            @Override
            public reactor.core.publisher.Mono<OllamaService.ModelInfo> getModelInfo(String model) {
                OllamaService.ModelInfo mockModel = new OllamaService.ModelInfo(
                    model, model, "2024-01-01", "1MB",
                    "mock-digest", "mock-details", "mock-family", "1",
                    "gguf", "1M", "Q4_0"
                );
                return reactor.core.publisher.Mono.just(mockModel);
            }

            @Override
            public reactor.core.publisher.Mono<Boolean> isModelAvailable(String model) {
                return reactor.core.publisher.Mono.just(true);
            }

            @Override
            public reactor.core.publisher.Flux<OllamaService.DownloadProgress> pullModel(String model) {
                OllamaService.DownloadProgress mockProgress = new OllamaService.DownloadProgress(
                    "success", "mock-digest", "1000000", "1000000"
                );
                return reactor.core.publisher.Flux.just(mockProgress);
            }

            @Override
            public reactor.core.publisher.Mono<Void> deleteModel(String model) {
                return reactor.core.publisher.Mono.empty();
            }

            @Override
            public reactor.core.publisher.Mono<OllamaService.ResourceUsage> getResourceUsage() {
                OllamaService.ResourceUsage mockUsage = new OllamaService.ResourceUsage(
                    8000000000L, 4000000000L, 4000000000L, 50.0,
                    100000000000L, 50000000000L, 50000000000L
                );
                return reactor.core.publisher.Mono.just(mockUsage);
            }

            @Override
            public reactor.core.publisher.Mono<Void> createModel(String model, OllamaService.ModelConfig config) {
                return reactor.core.publisher.Mono.empty();
            }

            @Override
            public reactor.core.publisher.Mono<Void> copyModel(String source, String destination) {
                return reactor.core.publisher.Mono.empty();
            }

            @Override
            public reactor.core.publisher.Mono<java.util.List<Double>> embedText(String model, String text) {
                return reactor.core.publisher.Mono.just(java.util.List.of(0.1, 0.2, 0.3, 0.4));
            }

            @Override
            public reactor.core.publisher.Flux<java.util.List<Double>> embedBatch(String model, java.util.List<String> texts) {
                return reactor.core.publisher.Flux.just(java.util.List.of(0.1, 0.2, 0.3, 0.4));
            }

            @Override
            public reactor.core.publisher.Mono<Void> cancelGeneration(String requestId) {
                return reactor.core.publisher.Mono.empty();
            }
        };
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:4200", "http://127.0.0.1:4200")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}