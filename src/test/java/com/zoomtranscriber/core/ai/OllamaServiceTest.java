package com.zoomtranscriber.core.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OllamaService interface.
 * Tests AI model functionality, text generation, and model management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OllamaService Tests")
class OllamaServiceTest {
    
    @Mock
    private OllamaService ollamaService;
    
    private String testModel;
    private String testPrompt;
    private GenerationOptions testOptions;
    private ModelInfo testModelInfo;
    private DownloadProgress testDownloadProgress;
    private ResourceUsage testResourceUsage;
    private ModelConfig testModelConfig;
    
    @BeforeEach
    void setUp() {
        testModel = "qwen2.5:0.5b";
        testPrompt = "Hello, how are you?";
        testOptions = GenerationOptions.defaults();
        testModelInfo = new ModelInfo(
            "qwen2.5:0.5b", "qwen2.5", "2023-12-01T10:00:00Z",
            "467MB", "sha256:abc123", "LLaMA 2 7B", "llama",
            "7B", "GGUF", "0.5B", "Q4_0"
        );
        testDownloadProgress = new DownloadProgress(
            "downloading", "sha256:abc123", "1000000", "500000"
        );
        testResourceUsage = new ResourceUsage(
            8589934592L, 4294967296L, 4294967296L, 50.0,
            107374182400L, 53687091200L, 53687091200L
        );
        testModelConfig = ModelConfig.basic("custom-model", "qwen2.5:0.5b");
    }
    
    @Test
    @DisplayName("Should generate text successfully")
    void shouldGenerateTextSuccessfully() {
        // Given
        var expectedResponse = "I'm doing well, thank you!";
        when(ollamaService.generateText(testModel, testPrompt))
            .thenReturn(Mono.just(expectedResponse));
        
        // When
        var result = ollamaService.generateText(testModel, testPrompt);
        
        // Then
        StepVerifier.create(result)
            .expectNext(expectedResponse)
            .verifyComplete();
        
        verify(ollamaService, times(1)).generateText(testModel, testPrompt);
    }
    
    @Test
    @DisplayName("Should handle text generation failure")
    void shouldHandleTextGenerationFailure() {
        // Given
        var error = new RuntimeException("Model not found");
        when(ollamaService.generateText(testModel, testPrompt))
            .thenReturn(Mono.error(error));
        
        // When
        var result = ollamaService.generateText(testModel, testPrompt);
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Model not found"))
            .verify();
        
        verify(ollamaService, times(1)).generateText(testModel, testPrompt);
    }
    
    @Test
    @DisplayName("Should generate text stream successfully")
    void shouldGenerateTextStreamSuccessfully() {
        // Given
        var expectedChunks = List.of("I'm", " doing", " well", "!");
        when(ollamaService.generateTextStream(testModel, testPrompt))
            .thenReturn(Flux.fromIterable(expectedChunks));
        
        // When
        var result = ollamaService.generateTextStream(testModel, testPrompt);
        
        // Then
        StepVerifier.create(result)
            .expectNext("I'm")
            .expectNext(" doing")
            .expectNext(" well")
            .expectNext("!")
            .verifyComplete();
        
        verify(ollamaService, times(1)).generateTextStream(testModel, testPrompt);
    }
    
    @Test
    @DisplayName("Should handle empty text generation stream")
    void shouldHandleEmptyTextGenerationStream() {
        // Given
        when(ollamaService.generateTextStream(testModel, testPrompt))
            .thenReturn(Flux.empty());
        
        // When
        var result = ollamaService.generateTextStream(testModel, testPrompt);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(ollamaService, times(1)).generateTextStream(testModel, testPrompt);
    }
    
    @Test
    @DisplayName("Should generate text with options successfully")
    void shouldGenerateTextWithOptionsSuccessfully() {
        // Given
        var expectedResponse = "Response with custom options";
        when(ollamaService.generateTextWithOptions(eq(testModel), eq(testPrompt), any(GenerationOptions.class)))
            .thenReturn(Mono.just(expectedResponse));
        
        // When
        var result = ollamaService.generateTextWithOptions(testModel, testPrompt, testOptions);
        
        // Then
        StepVerifier.create(result)
            .expectNext(expectedResponse)
            .verifyComplete();
        
        verify(ollamaService, times(1)).generateTextWithOptions(testModel, testPrompt, testOptions);
    }
    
