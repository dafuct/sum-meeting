package com.zoomtranscriber.core.audio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.sound.sampled.AudioFormat;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AudioCaptureService interface.
 * Tests audio capture functionality, source management, and quality settings.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AudioCaptureService Tests")
class AudioCaptureServiceTest {
    
    @Mock
    private AudioCaptureService audioCaptureService;
    
    private String testSourceId;
    private AudioFormat testFormat;
    private AudioQuality testQuality;
    private AudioChunk testAudioChunk;
    private AudioSource testAudioSource;
    
    @BeforeEach
    void setUp() {
        testSourceId = "mic-001";
        testFormat = new AudioFormat(44100, 16, 2, true, false);
        testQuality = AudioQuality.high();
        testAudioChunk = new AudioChunk(
            new byte[]{1, 2, 3, 4, 5}, testFormat, Duration.ofMillis(100), 
            System.currentTimeMillis(), 0.8
        );
        testAudioSource = new AudioSource(
            testSourceId, "Test Microphone", "Primary microphone", 
            AudioSource.AudioSourceType.MICROPHONE, true, true
        );
    }
    
    @Test
    @DisplayName("Should start audio capture successfully")
    void shouldStartAudioCaptureSuccessfully() {
        // Given
        when(audioCaptureService.startCapture(eq(testSourceId), any(AudioFormat.class)))
            .thenReturn(Mono.empty());
        
        // When
        var result = audioCaptureService.startCapture(testSourceId, testFormat);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(audioCaptureService, times(1)).startCapture(testSourceId, testFormat);
    }
    
    @Test
    @DisplayName("Should handle audio capture start failure")
    void shouldHandleAudioCaptureStartFailure() {
        // Given
        var error = new RuntimeException("Failed to start audio capture");
        when(audioCaptureService.startCapture(eq(testSourceId), any(AudioFormat.class)))
            .thenReturn(Mono.error(error));
        
        // When
        var result = audioCaptureService.startCapture(testSourceId, testFormat);
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Failed to start audio capture"))
            .verify();
        
        verify(audioCaptureService, times(1)).startCapture(testSourceId, testFormat);
    }
    
    @Test
    @DisplayName("Should stop audio capture successfully")
    void shouldStopAudioCaptureSuccessfully() {
        // Given
        when(audioCaptureService.stopCapture()).thenReturn(Mono.empty());
        
        // When
        var result = audioCaptureService.stopCapture();
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(audioCaptureService, times(1)).stopCapture();
    }
    
    @Test
    @DisplayName("Should handle audio capture stop failure")
    void shouldHandleAudioCaptureStopFailure() {
        // Given
        var error = new RuntimeException("Failed to stop audio capture");
        when(audioCaptureService.stopCapture()).thenReturn(Mono.error(error));
        
        // When
        var result = audioCaptureService.stopCapture();
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Failed to stop audio capture"))
            .verify();
        
        verify(audioCaptureService, times(1)).stopCapture();
    }
    
    @Test
    @DisplayName("Should return capture status correctly when capturing")
    void shouldReturnCaptureStatusCorrectlyWhenCapturing() {
        // Given
        when(audioCaptureService.isCapturing()).thenReturn(true);
        
        // When
        var result = audioCaptureService.isCapturing();
        
        // Then
        assertTrue(result);
        verify(audioCaptureService, times(1)).isCapturing();
    }
    
    @Test
    @DisplayName("Should return capture status correctly when not capturing")
    void shouldReturnCaptureStatusCorrectlyWhenNotCapturing() {
        // Given
        when(audioCaptureService.isCapturing()).thenReturn(false);
        
        // When
        var result = audioCaptureService.isCapturing();
        
        // Then
        assertFalse(result);
        verify(audioCaptureService, times(1)).isCapturing();
    }
    
