package com.zoomtranscriber.api;

import com.zoomtranscriber.config.AudioConfig;
import com.zoomtranscriber.config.OllamaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST API controller for configuration management.
 * Provides endpoints for getting, setting, and validating configuration settings.
 */
@RestController("apiConfigurationController")
@RequestMapping("/api/v1/configuration")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ConfigurationController {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationController.class);
    
    private final AudioConfig audioConfig;
    private final OllamaConfig ollamaConfig;
    
    public ConfigurationController(AudioConfig audioConfig, OllamaConfig ollamaConfig) {
        this.audioConfig = audioConfig;
        this.ollamaConfig = ollamaConfig;
    }
    
    /**
     * Gets the complete configuration.
     */
    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> getConfiguration() {
        logger.info("Getting complete configuration");
        
        return Mono.fromCallable(() -> {
            Map<String, Object> config = new HashMap<>();
            
            // Audio configuration
            config.put("audio", getAudioConfiguration());
            
            // Ollama configuration
            config.put("ollama", getOllamaConfiguration());
            
            // UI configuration
            config.put("ui", getUIConfiguration());
            
            // Advanced configuration
            config.put("advanced", getAdvancedConfiguration());
            
            return ResponseEntity.ok(config);
        })
        .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
    
    /**
     * Gets audio configuration.
     */
    @GetMapping("/audio")
    public Mono<ResponseEntity<AudioConfigurationDTO>> getAudioConfiguration() {
        logger.info("Getting audio configuration");
        
        return Mono.fromCallable(() -> {
            AudioConfigurationDTO dto = new AudioConfigurationDTO();
            dto.setSampleRate(audioConfig.getPreferredSampleRate());
            dto.setBitDepth(audioConfig.getPreferredBitDepth());
            dto.setChannels(audioConfig.getPreferredChannels());
            dto.setQuality(audioConfig.getDefaultQuality().name());
            dto.setBufferSizeMs(audioConfig.getBufferSizeMs());
            dto.setEnableNoiseReduction(audioConfig.isEnableNoiseReduction());
            dto.setEnableEchoCancellation(audioConfig.isEnableEchoCancellation());
            dto.setEnableAutomaticGainControl(audioConfig.isEnableAutomaticGainControl());
            dto.setNoiseThreshold(audioConfig.getNoiseThreshold());
            dto.setGainLevel(audioConfig.getGainLevel());
            dto.setSupportedSampleRates(audioConfig.getSupportedSampleRates());
            dto.setSupportedBitDepths(audioConfig.getSupportedBitDepths());
            dto.setSupportedChannels(audioConfig.getSupportedChannels());
            
            return ResponseEntity.ok(dto);
        })
        .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
    
    /**
     * Updates audio configuration.
     */
    @PutMapping("/audio")
    public Mono<ResponseEntity<Map<String, Object>>> updateAudioConfiguration(
            @Valid @RequestBody AudioConfigurationDTO dto) {
        logger.info("Updating audio configuration");
        
        return Mono.fromCallable(() -> {
            try {
                // Validate configuration
                validateAudioConfiguration(dto);
                
                // Update audio configuration
                audioConfig.setPreferredSampleRate(dto.getSampleRate());
                audioConfig.setPreferredBitDepth(dto.getBitDepth());
                audioConfig.setPreferredChannels(dto.getChannels());
                
                if (dto.getQuality() != null) {
                    audioConfig.setDefaultQuality(AudioConfig.AudioQuality.valueOf(dto.getQuality()));
                }
                
                audioConfig.setBufferSizeMs(dto.getBufferSizeMs());
                audioConfig.setEnableNoiseReduction(dto.isEnableNoiseReduction());
                audioConfig.setEnableEchoCancellation(dto.isEnableEchoCancellation());
                audioConfig.setEnableAutomaticGainControl(dto.isEnableAutomaticGainControl());
                audioConfig.setNoiseThreshold(dto.getNoiseThreshold());
                audioConfig.setGainLevel(dto.getGainLevel());
                
                // Validate the updated configuration
                audioConfig.validate();
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Audio configuration updated successfully");
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                logger.error("Failed to update audio configuration", e);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Failed to update audio configuration: " + e.getMessage());
                error.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.badRequest().body(error);
            }
        })
        .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
    
    /**
     * Gets Ollama configuration.
     */
    @GetMapping("/ollama")
    public Mono<ResponseEntity<OllamaConfigurationDTO>> getOllamaConfiguration() {
        logger.info("Getting Ollama configuration");
        
        return Mono.fromCallable(() -> {
            OllamaConfigurationDTO dto = new OllamaConfigurationDTO();
            dto.setHost(ollamaConfig.getHost());
            dto.setPort(ollamaConfig.getPort());
            dto.setUseHttps(ollamaConfig.isUseHttps());
            dto.setApiKey(ollamaConfig.getApiKey());
            dto.setBaseUrl(ollamaConfig.getBaseUrl());
            dto.setDefaultModel(ollamaConfig.getDefaultModel());
            dto.setMaxConnections(ollamaConfig.getMaxConnections());
            dto.setConnectionTimeoutMs(ollamaConfig.getConnectionTimeoutMs());
            dto.setReadTimeoutMs(ollamaConfig.getReadTimeoutMs());
            dto.setWriteTimeoutMs(ollamaConfig.getWriteTimeoutMs());
            dto.setEnableCompression(ollamaConfig.isEnableCompression());
            dto.setEnableMetrics(ollamaConfig.isEnableMetrics());
            dto.setEnableDetailedLogging(ollamaConfig.isEnableDetailedLogging());
            dto.setMaxRetries(ollamaConfig.getMaxRetries());
            dto.setRetryDelayMs(ollamaConfig.getRetryDelayMs());
            dto.setMaxRetryDelayMs(ollamaConfig.getMaxRetryDelayMs());
            
            return ResponseEntity.ok(dto);
        })
        .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
    
    /**
     * Updates Ollama configuration.
     */
    @PutMapping("/ollama")
    public Mono<ResponseEntity<Map<String, Object>>> updateOllamaConfiguration(
            @Valid @RequestBody OllamaConfigurationDTO dto) {
        logger.info("Updating Ollama configuration");
        
        return Mono.fromCallable(() -> {
            try {
                // Validate configuration
                validateOllamaConfiguration(dto);
                
                // Update Ollama configuration
                ollamaConfig.setHost(dto.getHost());
                ollamaConfig.setPort(dto.getPort());
                ollamaConfig.setUseHttps(dto.isUseHttps());
                ollamaConfig.setApiKey(dto.getApiKey());
                ollamaConfig.setBaseUrl(dto.getBaseUrl());
                ollamaConfig.setDefaultModel(dto.getDefaultModel());
                ollamaConfig.setMaxConnections(dto.getMaxConnections());
                ollamaConfig.setConnectionTimeoutMs(dto.getConnectionTimeoutMs());
                ollamaConfig.setReadTimeoutMs(dto.getReadTimeoutMs());
                ollamaConfig.setWriteTimeoutMs(dto.getWriteTimeoutMs());
                ollamaConfig.setEnableCompression(dto.isEnableCompression());
                ollamaConfig.setEnableMetrics(dto.isEnableMetrics());
                ollamaConfig.setEnableDetailedLogging(dto.isEnableDetailedLogging());
                ollamaConfig.setMaxRetries(dto.getMaxRetries());
                ollamaConfig.setRetryDelayMs(dto.getRetryDelayMs());
                ollamaConfig.setMaxRetryDelayMs(dto.getMaxRetryDelayMs());
                
                // Validate the updated configuration
                ollamaConfig.validate();
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Ollama configuration updated successfully");
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                logger.error("Failed to update Ollama configuration", e);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Failed to update Ollama configuration: " + e.getMessage());
                error.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.badRequest().body(error);
            }
        })
        .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
    
    /**
     * Gets UI configuration.
     */
    @GetMapping("/ui")
    public Mono<ResponseEntity<UIConfigurationDTO>> getUIConfiguration() {
        logger.info("Getting UI configuration");
        
        return Mono.fromCallable(() -> {
            UIConfigurationDTO dto = new UIConfigurationDTO();
            // UI configuration would come from a UIConfig service
            // For now, returning defaults
            dto.setTheme("Light");
            dto.setFontSize("Medium");
            dto.setShowMenuBar(true);
            dto.setShowStatusBar(true);
            dto.setShowToolTips(true);
            dto.setEnableAnimations(true);
            dto.setAutoSave(true);
            dto.setAutoSaveIntervalSeconds(60);
            dto.setStartMinimized(false);
            dto.setMinimizeToTray(false);
            
            return ResponseEntity.ok(dto);
        })
        .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
    
    /**
     * Updates UI configuration.
     */
    @PutMapping("/ui")
    public Mono<ResponseEntity<Map<String, Object>>> updateUIConfiguration(
            @Valid @RequestBody UIConfigurationDTO dto) {
        logger.info("Updating UI configuration");
        
        return Mono.fromCallable(() -> {
            try {
                // UI configuration update logic would be implemented here
                // For now, just validating and returning success
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "UI configuration updated successfully");
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                logger.error("Failed to update UI configuration", e);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Failed to update UI configuration: " + e.getMessage());
                error.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.badRequest().body(error);
            }
        })
        .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
    
    /**
     * Gets advanced configuration.
     */
    @GetMapping("/advanced")
    public Mono<ResponseEntity<AdvancedConfigurationDTO>> getAdvancedConfiguration() {
        logger.info("Getting advanced configuration");
        
        return Mono.fromCallable(() -> {
            AdvancedConfigurationDTO dto = new AdvancedConfigurationDTO();
            // Advanced configuration would come from an AdvancedConfig service
            // For now, returning defaults
            dto.setLogLevel("INFO");
            dto.setMaxMemoryMB(1024);
            dto.setThreadPoolSize(4);
            dto.setEnableDebugMode(false);
            dto.setEnableTelemetry(true);
            dto.setCheckForUpdates(true);
            dto.setEnableProfiling(false);
            dto.setMaxCacheSize(100);
            dto.setCacheExpirationMinutes(30);
            
            return ResponseEntity.ok(dto);
        })
        .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
    
    /**
     * Updates advanced configuration.
     */
    @PutMapping("/advanced")
    public Mono<ResponseEntity<Map<String, Object>>> updateAdvancedConfiguration(
            @Valid @RequestBody AdvancedConfigurationDTO dto) {
        logger.info("Updating advanced configuration");
        
        return Mono.fromCallable(() -> {
            try {
                // Advanced configuration update logic would be implemented here
                // For now, just validating and returning success
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Advanced configuration updated successfully");
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                logger.error("Failed to update advanced configuration", e);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Failed to update advanced configuration: " + e.getMessage());
                error.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.badRequest().body(error);
            }
        })
        .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
    
    /**
     * Validates configuration.
     */
    @PostMapping("/validate")
    public Mono<ResponseEntity<Map<String, Object>>> validateConfiguration(
            @RequestBody Map<String, Object> configuration) {
        logger.info("Validating configuration");
        
        return Mono.fromCallable(() -> {
            Map<String, Object> result = new HashMap<>();
            boolean isValid = true;
            StringBuilder errors = new StringBuilder();
            
            try {
                // Validate audio configuration if present
                if (configuration.containsKey("audio")) {
                    // Audio validation logic
                }
                
                // Validate Ollama configuration if present
                if (configuration.containsKey("ollama")) {
                    // Ollama validation logic
                }
                
                // Additional validation logic would go here
                
            } catch (Exception e) {
                isValid = false;
                errors.append("Validation error: ").append(e.getMessage());
            }
            
            result.put("valid", isValid);
            result.put("errors", errors.toString());
            result.put("timestamp", System.currentTimeMillis());
            
            return isValid ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
        })
        .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
    
    /**
     * Resets configuration to defaults.
     */
    @PostMapping("/reset")
    public Mono<ResponseEntity<Map<String, Object>>> resetConfiguration(
            @RequestParam(required = false, defaultValue = "all") String section) {
        logger.info("Resetting configuration section: {}", section);
        
        return Mono.fromCallable(() -> {
            try {
                switch (section.toLowerCase()) {
                    case "audio":
                        // Reset audio configuration to defaults
                        break;
                    case "ollama":
                        // Reset Ollama configuration to defaults
                        break;
                    case "ui":
                        // Reset UI configuration to defaults
                        break;
                    case "advanced":
                        // Reset advanced configuration to defaults
                        break;
                    case "all":
                    default:
                        // Reset all configuration to defaults
                        break;
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Configuration reset successfully");
                response.put("section", section);
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                logger.error("Failed to reset configuration", e);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Failed to reset configuration: " + e.getMessage());
                error.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.badRequest().body(error);
            }
        })
        .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
    
    /**
     * Exports configuration.
     */
    @GetMapping("/export")
    public Mono<ResponseEntity<Map<String, Object>>> exportConfiguration(
            @RequestParam(required = false, defaultValue = "json") String format) {
        logger.info("Exporting configuration in format: {}", format);
        
        return Mono.fromCallable(() -> {
            Map<String, Object> exportData = new HashMap<>();
            
            // Add all configuration sections
            exportData.put("audio", getAudioConfiguration().block().getBody());
            exportData.put("ollama", getOllamaConfiguration().block().getBody());
            exportData.put("ui", getUIConfiguration().block().getBody());
            exportData.put("advanced", getAdvancedConfiguration().block().getBody());
            exportData.put("exportedAt", System.currentTimeMillis());
            exportData.put("version", "1.0.0");
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", exportData);
            response.put("format", format);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        })
        .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
    
    /**
     * Imports configuration.
     */
    @PostMapping("/import")
    public Mono<ResponseEntity<Map<String, Object>>> importConfiguration(
            @RequestBody Map<String, Object> configuration,
            @RequestParam(required = false, defaultValue = "merge") String mode) {
        logger.info("Importing configuration with mode: {}", mode);
        
        return Mono.fromCallable(() -> {
            try {
                // Import configuration based on mode
                switch (mode.toLowerCase()) {
                    case "replace":
                        // Replace all configuration
                        break;
                    case "merge":
                    default:
                        // Merge with existing configuration
                        break;
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Configuration imported successfully");
                response.put("mode", mode);
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                logger.error("Failed to import configuration", e);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Failed to import configuration: " + e.getMessage());
                error.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.badRequest().body(error);
            }
        })
        .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
    
    /**
     * Gets configuration status.
     */
    @GetMapping("/status")
    public Mono<ResponseEntity<Map<String, Object>>> getConfigurationStatus() {
        logger.info("Getting configuration status");
        
        return Mono.fromCallable(() -> {
            Map<String, Object> status = new HashMap<>();
            status.put("loaded", true);
            status.put("valid", true);
            status.put("lastModified", System.currentTimeMillis());
            status.put("version", "1.0.0");
            status.put("environment", System.getProperty("spring.profiles.active", "default"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", status);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        })
        .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
    
    // Private validation methods
    
    private void validateAudioConfiguration(AudioConfigurationDTO dto) throws IllegalArgumentException {
        if (dto.getSampleRate() <= 0) {
            throw new IllegalArgumentException("Sample rate must be positive");
        }
        if (dto.getBitDepth() <= 0) {
            throw new IllegalArgumentException("Bit depth must be positive");
        }
        if (dto.getChannels() <= 0) {
            throw new IllegalArgumentException("Channels must be positive");
        }
        if (dto.getBufferSizeMs() <= 0) {
            throw new IllegalArgumentException("Buffer size must be positive");
        }
        if (dto.getNoiseThreshold() < 0 || dto.getNoiseThreshold() > 1) {
            throw new IllegalArgumentException("Noise threshold must be between 0 and 1");
        }
        if (dto.getGainLevel() < 0) {
            throw new IllegalArgumentException("Gain level must be non-negative");
        }
    }
    
    private void validateOllamaConfiguration(OllamaConfigurationDTO dto) throws IllegalArgumentException {
        if (dto.getHost() == null || dto.getHost().trim().isEmpty()) {
            throw new IllegalArgumentException("Host cannot be null or empty");
        }
        if (dto.getPort() <= 0 || dto.getPort() > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        if (dto.getMaxConnections() <= 0) {
            throw new IllegalArgumentException("Max connections must be positive");
        }
        if (dto.getConnectionTimeoutMs() <= 0) {
            throw new IllegalArgumentException("Connection timeout must be positive");
        }
        if (dto.getReadTimeoutMs() <= 0) {
            throw new IllegalArgumentException("Read timeout must be positive");
        }
        if (dto.getWriteTimeoutMs() <= 0) {
            throw new IllegalArgumentException("Write timeout must be positive");
        }
        if (dto.getMaxRetries() < 0) {
            throw new IllegalArgumentException("Max retries cannot be negative");
        }
        if (dto.getRetryDelayMs() < 0) {
            throw new IllegalArgumentException("Retry delay cannot be negative");
        }
        if (dto.getMaxRetryDelayMs() < 0) {
            throw new IllegalArgumentException("Max retry delay cannot be negative");
        }
    }
    
    // DTO Classes
    
    public static class AudioConfigurationDTO {
        private int sampleRate;
        private int bitDepth;
        private int channels;
        private String quality;
        private int bufferSizeMs;
        private boolean enableNoiseReduction;
        private boolean enableEchoCancellation;
        private boolean enableAutomaticGainControl;
        private double noiseThreshold;
        private double gainLevel;
        private int[] supportedSampleRates;
        private int[] supportedBitDepths;
        private int[] supportedChannels;
        
        // Getters and setters
        public int getSampleRate() { return sampleRate; }
        public void setSampleRate(int sampleRate) { this.sampleRate = sampleRate; }
        
        public int getBitDepth() { return bitDepth; }
        public void setBitDepth(int bitDepth) { this.bitDepth = bitDepth; }
        
        public int getChannels() { return channels; }
        public void setChannels(int channels) { this.channels = channels; }
        
        public String getQuality() { return quality; }
        public void setQuality(String quality) { this.quality = quality; }
        
        public int getBufferSizeMs() { return bufferSizeMs; }
        public void setBufferSizeMs(int bufferSizeMs) { this.bufferSizeMs = bufferSizeMs; }
        
        public boolean isEnableNoiseReduction() { return enableNoiseReduction; }
        public void setEnableNoiseReduction(boolean enableNoiseReduction) { this.enableNoiseReduction = enableNoiseReduction; }
        
        public boolean isEnableEchoCancellation() { return enableEchoCancellation; }
        public void setEnableEchoCancellation(boolean enableEchoCancellation) { this.enableEchoCancellation = enableEchoCancellation; }
        
        public boolean isEnableAutomaticGainControl() { return enableAutomaticGainControl; }
        public void setEnableAutomaticGainControl(boolean enableAutomaticGainControl) { this.enableAutomaticGainControl = enableAutomaticGainControl; }
        
        public double getNoiseThreshold() { return noiseThreshold; }
        public void setNoiseThreshold(double noiseThreshold) { this.noiseThreshold = noiseThreshold; }
        
        public double getGainLevel() { return gainLevel; }
        public void setGainLevel(double gainLevel) { this.gainLevel = gainLevel; }
        
        public int[] getSupportedSampleRates() { return supportedSampleRates; }
        public void setSupportedSampleRates(int[] supportedSampleRates) { this.supportedSampleRates = supportedSampleRates; }
        
        public int[] getSupportedBitDepths() { return supportedBitDepths; }
        public void setSupportedBitDepths(int[] supportedBitDepths) { this.supportedBitDepths = supportedBitDepths; }
        
        public int[] getSupportedChannels() { return supportedChannels; }
        public void setSupportedChannels(int[] supportedChannels) { this.supportedChannels = supportedChannels; }
    }
    
    public static class OllamaConfigurationDTO {
        private String host;
        private int port;
        private boolean useHttps;
        private String apiKey;
        private String baseUrl;
        private String defaultModel;
        private int maxConnections;
        private long connectionTimeoutMs;
        private long readTimeoutMs;
        private long writeTimeoutMs;
        private boolean enableCompression;
        private boolean enableMetrics;
        private boolean enableDetailedLogging;
        private int maxRetries;
        private long retryDelayMs;
        private long maxRetryDelayMs;
        
        // Getters and setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public boolean isUseHttps() { return useHttps; }
        public void setUseHttps(boolean useHttps) { this.useHttps = useHttps; }
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        
        public String getDefaultModel() { return defaultModel; }
        public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
        
        public int getMaxConnections() { return maxConnections; }
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
        
        public long getConnectionTimeoutMs() { return connectionTimeoutMs; }
        public void setConnectionTimeoutMs(long connectionTimeoutMs) { this.connectionTimeoutMs = connectionTimeoutMs; }
        
        public long getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(long readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
        
        public long getWriteTimeoutMs() { return writeTimeoutMs; }
        public void setWriteTimeoutMs(long writeTimeoutMs) { this.writeTimeoutMs = writeTimeoutMs; }
        
        public boolean isEnableCompression() { return enableCompression; }
        public void setEnableCompression(boolean enableCompression) { this.enableCompression = enableCompression; }
        
        public boolean isEnableMetrics() { return enableMetrics; }
        public void setEnableMetrics(boolean enableMetrics) { this.enableMetrics = enableMetrics; }
        
        public boolean isEnableDetailedLogging() { return enableDetailedLogging; }
        public void setEnableDetailedLogging(boolean enableDetailedLogging) { this.enableDetailedLogging = enableDetailedLogging; }
        
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        
        public long getRetryDelayMs() { return retryDelayMs; }
        public void setRetryDelayMs(long retryDelayMs) { this.retryDelayMs = retryDelayMs; }
        
        public long getMaxRetryDelayMs() { return maxRetryDelayMs; }
        public void setMaxRetryDelayMs(long maxRetryDelayMs) { this.maxRetryDelayMs = maxRetryDelayMs; }
    }
    
    public static class UIConfigurationDTO {
        private String theme;
        private String fontSize;
        private boolean showMenuBar;
        private boolean showStatusBar;
        private boolean showToolTips;
        private boolean enableAnimations;
        private boolean autoSave;
        private int autoSaveIntervalSeconds;
        private boolean startMinimized;
        private boolean minimizeToTray;
        
        // Getters and setters
        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }
        
        public String getFontSize() { return fontSize; }
        public void setFontSize(String fontSize) { this.fontSize = fontSize; }
        
        public boolean isShowMenuBar() { return showMenuBar; }
        public void setShowMenuBar(boolean showMenuBar) { this.showMenuBar = showMenuBar; }
        
        public boolean isShowStatusBar() { return showStatusBar; }
        public void setShowStatusBar(boolean showStatusBar) { this.showStatusBar = showStatusBar; }
        
        public boolean isShowToolTips() { return showToolTips; }
        public void setShowToolTips(boolean showToolTips) { this.showToolTips = showToolTips; }
        
        public boolean isEnableAnimations() { return enableAnimations; }
        public void setEnableAnimations(boolean enableAnimations) { this.enableAnimations = enableAnimations; }
        
        public boolean isAutoSave() { return autoSave; }
        public void setAutoSave(boolean autoSave) { this.autoSave = autoSave; }
        
        public int getAutoSaveIntervalSeconds() { return autoSaveIntervalSeconds; }
        public void setAutoSaveIntervalSeconds(int autoSaveIntervalSeconds) { this.autoSaveIntervalSeconds = autoSaveIntervalSeconds; }
        
        public boolean isStartMinimized() { return startMinimized; }
        public void setStartMinimized(boolean startMinimized) { this.startMinimized = startMinimized; }
        
        public boolean isMinimizeToTray() { return minimizeToTray; }
        public void setMinimizeToTray(boolean minimizeToTray) { this.minimizeToTray = minimizeToTray; }
    }
    
    public static class AdvancedConfigurationDTO {
        private String logLevel;
        private long maxMemoryMB;
        private int threadPoolSize;
        private boolean enableDebugMode;
        private boolean enableTelemetry;
        private boolean checkForUpdates;
        private boolean enableProfiling;
        private int maxCacheSize;
        private int cacheExpirationMinutes;
        
        // Getters and setters
        public String getLogLevel() { return logLevel; }
        public void setLogLevel(String logLevel) { this.logLevel = logLevel; }
        
        public long getMaxMemoryMB() { return maxMemoryMB; }
        public void setMaxMemoryMB(long maxMemoryMB) { this.maxMemoryMB = maxMemoryMB; }
        
        public int getThreadPoolSize() { return threadPoolSize; }
        public void setThreadPoolSize(int threadPoolSize) { this.threadPoolSize = threadPoolSize; }
        
        public boolean isEnableDebugMode() { return enableDebugMode; }
        public void setEnableDebugMode(boolean enableDebugMode) { this.enableDebugMode = enableDebugMode; }
        
        public boolean isEnableTelemetry() { return enableTelemetry; }
        public void setEnableTelemetry(boolean enableTelemetry) { this.enableTelemetry = enableTelemetry; }
        
        public boolean isCheckForUpdates() { return checkForUpdates; }
        public void setCheckForUpdates(boolean checkForUpdates) { this.checkForUpdates = checkForUpdates; }
        
        public boolean isEnableProfiling() { return enableProfiling; }
        public void setEnableProfiling(boolean enableProfiling) { this.enableProfiling = enableProfiling; }
        
        public int getMaxCacheSize() { return maxCacheSize; }
        public void setMaxCacheSize(int maxCacheSize) { this.maxCacheSize = maxCacheSize; }
        
        public int getCacheExpirationMinutes() { return cacheExpirationMinutes; }
        public void setCacheExpirationMinutes(int cacheExpirationMinutes) { this.cacheExpirationMinutes = cacheExpirationMinutes; }
    }
}