    @Test
    @DisplayName("Should generate text with options and streaming successfully")
    void shouldGenerateTextWithOptionsAndStreamingSuccessfully() {
        // Given
        var expectedChunks = List.of("Streamed", " response");
        when(ollamaService.generateTextStreamWithOptions(eq(testModel), eq(testPrompt), any(GenerationOptions.class)))
            .thenReturn(Flux.fromIterable(expectedChunks));
        
        // When
        var result = ollamaService.generateTextStreamWithOptions(testModel, testPrompt, testOptions);
        
        // Then
        StepVerifier.create(result)
            .expectNext("Streamed")
            .expectNext(" response")
            .verifyComplete();
        
        verify(ollamaService, times(1)).generateTextStreamWithOptions(testModel, testPrompt, testOptions);
    }
    
    @Test
    @DisplayName("Should check health successfully when service is healthy")
    void shouldCheckHealthSuccessfullyWhenServiceIsHealthy() {
        // Given
        when(ollamaService.checkHealth()).thenReturn(Mono.just(true));
        
        // When
        var result = ollamaService.checkHealth();
        
        // Then
        StepVerifier.create(result)
            .expectNext(true)
            .verifyComplete();
        
        verify(ollamaService, times(1)).checkHealth();
    }
    
    @Test
    @DisplayName("Should check health successfully when service is unhealthy")
    void shouldCheckHealthSuccessfullyWhenServiceIsUnhealthy() {
        // Given
        when(ollamaService.checkHealth()).thenReturn(Mono.just(false));
        
        // When
        var result = ollamaService.checkHealth();
        
        // Then
        StepVerifier.create(result)
            .expectNext(false)
            .verifyComplete();
        
        verify(ollamaService, times(1)).checkHealth();
    }
    
    @Test
    @DisplayName("Should handle health check failure")
    void shouldHandleHealthCheckFailure() {
        // Given
        var error = new RuntimeException("Health check failed");
        when(ollamaService.checkHealth()).thenReturn(Mono.error(error));
        
        // When
        var result = ollamaService.checkHealth();
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Health check failed"))
            .verify();
        
        verify(ollamaService, times(1)).checkHealth();
    }
    
    @Test
    @DisplayName("Should get version successfully")
    void shouldGetVersionSuccessfully() {
        // Given
        var expectedVersion = "0.1.42";
        when(ollamaService.getVersion()).thenReturn(Mono.just(expectedVersion));
        
        // When
        var result = ollamaService.getVersion();
        
        // Then
        StepVerifier.create(result)
            .expectNext(expectedVersion)
            .verifyComplete();
        
        verify(ollamaService, times(1)).getVersion();
    }
    
    @Test
    @DisplayName("Should handle version retrieval failure")
    void shouldHandleVersionRetrievalFailure() {
        // Given
        var error = new RuntimeException("Failed to get version");
        when(ollamaService.getVersion()).thenReturn(Mono.error(error));
        
        // When
        var result = ollamaService.getVersion();
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Failed to get version"))
            .verify();
        
        verify(ollamaService, times(1)).getVersion();
    }
    
    @Test
    @DisplayName("Should list models successfully")
    void shouldListModelsSuccessfully() {
        // Given
        var expectedModels = List.of(testModelInfo);
        when(ollamaService.listModels()).thenReturn(Flux.fromIterable(expectedModels));
        
        // When
        var result = ollamaService.listModels();
        
        // Then
        StepVerifier.create(result)
            .expectNext(testModelInfo)
            .verifyComplete();
        
        verify(ollamaService, times(1)).listModels();
    }
    
    @Test
    @DisplayName("Should handle empty models list")
    void shouldHandleEmptyModelsList() {
        // Given
        when(ollamaService.listModels()).thenReturn(Flux.empty());
        
        // When
        var result = ollamaService.listModels();
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(ollamaService, times(1)).listModels();
    }
    
    @Test
    @DisplayName("Should get model info successfully")
    void shouldGetModelInfoSuccessfully() {
        // Given
        when(ollamaService.getModelInfo(testModel)).thenReturn(Mono.just(testModelInfo));
        
        // When
        var result = ollamaService.getModelInfo(testModel);
        
        // Then
        StepVerifier.create(result)
            .expectNext(testModelInfo)
            .verifyComplete();
        
        verify(ollamaService, times(1)).getModelInfo(testModel);
    }
    
