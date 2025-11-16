package com.zoomtranscriber.config;

import com.zoomtranscriber.core.ai.ModelManager;
import com.zoomtranscriber.core.ai.SummaryGenerator;
import io.netty.channel.ChannelOption;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Spring Boot configuration class for Ollama settings.
 * Configures connection pooling, timeouts, and performance tuning parameters.
 */
@Configuration
@ConfigurationProperties(prefix = "zoom.transcriber.ollama")
public class OllamaConfig {
    
    // Connection settings
    private String host = "localhost";
    private int port = 11434;
    private String baseUrl;
    private boolean useHttps = false;
    private String apiKey;
    
    // Connection pooling
    private int maxConnections = 10;
    private int maxIdleConnections = 5;
    private long connectionTimeoutMs = 10000;
    private long readTimeoutMs = 60000;
    private long writeTimeoutMs = 60000;
    private boolean keepAlive = true;
    private long keepAliveDurationMs = 30000;
    
    // Model settings
    private String defaultModel = "qwen2.5:0.5b";
    private int maxRetries = 3;
    private long retryDelayMs = 1000;
    private long maxRetryDelayMs = 10000;
    private double retryBackoffMultiplier = 2.0;
    
    // Performance tuning
    private int requestBufferSize = 8192;
    private int responseBufferSize = 8192;
    private boolean enableCompression = true;
    private int maxConcurrentRequests = 5;
    private long healthCheckIntervalMs = 30000;
    private int healthCheckTimeoutMs = 5000;
    
    // Resource limits
    private long maxRequestSizeBytes = 10 * 1024 * 1024; // 10MB
    private long maxResponseSizeBytes = 50 * 1024 * 1024; // 50MB
    private long modelDownloadTimeoutMs = 300000; // 5 minutes
    private long generationTimeoutMs = 120000; // 2 minutes
    
    // Caching settings
    private boolean enableModelCache = true;
    private long modelCacheExpirationMs = 1800000; // 30 minutes
    private int maxCacheSize = 100;
    private boolean enableRequestCache = false;
    private long requestCacheExpirationMs = 300000; // 5 minutes
    
    // Monitoring and logging
    private boolean enableMetrics = true;
    private boolean enableDetailedLogging = false;
    private boolean enableRequestTracing = false;
    private double slowRequestThresholdMs = 1000.0;
    
    // Security settings
    private boolean validateSsl = true;
    private String trustStorePath;
    private String trustStorePassword;
    private String keyStorePath;
    private String keyStorePassword;
    
    public OllamaConfig() {
        // Initialize baseUrl based on default host and port
        this.baseUrl = "http://" + host + ":" + port;
    }
    
    /**
     * Creates a WebClient configured for Ollama communication.
     * 
     * @return configured WebClient
     */
    @Bean(name = "ollamaWebClient")
    public WebClient ollamaWebClient() {
        WebClient.Builder builder = WebClient.builder()
            .baseUrl(getBaseUrl())
            .codecs(configurer -> {
                configurer.defaultCodecs().maxInMemorySize((int) getMaxResponseSizeBytes());
                if (enableCompression) {
                    configurer.defaultCodecs().enableLoggingRequestDetails(enableDetailedLogging);
                }
            });
        
        // Configure connection pooling and timeouts
        builder.clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(
            reactor.netty.http.client.HttpClient.create()
                .responseTimeout(Duration.ofMillis(readTimeoutMs))
                .compress(enableCompression)
                .keepAlive(true)
                .option(ChannelOption.SO_KEEPALIVE, keepAlive)
        ));
        