    @Test
    @DisplayName("Should get audio stream successfully")
    void shouldGetAudioStreamSuccessfully() {
        // Given
        var expectedChunks = List.of(testAudioChunk);
        when(audioCaptureService.getAudioStream()).thenReturn(Flux.fromIterable(expectedChunks));
        
        // When
        var result = audioCaptureService.getAudioStream();
        
        // Then
        StepVerifier.create(result)
            .expectNext(testAudioChunk)
            .verifyComplete();
        
        verify(audioCaptureService, times(1)).getAudioStream();
    }
    
    @Test
    @DisplayName("Should handle empty audio stream")
    void shouldHandleEmptyAudioStream() {
        // Given
        when(audioCaptureService.getAudioStream()).thenReturn(Flux.empty());
        
        // When
        var result = audioCaptureService.getAudioStream();
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(audioCaptureService, times(1)).getAudioStream();
    }
    
    @Test
    @DisplayName("Should get current format correctly")
    void shouldGetCurrentFormatCorrectly() {
        // Given
        when(audioCaptureService.getCurrentFormat()).thenReturn(testFormat);
        
        // When
        var result = audioCaptureService.getCurrentFormat();
        
        // Then
        assertEquals(testFormat, result);
        verify(audioCaptureService, times(1)).getCurrentFormat();
    }
    
    @Test
    @DisplayName("Should return null current format when not capturing")
    void shouldReturnNullCurrentFormatWhenNotCapturing() {
        // Given
        when(audioCaptureService.getCurrentFormat()).thenReturn(null);
        
        // When
        var result = audioCaptureService.getCurrentFormat();
        
        // Then
        assertNull(result);
        verify(audioCaptureService, times(1)).getCurrentFormat();
    }
    
    @Test
    @DisplayName("Should get available audio sources successfully")
    void shouldGetAvailableAudioSourcesSuccessfully() {
        // Given
        var expectedSources = List.of(testAudioSource);
        when(audioCaptureService.getAvailableSources()).thenReturn(Flux.fromIterable(expectedSources));
        
        // When
        var result = audioCaptureService.getAvailableSources();
        
        // Then
        StepVerifier.create(result)
            .expectNext(testAudioSource)
            .verifyComplete();
        
        verify(audioCaptureService, times(1)).getAvailableSources();
    }
    
    @Test
    @DisplayName("Should handle empty available audio sources")
    void shouldHandleEmptyAvailableAudioSources() {
        // Given
        when(audioCaptureService.getAvailableSources()).thenReturn(Flux.empty());
        
        // When
        var result = audioCaptureService.getAvailableSources();
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(audioCaptureService, times(1)).getAvailableSources();
    }
    
    @Test
    @DisplayName("Should set audio source successfully")
    void shouldSetAudioSourceSuccessfully() {
        // Given
        when(audioCaptureService.setAudioSource(testSourceId)).thenReturn(Mono.empty());
        
        // When
        var result = audioCaptureService.setAudioSource(testSourceId);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(audioCaptureService, times(1)).setAudioSource(testSourceId);
    }
    
    @Test
    @DisplayName("Should handle audio source setting failure")
    void shouldHandleAudioSourceSettingFailure() {
        // Given
        var error = new RuntimeException("Failed to set audio source");
        when(audioCaptureService.setAudioSource(testSourceId)).thenReturn(Mono.error(error));
        
        // When
        var result = audioCaptureService.setAudioSource(testSourceId);
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Failed to set audio source"))
            .verify();
        
        verify(audioCaptureService, times(1)).setAudioSource(testSourceId);
    }
    
    @Test
    @DisplayName("Should get current audio source correctly")
    void shouldGetCurrentAudioSourceCorrectly() {
        // Given
        when(audioCaptureService.getCurrentSource()).thenReturn(testAudioSource);
        
        // When
        var result = audioCaptureService.getCurrentSource();
        
        // Then
        assertEquals(testAudioSource, result);
        verify(audioCaptureService, times(1)).getCurrentSource();
    }
    