    @Test
    @DisplayName("Should handle model info not found")
    void shouldHandleModelInfoNotFound() {
        // Given
        when(ollamaService.getModelInfo(testModel)).thenReturn(Mono.empty());
        
        // When
        var result = ollamaService.getModelInfo(testModel);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(ollamaService, times(1)).getModelInfo(testModel);
    }
    
    @Test
    @DisplayName("Should check model availability successfully when available")
    void shouldCheckModelAvailabilitySuccessfullyWhenAvailable() {
        // Given
        when(ollamaService.isModelAvailable(testModel)).thenReturn(Mono.just(true));
        
        // When
        var result = ollamaService.isModelAvailable(testModel);
        
        // Then
        StepVerifier.create(result)
            .expectNext(true)
            .verifyComplete();
        
        verify(ollamaService, times(1)).isModelAvailable(testModel);
    }
    
    @Test
    @DisplayName("Should check model availability successfully when not available")
    void shouldCheckModelAvailabilitySuccessfullyWhenNotAvailable() {
        // Given
        when(ollamaService.isModelAvailable(testModel)).thenReturn(Mono.just(false));
        
        // When
        var result = ollamaService.isModelAvailable(testModel);
        
        // Then
        StepVerifier.create(result)
            .expectNext(false)
            .verifyComplete();
        
        verify(ollamaService, times(1)).isModelAvailable(testModel);
    }
    
    @Test
    @DisplayName("Should pull model successfully")
    void shouldPullModelSuccessfully() {
        // Given
        var expectedProgress = List.of(testDownloadProgress);
        when(ollamaService.pullModel(testModel)).thenReturn(Flux.fromIterable(expectedProgress));
        
        // When
        var result = ollamaService.pullModel(testModel);
        
        // Then
        StepVerifier.create(result)
            .expectNext(testDownloadProgress)
            .verifyComplete();
        
        verify(ollamaService, times(1)).pullModel(testModel);
    }
    
    @Test
    @DisplayName("Should handle model pulling failure")
    void shouldHandleModelPullingFailure() {
        // Given
        var error = new RuntimeException("Failed to pull model");
        when(ollamaService.pullModel(testModel)).thenReturn(Flux.error(error));
        
        // When
        var result = ollamaService.pullModel(testModel);
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Failed to pull model"))
            .verify();
        
        verify(ollamaService, times(1)).pullModel(testModel);
    }
    
    @Test
    @DisplayName("Should delete model successfully")
    void shouldDeleteModelSuccessfully() {
        // Given
        when(ollamaService.deleteModel(testModel)).thenReturn(Mono.empty());
        
        // When
        var result = ollamaService.deleteModel(testModel);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(ollamaService, times(1)).deleteModel(testModel);
    }
    
    @Test
    @DisplayName("Should handle model deletion failure")
    void shouldHandleModelDeletionFailure() {
        // Given
        var error = new RuntimeException("Failed to delete model");
        when(ollamaService.deleteModel(testModel)).thenReturn(Mono.error(error));
        
        // When
        var result = ollamaService.deleteModel(testModel);
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Failed to delete model"))
            .verify();
        
        verify(ollamaService, times(1)).deleteModel(testModel);
    }
    
    @Test
    @DisplayName("Should get resource usage successfully")
    void shouldGetResourceUsageSuccessfully() {
        // Given
        when(ollamaService.getResourceUsage()).thenReturn(Mono.just(testResourceUsage));
        
        // When
        var result = ollamaService.getResourceUsage();
        
        // Then
        StepVerifier.create(result)
            .expectNext(testResourceUsage)
            .verifyComplete();
        
        verify(ollamaService, times(1)).getResourceUsage();
    }
    
    @Test
    @DisplayName("Should handle resource usage retrieval failure")
    void shouldHandleResourceUsageRetrievalFailure() {
        // Given
        var error = new RuntimeException("Failed to get resource usage");
        when(ollamaService.getResourceUsage()).thenReturn(Mono.error(error));
        
        // When
        var result = ollamaService.getResourceUsage();
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Failed to get resource usage"))
            .verify();
        
        verify(ollamaService, times(1)).getResourceUsage();
    }
    
