package com.zoomtranscriber.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@ConfigurationProperties(prefix = "zoom.transcriber")
public class ApplicationConfig {
    
    private String ollamaUrl = "http://localhost:11434";
    private String ollamaModel = "qwen2:0.5b";
    private int maxRetries = 3;
    private int timeoutSeconds = 30;
    private String audioFormat = "wav";
    private int sampleRate = 16000;
    private int channels = 1;
    private boolean enableDebug = false;
    
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .maxAge(3600);
            }
        };
    }
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        return Executors.newFixedThreadPool(10);
    }
    
    @Bean(name = "audioProcessingExecutor")
    public Executor audioProcessingExecutor() {
        return Executors.newFixedThreadPool(4);
    }
    
    @Bean(name = "transcriptionExecutor")
    public Executor transcriptionExecutor() {
        return Executors.newFixedThreadPool(2);
    }
    
    public String getOllamaUrl() {
        return ollamaUrl;
    }
    
    public void setOllamaUrl(String ollamaUrl) {
        this.ollamaUrl = ollamaUrl;
    }
    
    public String getOllamaModel() {
        return ollamaModel;
    }
    
    public void setOllamaModel(String ollamaModel) {
        this.ollamaModel = ollamaModel;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
    
    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
    
    public String getAudioFormat() {
        return audioFormat;
    }
    
    public void setAudioFormat(String audioFormat) {
        this.audioFormat = audioFormat;
    }
    
    public int getSampleRate() {
        return sampleRate;
    }
    
    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }
    
    public int getChannels() {
        return channels;
    }
    
    public void setChannels(int channels) {
        this.channels = channels;
    }
    
    public boolean isEnableDebug() {
        return enableDebug;
    }
    
    public void setEnableDebug(boolean enableDebug) {
        this.enableDebug = enableDebug;
    }
}