    @Test
    @DisplayName("Should return null current audio source when none selected")
    void shouldReturnNullCurrentAudioSourceWhenNoneSelected() {
        // Given
        when(audioCaptureService.getCurrentSource()).thenReturn(null);
        
        // When
        var result = audioCaptureService.getCurrentSource();
        
        // Then
        assertNull(result);
        verify(audioCaptureService, times(1)).getCurrentSource();
    }
    
    @Test
    @DisplayName("Should set audio quality successfully")
    void shouldSetAudioQualitySuccessfully() {
        // Given
        when(audioCaptureService.setQuality(any(AudioQuality.class))).thenReturn(Mono.empty());
        
        // When
        var result = audioCaptureService.setQuality(testQuality);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(audioCaptureService, times(1)).setQuality(testQuality);
    }
    
    @Test
    @DisplayName("Should handle audio quality setting failure")
    void shouldHandleAudioQualitySettingFailure() {
        // Given
        var error = new RuntimeException("Failed to set audio quality");
        when(audioCaptureService.setQuality(any(AudioQuality.class))).thenReturn(Mono.error(error));
        
        // When
        var result = audioCaptureService.setQuality(testQuality);
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Failed to set audio quality"))
            .verify();
        
        verify(audioCaptureService, times(1)).setQuality(testQuality);
    }
    
    @Test
    @DisplayName("Should get current audio quality correctly")
    void shouldGetCurrentAudioQualityCorrectly() {
        // Given
        when(audioCaptureService.getCurrentQuality()).thenReturn(testQuality);
        
        // When
        var result = audioCaptureService.getCurrentQuality();
        
        // Then
        assertEquals(testQuality, result);
        verify(audioCaptureService, times(1)).getCurrentQuality();
    }
    
    @Test
    @DisplayName("Should enable noise reduction successfully")
    void shouldEnableNoiseReductionSuccessfully() {
        // Given
        when(audioCaptureService.setNoiseReduction(true)).thenReturn(Mono.empty());
        
        // When
        var result = audioCaptureService.setNoiseReduction(true);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(audioCaptureService, times(1)).setNoiseReduction(true);
    }
    
    @Test
    @DisplayName("Should disable noise reduction successfully")
    void shouldDisableNoiseReductionSuccessfully() {
        // Given
        when(audioCaptureService.setNoiseReduction(false)).thenReturn(Mono.empty());
        
        // When
        var result = audioCaptureService.setNoiseReduction(false);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(audioCaptureService, times(1)).setNoiseReduction(false);
    }
    
    @Test
    @DisplayName("Should handle noise reduction setting failure")
    void shouldHandleNoiseReductionSettingFailure() {
        // Given
        var error = new RuntimeException("Failed to set noise reduction");
        when(audioCaptureService.setNoiseReduction(anyBoolean())).thenReturn(Mono.error(error));
        
        // When
        var result = audioCaptureService.setNoiseReduction(true);
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Failed to set noise reduction"))
            .verify();
        
        verify(audioCaptureService, times(1)).setNoiseReduction(true);
    }
    
    @Test
    @DisplayName("Should return noise reduction status correctly when enabled")
    void shouldReturnNoiseReductionStatusCorrectlyWhenEnabled() {
        // Given
        when(audioCaptureService.isNoiseReductionEnabled()).thenReturn(true);
        
        // When
        var result = audioCaptureService.isNoiseReductionEnabled();
        
        // Then
        assertTrue(result);
        verify(audioCaptureService, times(1)).isNoiseReductionEnabled();
    }
    
    @Test
    @DisplayName("Should return noise reduction status correctly when disabled")
    void shouldReturnNoiseReductionStatusCorrectlyWhenDisabled() {
        // Given
        when(audioCaptureService.isNoiseReductionEnabled()).thenReturn(false);
        
        // When
        var result = audioCaptureService.isNoiseReductionEnabled();
        
        // Then
        assertFalse(result);
        verify(audioCaptureService, times(1)).isNoiseReductionEnabled();
    }
    