    @Test
    @DisplayName("Should create model successfully")
    void shouldCreateModelSuccessfully() {
        // Given
        when(ollamaService.createModel(eq(testModel), any(ModelConfig.class)))
            .thenReturn(Mono.empty());
        
        // When
        var result = ollamaService.createModel(testModel, testModelConfig);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(ollamaService, times(1)).createModel(testModel, testModelConfig);
    }
    
    @Test
    @DisplayName("Should handle model creation failure")
    void shouldHandleModelCreationFailure() {
        // Given
        var error = new RuntimeException("Failed to create model");
        when(ollamaService.createModel(eq(testModel), any(ModelConfig.class)))
            .thenReturn(Mono.error(error));
        
        // When
        var result = ollamaService.createModel(testModel, testModelConfig);
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Failed to create model"))
            .verify();
        
        verify(ollamaService, times(1)).createModel(testModel, testModelConfig);
    }
    
    @Test
    @DisplayName("Should copy model successfully")
    void shouldCopyModelSuccessfully() {
        // Given
        var sourceModel = "qwen2.5:0.5b";
        var destModel = "custom-qwen2.5:0.5b";
        when(ollamaService.copyModel(sourceModel, destModel)).thenReturn(Mono.empty());
        
        // When
        var result = ollamaService.copyModel(sourceModel, destModel);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(ollamaService, times(1)).copyModel(sourceModel, destModel);
    }
    
    @Test
    @DisplayName("Should handle model copy failure")
    void shouldHandleModelCopyFailure() {
        // Given
        var sourceModel = "qwen2.5:0.5b";
        var destModel = "custom-qwen2.5:0.5b";
        var error = new RuntimeException("Failed to copy model");
        when(ollamaService.copyModel(sourceModel, destModel)).thenReturn(Mono.error(error));
        
        // When
        var result = ollamaService.copyModel(sourceModel, destModel);
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Failed to copy model"))
            .verify();
        
        verify(ollamaService, times(1)).copyModel(sourceModel, destModel);
    }
    
    @Test
    @DisplayName("Should embed text successfully")
    void shouldEmbedTextSuccessfully() {
        // Given
        var text = "Hello world";
        var expectedEmbedding = List.of(0.1, 0.2, 0.3);
        when(ollamaService.embedText(testModel, text)).thenReturn(Mono.just(expectedEmbedding));
        
        // When
        var result = ollamaService.embedText(testModel, text);
        
        // Then
        StepVerifier.create(result)
            .expectNext(expectedEmbedding)
            .verifyComplete();
        
        verify(ollamaService, times(1)).embedText(testModel, text);
    }
    
    @Test
    @DisplayName("Should handle text embedding failure")
    void shouldHandleTextEmbeddingFailure() {
        // Given
        var text = "Hello world";
        var error = new RuntimeException("Failed to embed text");
        when(ollamaService.embedText(testModel, text)).thenReturn(Mono.error(error));
        
        // When
        var result = ollamaService.embedText(testModel, text);
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Failed to embed text"))
            .verify();
        
        verify(ollamaService, times(1)).embedText(testModel, text);
    }
    
    @Test
    @DisplayName("Should embed batch successfully")
    void shouldEmbedBatchSuccessfully() {
        // Given
        var texts = List.of("Hello", "world", "test");
        var expectedEmbeddings = List.of(
            List.of(0.1, 0.2, 0.3),
            List.of(0.4, 0.5, 0.6),
            List.of(0.7, 0.8, 0.9)
        );
        when(ollamaService.embedBatch(eq(testModel), any(List.class)))
            .thenReturn(Flux.fromIterable(expectedEmbeddings));
        
        // When
        var result = ollamaService.embedBatch(testModel, texts);
        
        // Then
        StepVerifier.create(result)
            .expectNext(List.of(0.1, 0.2, 0.3))
            .expectNext(List.of(0.4, 0.5, 0.6))
            .expectNext(List.of(0.7, 0.8, 0.9))
            .verifyComplete();
        
        verify(ollamaService, times(1)).embedBatch(testModel, texts);
    }
    
    @Test
    @DisplayName("Should handle empty batch embedding")
    void shouldHandleEmptyBatchEmbedding() {
        // Given
        var emptyTexts = List.<String>of();
        when(ollamaService.embedBatch(eq(testModel), any(List.class)))
            .thenReturn(Flux.empty());
        
        // When
        var result = ollamaService.embedBatch(testModel, emptyTexts);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(ollamaService, times(1)).embedBatch(testModel, emptyTexts);
    }
    
