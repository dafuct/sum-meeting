package com.zoomtranscriber.core.ai;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Service interface for interacting with local Ollama instance.
 * Provides reactive streams for text generation, model management, and health checks.
 */
public interface OllamaService {
    
    /**
     * Generates text using the specified model.
     * 
     * @param model the model name (e.g., "qwen2.5:0.5b")
     * @param prompt the input prompt
     * @return Mono containing the generated response
     */
    Mono<String> generateText(String model, String prompt);
    
    /**
     * Generates text with streaming response.
     * 
     * @param model the model name
     * @param prompt the input prompt
     * @return Flux of text chunks as they are generated
     */
    Flux<String> generateTextStream(String model, String prompt);
    
    /**
     * Generates text with custom options.
     * 
     * @param model the model name
     * @param prompt the input prompt
     * @param options generation options (temperature, max_tokens, etc.)
     * @return Mono containing the generated response
     */
    Mono<String> generateTextWithOptions(String model, String prompt, GenerationOptions options);
    
    /**
     * Generates text with custom options and streaming.
     * 
     * @param model the model name
     * @param prompt the input prompt
     * @param options generation options
     * @return Flux of text chunks as they are generated
     */
    Flux<String> generateTextStreamWithOptions(String model, String prompt, GenerationOptions options);
    
    /**
     * Checks if the Ollama service is healthy and accessible.
     * 
     * @return Mono containing true if service is healthy
     */
    Mono<Boolean> checkHealth();
    
    /**
     * Gets the version of the Ollama service.
     * 
     * @return Mono containing the version string
     */
    Mono<String> getVersion();
    
    /**
     * Lists all available models.
     * 
     * @return Flux of ModelInfo objects
     */
    Flux<ModelInfo> listModels();
    
    /**
     * Gets information about a specific model.
     * 
     * @param model the model name
     * @return Mono containing ModelInfo
     */
    Mono<ModelInfo> getModelInfo(String model);
    
    /**
     * Checks if a model is available locally.
     * 
     * @param model the model name
     * @return Mono containing true if model is available
     */
    Mono<Boolean> isModelAvailable(String model);
    
    /**
     * Pulls a model from the registry.
     * 
     * @param model the model name
     * @return Flux of download progress
     */
    Flux<DownloadProgress> pullModel(String model);
    
    /**
     * Deletes a model from local storage.
     * 
     * @param model the model name
     * @return Mono that completes when model is deleted
     */
    Mono<Void> deleteModel(String model);
    
    /**
     * Gets system resource usage.
     * 
     * @return Mono containing ResourceUsage
     */
    Mono<ResourceUsage> getResourceUsage();
    
    /**
     * Creates a new model with custom configuration.
     * 
     * @param model the model name
     * @param config the model configuration
     * @return Mono that completes when model is created
     */
    Mono<Void> createModel(String model, ModelConfig config);
    
    /**
     * Copies a model to a new name.
     * 
     * @param source the source model name
     * @param destination the destination model name
     * @return Mono that completes when model is copied
     */
    Mono<Void> copyModel(String source, String destination);
    
    /**
     * Embeds text using the specified model.
     * 
     * @param model the model name
     * @param text the text to embed
     * @return Mono containing the embedding vector
     */
    Mono<List<Double>> embedText(String model, String text);
    
    /**
     * Batch embed multiple texts.
     * 
     * @param model the model name
     * @param texts list of texts to embed
     * @return Flux of embedding vectors
     */
    Flux<List<Double>> embedBatch(String model, List<String> texts);
    
    /**
     * Cancels an ongoing generation request.
     * 
     * @param requestId the request ID to cancel
     * @return Mono that completes when request is cancelled
     */
    Mono<Void> cancelGeneration(String requestId);
    
    /**
     * Represents generation options.
     */
    record GenerationOptions(
        Double temperature,
        Integer topK,
        Double topP,
        Integer maxTokens,
        Double repeatPenalty,
        Integer repeatLastN,
        Integer seed,
        Boolean stopSequences,
        List<String> stop,
        Double typicalP,
        Integer repeatPenaltyLastN,
        Integer mirostat,
        Double mirostatTau,
        Double mirostatEta,
        String numCtx,
        String numBatch,
        String numGqa,
        String numGpu,
        Integer mainGpu,
        Double lowVram,
        Double f16Kv,
        Boolean logitsAll,
        Double vocabOnly,
        Boolean useMmap,
        Boolean useMlock,
        Double numThread,
        String numKeep,
        Double seedRng
    ) {
        /**
         * Creates default generation options.
         * 
         * @return default GenerationOptions
         */
        public static GenerationOptions defaults() {
            return new GenerationOptions(
                0.7,    // temperature
                40,     // topK
                0.9,    // topP
                2048,   // maxTokens
                1.1,    // repeatPenalty
                64,     // repeatLastN
                null,   // seed
                false,  // stopSequences
                null,   // stop
                1.0,    // typicalP
                64,     // repeatPenaltyLastN
                0,      // mirostat
                5.0,    // mirostatTau
                0.1,    // mirostatEta
                "2048", // numCtx
                "512",  // numBatch
        null,   // numGqa
                null,   // numGpu
                0,      // mainGpu
                null,   // lowVram
                null,   // f16Kv
                null,   // logitsAll
                null,   // vocabOnly
                true,   // useMmap
                false,  // useMlock
                null,   // numThread
                null,   // numKeep
                null    // seedRng
            );
        }
        
        /**
         * Creates fast generation options.
         * 
         * @return fast GenerationOptions
         */
        public static GenerationOptions fast() {
            return new GenerationOptions(
                0.5,    // temperature
                20,     // topK
                0.8,    // topP
                1024,   // maxTokens
                1.0,    // repeatPenalty
                32,     // repeatLastN
                null,   // seed
                false,  // stopSequences
                null,   // stop
                1.0,    // typicalP
                32,     // repeatPenaltyLastN
                0,      // mirostat
                5.0,    // mirostatTau
                0.1,    // mirostatEta
                "1024", // numCtx
                "256",  // numBatch
                null,   // numGqa
                null,   // numGpu
                0,      // mainGpu
                null,   // lowVram
                null,   // f16Kv
                null,   // logitsAll
                null,   // vocabOnly
                true,   // useMmap
                false,  // useMlock
                null,   // numThread
                null,   // numKeep
                null    // seedRng
            );
        }
        
        /**
         * Creates creative generation options.
         * 
         * @return creative GenerationOptions
         */
        public static GenerationOptions creative() {
            return new GenerationOptions(
                0.9,    // temperature
                50,     // topK
                0.95,   // topP
                4096,   // maxTokens
                1.2,    // repeatPenalty
                128,    // repeatLastN
                null,   // seed
                false,  // stopSequences
                null,   // stop
                1.0,    // typicalP
                128,    // repeatPenaltyLastN
                0,      // mirostat
                5.0,    // mirostatTau
                0.1,    // mirostatEta
                "4096", // numCtx
                "512",  // numBatch
                null,   // numGqa
                null,   // numGpu
                0,      // mainGpu
                null,   // lowVram
                null,   // f16Kv
                null,   // logitsAll
                null,   // vocabOnly
                true,   // useMmap
                false,  // useMlock
                null,   // numThread
                null,   // numKeep
                null    // seedRng
            );
        }
    }
    