    @Test
    @DisplayName("Should create audio chunk record correctly")
    void shouldCreateAudioChunkRecordCorrectly() {
        // When
        var chunk = new AudioChunk(
            testAudioChunk.data(), testAudioChunk.format(), 
            testAudioChunk.duration(), testAudioChunk.timestamp(), 
            testAudioChunk.volumeLevel()
        );
        
        // Then
        assertArrayEquals(testAudioChunk.data(), chunk.data());
        assertEquals(testAudioChunk.format(), chunk.format());
        assertEquals(testAudioChunk.duration(), chunk.duration());
        assertEquals(testAudioChunk.timestamp(), chunk.timestamp());
        assertEquals(testAudioChunk.volumeLevel(), chunk.volumeLevel());
    }
    
    @Test
    @DisplayName("Should get audio chunk size correctly")
    void shouldGetAudioChunkSizeCorrectly() {
        // When
        var size = testAudioChunk.getSize();
        
        // Then
        assertEquals(5, size); // 5 bytes in test data
    }
    
    @Test
    @DisplayName("Should handle audio chunk with null data")
    void shouldHandleAudioChunkWithNullData() {
        // Given
        var chunk = new AudioChunk(null, testFormat, Duration.ofMillis(100), 
                                  System.currentTimeMillis(), 0.0);
        
        // When
        var size = chunk.getSize();
        
        // Then
        assertEquals(0, size);
    }
    
    @Test
    @DisplayName("Should get audio chunk sample rate correctly")
    void shouldGetAudioChunkSampleRateCorrectly() {
        // When
        var sampleRate = testAudioChunk.getSampleRate();
        
        // Then
        assertEquals(44100.0f, sampleRate);
    }
    
    @Test
    @DisplayName("Should handle audio chunk with null format")
    void shouldHandleAudioChunkWithNullFormat() {
        // Given
        var chunk = new AudioChunk(new byte[]{1, 2, 3}, null, Duration.ofMillis(100), 
                                  System.currentTimeMillis(), 0.5);
        
        // When
        var sampleRate = chunk.getSampleRate();
        
        // Then
        assertEquals(0.0f, sampleRate);
    }
    
    @Test
    @DisplayName("Should get audio chunk channels correctly")
    void shouldGetAudioChunkChannelsCorrectly() {
        // When
        var channels = testAudioChunk.getChannels();
        
        // Then
        assertEquals(2, channels);
    }
    
    @Test
    @DisplayName("Should create audio source record correctly")
    void shouldCreateAudioSourceRecordCorrectly() {
        // When
        var source = new AudioSource(
            testAudioSource.id(), testAudioSource.name(), testAudioSource.description(),
            testAudioSource.type(), testAudioSource.isDefault(), testAudioSource.isAvailable()
        );
        
        // Then
        assertEquals(testAudioSource.id(), source.id());
        assertEquals(testAudioSource.name(), source.name());
        assertEquals(testAudioSource.description(), source.description());
        assertEquals(testAudioSource.type(), source.type());
        assertEquals(testAudioSource.isDefault(), source.isDefault());
        assertEquals(testAudioSource.isAvailable(), source.isAvailable());
    }
    
    @ParameterizedTest
    @EnumSource(AudioSource.AudioSourceType.class)
    @DisplayName("Should handle all audio source types")
    void shouldHandleAllAudioSourceTypes(AudioSource.AudioSourceType type) {
        // When
        var source = new AudioSource(
            "test-id", "Test Source", "Test description",
            type, false, true
        );
        
        // Then
        assertEquals(type, source.type());
    }
    