    @Test
    @DisplayName("Should cancel generation successfully")
    void shouldCancelGenerationSuccessfully() {
        // Given
        var requestId = "req-123";
        when(ollamaService.cancelGeneration(requestId)).thenReturn(Mono.empty());
        
        // When
        var result = ollamaService.cancelGeneration(requestId);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(ollamaService, times(1)).cancelGeneration(requestId);
    }
    
    @Test
    @DisplayName("Should handle generation cancellation failure")
    void shouldHandleGenerationCancellationFailure() {
        // Given
        var requestId = "req-123";
        var error = new RuntimeException("Failed to cancel generation");
        when(ollamaService.cancelGeneration(requestId)).thenReturn(Mono.error(error));
        
        // When
        var result = ollamaService.cancelGeneration(requestId);
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Failed to cancel generation"))
            .verify();
        
        verify(ollamaService, times(1)).cancelGeneration(requestId);
    }
    
    @Test
    @DisplayName("Should create generation options record correctly")
    void shouldCreateGenerationOptionsRecordCorrectly() {
        // When
        var options = new GenerationOptions(
            testOptions.temperature(), testOptions.topK(), testOptions.topP(),
            testOptions.maxTokens(), testOptions.repeatPenalty(), testOptions.repeatLastN(),
            testOptions.seed(), testOptions.stopSequences(), testOptions.stop(),
            testOptions.typicalP(), testOptions.repeatPenaltyLastN(), testOptions.mirostat(),
            testOptions.mirostatTau(), testOptions.mirostatEta(), testOptions.numCtx(),
            testOptions.numBatch(), testOptions.numGqa(), testOptions.numGpu(),
            testOptions.mainGpu(), testOptions.lowVram(), testOptions.f16Kv(),
            testOptions.logitsAll(), testOptions.vocabOnly(), testOptions.useMmap(),
            testOptions.useMlock(), testOptions.numThread(), testOptions.numKeep(),
            testOptions.seedRng()
        );
        
        // Then
        assertEquals(testOptions.temperature(), options.temperature());
        assertEquals(testOptions.topK(), options.topK());
        assertEquals(testOptions.topP(), options.topP());
        assertEquals(testOptions.maxTokens(), options.maxTokens());
        assertEquals(testOptions.repeatPenalty(), options.repeatPenalty());
    }
    
    @Test
    @DisplayName("Should create default generation options")
    void shouldCreateDefaultGenerationOptions() {
        // When
        var options = GenerationOptions.defaults();
        
        // Then
        assertEquals(0.7, options.temperature());
        assertEquals(40, options.topK());
        assertEquals(0.9, options.topP());
        assertEquals(2048, options.maxTokens());
        assertEquals(1.1, options.repeatPenalty());
        assertTrue(options.useMmap());
        assertFalse(options.useMlock());
    }
    
    @Test
    @DisplayName("Should create fast generation options")
    void shouldCreateFastGenerationOptions() {
        // When
        var options = GenerationOptions.fast();
        
        // Then
        assertEquals(0.5, options.temperature());
        assertEquals(20, options.topK());
        assertEquals(0.8, options.topP());
        assertEquals(1024, options.maxTokens());
        assertEquals(1.0, options.repeatPenalty());
        assertTrue(options.useMmap());
        assertFalse(options.useMlock());
    }
    
    @Test
    @DisplayName("Should create creative generation options")
    void shouldCreateCreativeGenerationOptions() {
        // When
        var options = GenerationOptions.creative();
        
        // Then
        assertEquals(0.9, options.temperature());
        assertEquals(50, options.topK());
        assertEquals(0.95, options.topP());
        assertEquals(4096, options.maxTokens());
        assertEquals(1.2, options.repeatPenalty());
        assertTrue(options.useMmap());
        assertFalse(options.useMlock());
    }
    