    /**
     * Represents model information.
     */
    record ModelInfo(
        String name,
        String model,
        String modifiedAt,
        String size,
        String digest,
        String details,
        String families,
        String familiesSize,
        String format,
        String parameterSize,
        String quantizationLevel
    ) {
        /**
         * Gets the model size in bytes.
         * 
         * @return size in bytes
         */
        public long getSizeInBytes() {
            if (size == null || size.isEmpty()) return 0;
            try {
                if (size.endsWith("GB")) {
                    return (long) (Double.parseDouble(size.replace("GB", "")) * 1024 * 1024 * 1024);
                } else if (size.endsWith("MB")) {
                    return (long) (Double.parseDouble(size.replace("MB", "")) * 1024 * 1024);
                } else if (size.endsWith("KB")) {
                    return (long) (Double.parseDouble(size.replace("KB", "")) * 1024);
                } else {
                    return Long.parseLong(size);
                }
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
    
    /**
     * Represents download progress.
     */
    record DownloadProgress(
        String status,
        String digest,
        String total,
        String completed
    ) {
        /**
         * Gets the download progress as a percentage.
         * 
         * @return progress percentage (0.0 to 100.0)
         */
        public double getProgressPercentage() {
            if (total == null || completed == null || total.isEmpty() || completed.isEmpty()) {
                return 0.0;
            }
            
            try {
                long totalBytes = Long.parseLong(total);
                long completedBytes = Long.parseLong(completed);
                if (totalBytes == 0) return 0.0;
                return (double) completedBytes / totalBytes * 100.0;
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        
        /**
         * Checks if the download is complete.
         * 
         * @return true if download is complete
         */
        public boolean isComplete() {
            return "success".equals(status) && getProgressPercentage() >= 100.0;
        }
    }
    
    /**
     * Represents system resource usage.
     */
    record ResourceUsage(
        long memoryTotal,
        long memoryUsed,
        long memoryAvailable,
        double cpuUsage,
        long diskTotal,
        long diskUsed,
        long diskAvailable
    ) {
        /**
         * Gets memory usage percentage.
         * 
         * @return memory usage percentage (0.0 to 100.0)
         */
        public double getMemoryUsagePercentage() {
            if (memoryTotal == 0) return 0.0;
            return (double) memoryUsed / memoryTotal * 100.0;
        }
        
        /**
         * Gets disk usage percentage.
         * 
         * @return disk usage percentage (0.0 to 100.0)
         */
        public double getDiskUsagePercentage() {
            if (diskTotal == 0) return 0.0;
            return (double) diskUsed / diskTotal * 100.0;
        }
        
        /**
         * Checks if system has enough memory for model operations.
         * 
         * @param requiredMemoryMB required memory in MB
         * @return true if enough memory is available
         */
        public boolean hasEnoughMemory(long requiredMemoryMB) {
            return memoryAvailable >= requiredMemoryMB * 1024 * 1024;
        }
    }
    
    /**
     * Represents model configuration.
     */
    record ModelConfig(
        String model,
        String from,
        String license,
        String modelfile,
        String template,
        String system,
        String parameters,
        String adapter
    ) {
        /**
         * Creates a basic model configuration.
         * 
         * @param model the model name
         * @param from the base model
         * @return basic ModelConfig
         */
        public static ModelConfig basic(String model, String from) {
            return new ModelConfig(
                model,
                from,
                null,
                null,
                null,
                null,
                null,
                null
            );
        }
        
        /**
         * Creates a model configuration with custom template.
         * 
         * @param model the model name
         * @param from the base model
         * @param template the custom template
         * @return ModelConfig with custom template
         */
        public static ModelConfig withTemplate(String model, String from, String template) {
            return new ModelConfig(
                model,
                from,
                null,
                null,
                template,
                null,
                null,
                null
            );
        }
    }
}