    @Test
    @DisplayName("Should create audio quality record correctly")
    void shouldCreateAudioQualityRecordCorrectly() {
        // When
        var quality = new AudioQuality(
            testQuality.sampleRate(), testQuality.sampleSizeInBits(), testQuality.channels(),
            testQuality.frameRate(), testQuality.bigEndian(), testQuality.bufferSize()
        );
        
        // Then
        assertEquals(testQuality.sampleRate(), quality.sampleRate());
        assertEquals(testQuality.sampleSizeInBits(), quality.sampleSizeInBits());
        assertEquals(testQuality.channels(), quality.channels());
        assertEquals(testQuality.frameRate(), quality.frameRate());
        assertEquals(testQuality.bigEndian(), quality.bigEndian());
        assertEquals(testQuality.bufferSize(), quality.bufferSize());
    }
    
    @Test
    @DisplayName("Should create high quality audio configuration")
    void shouldCreateHighQualityAudioConfiguration() {
        // When
        var quality = AudioQuality.high();
        
        // Then
        assertEquals(44100, quality.sampleRate());
        assertEquals(16, quality.sampleSizeInBits());
        assertEquals(2, quality.channels());
        assertEquals(44100.0f, quality.frameRate());
        assertFalse(quality.bigEndian());
        assertEquals(4096, quality.bufferSize());
    }
    
    @Test
    @DisplayName("Should create medium quality audio configuration")
    void shouldCreateMediumQualityAudioConfiguration() {
        // When
        var quality = AudioQuality.medium();
        
        // Then
        assertEquals(22050, quality.sampleRate());
        assertEquals(16, quality.sampleSizeInBits());
        assertEquals(1, quality.channels());
        assertEquals(22050.0f, quality.frameRate());
        assertFalse(quality.bigEndian());
        assertEquals(2048, quality.bufferSize());
    }
    
    @Test
    @DisplayName("Should create low quality audio configuration")
    void shouldCreateLowQualityAudioConfiguration() {
        // When
        var quality = AudioQuality.low();
        
        // Then
        assertEquals(16000, quality.sampleRate());
        assertEquals(16, quality.sampleSizeInBits());
        assertEquals(1, quality.channels());
        assertEquals(16000.0f, quality.frameRate());
        assertFalse(quality.bigEndian());
        assertEquals(1024, quality.bufferSize());
    }
    
    @Test
    @DisplayName("Should convert audio quality to audio format")
    void shouldConvertAudioQualityToAudioFormat() {
        // When
        var format = testQuality.toAudioFormat();
        
        // Then
        assertEquals(AudioFormat.Encoding.PCM_SIGNED, format.getEncoding());
        assertEquals(testQuality.sampleRate(), format.getSampleRate());
        assertEquals(testQuality.sampleSizeInBits(), format.getSampleSizeInBits());
        assertEquals(testQuality.channels(), format.getChannels());
        assertTrue(format.isBigEndian() == testQuality.bigEndian());
    }
    
    @Test
    @DisplayName("Should create default capture configuration")
    void shouldCreateDefaultCaptureConfiguration() {
        // When
        var config = AudioCaptureService.CaptureConfig.defaultConfig();
        
        // Then
        assertNull(config.sourceId());
        assertEquals(AudioQuality.medium(), config.quality());
        assertTrue(config.noiseReduction());
        assertEquals(Duration.ofMillis(100), config.chunkDuration());
        assertTrue(config.autoGainControl());
    }
    
