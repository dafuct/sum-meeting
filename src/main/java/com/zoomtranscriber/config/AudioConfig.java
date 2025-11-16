package com.zoomtranscriber.config;

import com.zoomtranscriber.core.audio.AudioCaptureService;
import com.zoomtranscriber.core.audio.AudioProcessor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Schedulers;

import javax.sound.sampled.AudioFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Spring Boot configuration class for audio settings.
 * Configures audio capture, processing, buffer sizes, and quality settings.
 */
@Configuration
@ConfigurationProperties(prefix = "zoom.transcriber.audio")
public class AudioConfig {
    
    // Sample rate configurations
    private int defaultSampleRate = 16000; // 16kHz default for speech recognition
    private int supportedSampleRates[] = {8000, 11025, 16000, 22050, 44100, 48000};
    private int preferredSampleRate = 16000;
    
    // Bit depth configurations
    private int defaultBitDepth = 16; // 16-bit default
    private int supportedBitDepths[] = {8, 16, 24, 32};
    private int preferredBitDepth = 16;
    
    // Channel configurations
    private int defaultChannels = 1; // Mono default for speech recognition
    private int supportedChannels[] = {1, 2}; // Mono and Stereo
    private int preferredChannels = 1;
    
    // Buffer configurations
    private int bufferSizeMs = 100; // 100ms buffer size
    private int minBufferSizeMs = 20;
    private int maxBufferSizeMs = 1000;
    private int bufferCount = 10; // Number of buffers to maintain
    private boolean useDirectBuffers = true;
    
    // Quality settings
    private AudioQuality defaultQuality = AudioQuality.MEDIUM;
    private boolean enableNoiseReduction = true;
    private boolean enableEchoCancellation = false;
    private boolean enableAutomaticGainControl = true;
    private double noiseThreshold = 0.01;
    private double gainLevel = 1.0;
    
    // Performance settings
    private int maxProcessingThreads = 2;
    private int captureThreadPoolSize = 4;
    private int processingThreadPoolSize = 2;
    private boolean useAsyncProcessing = true;
    private long processingTimeoutMs = 5000;
    
    // Platform-specific configurations
    private PlatformAudioConfig windows = new PlatformAudioConfig();
    private PlatformAudioConfig mac = new PlatformAudioConfig();
    private PlatformAudioConfig linux = new PlatformAudioConfig();
    
    // Audio format settings
    private boolean enableAudioNormalization = true;
    private double normalizationTargetLevel = 0.8;
    private boolean enableHighPassFilter = true;
    private double highPassFrequency = 80.0; // Hz
    private boolean enableLowPassFilter = false;
    private double lowPassFrequency = 8000.0; // Hz
    
    // Detection settings
    private double voiceActivityThreshold = 0.5;
    private int voiceActivityMinDurationMs = 500;
    private int voiceActivityMaxSilenceMs = 2000;
    private boolean enableVoiceActivityDetection = true;
    
    // Recording settings
    private boolean enableRecording = false;
    private String recordingFormat = "wav";
    private String recordingDirectory = "./recordings";
    private long maxRecordingSizeBytes = 100 * 1024 * 1024; // 100MB
    private int maxRecordingDurationMinutes = 120; // 2 hours
    
    /**
     * Audio quality enumeration.
     */
    public enum AudioQuality {
        LOW(8000, 8, 1, 50),
        MEDIUM(16000, 16, 1, 100),
        HIGH(44100, 16, 2, 50),
        ULTRA(48000, 24, 2, 25);
        
        private final int sampleRate;
        private final int bitDepth;
        private final int channels;
        private final int bufferSizeMs;
        
        AudioQuality(int sampleRate, int bitDepth, int channels, int bufferSizeMs) {
            this.sampleRate = sampleRate;
            this.bitDepth = bitDepth;
            this.channels = channels;
            this.bufferSizeMs = bufferSizeMs;
        }
        
        public int getSampleRate() { return sampleRate; }
        public int getBitDepth() { return bitDepth; }
        public int getChannels() { return channels; }
        public int getBufferSizeMs() { return bufferSizeMs; }
        
        public AudioFormat toAudioFormat() {
            return new AudioFormat(
                sampleRate,
                bitDepth,
                channels,
                true, // signed
                false // little-endian
            );
        }
    }
    
