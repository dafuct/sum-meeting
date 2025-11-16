package com.zoomtranscriber.core.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Processes audio data for real-time transcription.
 * Handles audio format conversion, noise reduction, and chunking.
 */
@Component
public class AudioProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(AudioProcessor.class);
    
    private static final int OVERLAP_SAMPLES = 512;
    private static final double NOISE_THRESHOLD = 0.01;
    private static final double SILENCE_THRESHOLD = 0.001;
    private static final Duration SILENCE_TIMEOUT = Duration.ofSeconds(2);
    
    private final ConcurrentLinkedQueue<byte[]> audioBuffer = new ConcurrentLinkedQueue<>();
    private AudioFormat currentFormat;
    private com.zoomtranscriber.core.audio.AudioCaptureService.AudioQuality currentQuality;
    private boolean noiseReductionEnabled = true;
    private boolean autoGainControlEnabled = true;
    private double currentGain = 1.0;
    private long lastAudioTime = System.currentTimeMillis();
    
    /**
     * Processes raw audio data and returns processed chunks.
     * 
     * @param audioData raw audio data
     * @param format audio format
     * @return Flux of processed AudioChunk objects
     */
    public Flux<AudioCaptureService.AudioChunk> processAudio(byte[] audioData, AudioFormat format) {
        return Mono.fromCallable(() -> {
            if (currentFormat == null || !currentFormat.matches(format)) {
                currentFormat = format;
                logger.info("Audio format changed to: {}", format);
            }
            
            // Add to buffer
            audioBuffer.offer(audioData);
            
            // Process if we have enough data
            var chunkSize = calculateChunkSize(format);
            var totalSize = audioBuffer.stream().mapToInt(arr -> arr.length).sum();
            
            if (totalSize >= chunkSize) {
                return createProcessedChunk(chunkSize);
            }
            
            return null;
        })
        .filter(chunk -> chunk != null)
        .flux()
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * Applies noise reduction to audio data.
     * 
     * @param audioData raw audio data
     * @param format audio format
     * @return noise-reduced audio data
     */
    private byte[] applyNoiseReduction(byte[] audioData, AudioFormat format) {
        if (!noiseReductionEnabled) {
            return audioData;
        }
        
        var samples = convertToSamples(audioData, format);
        var filteredSamples = new double[samples.length];
        
        // Simple high-pass filter for noise reduction
        var alpha = 0.95;
        var previousSample = 0.0;
        
        for (int i = 0; i < samples.length; i++) {
            var currentSample = samples[i];
            filteredSamples[i] = alpha * previousSample + (1 - alpha) * currentSample;
            previousSample = filteredSamples[i];
        }
        
        return convertFromSamples(filteredSamples, format);
    }
    
    /**
     * Applies automatic gain control to audio data.
     * 
     * @param audioData raw audio data
     * @param format audio format
     * @return gain-adjusted audio data
     */
    private byte[] applyAutoGainControl(byte[] audioData, AudioFormat format) {
        if (!autoGainControlEnabled) {
            return audioData;
        }
        
        var samples = convertToSamples(audioData, format);
        var maxAmplitude = 0.0;
        
        // Find maximum amplitude
        for (var sample : samples) {
            maxAmplitude = Math.max(maxAmplitude, Math.abs(sample));
        }
        
        // Calculate target gain
        var targetAmplitude = 0.8;
        var newGain = maxAmplitude > 0 ? targetAmplitude / maxAmplitude : 1.0;
        
        // Smooth gain changes
        var gainSmoothingFactor = 0.1;
        currentGain = gainSmoothingFactor * newGain + (1 - gainSmoothingFactor) * currentGain;
        
        // Apply gain
        for (int i = 0; i < samples.length; i++) {
            samples[i] *= currentGain;
        }
        
        return convertFromSamples(samples, format);
    }
    
    /**
     * Detects voice activity in audio data.
     * 
     * @param audioData raw audio data
     * @param format audio format
     * @return true if voice activity is detected
     */
    private boolean detectVoiceActivity(byte[] audioData, AudioFormat format) {
        var samples = convertToSamples(audioData, format);
        var energy = 0.0;
        
        for (var sample : samples) {
            energy += sample * sample;
        }
        
        var rmsEnergy = Math.sqrt(energy / samples.length);
        var hasVoiceActivity = rmsEnergy > NOISE_THRESHOLD;
        
        if (hasVoiceActivity) {
            lastAudioTime = System.currentTimeMillis();
        }
        
        return hasVoiceActivity;
    }
    
    /**
     * Creates a processed audio chunk from the buffer.
     * 
     * @param chunkSize desired chunk size
     * @return processed AudioChunk
     */
    private AudioCaptureService.AudioChunk createProcessedChunk(int chunkSize) {
        var chunkData = new byte[chunkSize];
        var bytesWritten = 0;
        
        // Collect data from buffer
        while (bytesWritten < chunkSize && !audioBuffer.isEmpty()) {
            var data = audioBuffer.poll();
            if (data != null) {
                var copyLength = Math.min(data.length, chunkSize - bytesWritten);
                System.arraycopy(data, 0, chunkData, bytesWritten, copyLength);
                bytesWritten += copyLength;
            }
        }
        
        // Apply processing
        if (currentFormat != null) {
            chunkData = applyNoiseReduction(chunkData, currentFormat);
            chunkData = applyAutoGainControl(chunkData, currentFormat);
        }
        
        // Calculate volume level
        var volumeLevel = calculateVolumeLevel(chunkData, currentFormat);
        
        // Update last audio time if there's voice activity
        if (detectVoiceActivity(chunkData, currentFormat)) {
            lastAudioTime = System.currentTimeMillis();
        }
        
        return new AudioCaptureService.AudioChunk(
            chunkData,
            currentFormat,
            Duration.ofMillis((long) (chunkSize * 1000.0 / (currentFormat.getSampleRate() * currentFormat.getChannels() * currentFormat.getSampleSizeInBits() / 8))),
            System.currentTimeMillis(),
            volumeLevel
        );
    }
    
    /**
     * Converts raw audio data to sample array.
     * 
     * @param audioData raw audio data
     * @param format audio format
     * @return array of sample values
     */
    private double[] convertToSamples(byte[] audioData, AudioFormat format) {
        var samples = new double[audioData.length / (format.getSampleSizeInBits() / 8)];
        var buffer = ByteBuffer.wrap(audioData).order(ByteOrder.LITTLE_ENDIAN);
        
        if (format.getSampleSizeInBits() == 16) {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = buffer.getShort() / 32768.0;
            }
        } else if (format.getSampleSizeInBits() == 8) {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = buffer.get() / 128.0;
            }
        }
        
        return samples;
    }
    
    /**
     * Converts sample array back to raw audio data.
     * 
     * @param samples array of sample values
     * @param format audio format
     * @return raw audio data
     */
    private byte[] convertFromSamples(double[] samples, AudioFormat format) {
        var audioData = new byte[samples.length * (format.getSampleSizeInBits() / 8)];
        var buffer = ByteBuffer.wrap(audioData).order(ByteOrder.LITTLE_ENDIAN);
        
        if (format.getSampleSizeInBits() == 16) {
            for (var sample : samples) {
                var clampedSample = Math.max(-1.0, Math.min(1.0, sample));
                buffer.putShort((short) (clampedSample * 32767));
            }
        } else if (format.getSampleSizeInBits() == 8) {
            for (var sample : samples) {
                var clampedSample = Math.max(-1.0, Math.min(1.0, sample));
                buffer.put((byte) (clampedSample * 127));
            }
        }
        
        return audioData;
    }
    
    /**
     * Calculates the volume level of audio data.
     * 
     * @param audioData raw audio data
     * @param format audio format
     * @return volume level (0.0 to 1.0)
     */
    private double calculateVolumeLevel(byte[] audioData, AudioFormat format) {
        var samples = convertToSamples(audioData, format);
        var sum = 0.0;
        
        for (var sample : samples) {
            sum += sample * sample;
        }
        
        var rms = Math.sqrt(sum / samples.length);
        return Math.min(1.0, rms * 10); // Scale for better visibility
    }
    
    /**
     * Calculates optimal chunk size based on audio format.
     * 
     * @param format audio format
     * @return chunk size in bytes
     */
    private int calculateChunkSize(AudioFormat format) {
        var bytesPerSample = format.getSampleSizeInBits() / 8;
        var samplesPerChunk = (int) (format.getSampleRate() * 0.1); // 100ms chunks
        return samplesPerChunk * format.getChannels() * bytesPerSample;
    }
    
    /**
     * Checks if there has been silence for the specified duration.
     * 
     * @return true if silence detected
     */
    public boolean isSilenceDetected() {
        var timeSinceLastAudio = System.currentTimeMillis() - lastAudioTime;
        return timeSinceLastAudio > SILENCE_TIMEOUT.toMillis();
    }
    
    /**
     * Sets the audio quality for processing.
     * 
     * @param quality audio quality settings
     */
    public void setAudioQuality(AudioCaptureService.AudioQuality quality) {
        this.currentQuality = quality;
        logger.info("Audio quality set to: {}", quality);
    }
    
    /**
     * Gets the current audio quality.
     * 
     * @return current AudioQuality
     */
    public AudioCaptureService.AudioQuality getCurrentAudioQuality() {
        return currentQuality;
    }
    
    /**
     * Enables or disables noise reduction.
     * 
     * @param enabled true to enable noise reduction
     */
    public void setNoiseReductionEnabled(boolean enabled) {
        this.noiseReductionEnabled = enabled;
        logger.info("Noise reduction {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Checks if noise reduction is enabled.
     * 
     * @return true if noise reduction is enabled
     */
    public boolean isNoiseReductionEnabled() {
        return noiseReductionEnabled;
    }
    
    /**
     * Enables or disables automatic gain control.
     * 
     * @param enabled true to enable auto gain control
     */
    public void setAutoGainControlEnabled(boolean enabled) {
        this.autoGainControlEnabled = enabled;
        logger.info("Auto gain control {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Checks if auto gain control is enabled.
     * 
     * @return true if auto gain control is enabled
     */
    public boolean isAutoGainControlEnabled() {
        return autoGainControlEnabled;
    }
    
    /**
     * Clears the audio buffer.
     */
    public void clearBuffer() {
        audioBuffer.clear();
        logger.debug("Audio buffer cleared");
    }
    
    /**
     * Gets the current buffer size in bytes.
     * 
     * @return buffer size
     */
    public int getBufferSize() {
        return audioBuffer.stream().mapToInt(arr -> arr.length).sum();
    }
}