    @Test
    @DisplayName("Should create capture configuration with custom parameters")
    void shouldCreateCaptureConfigurationWithCustomParameters() {
        // Given
        var customQuality = AudioQuality.high();
        
        // When
        var config = new AudioCaptureService.CaptureConfig(
            testSourceId, customQuality, false, Duration.ofMillis(200), false
        );
        
        // Then
        assertEquals(testSourceId, config.sourceId());
        assertEquals(customQuality, config.quality());
        assertFalse(config.noiseReduction());
        assertEquals(Duration.ofMillis(200), config.chunkDuration());
        assertFalse(config.autoGainControl());
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.5, 0.8, 1.0})
    @DisplayName("Should handle various volume levels")
    void shouldHandleVariousVolumeLevels(double volumeLevel) {
        // When
        var chunk = new AudioChunk(
            new byte[]{1, 2, 3}, testFormat, Duration.ofMillis(100),
            System.currentTimeMillis(), volumeLevel
        );
        
        // Then
        assertEquals(volumeLevel, chunk.volumeLevel());
    }
    
    @Test
    @DisplayName("Should handle audio chunk with zero volume")
    void shouldHandleAudioChunkWithZeroVolume() {
        // When
        var chunk = new AudioChunk(
            new byte[]{1, 2, 3}, testFormat, Duration.ofMillis(100),
            System.currentTimeMillis(), 0.0
        );
        
        // Then
        assertEquals(0.0, chunk.volumeLevel());
    }
    
    @Test
    @DisplayName("Should handle audio chunk with maximum volume")
    void shouldHandleAudioChunkWithMaximumVolume() {
        // When
        var chunk = new AudioChunk(
            new byte[]{1, 2, 3}, testFormat, Duration.ofMillis(100),
            System.currentTimeMillis(), 1.0
        );
        
        // Then
        assertEquals(1.0, chunk.volumeLevel());
    }
    
    @Test
    @DisplayName("Should handle audio chunk with negative volume")
    void shouldHandleAudioChunkWithNegativeVolume() {
        // When
        var chunk = new AudioChunk(
            new byte[]{1, 2, 3}, testFormat, Duration.ofMillis(100),
            System.currentTimeMillis(), -0.5
        );
        
        // Then
        assertEquals(-0.5, chunk.volumeLevel());
    }
    
    @ParameterizedTest
    @ValueSource(ints = {0, 50, 100, 1000})
    @DisplayName("Should handle various buffer sizes")
    void shouldHandleVariousBufferSizes(int bufferSize) {
        // When
        var quality = new AudioQuality(
            44100, 16, 2, 44100.0f, false, bufferSize
        );
        
        // Then
        assertEquals(bufferSize, quality.bufferSize());
    }
    
    @Test
    @DisplayName("Should handle audio source with null description")
    void shouldHandleAudioSourceWithNullDescription() {
        // When
        var source = new AudioSource(
            testSourceId, "Test Source", null,
            AudioSource.AudioSourceType.MICROPHONE, false, true
        );
        
        // Then
        assertNull(source.description());
    }
    
    @Test
    @DisplayName("Should handle audio source with empty name")
    void shouldHandleAudioSourceWithEmptyName() {
        // When
        var source = new AudioSource(
            testSourceId, "", "Test description",
            AudioSource.AudioSourceType.MICROPHONE, false, true
        );
        
        // Then
        assertEquals("", source.name());
    }
    
    @Test
    @DisplayName("Should handle audio chunk with very large data")
    void shouldHandleAudioChunkWithVeryLargeData() {
        // Given
        var largeData = new byte[1024 * 1024]; // 1MB of data
        
        // When
        var chunk = new AudioChunk(
            largeData, testFormat, Duration.ofSeconds(1),
            System.currentTimeMillis(), 0.8
        );
        
        // Then
        assertEquals(largeData.length, chunk.getSize());
    }
    
    @Test
    @DisplayName("Should handle audio chunk with zero duration")
    void shouldHandleAudioChunkWithZeroDuration() {
        // When
        var chunk = new AudioChunk(
            new byte[]{1, 2, 3}, testFormat, Duration.ZERO,
            System.currentTimeMillis(), 0.5
        );
        
        // Then
        assertEquals(Duration.ZERO, chunk.duration());
    }
    
    @Test
    @DisplayName("Should handle audio chunk with negative timestamp")
    void shouldHandleAudioChunkWithNegativeTimestamp() {
        // When
        var chunk = new AudioChunk(
            new byte[]{1, 2, 3}, testFormat, Duration.ofMillis(100),
            -1000, 0.5
        );
        
        // Then
        assertEquals(-1000, chunk.timestamp());
    }
}