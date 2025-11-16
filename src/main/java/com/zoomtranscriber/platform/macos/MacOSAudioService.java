package com.zoomtranscriber.platform.macos;

import com.zoomtranscriber.core.audio.AudioCaptureService;
import com.zoomtranscriber.core.audio.PlatformAudioService;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * macOS-specific implementation of audio services.
 * Uses Core Audio and Java Sound API for audio capture and playback.
 */
@Component
public class MacOSAudioService implements PlatformAudioService {
    
    private static final Logger logger = LoggerFactory.getLogger(MacOSAudioService.class);
    
    // Native library interfaces
    private static final CoreAudioLib CORE_AUDIO = Native.load("CoreAudio", CoreAudioLib.class);
    private static final AudioUnitLib AUDIO_UNIT = Native.load("AudioUnit", AudioUnitLib.class);
    
    private final ConcurrentHashMap<String, AudioDevice> deviceCache = new ConcurrentHashMap<>();
    private boolean initialized = false;
    private String currentMixerId;
    
    @Override
    public Mono<Void> initialize() {
        return Mono.fromRunnable(() -> {
            logger.info("Initializing macOS audio service");
            
            try {
                // Initialize Core Audio subsystem
                var result = CORE_AUDIO.AudioHardwareInitialize();
                if (result != 0) {
                    throw new RuntimeException("Failed to initialize Core Audio: " + result);
                }
                
                // Cache available devices
                refreshDeviceCache();
                
                initialized = true;
                logger.info("macOS audio service initialized successfully");
                
            } catch (Exception e) {
                logger.error("Failed to initialize macOS audio service", e);
                throw new RuntimeException("Audio service initialization failed", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    @Override
    public Mono<Void> shutdown() {
        return Mono.fromRunnable(() -> {
            logger.info("Shutting down macOS audio service");
            
            try {
                // Cleanup Core Audio subsystem
                var result = CORE_AUDIO.AudioHardwareTerminate();
                if (result != 0) {
                    logger.warn("Core Audio termination warning: {}", result);
                }
                
                deviceCache.clear();
                initialized = false;
                logger.info("macOS audio service shut down successfully");
                
            } catch (Exception e) {
                logger.error("Failed to shutdown macOS audio service", e);
                throw new RuntimeException("Audio service shutdown failed", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    @Override
    public Flux<AudioDevice> getInputDevices() {
        return Mono.fromRunnable(this::refreshDeviceCache)
            .flatMapMany(ignored -> Flux.fromIterable(deviceCache.values()))
            .filter(device -> device.type() == AudioDevice.AudioDeviceType.INPUT || 
                              device.type() == AudioDevice.AudioDeviceType.DUPLEX)
            .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Flux<AudioDevice> getOutputDevices() {
        return Mono.fromRunnable(this::refreshDeviceCache)
            .flatMapMany(ignored -> Flux.fromIterable(deviceCache.values()))
            .filter(device -> device.type() == AudioDevice.AudioDeviceType.OUTPUT || 
                              device.type() == AudioDevice.AudioDeviceType.DUPLEX)
            .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<AudioDevice> getDefaultInputDevice() {
        return Mono.fromCallable(() -> {
            try {
                var mixers = AudioSystem.getMixerInfo();
                for (var mixerInfo : mixers) {
                    var mixer = AudioSystem.getMixer(mixerInfo);
                    var sourceLines = mixer.getTargetLineInfo();
                    
                    for (var lineInfo : sourceLines) {
                        if (lineInfo.getLineClass().equals(TargetDataLine.class)) {
                            var device = createAudioDeviceFromMixer(mixerInfo, lineInfo, AudioDevice.AudioDeviceType.INPUT);
                            if (device.isDefault()) {
                                return device;
                            }
                        }
                    }
                }
                return null;
            } catch (Exception e) {
                logger.error("Failed to get default input device", e);
                return null;
            }
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<AudioDevice> getDefaultOutputDevice() {
        return Mono.fromCallable(() -> {
            try {
                var mixers = AudioSystem.getMixerInfo();
                for (var mixerInfo : mixers) {
                    var mixer = AudioSystem.getMixer(mixerInfo);
                    var sourceLines = mixer.getSourceLineInfo();
                    
                    for (var lineInfo : sourceLines) {
                        var device = createAudioDeviceFromMixer(mixerInfo, lineInfo, AudioDevice.AudioDeviceType.OUTPUT);
                        if (device.isDefault()) {
                            return device;
                        }
                    }
                }
                return null;
            } catch (Exception e) {
                logger.error("Failed to get default output device", e);
                return null;
            }
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<TargetDataLine> openInputDevice(String deviceId, AudioFormat format) {
        return Mono.fromCallable(() -> {
            try {
                var device = deviceCache.get(deviceId);
                if (device == null) {
                    throw new IllegalArgumentException("Device not found: " + deviceId);
                }
                
                // Find a compatible mixer for this device
                var mixers = AudioSystem.getMixerInfo();
                Mixer.Info selectedMixer = null;
                
                for (var mixerInfo : mixers) {
                    if (mixerInfo.getName().equals(device.name()) || 
                        mixerInfo.getDescription().contains(device.name())) {
                        selectedMixer = mixerInfo;
                        break;
                    }
                }
                
                if (selectedMixer == null) {
                    selectedMixer = AudioSystem.getMixer(null).getMixerInfo(); // Use default
                }
                
                var mixer = AudioSystem.getMixer(selectedMixer);
                var dataLineInfo = new javax.sound.sampled.DataLine.Info(
                    TargetDataLine.class, 
                    new AudioFormat[]{format}, 
                    format.getFrameSize() * 10, // 10 second buffer
                    (int)format.getFrameRate()
                );
                
                var line = (TargetDataLine) mixer.getLine(dataLineInfo);
                line.open(format);
                
                logger.info("Opened input device: {} with format: {}", deviceId, format);
                return line;
                
            } catch (Exception e) {
                logger.error("Failed to open input device: {}", deviceId, e);
                throw new RuntimeException("Failed to open input device", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<javax.sound.sampled.SourceDataLine> openOutputDevice(String deviceId, AudioFormat format) {
        return Mono.fromCallable(() -> {
            try {
                var device = deviceCache.get(deviceId);
                if (device == null) {
                    throw new IllegalArgumentException("Device not found: " + deviceId);
                }
                
                // Find a compatible mixer for this device
                var mixers = AudioSystem.getMixerInfo();
                Mixer.Info selectedMixer = null;
                
                for (var mixerInfo : mixers) {
                    if (mixerInfo.getName().equals(device.name()) || 
                        mixerInfo.getDescription().contains(device.name())) {
                        selectedMixer = mixerInfo;
                        break;
                    }
                }
                
                if (selectedMixer == null) {
                    selectedMixer = AudioSystem.getMixer(null).getMixerInfo(); // Use default
                }
                
                var mixer = AudioSystem.getMixer(selectedMixer);
                var dataLineInfo = new javax.sound.sampled.DataLine.Info(
                    javax.sound.sampled.SourceDataLine.class, 
                    new AudioFormat[]{format}, 
                    format.getFrameSize() * 10, // 10 second buffer
                    (int)format.getFrameRate()
                );
                
                var line = (javax.sound.sampled.SourceDataLine) mixer.getLine(dataLineInfo);
                line.open(format);
                
                logger.info("Opened output device: {} with format: {}", deviceId, format);
                return line;
                
            } catch (Exception e) {
                logger.error("Failed to open output device: {}", deviceId, e);
                throw new RuntimeException("Failed to open output device", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Flux<byte[]> captureSystemAudio(AudioFormat format) {
        return Mono.fromCallable(() -> {
            logger.info("Starting system audio capture on macOS");
            
            // Use Core Audio to capture system audio
            var audioUnit = new AudioUnit();
            var result = AUDIO_UNIT.AudioUnitInitialize(audioUnit, AudioUnitType.kAudioUnitType_Output, 
                                             AudioUnitSubType.kAudioUnitSubType_RemoteIO);
            
            if (result != 0) {
                throw new RuntimeException("Failed to initialize audio unit: " + result);
            }
            
            // Configure audio unit for system audio capture
            var streamFormat = createAudioStreamDescription(format);
            result = AUDIO_UNIT.AudioUnitSetProperty(audioUnit, 
                AudioUnitProperty.kAudioOutputUnitProperty_EnableIO, 
                AudioUnitScope.kAudioUnitScope_Output, 
                1, // Enable output
                streamFormat);
            
            if (result != 0) {
                throw new RuntimeException("Failed to configure audio unit: " + result);
            }
            
            return audioUnit;
        })
        .flatMapMany(audioUnit -> {
            // Create flux that captures audio data
            return Flux.create(emitter -> {
                // Start audio capture loop
                var captureThread = new Thread(() -> {
                    var buffer = new byte[4096];
                    var audioBuffer = new AudioBuffer();
                    
                    while (!emitter.isCancelled()) {
                        try {
                            // Capture audio data
                            var dataSize = new IntByReference();
                            var result = AUDIO_UNIT.AudioUnitRender(audioUnit, audioBuffer, dataSize);
                            
                            if (result == 0 && dataSize.getValue() > 0) {
                                var audioData = new byte[dataSize.getValue()];
                                System.arraycopy(audioBuffer.data, 0, audioData, 0, dataSize.getValue());
                                emitter.next(audioData);
                            }
                            
                            Thread.sleep(10); // Small delay to prevent CPU overload
                            
                        } catch (Exception e) {
                            if (!emitter.isCancelled()) {
                                emitter.error(e);
                            }
                            break;
                        }
                    }
                    
                    // Cleanup
                    AUDIO_UNIT.AudioUnitUninitialize(audioUnit);
                    emitter.complete();
                });
                
                captureThread.setDaemon(true);
                captureThread.start();
            });
        })
        .cast(byte[].class)
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Flux<byte[]> captureApplicationAudio(String applicationName, AudioFormat format) {
        return Mono.fromCallable(() -> {
            logger.info("Starting application audio capture for: {}", applicationName);
            
            // Use macOS accessibility APIs to capture audio from specific application
            var pid = getApplicationPid(applicationName);
            if (pid == -1) {
                throw new IllegalArgumentException("Application not found: " + applicationName);
            }
            
            return pid;
        })
        .flatMapMany(pid -> {
            // Capture audio from specific application process
            return captureSystemAudio(format)
                .filter(audioData -> isFromApplication(audioData, pid));
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<javax.sound.sampled.Mixer.Info> getCurrentMixer() {
        return Mono.fromCallable(() -> {
            if (currentMixerId == null) {
                return null;
            }
            
            var mixers = AudioSystem.getMixerInfo();
            for (var mixerInfo : mixers) {
                if (mixerInfo.getName().equals(currentMixerId)) {
                    return mixerInfo;
                }
            }
            return null;
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<Void> setMixer(javax.sound.sampled.Mixer.Info mixerInfo) {
        return Mono.fromRunnable(() -> {
            currentMixerId = mixerInfo.getName();
            logger.info("Audio mixer set to: {}", mixerInfo.getName());
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    @Override
    public Flux<AudioFormat> getSupportedFormats(String deviceId) {
        return Mono.fromCallable(() -> {
            var device = deviceCache.get(deviceId);
            if (device == null) {
                return new ArrayList<AudioFormat>();
            }
            
            // Find a compatible mixer for this device
            var mixers = AudioSystem.getMixerInfo();
            Mixer.Info selectedMixer = null;
            
            for (var mixerInfo : mixers) {
                if (mixerInfo.getName().equals(device.name()) || 
                    mixerInfo.getDescription().contains(device.name())) {
                    selectedMixer = mixerInfo;
                    break;
                }
            }
            
            if (selectedMixer == null) {
                selectedMixer = AudioSystem.getMixer(null).getMixerInfo(); // Use default
            }
            
            var mixer = AudioSystem.getMixer(selectedMixer);
            var sourceLines = mixer.getTargetLineInfo();
            var formats = new ArrayList<AudioFormat>();
            
            for (var lineInfo : sourceLines) {
                // Create default supported formats based on device capabilities
                var sampleRate = device.sampleRates() != null && device.sampleRates().length > 0 ? 
                    device.sampleRates()[0] : 44100.0f;
                var sampleSize = device.sampleSizes() != null && device.sampleSizes().length > 0 ? 
                    device.sampleSizes()[0] : 16;
                var channels = device.maxChannels();
                
                formats.add(new AudioFormat(sampleRate, sampleSize, channels, true, false));
                formats.add(new AudioFormat(sampleRate, sampleSize, channels, false, false));
            }
            
            return formats;
        })
        .flatMapMany(formats -> Flux.fromIterable(formats))
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<Boolean> isFormatSupported(String deviceId, AudioFormat format) {
        return getSupportedFormats(deviceId)
            .any(supportedFormat -> supportedFormat.matches(format));
    }
    
    @Override
    public Mono<Long> getDeviceLatency(String deviceId) {
        return Mono.fromCallable(() -> {
            var device = deviceCache.get(deviceId);
            if (device == null) {
                return 0L;
            }
            
            // Use Core Audio to get device latency - simplified for now
            // In a real implementation, this would use the device ID from Core Audio
            var latency = new FloatByReference();
            var result = CORE_AUDIO.AudioDeviceGetProperty(0, // Use default device ID for now
                AudioDeviceProperty.kAudioDevicePropertyLatency, latency);
            
            if (result == 0) {
                return (long) (latency.getValue() * 1000); // Convert to milliseconds
            }
            
            // Fallback to estimated latency
            return 50L; // 50ms default latency
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<Void> setBufferSize(String deviceId, int bufferSize) {
        return Mono.fromRunnable(() -> {
            logger.info("Setting buffer size for device {}: {}", deviceId, bufferSize);
            // Buffer size setting would be handled when opening the device
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    /**
     * Refreshes the device cache with current system devices.
     */
    private void refreshDeviceCache() {
        try {
            deviceCache.clear();
            
            var mixers = AudioSystem.getMixerInfo();
            for (var mixerInfo : mixers) {
                var mixer = AudioSystem.getMixer(mixerInfo);
                
                // Input devices
                var targetLines = mixer.getTargetLineInfo();
                for (var lineInfo : targetLines) {
                    var device = createAudioDeviceFromMixer(mixerInfo, lineInfo, AudioDevice.AudioDeviceType.INPUT);
                    deviceCache.put(device.id(), device);
                }
                
                // Output devices
                var sourceLines = mixer.getSourceLineInfo();
                for (var lineInfo : sourceLines) {
                    var device = createAudioDeviceFromMixer(mixerInfo, lineInfo, AudioDevice.AudioDeviceType.OUTPUT);
                    deviceCache.put(device.id(), device);
                }
            }
            
            logger.debug("Device cache refreshed with {} devices", deviceCache.size());
            
        } catch (Exception e) {
            logger.error("Failed to refresh device cache", e);
        }
    }
    
    /**
     * Creates an AudioDevice from mixer information.
     */
    private AudioDevice createAudioDeviceFromMixer(javax.sound.sampled.Mixer.Info mixerInfo, 
                                                   javax.sound.sampled.Line.Info lineInfo,
                                                   AudioDevice.AudioDeviceType type) {
        var deviceId = mixerInfo.getName() + "_" + lineInfo.toString();
        var isDefault = mixerInfo.getName().equals(AudioSystem.getMixer(null).getMixerInfo().getName());
        
        // Create default formats - lineInfo.getFormats() doesn't exist
        var sampleRates = new float[]{44100.0f, 48000.0f, 22050.0f, 16000.0f};
        var sampleSizes = new int[]{16, 8};
        
        return new AudioDevice(
            deviceId,
            lineInfo.toString(),
            mixerInfo.getDescription(),
            type,
            isDefault,
            true, // Assume available if we can get info
            2, // Default stereo
            sampleRates,
            sampleSizes
        );
    }
    
    /**
     * Creates an audio stream description for Core Audio.
     */
    private AudioStreamDescription createAudioStreamDescription(AudioFormat format) {
        var description = new AudioStreamDescription();
        description.mSampleRate = format.getSampleRate();
        description.mFormatID = AudioFormatID.kAudioFormatLinearPCM;
        description.mFormatFlags = AudioFormatFlags.kAudioFormatFlagIsFloat;
        description.mBytesPerPacket = format.getFrameSize();
        description.mFramesPerPacket = 1;
        description.mChannelsPerFrame = format.getChannels();
        description.mBitsPerChannel = format.getSampleSizeInBits();
        return description;
    }
    
    /**
     * Gets the process ID for an application name.
     */
    private int getApplicationPid(String applicationName) {
        try {
            var process = Runtime.getRuntime().exec(
                new String[]{"pgrep", "-f", "-n", "1", applicationName});
            var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            
            var pidLine = reader.readLine();
            reader.close();
            process.waitFor();
            
            if (pidLine != null && !pidLine.trim().isEmpty()) {
                return Integer.parseInt(pidLine.trim());
            }
            
        } catch (Exception e) {
            logger.debug("Failed to get PID for application: {}", applicationName, e);
        }
        
        return -1;
    }
    
    /**
     * Checks if audio data is from a specific application.
     */
    private boolean isFromApplication(byte[] audioData, int pid) {
        // In a real implementation, this would check the audio source
        // For now, we'll assume all system audio could be from any application
        return true;
    }
    
    // JNA interfaces for Core Audio
    public interface CoreAudioLib extends com.sun.jna.Library {
        int AudioHardwareInitialize();
        int AudioHardwareTerminate();
        int AudioDeviceGetProperty(int deviceID, int property, FloatByReference value);
        int AudioObjectGetPropertyAddress(int objectID, int property, PointerByReference address);
    }
    
    public interface AudioUnitLib extends com.sun.jna.Library {
        int AudioUnitInitialize(AudioUnit audioUnit, int type, int subType);
        int AudioUnitUninitialize(AudioUnit audioUnit);
        int AudioUnitSetProperty(AudioUnit audioUnit, int property, int scope, int element, AudioStreamDescription value);
        int AudioUnitRender(AudioUnit audioUnit, AudioBuffer buffer, IntByReference dataSize);
    }
    
    // JNA structures
    public static class AudioUnit extends Structure {
        public int component;
        public int node;
        public int scope;
        public int element;
    }
    
    public static class AudioBuffer extends Structure {
        public byte[] data = new byte[4096];
        public int size;
    }
    
    public static class AudioStreamDescription extends Structure {
        public double mSampleRate;
        public int mFormatID;
        public int mFormatFlags;
        public int mBytesPerPacket;
        public int mFramesPerPacket;
        public int mChannelsPerFrame;
        public int mBitsPerChannel;
        
        public Pointer getPointer() {
            return getPointer();
        }
    }
    
    public static class FloatByReference extends Structure implements Structure.ByReference {
        public float value;
        
        public float getValue() {
            return value;
        }
    }
    
    // Core Audio constants
    public static class AudioDeviceProperty {
        public static final int kAudioDevicePropertyLatency = 2004;
    }
    
    public static class AudioUnitType {
        public static final int kAudioUnitType_Output = 1635086177;
    }
    
    public static class AudioUnitSubType {
        public static final int kAudioUnitSubType_RemoteIO = 1635238512;
    }
    
    public static class AudioUnitProperty {
        public static final int kAudioOutputUnitProperty_EnableIO = 2006;
    }
    
    public static class AudioUnitScope {
        public static final int kAudioUnitScope_Output = 2;
    }
    
    public static class AudioFormatID {
        public static final int kAudioFormatLinearPCM = 1819304813;
    }
    
    public static class AudioFormatFlags {
        public static final int kAudioFormatFlagIsFloat = 1;
    }
}