    @Test
    @DisplayName("Should create model info record correctly")
    void shouldCreateModelInfoRecordCorrectly() {
        // When
        var info = new ModelInfo(
            testModelInfo.name(), testModelInfo.model(), testModelInfo.modifiedAt(),
            testModelInfo.size(), testModelInfo.digest(), testModelInfo.details(),
            testModelInfo.families(), testModelInfo.familiesSize(), testModelInfo.format(),
            testModelInfo.parameterSize(), testModelInfo.quantizationLevel()
        );
        
        // Then
        assertEquals(testModelInfo.name(), info.name());
        assertEquals(testModelInfo.model(), info.model());
        assertEquals(testModelInfo.modifiedAt(), info.modifiedAt());
        assertEquals(testModelInfo.size(), info.size());
        assertEquals(testModelInfo.digest(), info.digest());
    }
    
    @Test
    @DisplayName("Should get model size in bytes correctly")
    void shouldGetModelSizeInBytesCorrectly() {
        // When
        var sizeInBytes = testModelInfo.getSizeInBytes();
        
        // Then
        assertEquals(467 * 1024 * 1024L, sizeInBytes); // 467MB
    }
    
    @Test
    @DisplayName("Should handle model size with GB unit")
    void shouldHandleModelSizeWithGBUnit() {
        // Given
        var infoWithGB = new ModelInfo(
            "test", "test", "2023-12-01", "2.5GB", "sha256:abc",
            "test", "test", "test", "test", "test", "test"
        );
        
        // When
        var sizeInBytes = infoWithGB.getSizeInBytes();
        
        // Then
        assertEquals((long) (2.5 * 1024 * 1024 * 1024), sizeInBytes);
    }
    
    @Test
    @DisplayName("Should handle model size with MB unit")
    void shouldHandleModelSizeWithMBUnit() {
        // Given
        var infoWithMB = new ModelInfo(
            "test", "test", "2023-12-01", "500MB", "sha256:abc",
            "test", "test", "test", "test", "test", "test"
        );
        
        // When
        var sizeInBytes = infoWithMB.getSizeInBytes();
        
        // Then
        assertEquals(500 * 1024 * 1024L, sizeInBytes);
    }
    
    @Test
    @DisplayName("Should handle model size with KB unit")
    void shouldHandleModelSizeWithKBUnit() {
        // Given
        var infoWithKB = new ModelInfo(
            "test", "test", "2023-12-01", "1024KB", "sha256:abc",
            "test", "test", "test", "test", "test", "test"
        );
        
        // When
        var sizeInBytes = infoWithKB.getSizeInBytes();
        
        // Then
        assertEquals(1024 * 1024L, sizeInBytes);
    }
    
    @Test
    @DisplayName("Should handle model size with invalid format")
    void shouldHandleModelSizeWithInvalidFormat() {
        // Given
        var infoWithInvalid = new ModelInfo(
            "test", "test", "2023-12-01", "invalid", "sha256:abc",
            "test", "test", "test", "test", "test", "test"
        );
        
        // When
        var sizeInBytes = infoWithInvalid.getSizeInBytes();
        
        // Then
        assertEquals(0L, sizeInBytes);
    }
    
    @Test
    @DisplayName("Should create download progress record correctly")
    void shouldCreateDownloadProgressRecordCorrectly() {
        // When
        var progress = new DownloadProgress(
            testDownloadProgress.status(), testDownloadProgress.digest(),
            testDownloadProgress.total(), testDownloadProgress.completed()
        );
        
        // Then
        assertEquals(testDownloadProgress.status(), progress.status());
        assertEquals(testDownloadProgress.digest(), progress.digest());
        assertEquals(testDownloadProgress.total(), progress.total());
        assertEquals(testDownloadProgress.completed(), progress.completed());
    }
    
    @Test
    @DisplayName("Should calculate download progress percentage correctly")
    void shouldCalculateDownloadProgressPercentageCorrectly() {
        // When
        var percentage = testDownloadProgress.getProgressPercentage();
        
        // Then
        assertEquals(50.0, percentage, 0.01); // 500000 / 1000000 = 50%
    }
    
    @Test
    @DisplayName("Should handle download progress with null values")
    void shouldHandleDownloadProgressWithNullValues() {
        // Given
        var progressWithNulls = new DownloadProgress(
            "downloading", "sha256:abc", null, "500000"
        );
        
        // When
        var percentage = progressWithNulls.getProgressPercentage();
        
        // Then
        assertEquals(0.0, percentage);
    }
    