    /**
     * Platform-specific audio configuration.
     */
    public static class PlatformAudioConfig {
        private String preferredDevice = "default";
        private boolean enableExclusiveMode = false;
        private int bufferSizeMultiplier = 1;
        private String audioAPI = "default"; // e.g., DirectSound, ASIO, CoreAudio
        private boolean enableDeviceMonitoring = true;
        private int deviceRefreshIntervalMs = 1000;
        
        public String getPreferredDevice() { return preferredDevice; }
        public void setPreferredDevice(String preferredDevice) { this.preferredDevice = preferredDevice; }
        
        public boolean isEnableExclusiveMode() { return enableExclusiveMode; }
        public void setEnableExclusiveMode(boolean enableExclusiveMode) { this.enableExclusiveMode = enableExclusiveMode; }
        
        public int getBufferSizeMultiplier() { return bufferSizeMultiplier; }
        public void setBufferSizeMultiplier(int bufferSizeMultiplier) { this.bufferSizeMultiplier = bufferSizeMultiplier; }
        
        public String getAudioAPI() { return audioAPI; }
        public void setAudioAPI(String audioAPI) { this.audioAPI = audioAPI; }
        
        public boolean isEnableDeviceMonitoring() { return enableDeviceMonitoring; }
        public void setEnableDeviceMonitoring(boolean enableDeviceMonitoring) { this.enableDeviceMonitoring = enableDeviceMonitoring; }
        
        public int getDeviceRefreshIntervalMs() { return deviceRefreshIntervalMs; }
        public void setDeviceRefreshIntervalMs(int deviceRefreshIntervalMs) { this.deviceRefreshIntervalMs = deviceRefreshIntervalMs; }
    }
    
    /**
     * Creates audio capture service configuration.
     * This is a factory method, not a Spring bean.
     */
    public AudioCaptureService.CaptureConfig createAudioCaptureConfig() {
        AudioCaptureService.AudioQuality quality = new AudioCaptureService.AudioQuality(
            defaultQuality.getSampleRate(),
            defaultQuality.getBitDepth(),
            defaultQuality.getChannels(),
            defaultQuality.getSampleRate(),
            false,
            calculateBufferSizeBytes()
        );

        PlatformAudioConfig platformConfig = getCurrentPlatformConfig();
        String sourceId = platformConfig.getPreferredDevice().equals("default") ? null : platformConfig.getPreferredDevice();

        return new AudioCaptureService.CaptureConfig(
            sourceId, // Use platform-specific device or auto-detect
            quality,
            enableNoiseReduction,
            Duration.ofMillis(bufferSizeMs),
            enableAutomaticGainControl
        );
    }
    
    /**
     * Creates audio processor configuration.
     * Since AudioProcessor doesn't have a config class, we create a simple properties map.
     */
    @Bean(name = "audioProcessorConfig")
    public java.util.Map<String, Object> audioProcessorConfig() {
        java.util.Map<String, Object> config = new java.util.HashMap<>();
        
        config.put("sampleRate", defaultQuality.getSampleRate());
        config.put("bitDepth", defaultQuality.getBitDepth());
        config.put("channels", defaultQuality.getChannels());
        config.put("bufferSizeMs", bufferSizeMs);
        config.put("enableNormalization", enableAudioNormalization);
        config.put("normalizationTargetLevel", normalizationTargetLevel);
        config.put("enableHighPassFilter", enableHighPassFilter);
        config.put("highPassFrequency", highPassFrequency);
        config.put("enableLowPassFilter", enableLowPassFilter);
        config.put("lowPassFrequency", lowPassFrequency);
        config.put("voiceActivityThreshold", voiceActivityThreshold);
        config.put("voiceActivityMinDurationMs", voiceActivityMinDurationMs);
        config.put("voiceActivityMaxSilenceMs", voiceActivityMaxSilenceMs);
        config.put("enableVoiceActivityDetection", enableVoiceActivityDetection);
        
        return config;
    }
    
    /**
     * Creates executor for audio capture operations.
     */
    @Bean(name = "audioCaptureExecutor")
    public Executor audioCaptureExecutor() {
        return Executors.newFixedThreadPool(captureThreadPoolSize);
    }
    
    /**
     * Creates executor for audio processing operations.
     */
    @Bean(name = "audioConfigProcessingExecutor")
    public Executor audioConfigProcessingExecutor() {
        return Executors.newFixedThreadPool(processingThreadPoolSize);
    }
    
    /**
     * Creates scheduler for audio reactive operations.
     */
    @Bean(name = "audioScheduler")
    public reactor.core.scheduler.Scheduler audioScheduler() {
        return Schedulers.newBoundedElastic(
            captureThreadPoolSize + processingThreadPoolSize,
            (captureThreadPoolSize + processingThreadPoolSize) * 2,
            "audio-scheduler",
            60,
            true
        );
    }
    
