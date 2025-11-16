package com.zoomtranscriber.platform.linux;

import com.zoomtranscriber.core.audio.AudioCaptureService;
import com.zoomtranscriber.core.audio.PlatformAudioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Linux-specific implementation of audio services.
 * Uses ALSA, PulseAudio, and Java Sound API for audio operations.
 */
@Component
public class LinuxAudioService implements PlatformAudioService {
    
    private static final Logger logger = LoggerFactory.getLogger(LinuxAudioService.class);
    
    // Paths for Linux audio subsystems
    private static final Path PROC_ASOUND_PATH = Paths.get("/proc/asound");
    private static final Path PULSE_AUDIO_PATH = Paths.get("/run/user/1000/pulse");
    private static final Path ALSA_DEVICES_PATH = Paths.get("/proc/asound/cards");
    
    private final ConcurrentHashMap<String, AudioDevice> deviceCache = new ConcurrentHashMap<>();
    private boolean initialized = false;
    private String currentMixerId;
    private boolean pulseAudioAvailable = false;
    private boolean alsaAvailable = false;
    
    @Override
    public Mono<Void> initialize() {
        return Mono.fromRunnable(() -> {
            logger.info("Initializing Linux audio service");
            
            try {
                // Check available audio subsystems
                pulseAudioAvailable = Files.exists(PULSE_AUDIO_PATH);
                alsaAvailable = Files.exists(ALSA_DEVICES_PATH);
                
                if (!pulseAudioAvailable && !alsaAvailable) {
                    throw new RuntimeException("No audio subsystem found (PulseAudio or ALSA)");
                }
                
                logger.info("Audio subsystems - PulseAudio: {}, ALSA: {}", 
                    pulseAudioAvailable, alsaAvailable);
                
                // Cache available devices
                refreshDeviceCache();
                
                initialized = true;
                logger.info("Linux audio service initialized successfully");
                
            } catch (Exception e) {
                logger.error("Failed to initialize Linux audio service", e);
                throw new RuntimeException("Audio service initialization failed", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    @Override
    public Mono<Void> shutdown() {
        return Mono.fromRunnable(() -> {
            logger.info("Shutting down Linux audio service");
            
            try {
                deviceCache.clear();
                initialized = false;
                logger.info("Linux audio service shut down successfully");
                
            } catch (Exception e) {
                logger.error("Failed to shutdown Linux audio service", e);
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
    public Mono<SourceDataLine> openOutputDevice(String deviceId, AudioFormat format) {
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
                    SourceDataLine.class, 
                    new AudioFormat[]{format}, 
                    format.getFrameSize() * 10, // 10 second buffer
                    (int)format.getFrameRate()
                );
                
                var line = (SourceDataLine) mixer.getLine(dataLineInfo);
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
            logger.info("Starting system audio capture on Linux");
            
            if (pulseAudioAvailable) {
                return capturePulseAudioSystemAudio(format);
            } else if (alsaAvailable) {
                return captureAlsaSystemAudio(format);
            } else {
                throw new RuntimeException("No audio subsystem available for system capture");
            }
        })
        .flatMapMany(captureFunction -> captureFunction)
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Flux<byte[]> captureApplicationAudio(String applicationName, AudioFormat format) {
        return Mono.fromCallable(() -> {
            logger.info("Starting application audio capture for: {}", applicationName);
            
            var pid = getApplicationPid(applicationName);
            if (pid == -1) {
                throw new IllegalArgumentException("Application not found: " + applicationName);
            }
            
            return pid;
        })
        .flatMapMany(pid -> {
            // Use PulseAudio or ALSA to capture from specific application
            if (pulseAudioAvailable) {
                return capturePulseAudioApplication(pid, format);
            } else if (alsaAvailable) {
                return captureAlsaApplicationAudio(pid, format);
            } else {
                return Flux.empty();
            }
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
            
            // Create supported formats based on device capabilities
            var formats = new ArrayList<AudioFormat>();
            var sampleRates = device.sampleRates() != null ? device.sampleRates() : new float[]{44100.0f, 48000.0f};
            var sampleSizes = device.sampleSizes() != null ? device.sampleSizes() : new int[]{16, 8};
            var channels = device.maxChannels();
            
            for (var sampleRate : sampleRates) {
                for (var sampleSize : sampleSizes) {
                    formats.add(new AudioFormat(sampleRate, sampleSize, channels, true, false));
                    formats.add(new AudioFormat(sampleRate, sampleSize, channels, false, false));
                }
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
            var deviceInfo = deviceCache.get(deviceId);
            if (deviceInfo == null) {
                return 0L;
            }
            
            if (pulseAudioAvailable) {
                return getPulseAudioLatency(deviceId);
            } else if (alsaAvailable) {
                return getAlsaLatency(deviceId);
            }
            
            return 0L;
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
            
            // Add PulseAudio devices if available
            if (pulseAudioAvailable) {
                addPulseAudioDevices();
            }
            
            // Add ALSA devices if available
            if (alsaAvailable) {
                addAlsaDevices();
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
     * Captures system audio using PulseAudio.
     */
    private Flux<byte[]> capturePulseAudioSystemAudio(AudioFormat format) {
        return Flux.create(emitter -> {
            var captureThread = new Thread(() -> {
                try {
                    // Use pactl or parec to capture system audio
                    var process = Runtime.getRuntime().exec(new String[]{
                        "parec", 
                        "--format=s16le", 
                        "--rate=" + (int)format.getSampleRate(),
                        "--channels=" + format.getChannels(),
                        "--raw"
                    });
                    
                    var inputStream = process.getInputStream();
                    var buffer = new byte[4096];
                    
                    while (!emitter.isCancelled()) {
                        var bytesRead = inputStream.read(buffer);
                        if (bytesRead > 0) {
                            var audioData = new byte[bytesRead];
                            System.arraycopy(buffer, 0, audioData, 0, bytesRead);
                            emitter.next(audioData);
                        }
                    }
                    
                } catch (Exception e) {
                    if (!emitter.isCancelled()) {
                        emitter.error(e);
                    }
                } finally {
                    emitter.complete();
                }
            });
            
            captureThread.setDaemon(true);
            captureThread.start();
        });
    }
    
    /**
     * Captures system audio using ALSA.
     */
    private Flux<byte[]> captureAlsaSystemAudio(AudioFormat format) {
        return Flux.create(emitter -> {
            var captureThread = new Thread(() -> {
                try {
                    // Use arecord to capture system audio
                    var process = Runtime.getRuntime().exec(new String[]{
                        "arecord",
                        "-f", "S16_LE",
                        "-r", String.valueOf((int)format.getSampleRate()),
                        "-c", String.valueOf(format.getChannels()),
                        "-t", "raw"
                    });
                    
                    var inputStream = process.getInputStream();
                    var buffer = new byte[4096];
                    
                    while (!emitter.isCancelled()) {
                        var bytesRead = inputStream.read(buffer);
                        if (bytesRead > 0) {
                            var audioData = new byte[bytesRead];
                            System.arraycopy(buffer, 0, audioData, 0, bytesRead);
                            emitter.next(audioData);
                        }
                    }
                    
                } catch (Exception e) {
                    if (!emitter.isCancelled()) {
                        emitter.error(e);
                    }
                } finally {
                    emitter.complete();
                }
            });
            
            captureThread.setDaemon(true);
            captureThread.start();
        });
    }
    
    /**
     * Captures audio from specific application using PulseAudio.
     */
    private Flux<byte[]> capturePulseAudioApplication(int pid, AudioFormat format) {
        return Flux.create(emitter -> {
            var captureThread = new Thread(() -> {
                try {
                    // Use pactl to monitor application audio
                    var process = Runtime.getRuntime().exec(new String[]{
                        "pactl",
                        "monitor",
                        String.valueOf(pid)
                    });
                    
                    var reader = new BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
                    String line;
                    
                    while (!emitter.isCancelled() && (line = reader.readLine()) != null) {
                        // Parse pactl output for audio data
                        if (line.contains("sink-input")) {
                            // Extract audio data from pactl output
                            var audioData = parsePactlAudioData(line);
                            if (audioData != null) {
                                emitter.next(audioData);
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    if (!emitter.isCancelled()) {
                        emitter.error(e);
                    }
                } finally {
                    emitter.complete();
                }
            });
            
            captureThread.setDaemon(true);
            captureThread.start();
        });
    }
    
    /**
     * Captures audio from specific application using ALSA.
     */
    private Flux<byte[]> captureAlsaApplicationAudio(int pid, AudioFormat format) {
        // ALSA application-specific capture is complex and would require
        // additional setup. For now, fall back to system capture.
        return captureAlsaSystemAudio(format);
    }
    
    /**
     * Adds PulseAudio devices to the cache.
     */
    private void addPulseAudioDevices() {
        try {
            var process = Runtime.getRuntime().exec("pactl list sinks");
            var reader = new BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            
            String line;
            var sinkIndex = 0;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Sink Input")) {
                    var name = extractDeviceName(line);
                    var deviceId = "pulse_" + sinkIndex;
                    
                    var device = new AudioDevice(
                        deviceId,
                        name,
                        "PulseAudio Sink Input",
                        AudioDevice.AudioDeviceType.INPUT,
                        sinkIndex == 0, // First sink is default
                        true,
                        2,
                        new float[]{44100, 48000},
                        new int[]{16}
                    );
                    
                    deviceCache.put(deviceId, device);
                    sinkIndex++;
                }
            }
            reader.close();
            
        } catch (Exception e) {
            logger.debug("Failed to add PulseAudio devices", e);
        }
    }
    
    /**
     * Adds ALSA devices to the cache.
     */
    private void addAlsaDevices() {
        try {
            var cardsPath = Paths.get("/proc/asound/cards");
            if (!Files.exists(cardsPath)) {
                return;
            }
            
            try (var cards = Files.list(cardsPath)) {
                var cardList = cards.toList();
                for (var card : cardList) {
                var deviceId = "alsa_" + card.getFileName();
                var name = readAlsaCardName(card.getFileName().toString());
                
                var device = new AudioDevice(
                    deviceId,
                    name,
                    "ALSA Card",
                    AudioDevice.AudioDeviceType.INPUT,
                    deviceCache.isEmpty(), // First device is default
                    true,
                    2,
                    new float[]{44100, 48000},
                    new int[]{16}
                );
                
                deviceCache.put(deviceId, device);
            }
            }
            
        } catch (Exception e) {
            logger.debug("Failed to add ALSA devices", e);
        }
    }
    
    /**
     * Reads ALSA card name.
     */
    private String readAlsaCardName(String card) {
        try {
            var cardPath = Paths.get("/proc/asound/cards/" + card + "/id");
            if (Files.exists(cardPath)) {
                var content = Files.readString(cardPath);
                return content.split("\n")[0].trim();
            }
        } catch (Exception e) {
            logger.debug("Failed to read ALSA card name for {}", card, e);
        }
        return "ALSA Card " + card;
    }
    
    /**
     * Gets the process ID for an application name.
     */
    private int getApplicationPid(String applicationName) {
        try {
            var process = Runtime.getRuntime().exec(
                new String[]{"pgrep", "-f", "-n", "1", applicationName});
            var reader = new BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            
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
     * Parses audio data from pactl output.
     */
    private byte[] parsePactlAudioData(String pactlLine) {
        // This is a simplified parser - in a real implementation,
        // this would properly parse the binary pactl output
        var matcher = Pattern.compile("audio data: ([0-9a-fA-F]+)").matcher(pactlLine);
        if (matcher.find()) {
            var hexData = matcher.group(1);
            return parseHexString(hexData);
        }
        return null;
    }
    
    /**
     * Parses hex string to byte array.
     */
    private byte[] parseHexString(String hex) {
        try {
            var bytes = new byte[hex.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                var hexByte = hex.substring(i * 2, (i + 1) * 2);
                bytes[i] = (byte) Integer.parseInt(hexByte, 16);
            }
            return bytes;
        } catch (Exception e) {
            logger.debug("Failed to parse hex string: {}", hex, e);
            return new byte[0];
        }
    }
    
    /**
     * Extracts device name from pactl output.
     */
    private String extractDeviceName(String line) {
        var matcher = Pattern.compile("Description: (.+)").matcher(line);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "Unknown Device";
    }
    
    /**
     * Gets PulseAudio device latency.
     */
    private long getPulseAudioLatency(String deviceId) {
        try {
            var process = Runtime.getRuntime().exec(
                new String[]{"pactl", "get-sink-volume", deviceId});
            var reader = new BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Latency:")) {
                    var matcher = Pattern.compile("Latency: ([0-9.]+)").matcher(line);
                    if (matcher.find()) {
                        var latencyMs = Double.parseDouble(matcher.group(1));
                        return (long) latencyMs;
                    }
                }
            }
            reader.close();
            
        } catch (Exception e) {
            logger.debug("Failed to get PulseAudio latency for {}", deviceId, e);
        }
        
        return 0L;
    }
    
    /**
     * Gets ALSA device latency.
     */
    private long getAlsaLatency(String deviceId) {
        try {
            // ALSA latency information is typically in /proc/asound/cardX/pcm0p/substream0/latency
            var cardNum = deviceId.replace("alsa_", "");
            var latencyPath = Paths.get("/proc/asound/card" + cardNum + "/pcm0p/substream0/latency");
            
            if (Files.exists(latencyPath)) {
                var latencyStr = Files.readString(latencyPath).trim();
                return Long.parseLong(latencyStr);
            }
            
        } catch (Exception e) {
            logger.debug("Failed to get ALSA latency for {}", deviceId, e);
        }
        
        return 0L;
    }
}