    @Test
    @DisplayName("Should check if download is complete correctly")
    void shouldCheckIfDownloadIsCompleteCorrectly() {
        // Given
        var completeProgress = new DownloadProgress(
            "success", "sha256:abc", "1000000", "1000000"
        );
        
        // When
        var isComplete = completeProgress.isComplete();
        
        // Then
        assertTrue(isComplete);
    }
    
    @Test
    @DisplayName("Should check if download is not complete correctly")
    void shouldCheckIfDownloadIsNotCompleteCorrectly() {
        // When
        var isComplete = testDownloadProgress.isComplete();
        
        // Then
        assertFalse(isComplete);
    }
    
    @Test
    @DisplayName("Should create resource usage record correctly")
    void shouldCreateResourceUsageRecordCorrectly() {
        // When
        var usage = new ResourceUsage(
            testResourceUsage.memoryTotal(), testResourceUsage.memoryUsed(),
            testResourceUsage.memoryAvailable(), testResourceUsage.cpuUsage(),
            testResourceUsage.diskTotal(), testResourceUsage.diskUsed(),
            testResourceUsage.diskAvailable()
        );
        
        // Then
        assertEquals(testResourceUsage.memoryTotal(), usage.memoryTotal());
        assertEquals(testResourceUsage.memoryUsed(), usage.memoryUsed());
        assertEquals(testResourceUsage.memoryAvailable(), usage.memoryAvailable());
        assertEquals(testResourceUsage.cpuUsage(), usage.cpuUsage());
    }
    
    @Test
    @DisplayName("Should calculate memory usage percentage correctly")
    void shouldCalculateMemoryUsagePercentageCorrectly() {
        // When
        var percentage = testResourceUsage.getMemoryUsagePercentage();
        
        // Then
        assertEquals(50.0, percentage, 0.01); // 4GB / 8GB = 50%
    }
    
    @Test
    @DisplayName("Should calculate disk usage percentage correctly")
    void shouldCalculateDiskUsagePercentageCorrectly() {
        // When
        var percentage = testResourceUsage.getDiskUsagePercentage();
        
        // Then
        assertEquals(50.0, percentage, 0.01); // 50GB / 100GB = 50%
    }
    
    @Test
    @DisplayName("Should check if system has enough memory correctly")
    void shouldCheckIfSystemHasEnoughMemoryCorrectly() {
        // When
        var hasEnough = testResourceUsage.hasEnoughMemory(2048); // 2GB required
        
        // Then
        assertTrue(hasEnough); // 4GB available > 2GB required
    }
    
    @Test
    @DisplayName("Should check if system doesn't have enough memory correctly")
    void shouldCheckIfSystemDoesntHaveEnoughMemoryCorrectly() {
        // When
        var hasEnough = testResourceUsage.hasEnoughMemory(5120); // 5GB required
        
        // Then
        assertFalse(hasEnough); // 4GB available < 5GB required
    }
    
    @Test
    @DisplayName("Should handle resource usage with zero total memory")
    void shouldHandleResourceUsageWithZeroTotalMemory() {
        // Given
        var usageWithZeroMemory = new ResourceUsage(
            0L, 0L, 0L, 0.0, 0L, 0L, 0L
        );
        
        // When
        var percentage = usageWithZeroMemory.getMemoryUsagePercentage();
        
        // Then
        assertEquals(0.0, percentage);
    }
    
    @Test
    @DisplayName("Should create model config record correctly")
    void shouldCreateModelConfigRecordCorrectly() {
        // When
        var config = new ModelConfig(
            testModelConfig.model(), testModelConfig.from(),
            testModelConfig.license(), testModelConfig.modelfile(),
            testModelConfig.template(), testModelConfig.system(),
            testModelConfig.parameters(), testModelConfig.adapter()
        );
        
        // Then
        assertEquals(testModelConfig.model(), config.model());
        assertEquals(testModelConfig.from(), config.from());
        assertEquals(testModelConfig.license(), config.license());
        assertEquals(testModelConfig.modelfile(), config.modelfile());
    }
    
    @Test
    @DisplayName("Should create basic model config")
    void shouldCreateBasicModelConfig() {
        // When
        var config = ModelConfig.basic("custom-model", "qwen2.5:0.5b");
        
        // Then
        assertEquals("custom-model", config.model());
        assertEquals("qwen2.5:0.5b", config.from());
        assertNull(config.license());
        assertNull(config.modelfile());
        assertNull(config.template());
    }
    
