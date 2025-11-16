package com.zoomtranscriber.core.audio;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.sound.sampled.AudioFormat;
import java.util.List;

/**
 * Platform-specific interface for accessing system audio services.
 * Provides abstraction for different operating systems.
 */
public interface PlatformAudioService {
    
    /**
     * Initializes the audio service for the current platform.
     * 
     * @return Mono that completes when initialization is successful
     */
    Mono<Void> initialize();
    
    /**
     * Shuts down the audio service and releases resources.
     * 
     * @return Mono that completes when shutdown is successful
     */
    Mono<Void> shutdown();
    
    /**
     * Lists all available audio input devices.
     * 
     * @return Flux of AudioDevice objects
     */
    Flux<AudioDevice> getInputDevices();
    
    /**
     * Lists all available audio output devices.
     * 
     * @return Flux of AudioDevice objects
     */
    Flux<AudioDevice> getOutputDevices();
    
    /**
     * Gets the default input device.
     * 
     * @return Mono containing the default AudioDevice or empty if none
     */
    Mono<AudioDevice> getDefaultInputDevice();
    
    /**
     * Gets the default output device.
     * 
     * @return Mono containing the default AudioDevice or empty if none
     */
    Mono<AudioDevice> getDefaultOutputDevice();
    
    /**
     * Opens an audio input device for capture.
     * 
     * @param deviceId the device identifier
     * @param format the desired audio format
     * @return Mono containing AudioInputStream or error if failed
     */
    Mono<javax.sound.sampled.TargetDataLine> openInputDevice(String deviceId, AudioFormat format);
    
    /**
     * Opens an audio output device for playback.
     * 
     * @param deviceId the device identifier
     * @param format the desired audio format
     * @return Mono containing SourceDataLine or error if failed
     */
    Mono<javax.sound.sampled.SourceDataLine> openOutputDevice(String deviceId, AudioFormat format);
    
    /**
     * Captures system audio (what you hear).
     * 
     * @param format the desired audio format
     * @return Flux of audio data chunks
     */
    Flux<byte[]> captureSystemAudio(AudioFormat format);
    
    /**
     * Captures audio from a specific application.
     * 
     * @param applicationName the application name
     * @param format the desired audio format
     * @return Flux of audio data chunks
     */
    Flux<byte[]> captureApplicationAudio(String applicationName, AudioFormat format);
    
    /**
     * Gets the current audio mixer configuration.
     * 
     * @return Mono containing MixerInfo or error if failed
     */
    Mono<javax.sound.sampled.Mixer.Info> getCurrentMixer();
    
    /**
     * Sets the audio mixer to use.
     * 
     * @param mixerInfo the mixer information
     * @return Mono that completes when mixer is set
     */
    Mono<Void> setMixer(javax.sound.sampled.Mixer.Info mixerInfo);
    
    /**
     * Gets supported audio formats for a device.
     * 
     * @param deviceId the device identifier
     * @return Flux of supported AudioFormat objects
     */
    Flux<AudioFormat> getSupportedFormats(String deviceId);
    
    /**
     * Checks if a device supports a specific format.
     * 
     * @param deviceId the device identifier
     * @param format the audio format to check
     * @return Mono containing true if supported, false otherwise
     */
    Mono<Boolean> isFormatSupported(String deviceId, AudioFormat format);
    
    /**
     * Gets the current audio latency for a device.
     * 
     * @param deviceId the device identifier
     * @return Mono containing latency in milliseconds
     */
    Mono<Long> getDeviceLatency(String deviceId);
    
    /**
     * Sets the buffer size for a device.
     * 
     * @param deviceId the device identifier
     * @param bufferSize the buffer size in bytes
     * @return Mono that completes when buffer size is set
     */
    Mono<Void> setBufferSize(String deviceId, int bufferSize);
    
    /**
     * Represents an audio device on the system.
     */
    record AudioDevice(
        String id,
        String name,
        String description,
        AudioDeviceType type,
        boolean isDefault,
        boolean isAvailable,
        int maxChannels,
        float[] sampleRates,
        int[] sampleSizes
    ) {
        public enum AudioDeviceType {
            INPUT,
            OUTPUT,
            DUPLEX
        }
        
        /**
         * Checks if the device supports a specific sample rate.
         * 
         * @param sampleRate the sample rate to check
         * @return true if supported
         */
        public boolean supportsSampleRate(float sampleRate) {
            if (sampleRates == null) return false;
            for (var rate : sampleRates) {
                if (Math.abs(rate - sampleRate) < 0.001f) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Checks if the device supports a specific sample size.
         * 
         * @param sampleSize the sample size to check
         * @return true if supported
         */
        public boolean supportsSampleSize(int sampleSize) {
            if (sampleSizes == null) return false;
            for (var size : sampleSizes) {
                if (size == sampleSize) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Gets the maximum supported sample rate.
         * 
         * @return maximum sample rate
         */
        public float getMaxSampleRate() {
            if (sampleRates == null || sampleRates.length == 0) return 0;
            var maxRate = sampleRates[0];
            for (var rate : sampleRates) {
                if (rate > maxRate) maxRate = rate;
            }
            return maxRate;
        }
        
        /**
         * Gets the maximum supported sample size.
         * 
         * @return maximum sample size
         */
        public int getMaxSampleSize() {
            if (sampleSizes == null || sampleSizes.length == 0) return 0;
            var maxSize = sampleSizes[0];
            for (var size : sampleSizes) {
                if (size > maxSize) maxSize = size;
            }
            return maxSize;
        }
    }
    
    /**
     * Audio device configuration.
     */
    record AudioDeviceConfig(
        String deviceId,
        AudioFormat format,
        int bufferSize,
        float gain,
        boolean mute
    ) {
        /**
         * Creates a default device configuration.
         * 
         * @param deviceId the device identifier
         * @return default AudioDeviceConfig
         */
        public static AudioDeviceConfig defaultConfig(String deviceId) {
            return new AudioDeviceConfig(
                deviceId,
                new AudioFormat(44100, 16, 2, true, false),
                4096,
                1.0f,
                false
            );
        }
    }
    
    /**
     * System audio capture configuration.
     */
    record SystemAudioConfig(
        boolean captureSystemAudio,
        boolean captureApplicationAudio,
        List<String> targetApplications,
        AudioFormat format,
        boolean includeLoopback
    ) {
        /**
         * Creates a default system audio configuration.
         * 
         * @return default SystemAudioConfig
         */
        public static SystemAudioConfig defaultConfig() {
            return new SystemAudioConfig(
                false,
                false,
                List.of(),
                new AudioFormat(44100, 16, 2, true, false),
                false
            );
        }
    }
}