        return builder.build();
    }
    
    /**
     * Creates executor for Ollama operations.
     * 
     * @return executor for Ollama tasks
     */
    @Bean(name = "ollamaExecutor")
    public Executor ollamaExecutor() {
        return Executors.newFixedThreadPool(maxConcurrentRequests);
    }
    
    /**
     * Creates scheduler for Ollama reactive operations.
     * 
     * @return bounded elastic scheduler
     */
    @Bean(name = "ollamaScheduler")
    public reactor.core.scheduler.Scheduler ollamaScheduler() {
        return Schedulers.newBoundedElastic(
            maxConcurrentRequests,
            maxConcurrentRequests * 2,
            "ollama-scheduler",
            60,
            true
        );
    }
    
    /**
     * Creates summary generator configuration.
     * 
     * @return SummaryGenerator configuration
     */
    @Bean(name = "summaryGeneratorConfig")
    public SummaryGenerator.SummaryConfig summaryGeneratorConfig() {
        SummaryGenerator.SummaryConfig config = new SummaryGenerator.SummaryConfig();
        config.setModel(defaultModel);
        config.setMaxRetries(maxRetries);
        config.setTimeoutSeconds((int) (generationTimeoutMs / 1000));
        return config;
    }
    
    /**
     * Creates model manager configuration.
     * 
     * @return ModelManager configuration
     */
    @Bean(name = "modelManagerConfig")
    public ModelManager.ModelManagerConfig modelManagerConfig() {
        ModelManager.ModelManagerConfig config = new ModelManager.ModelManagerConfig();
        config.setDefaultModels(java.util.List.of(defaultModel));
        config.setCacheExpirationMinutes((int) (modelCacheExpirationMs / 60000));
        config.setMaxConcurrentDownloads(Math.min(3, maxConcurrentRequests));
        config.setMaxModelSizeBytes(maxRequestSizeBytes);
        return config;
    }
    
    /**
     * Gets the complete base URL for Ollama API.
     * 
     * @return complete base URL
     */
    public String getBaseUrl() {
        if (baseUrl != null && !baseUrl.isEmpty()) {
            return baseUrl;
        }
        
        String protocol = useHttps ? "https" : "http";
        return protocol + "://" + host + ":" + port;
    }
    
    /**
     * Gets the effective connection timeout.
     * 
     * @return connection timeout duration
     */
    public Duration getConnectionTimeout() {
        return Duration.ofMillis(connectionTimeoutMs);
    }
    
    /**
     * Gets the effective read timeout.
     * 
     * @return read timeout duration
     */
    public Duration getReadTimeout() {
        return Duration.ofMillis(readTimeoutMs);
    }
    
    /**
     * Gets the effective write timeout.
     * 
     * @return write timeout duration
     */
    public Duration getWriteTimeout() {
        return Duration.ofMillis(writeTimeoutMs);
    }
    
    /**
     * Gets the health check interval.
     * 
     * @return health check interval duration
     */
    public Duration getHealthCheckInterval() {
        return Duration.ofMillis(healthCheckIntervalMs);
    }
    
    /**
     * Gets the generation timeout.
     * 
     * @return generation timeout duration
     */
    public Duration getGenerationTimeout() {
        return Duration.ofMillis(generationTimeoutMs);
    }
    
    /**
     * Gets the model download timeout.
     * 
     * @return model download timeout duration
     */
    public Duration getModelDownloadTimeout() {
        return Duration.ofMillis(modelDownloadTimeoutMs);
    }
    
    /**
     * Gets the model cache expiration duration.
     * 
     * @return model cache expiration duration
     */
    public Duration getModelCacheExpiration() {
        return Duration.ofMillis(modelCacheExpirationMs);
    }
    
    /**
     * Gets the request cache expiration duration.
     * 
     * @return request cache expiration duration
     */
    public Duration getRequestCacheExpiration() {
        return Duration.ofMillis(requestCacheExpirationMs);
    }
    
    /**
     * Gets the retry delay duration.
     * 
     * @return retry delay duration
     */
    public Duration getRetryDelay() {
        return Duration.ofMillis(retryDelayMs);
    }
    
    /**
     * Gets the max retry delay duration.
     * 
     * @return max retry delay duration
     */
    public Duration getMaxRetryDelay() {
        return Duration.ofMillis(maxRetryDelayMs);
    }
    
    /**
     * Gets the slow request threshold duration.
     * 
     * @return slow request threshold duration
     */
    public Duration getSlowRequestThreshold() {
        return Duration.ofMillis((long) slowRequestThresholdMs);
    }
    
    /**
     * Validates the configuration.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("Ollama host cannot be null or empty");
        }
        
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Ollama port must be between 1 and 65535");
        }
        
        if (maxConnections <= 0) {
            throw new IllegalArgumentException("Max connections must be positive");
        }
        
        if (maxRetries < 0) {
            throw new IllegalArgumentException("Max retries cannot be negative");
        }
        
        if (maxRequestSizeBytes <= 0) {
            throw new IllegalArgumentException("Max request size must be positive");
        }
        
        if (maxResponseSizeBytes <= 0) {
            throw new IllegalArgumentException("Max response size must be positive");
        }
        
        if (defaultModel == null || defaultModel.isEmpty()) {
            throw new IllegalArgumentException("Default model cannot be null or empty");
        }
    }
    
    // Getters and setters with validation
    
    public String getHost() { return host; }
    public void setHost(String host) { 
        if (host != null && !host.isEmpty()) {
            this.host = host;
            this.baseUrl = null; // Reset to force regeneration
        }
    }
    
    public int getPort() { return port; }
    public void setPort(int port) { 
        if (port > 0 && port <= 65535) {
            this.port = port;
            this.baseUrl = null; // Reset to force regeneration
        }
    }
    

    public void setBaseUrl(String baseUrl) { 
        this.baseUrl = baseUrl;
    }
    
    public boolean isUseHttps() { return useHttps; }
    public void setUseHttps(boolean useHttps) { 
        this.useHttps = useHttps;
        this.baseUrl = null; // Reset to force regeneration
    }
    
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    
    public int getMaxConnections() { return maxConnections; }
    public void setMaxConnections(int maxConnections) { 
        if (maxConnections > 0) this.maxConnections = maxConnections;
    }
    
    public int getMaxIdleConnections() { return maxIdleConnections; }
    public void setMaxIdleConnections(int maxIdleConnections) { 
        if (maxIdleConnections >= 0) this.maxIdleConnections = maxIdleConnections;
    }
    
    public long getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public void setConnectionTimeoutMs(long connectionTimeoutMs) { 
        if (connectionTimeoutMs > 0) this.connectionTimeoutMs = connectionTimeoutMs;
    }
    
    public long getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(long readTimeoutMs) { 
        if (readTimeoutMs > 0) this.readTimeoutMs = readTimeoutMs;
    }
    
    public long getWriteTimeoutMs() { return writeTimeoutMs; }
    public void setWriteTimeoutMs(long writeTimeoutMs) { 
        if (writeTimeoutMs > 0) this.writeTimeoutMs = writeTimeoutMs;
    }
    
    public boolean isKeepAlive() { return keepAlive; }
    public void setKeepAlive(boolean keepAlive) { this.keepAlive = keepAlive; }
    
    public long getKeepAliveDurationMs() { return keepAliveDurationMs; }
    public void setKeepAliveDurationMs(long keepAliveDurationMs) { 
        if (keepAliveDurationMs > 0) this.keepAliveDurationMs = keepAliveDurationMs;
    }
    
    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String defaultModel) { 
        if (defaultModel != null && !defaultModel.isEmpty()) {
            this.defaultModel = defaultModel;
        }
    }
    
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { 
        if (maxRetries >= 0) this.maxRetries = maxRetries;
    }
    
    public long getRetryDelayMs() { return retryDelayMs; }
    public void setRetryDelayMs(long retryDelayMs) { 
        if (retryDelayMs >= 0) this.retryDelayMs = retryDelayMs;
    }
    
    public long getMaxRetryDelayMs() { return maxRetryDelayMs; }
    public void setMaxRetryDelayMs(long maxRetryDelayMs) { 
        if (maxRetryDelayMs >= 0) this.maxRetryDelayMs = maxRetryDelayMs;
    }
    
    public double getRetryBackoffMultiplier() { return retryBackoffMultiplier; }
    public void setRetryBackoffMultiplier(double retryBackoffMultiplier) { 
        if (retryBackoffMultiplier > 0) this.retryBackoffMultiplier = retryBackoffMultiplier;
    }
    
    public int getRequestBufferSize() { return requestBufferSize; }
    public void setRequestBufferSize(int requestBufferSize) { 
        if (requestBufferSize > 0) this.requestBufferSize = requestBufferSize;
    }
    
    public int getResponseBufferSize() { return responseBufferSize; }
    public void setResponseBufferSize(int responseBufferSize) { 
        if (responseBufferSize > 0) this.responseBufferSize = responseBufferSize;
    }
    
    public boolean isEnableCompression() { return enableCompression; }
    public void setEnableCompression(boolean enableCompression) { this.enableCompression = enableCompression; }
    
    public int getMaxConcurrentRequests() { return maxConcurrentRequests; }
    public void setMaxConcurrentRequests(int maxConcurrentRequests) { 
        if (maxConcurrentRequests > 0) this.maxConcurrentRequests = maxConcurrentRequests;
    }
    
    public long getHealthCheckIntervalMs() { return healthCheckIntervalMs; }
    public void setHealthCheckIntervalMs(long healthCheckIntervalMs) { 
        if (healthCheckIntervalMs > 0) this.healthCheckIntervalMs = healthCheckIntervalMs;
    }
    
    public int getHealthCheckTimeoutMs() { return healthCheckTimeoutMs; }
    public void setHealthCheckTimeoutMs(int healthCheckTimeoutMs) { 
        if (healthCheckTimeoutMs > 0) this.healthCheckTimeoutMs = healthCheckTimeoutMs;
    }
    
    public long getMaxRequestSizeBytes() { return maxRequestSizeBytes; }
    public void setMaxRequestSizeBytes(long maxRequestSizeBytes) { 
        if (maxRequestSizeBytes > 0) this.maxRequestSizeBytes = maxRequestSizeBytes;
    }
    
    public long getMaxResponseSizeBytes() { return maxResponseSizeBytes; }
    public void setMaxResponseSizeBytes(long maxResponseSizeBytes) { 
        if (maxResponseSizeBytes > 0) this.maxResponseSizeBytes = maxResponseSizeBytes;
    }
    
    public long getModelDownloadTimeoutMs() { return modelDownloadTimeoutMs; }
    public void setModelDownloadTimeoutMs(long modelDownloadTimeoutMs) { 
        if (modelDownloadTimeoutMs > 0) this.modelDownloadTimeoutMs = modelDownloadTimeoutMs;
    }
    
    public long getGenerationTimeoutMs() { return generationTimeoutMs; }
    public void setGenerationTimeoutMs(long generationTimeoutMs) { 
        if (generationTimeoutMs > 0) this.generationTimeoutMs = generationTimeoutMs;
    }
    
    public boolean isEnableModelCache() { return enableModelCache; }
    public void setEnableModelCache(boolean enableModelCache) { this.enableModelCache = enableModelCache; }
    
    public long getModelCacheExpirationMs() { return modelCacheExpirationMs; }
    public void setModelCacheExpirationMs(long modelCacheExpirationMs) { 
        if (modelCacheExpirationMs > 0) this.modelCacheExpirationMs = modelCacheExpirationMs;
    }
    
    public int getMaxCacheSize() { return maxCacheSize; }
    public void setMaxCacheSize(int maxCacheSize) { 
        if (maxCacheSize > 0) this.maxCacheSize = maxCacheSize;
    }
    
    public boolean isEnableRequestCache() { return enableRequestCache; }
    public void setEnableRequestCache(boolean enableRequestCache) { this.enableRequestCache = enableRequestCache; }
    
    public long getRequestCacheExpirationMs() { return requestCacheExpirationMs; }
    public void setRequestCacheExpirationMs(long requestCacheExpirationMs) { 
        if (requestCacheExpirationMs > 0) this.requestCacheExpirationMs = requestCacheExpirationMs;
    }
    
    public boolean isEnableMetrics() { return enableMetrics; }
    public void setEnableMetrics(boolean enableMetrics) { this.enableMetrics = enableMetrics; }
    
    public boolean isEnableDetailedLogging() { return enableDetailedLogging; }
    public void setEnableDetailedLogging(boolean enableDetailedLogging) { this.enableDetailedLogging = enableDetailedLogging; }
    
    public boolean isEnableRequestTracing() { return enableRequestTracing; }
    public void setEnableRequestTracing(boolean enableRequestTracing) { this.enableRequestTracing = enableRequestTracing; }
    
    public double getSlowRequestThresholdMs() { return slowRequestThresholdMs; }
    public void setSlowRequestThresholdMs(double slowRequestThresholdMs) { 
        if (slowRequestThresholdMs > 0) this.slowRequestThresholdMs = slowRequestThresholdMs;
    }
    
    public boolean isValidateSsl() { return validateSsl; }
    public void setValidateSsl(boolean validateSsl) { this.validateSsl = validateSsl; }
    
    public String getTrustStorePath() { return trustStorePath; }
    public void setTrustStorePath(String trustStorePath) { this.trustStorePath = trustStorePath; }
    
    public String getTrustStorePassword() { return trustStorePassword; }
    public void setTrustStorePassword(String trustStorePassword) { this.trustStorePassword = trustStorePassword; }
    
    public String getKeyStorePath() { return keyStorePath; }
    public void setKeyStorePath(String keyStorePath) { this.keyStorePath = keyStorePath; }
    
    public String getKeyStorePassword() { return keyStorePassword; }
    public void setKeyStorePassword(String keyStorePassword) { this.keyStorePassword = keyStorePassword; }
}