    @Test
    @DisplayName("Should create model config with custom template")
    void shouldCreateModelConfigWithCustomTemplate() {
        // Given
        var template = "Custom template: {{.Prompt}}";
        
        // When
        var config = ModelConfig.withTemplate("custom-model", "qwen2.5:0.5b", template);
        
        // Then
        assertEquals("custom-model", config.model());
        assertEquals("qwen2.5:0.5b", config.from());
        assertEquals(template, config.template());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "Hello", "This is a longer test prompt with multiple words"})
    @DisplayName("Should handle various prompt lengths")
    void shouldHandleVariousPromptLengths(String prompt) {
        // Given
        when(ollamaService.generateText(testModel, prompt))
            .thenReturn(Mono.just("Response"));
        
        // When
        var result = ollamaService.generateText(testModel, prompt);
        
        // Then
        StepVerifier.create(result)
            .expectNext("Response")
            .verifyComplete();
        
        verify(ollamaService, times(1)).generateText(testModel, prompt);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"qwen2.5:0.5b", "llama2:7b", "mistral:7b", "codellama:7b"})
    @DisplayName("Should handle various model names")
    void shouldHandleVariousModelNames(String model) {
        // Given
        when(ollamaService.isModelAvailable(model)).thenReturn(Mono.just(true));
        
        // When
        var result = ollamaService.isModelAvailable(model);
        
        // Then
        StepVerifier.create(result)
            .expectNext(true)
            .verifyComplete();
        
        verify(ollamaService, times(1)).isModelAvailable(model);
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.1, 0.5, 0.9, 1.0, 1.5, 2.0})
    @DisplayName("Should handle various temperature values")
    void shouldHandleVariousTemperatureValues(double temperature) {
        // Given
        var options = new GenerationOptions(
            temperature, 40, 0.9, 2048, 1.1, 64, null,
            false, null, 1.0, 64, 0, 5.0, 0.1,
            "2048", "512", null, null, 0, null, null,
            null, null, true, false, null, null, null
        );
        
        when(ollamaService.generateTextWithOptions(eq(testModel), eq(testPrompt), any(GenerationOptions.class)))
            .thenReturn(Mono.just("Response"));
        
        // When
        var result = ollamaService.generateTextWithOptions(testModel, testPrompt, options);
        
        // Then
        StepVerifier.create(result)
            .expectNext("Response")
            .verifyComplete();
        
        verify(ollamaService, times(1)).generateTextWithOptions(testModel, testPrompt, options);
    }
    
    @Test
    @DisplayName("Should handle embedding with null text")
    void shouldHandleEmbeddingWithNullText() {
        // Given
        when(ollamaService.embedText(testModel, isNull()))
            .thenReturn(Mono.just(List.of()));
        
        // When
        var result = ollamaService.embedText(testModel, null);
        
        // Then
        StepVerifier.create(result)
            .expectNext(List.of())
            .verifyComplete();
        
        verify(ollamaService, times(1)).embedText(testModel, null);
    }
    
    @Test
    @DisplayName("Should handle embedding with empty text")
    void shouldHandleEmbeddingWithEmptyText() {
        // Given
        when(ollamaService.embedText(testModel, ""))
            .thenReturn(Mono.just(List.of(0.0)));
        
        // When
        var result = ollamaService.embedText(testModel, "");
        
        // Then
        StepVerifier.create(result)
            .expectNext(List.of(0.0))
            .verifyComplete();
        
        verify(ollamaService, times(1)).embedText(testModel, "");
    }
    
    @Test
    @DisplayName("Should handle resource usage with negative values")
    void shouldHandleResourceUsageWithNegativeValues() {
        // Given
        var usageWithNegative = new ResourceUsage(
            -1L, -1L, -1L, -1.0, -1L, -1L, -1L
        );
        
        // When
        var memPercentage = usageWithNegative.getMemoryUsagePercentage();
        var diskPercentage = usageWithNegative.getDiskUsagePercentage();
        var hasEnough = usageWithNegative.hasEnoughMemory(1000);
        
        // Then
        assertEquals(0.0, memPercentage);
        assertEquals(0.0, diskPercentage);
        assertFalse(hasEnough);
    }
}