    /**
     * Gets platform-specific configuration for current platform.
     */
    private PlatformAudioConfig getCurrentPlatformConfig() {
        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("win")) {
            return windows;
        } else if (osName.contains("mac")) {
            return mac;
        } else if (osName.contains("nux") || osName.contains("nix")) {
            return linux;
        } else {
            return new PlatformAudioConfig(); // Default configuration
        }
    }
    
    /**
     * Gets the current operating system.
     */
    public String getCurrentOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("win")) {
            return "windows";
        } else if (osName.contains("mac")) {
            return "mac";
        } else if (osName.contains("nux") || osName.contains("nix")) {
            return "linux";
        } else {
            return "unknown";
        }
    }
    
    /**
     * Gets the platform-specific configuration.
     */
    public PlatformAudioConfig getPlatformConfig(String platform) {
        return switch (platform.toLowerCase()) {
            case "windows" -> windows;
            case "mac" -> mac;
            case "linux" -> linux;
            default -> new PlatformAudioConfig();
        };
    }
    
    /**
     * Creates an AudioFormat from current settings.
     */
    public AudioFormat createAudioFormat() {
        return new AudioFormat(
            preferredSampleRate,
            preferredBitDepth,
            preferredChannels,
            true, // signed
            false // little-endian
        );
    }
    
    /**
     * Creates an AudioFormat for specified quality.
     */
    public AudioFormat createAudioFormat(AudioQuality quality) {
        return quality.toAudioFormat();
    }
    
    /**
     * Calculates buffer size in bytes.
     */
    public int calculateBufferSizeBytes() {
        AudioFormat format = createAudioFormat();
        int frameSize = format.getFrameSize();
        float framesPerMs = format.getSampleRate() / 1000.0f;
        return (int) (framesPerMs * bufferSizeMs * frameSize);
    }
    
    /**
     * Validates the configuration.
     */
    public void validate() {
        if (defaultSampleRate <= 0) {
            throw new IllegalArgumentException("Sample rate must be positive");
        }
        
        if (preferredBitDepth <= 0) {
            throw new IllegalArgumentException("Bit depth must be positive");
        }
        
        if (preferredChannels <= 0) {
            throw new IllegalArgumentException("Channels must be positive");
        }
        
        if (bufferSizeMs <= 0) {
            throw new IllegalArgumentException("Buffer size must be positive");
        }
        
        if (noiseThreshold < 0 || noiseThreshold > 1) {
            throw new IllegalArgumentException("Noise threshold must be between 0 and 1");
        }
        
        if (gainLevel < 0) {
            throw new IllegalArgumentException("Gain level must be non-negative");
        }
        
        if (maxProcessingThreads <= 0) {
            throw new IllegalArgumentException("Max processing threads must be positive");
        }
    }
    
    // Getters and setters with validation
    
    public int getDefaultSampleRate() { return defaultSampleRate; }
    public void setDefaultSampleRate(int defaultSampleRate) { 
        if (defaultSampleRate > 0) this.defaultSampleRate = defaultSampleRate;
    }
    
    public int[] getSupportedSampleRates() { return supportedSampleRates; }
    public void setSupportedSampleRates(int[] supportedSampleRates) { 
        if (supportedSampleRates != null && supportedSampleRates.length > 0) {
            this.supportedSampleRates = supportedSampleRates;
        }
    }
    
    public int getPreferredSampleRate() { return preferredSampleRate; }
    public void setPreferredSampleRate(int preferredSampleRate) { 
        if (preferredSampleRate > 0) this.preferredSampleRate = preferredSampleRate;
    }
    
    public int getDefaultBitDepth() { return defaultBitDepth; }
    public void setDefaultBitDepth(int defaultBitDepth) { 
        if (defaultBitDepth > 0) this.defaultBitDepth = defaultBitDepth;
    }
    
    public int[] getSupportedBitDepths() { return supportedBitDepths; }
    public void setSupportedBitDepths(int[] supportedBitDepths) { 
        if (supportedBitDepths != null && supportedBitDepths.length > 0) {
            this.supportedBitDepths = supportedBitDepths;
        }
    }
    
    public int getPreferredBitDepth() { return preferredBitDepth; }
    public void setPreferredBitDepth(int preferredBitDepth) { 
        if (preferredBitDepth > 0) this.preferredBitDepth = preferredBitDepth;
    }
    
    public int getDefaultChannels() { return defaultChannels; }
    public void setDefaultChannels(int defaultChannels) { 
        if (defaultChannels > 0) this.defaultChannels = defaultChannels;
    }
    
    public int[] getSupportedChannels() { return supportedChannels; }
    public void setSupportedChannels(int[] supportedChannels) { 
        if (supportedChannels != null && supportedChannels.length > 0) {
            this.supportedChannels = supportedChannels;
        }
    }
    
    public int getPreferredChannels() { return preferredChannels; }
    public void setPreferredChannels(int preferredChannels) { 
        if (preferredChannels > 0) this.preferredChannels = preferredChannels;
    }
    
    public int getBufferSizeMs() { return bufferSizeMs; }
    public void setBufferSizeMs(int bufferSizeMs) { 
        if (bufferSizeMs > 0) this.bufferSizeMs = bufferSizeMs;
    }
    
    public int getMinBufferSizeMs() { return minBufferSizeMs; }
    public void setMinBufferSizeMs(int minBufferSizeMs) { 
        if (minBufferSizeMs > 0) this.minBufferSizeMs = minBufferSizeMs;
    }
    
    public int getMaxBufferSizeMs() { return maxBufferSizeMs; }
    public void setMaxBufferSizeMs(int maxBufferSizeMs) { 
        if (maxBufferSizeMs > 0) this.maxBufferSizeMs = maxBufferSizeMs;
    }
    
    public int getBufferCount() { return bufferCount; }
    public void setBufferCount(int bufferCount) { 
        if (bufferCount > 0) this.bufferCount = bufferCount;
    }
    
    public boolean isUseDirectBuffers() { return useDirectBuffers; }
    public void setUseDirectBuffers(boolean useDirectBuffers) { this.useDirectBuffers = useDirectBuffers; }
    
    public AudioQuality getDefaultQuality() { return defaultQuality; }
    public void setDefaultQuality(AudioQuality defaultQuality) { 
        if (defaultQuality != null) this.defaultQuality = defaultQuality;
    }
    
    public boolean isEnableNoiseReduction() { return enableNoiseReduction; }
    public void setEnableNoiseReduction(boolean enableNoiseReduction) { this.enableNoiseReduction = enableNoiseReduction; }
    
    public boolean isEnableEchoCancellation() { return enableEchoCancellation; }
    public void setEnableEchoCancellation(boolean enableEchoCancellation) { this.enableEchoCancellation = enableEchoCancellation; }
    
    public boolean isEnableAutomaticGainControl() { return enableAutomaticGainControl; }
    public void setEnableAutomaticGainControl(boolean enableAutomaticGainControl) { this.enableAutomaticGainControl = enableAutomaticGainControl; }
    
    public double getNoiseThreshold() { return noiseThreshold; }
    public void setNoiseThreshold(double noiseThreshold) { 
        if (noiseThreshold >= 0 && noiseThreshold <= 1) this.noiseThreshold = noiseThreshold;
    }
    
    public double getGainLevel() { return gainLevel; }
    public void setGainLevel(double gainLevel) { 
        if (gainLevel >= 0) this.gainLevel = gainLevel;
    }
    
    public int getMaxProcessingThreads() { return maxProcessingThreads; }
    public void setMaxProcessingThreads(int maxProcessingThreads) { 
        if (maxProcessingThreads > 0) this.maxProcessingThreads = maxProcessingThreads;
    }
    
    public int getCaptureThreadPoolSize() { return captureThreadPoolSize; }
    public void setCaptureThreadPoolSize(int captureThreadPoolSize) { 
        if (captureThreadPoolSize > 0) this.captureThreadPoolSize = captureThreadPoolSize;
    }
    
    public int getProcessingThreadPoolSize() { return processingThreadPoolSize; }
    public void setProcessingThreadPoolSize(int processingThreadPoolSize) { 
        if (processingThreadPoolSize > 0) this.processingThreadPoolSize = processingThreadPoolSize;
    }
    
    public boolean isUseAsyncProcessing() { return useAsyncProcessing; }
    public void setUseAsyncProcessing(boolean useAsyncProcessing) { this.useAsyncProcessing = useAsyncProcessing; }
    
    public long getProcessingTimeoutMs() { return processingTimeoutMs; }
    public void setProcessingTimeoutMs(long processingTimeoutMs) { 
        if (processingTimeoutMs > 0) this.processingTimeoutMs = processingTimeoutMs;
    }
    
    public PlatformAudioConfig getWindows() { return windows; }
    public void setWindows(PlatformAudioConfig windows) { this.windows = windows; }
    
    public PlatformAudioConfig getMac() { return mac; }
    public void setMac(PlatformAudioConfig mac) { this.mac = mac; }
    
    public PlatformAudioConfig getLinux() { return linux; }
    public void setLinux(PlatformAudioConfig linux) { this.linux = linux; }
    
    public boolean isEnableAudioNormalization() { return enableAudioNormalization; }
    public void setEnableAudioNormalization(boolean enableAudioNormalization) { this.enableAudioNormalization = enableAudioNormalization; }
    
    public double getNormalizationTargetLevel() { return normalizationTargetLevel; }
    public void setNormalizationTargetLevel(double normalizationTargetLevel) { 
        if (normalizationTargetLevel > 0 && normalizationTargetLevel <= 1.0) {
            this.normalizationTargetLevel = normalizationTargetLevel;
        }
    }
    
    public boolean isEnableHighPassFilter() { return enableHighPassFilter; }
    public void setEnableHighPassFilter(boolean enableHighPassFilter) { this.enableHighPassFilter = enableHighPassFilter; }
    
    public double getHighPassFrequency() { return highPassFrequency; }
    public void setHighPassFrequency(double highPassFrequency) { 
        if (highPassFrequency > 0) this.highPassFrequency = highPassFrequency;
    }
    
    public boolean isEnableLowPassFilter() { return enableLowPassFilter; }
    public void setEnableLowPassFilter(boolean enableLowPassFilter) { this.enableLowPassFilter = enableLowPassFilter; }
    
    public double getLowPassFrequency() { return lowPassFrequency; }
    public void setLowPassFrequency(double lowPassFrequency) { 
        if (lowPassFrequency > 0) this.lowPassFrequency = lowPassFrequency;
    }
    
    public double getVoiceActivityThreshold() { return voiceActivityThreshold; }
    public void setVoiceActivityThreshold(double voiceActivityThreshold) { 
        if (voiceActivityThreshold >= 0 && voiceActivityThreshold <= 1) {
            this.voiceActivityThreshold = voiceActivityThreshold;
        }
    }
    
    public int getVoiceActivityMinDurationMs() { return voiceActivityMinDurationMs; }
    public void setVoiceActivityMinDurationMs(int voiceActivityMinDurationMs) { 
        if (voiceActivityMinDurationMs >= 0) this.voiceActivityMinDurationMs = voiceActivityMinDurationMs;
    }
    
    public int getVoiceActivityMaxSilenceMs() { return voiceActivityMaxSilenceMs; }
    public void setVoiceActivityMaxSilenceMs(int voiceActivityMaxSilenceMs) { 
        if (voiceActivityMaxSilenceMs >= 0) this.voiceActivityMaxSilenceMs = voiceActivityMaxSilenceMs;
    }
    
    public boolean isEnableVoiceActivityDetection() { return enableVoiceActivityDetection; }
    public void setEnableVoiceActivityDetection(boolean enableVoiceActivityDetection) { this.enableVoiceActivityDetection = enableVoiceActivityDetection; }
    
    public boolean isEnableRecording() { return enableRecording; }
    public void setEnableRecording(boolean enableRecording) { this.enableRecording = enableRecording; }
    
    public String getRecordingFormat() { return recordingFormat; }
    public void setRecordingFormat(String recordingFormat) { 
        if (recordingFormat != null && !recordingFormat.isEmpty()) {
            this.recordingFormat = recordingFormat;
        }
    }
    
    public String getRecordingDirectory() { return recordingDirectory; }
    public void setRecordingDirectory(String recordingDirectory) { 
        if (recordingDirectory != null && !recordingDirectory.isEmpty()) {
            this.recordingDirectory = recordingDirectory;
        }
    }
    
    public long getMaxRecordingSizeBytes() { return maxRecordingSizeBytes; }
    public void setMaxRecordingSizeBytes(long maxRecordingSizeBytes) { 
        if (maxRecordingSizeBytes > 0) this.maxRecordingSizeBytes = maxRecordingSizeBytes;
    }
    
    public int getMaxRecordingDurationMinutes() { return maxRecordingDurationMinutes; }
    public void setMaxRecordingDurationMinutes(int maxRecordingDurationMinutes) { 
        if (maxRecordingDurationMinutes > 0) this.maxRecordingDurationMinutes = maxRecordingDurationMinutes;
    }
}