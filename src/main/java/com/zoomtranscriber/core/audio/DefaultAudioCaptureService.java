package com.zoomtranscriber.core.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of AudioCaptureService.
 * Uses Java Sound API for audio capture from system devices.
 */
@Service
public class DefaultAudioCaptureService implements AudioCaptureService {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultAudioCaptureService.class);
    
    private final ConcurrentHashMap<String, AudioSource> availableSources = new ConcurrentHashMap<>();
    private final Sinks.Many<AudioChunk> audioSink = Sinks.many().multicast().onBackpressureBuffer();
    private final AtomicBoolean isCapturing = new AtomicBoolean(false);
    private volatile TargetDataLine targetLine;
    private volatile Thread captureThread;
    private volatile AudioSource currentSource;
    private volatile AudioFormat currentFormat;
    private volatile AudioQuality currentQuality;
    private volatile boolean noiseReductionEnabled = true;
    
    @Override
    public Mono<Void> startCapture(String sourceId, AudioFormat format) {
        return Mono.fromRunnable(() -> {
            if (isCapturing.get()) {
                logger.warn("Audio capture is already active");
                return;
            }
            
            logger.info("Starting audio capture with format: {}", format);
            
            try {
                currentFormat = format;
                
                // Find and open the audio line
                TargetDataLine line = getTargetDataLine(sourceId, format);
                line.open(format);
                line.start();
                
                targetLine = line;
                isCapturing.set(true);
                
                // Start capture thread
                startCaptureThread(line, format);
                
                // Update current source
                if (sourceId != null) {
                    currentSource = availableSources.get(sourceId);
                } else {
                    currentSource = getDefaultSource();
                }
                
                logger.info("Audio capture started successfully");
                
            } catch (LineUnavailableException e) {
                logger.error("Audio line not available", e);
                throw new RuntimeException("Failed to start audio capture", e);
            } catch (Exception e) {
                logger.error("Failed to start audio capture", e);
                throw new RuntimeException("Failed to start audio capture", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    @Override
    public Mono<Void> stopCapture() {
        return Mono.fromRunnable(() -> {
            if (!isCapturing.get()) {
                logger.warn("Audio capture is not active");
                return;
            }
            
            logger.info("Stopping audio capture");
            
            isCapturing.set(false);
            
            // Stop and close the audio line
            if (targetLine != null) {
                targetLine.stop();
                targetLine.close();
                targetLine = null;
            }
            
            // Stop capture thread
            if (captureThread != null) {
                captureThread.interrupt();
                captureThread = null;
            }
            
            // Complete the audio stream
            audioSink.tryEmitComplete();
            
            logger.info("Audio capture stopped successfully");
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    @Override
    public boolean isCapturing() {
        return isCapturing.get();
    }
    
    @Override
    public Flux<AudioChunk> getAudioStream() {
        return audioSink.asFlux()
            .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public AudioFormat getCurrentFormat() {
        return currentFormat;
    }
    
    @Override
    public Flux<AudioSource> getAvailableSources() {
        return Mono.fromCallable(() -> {
            refreshAvailableSources();
            return null;
        })
        .flatMapMany(ignored -> Flux.fromIterable(availableSources.values()))
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<Void> setAudioSource(String sourceId) {
        return Mono.fromRunnable(() -> {
            AudioSource source = availableSources.get(sourceId);
            if (source == null) {
                throw new IllegalArgumentException("Audio source not found: " + sourceId);
            }
            
            // If currently capturing, stop and restart with new source
            boolean wasCapturing = isCapturing.get();
            if (wasCapturing) {
                stopCapture().block();
            }
            
            currentSource = source;
            
            if (wasCapturing && currentFormat != null) {
                startCapture(sourceId, currentFormat).block();
            }
            
            logger.info("Audio source set to: {}", source.name());
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    @Override
    public AudioSource getCurrentSource() {
        return currentSource;
    }
    
    @Override
    public Mono<Void> setQuality(AudioQuality quality) {
        return Mono.fromRunnable(() -> {
            currentQuality = quality;
            logger.info("Audio quality set to: {}", quality);
            
            // If currently capturing, restart with new quality
            if (isCapturing.get() && currentSource != null) {
                var newFormat = quality.toAudioFormat();
                stopCapture().block();
                startCapture(currentSource.id(), newFormat).block();
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    @Override
    public AudioQuality getCurrentQuality() {
        return currentQuality != null ? currentQuality : AudioQuality.medium();
    }
    
    @Override
    public Mono<Void> setNoiseReduction(boolean enabled) {
        return Mono.fromRunnable(() -> {
            noiseReductionEnabled = enabled;
            logger.info("Noise reduction {}", enabled ? "enabled" : "disabled");
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    @Override
    public boolean isNoiseReductionEnabled() {
        return noiseReductionEnabled;
    }
    
    /**
     * Starts the audio capture thread.
     */
    private void startCaptureThread(TargetDataLine line, AudioFormat format) {
        captureThread = new Thread(() -> {
            var bufferSize = (int) (format.getSampleRate() * format.getChannels() * 
                (format.getSampleSizeInBits() / 8) * 0.1); // 100ms buffer
            var buffer = new byte[bufferSize];
            
            while (isCapturing.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    var bytesRead = line.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        var audioData = new byte[bytesRead];
                        System.arraycopy(buffer, 0, audioData, 0, bytesRead);
                        
                        // Create audio chunk
                        var chunk = new AudioChunk(
                            audioData,
                            format,
                            Duration.ofMillis((long) (bytesRead * 1000.0 / 
                                (format.getSampleRate() * format.getChannels() * format.getSampleSizeInBits() / 8))),
                            System.currentTimeMillis(),
                            calculateVolumeLevel(audioData, format)
                        );
                        
                        // Emit to stream
                        audioSink.tryEmitNext(chunk);
                    }
                } catch (Exception e) {
                    if (isCapturing.get()) {
                        logger.error("Error in audio capture thread", e);
                        audioSink.tryEmitError(e);
                    }
                    break;
                }
            }
        });
        
        captureThread.setDaemon(true);
        captureThread.start();
    }
    
    /**
     * Gets a TargetDataLine for the specified source.
     */
    private TargetDataLine getTargetDataLine(String sourceId, AudioFormat format) throws LineUnavailableException {
        Mixer.Info mixerInfo = null;
        
        if (sourceId != null) {
            // Find mixer for the specific source
            for (Mixer.Info info : AudioSystem.getMixerInfo()) {
                if (info.getName().equals(sourceId) || 
                    info.getDescription().contains(sourceId)) {
                    mixerInfo = info;
                    break;
                }
            }
        }
        
        // Get mixer (default if not found)
        Mixer mixer = mixerInfo != null ? 
            AudioSystem.getMixer(mixerInfo) : 
            AudioSystem.getMixer(null);
        
        var dataLineInfo = new Line.Info(TargetDataLine.class);
        
        // Find matching line
        for (Line.Info lineInfo : mixer.getTargetLineInfo()) {
            if (lineInfo.getLineClass().equals(TargetDataLine.class)) {
                var line = (TargetDataLine) mixer.getLine(lineInfo);
                if (line.getFormat().matches(format)) {
                    return line;
                }
            }
        }
        
        // Create new line with specified format
        return (TargetDataLine) mixer.getLine(dataLineInfo);
    }
    
    /**
     * Refreshes the list of available audio sources.
     */
    private void refreshAvailableSources() {
        availableSources.clear();
        
        var mixers = AudioSystem.getMixerInfo();
        for (int i = 0; i < mixers.length; i++) {
            var mixerInfo = mixers[i];
            var mixer = AudioSystem.getMixer(mixerInfo);
            
            var targetLines = mixer.getTargetLineInfo();
            for (var lineInfo : targetLines) {
                if (lineInfo.getLineClass().equals(TargetDataLine.class)) {
                    var source = new AudioSource(
                        mixerInfo.getName(),
                        mixerInfo.getName(),
                        mixerInfo.getDescription(),
                        AudioSource.AudioSourceType.MICROPHONE,
                        i == 0, // First mixer is default
                        true
                    );
                    
                    availableSources.put(source.id(), source);
                    logger.debug("Found audio source: {}", source.name());
                }
            }
        }
        
        logger.info("Found {} audio sources", availableSources.size());
    }
    
    /**
     * Gets the default audio source.
     */
    private AudioSource getDefaultSource() {
        refreshAvailableSources();
        return availableSources.values().stream()
            .filter(AudioSource::isDefault)
            .findFirst()
            .orElse(availableSources.values().stream().findFirst().orElse(null));
    }
    
    /**
     * Calculates the volume level of audio data.
     */
    private double calculateVolumeLevel(byte[] audioData, AudioFormat format) {
        var sum = 0.0;
        var samples = audioData.length / (format.getSampleSizeInBits() / 8);
        
        if (format.getSampleSizeInBits() == 16) {
            for (int i = 0; i < audioData.length - 1; i += 2) {
                var sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xff));
                sum += (sample / 32768.0) * (sample / 32768.0);
            }
        } else if (format.getSampleSizeInBits() == 8) {
            for (byte sample : audioData) {
                var normalized = sample / 128.0;
                sum += normalized * normalized;
            }
        }
        
        var rms = Math.sqrt(sum / samples);
        return Math.min(1.0, rms * 10); // Scale for better visibility
    }
}