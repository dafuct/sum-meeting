package com.zoomtranscriber.core.audio;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.sound.sampled.AudioFormat;
import java.time.Duration;
import java.util.UUID;

/**
 * Service interface for capturing audio from system sources.
 * Provides reactive streams for real-time audio data capture.
 */
public interface AudioCaptureService {
    
    /**
     * Starts audio capture from the specified source.
     * 
     * @param sourceId the audio source identifier
     * @param format the desired audio format
     * @return Mono that completes when capture starts successfully
     */
    Mono<Void> startCapture(String sourceId, AudioFormat format);
    
    /**
     * Stops audio capture.
     * 
     * @return Mono that completes when capture stops successfully
     */
    Mono<Void> stopCapture();
    
    /**
     * Gets the current capture status.
     * 
     * @return true if capture is active, false otherwise
     */
    boolean isCapturing();
    
    /**
     * Gets a stream of audio data chunks.
     * 
     * @return Flux of AudioChunk objects containing captured audio data
     */
    Flux<AudioChunk> getAudioStream();
    
    /**
     * Gets the current audio format being used.
     * 
     * @return current AudioFormat or null if not capturing
     */
    AudioFormat getCurrentFormat();
    
    /**
     * Lists available audio input sources.
     * 
     * @return Flux of AudioSource objects representing available inputs
     */
    Flux<AudioSource> getAvailableSources();
    
    /**
     * Sets the audio input source.
     * 
     * @param sourceId the source identifier
     * @return Mono that completes when source is set successfully
     */
    Mono<Void> setAudioSource(String sourceId);
    
    /**
     * Gets the current audio input source.
     * 
     * @return current AudioSource or null if none selected
     */
    AudioSource getCurrentSource();
    
    /**
     * Sets the audio capture quality settings.
     * 
     * @param quality the quality settings
     * @return Mono that completes when settings are applied
     */
    Mono<Void> setQuality(AudioQuality quality);
    
    /**
     * Gets the current audio quality settings.
     * 
     * @return current AudioQuality
     */
    AudioQuality getCurrentQuality();
    
    /**
     * Enables or disables noise reduction.
     * 
     * @param enabled true to enable noise reduction
     * @return Mono that completes when setting is applied
     */
    Mono<Void> setNoiseReduction(boolean enabled);
    
    /**
     * Checks if noise reduction is enabled.
     * 
     * @return true if noise reduction is enabled
     */
    boolean isNoiseReductionEnabled();
    
    /**
     * Represents a chunk of captured audio data.
     */
    record AudioChunk(
        byte[] data,
        AudioFormat format,
        Duration duration,
        long timestamp,
        double volumeLevel
    ) {
        /**
         * Gets the size of the audio data in bytes.
         * 
         * @return data size
         */
        public int getSize() {
            return data != null ? data.length : 0;
        }
        
        /**
         * Gets the sample rate of the audio chunk.
         * 
         * @return sample rate in Hz
         */
        public float getSampleRate() {
            return format != null ? format.getSampleRate() : 0.0f;
        }
        
        /**
         * Gets the number of channels in the audio chunk.
         * 
         * @return number of channels
         */
        public int getChannels() {
            return format != null ? format.getChannels() : 0;
        }
    }
    
    /**
     * Represents an available audio input source.
     */
    record AudioSource(
        String id,
        String name,
        String description,
        AudioSourceType type,
        boolean isDefault,
        boolean isAvailable
    ) {
        public enum AudioSourceType {
            MICROPHONE,
            LINE_IN,
            USB_DEVICE,
            BLUETOOTH,
            VIRTUAL,
            SYSTEM_AUDIO
        }
    }
    
    /**
     * Represents audio quality settings.
     */
    record AudioQuality(
        int sampleRate,
        int sampleSizeInBits,
        int channels,
        float frameRate,
        boolean bigEndian,
        int bufferSize
    ) {
        /**
         * Creates a high-quality audio configuration.
         * 
         * @return high-quality AudioQuality
         */
        public static AudioQuality high() {
            return new AudioQuality(44100, 16, 2, 44100.0f, false, 4096);
        }
        
        /**
         * Creates a medium-quality audio configuration.
         * 
         * @return medium-quality AudioQuality
         */
        public static AudioQuality medium() {
            return new AudioQuality(22050, 16, 1, 22050.0f, false, 2048);
        }
        
        /**
         * Creates a low-quality audio configuration.
         * 
         * @return low-quality AudioQuality
         */
        public static AudioQuality low() {
            return new AudioQuality(16000, 16, 1, 16000.0f, false, 1024);
        }
        
        /**
         * Creates an AudioFormat from this quality configuration.
         * 
         * @return AudioFormat object
         */
        public AudioFormat toAudioFormat() {
            return new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate,
                sampleSizeInBits,
                channels,
                (sampleRate * sampleSizeInBits * channels) / 8,
                frameRate,
                bigEndian
            );
        }
    }
    
    /**
     * Audio capture configuration.
     */
    record CaptureConfig(
        String sourceId,
        AudioQuality quality,
        boolean noiseReduction,
        Duration chunkDuration,
        boolean autoGainControl
    ) {
        /**
         * Creates a default capture configuration.
         * 
         * @return default CaptureConfig
         */
        public static CaptureConfig defaultConfig() {
            return new CaptureConfig(
                null, // Auto-detect source
                AudioQuality.medium(),
                true,
                Duration.ofMillis(100),
                true
            );
        }
    }
}