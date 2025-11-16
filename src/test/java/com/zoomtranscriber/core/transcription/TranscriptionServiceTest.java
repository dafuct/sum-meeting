package com.zoomtranscriber.core.transcription;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TranscriptionService interface.
 * Tests transcription functionality, language settings, and export features.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TranscriptionService Tests")
class TranscriptionServiceTest {
    
    @Mock
    private TranscriptionService transcriptionService;
    
    private UUID testMeetingId;
    private TranscriptionConfig testConfig;
    private TranscriptionSegment testSegment;
    private TranscriptionStats testStats;
    
    @BeforeEach
    void setUp() {
        testMeetingId = UUID.randomUUID();
        testConfig = TranscriptionConfig.defaultConfig();
        testSegment = new TranscriptionSegment(
            UUID.randomUUID(), testMeetingId, 0, 5000, 
            "Hello world", 0.95, 0, LocalDateTime.now()
        );
        testStats = new TranscriptionStats(
            testMeetingId, 10, 50, 0.88,
            LocalDateTime.now().minusMinutes(10), LocalDateTime.now(),
            java.time.Duration.ofMinutes(10), 2, List.of("en-US", "es-ES")
        );
    }
    
    @Test
    @DisplayName("Should start transcription successfully")
    void shouldStartTranscriptionSuccessfully() {
        // Given
        when(transcriptionService.startTranscription(eq(testMeetingId), any(TranscriptionConfig.class)))
            .thenReturn(Mono.empty());
        
        // When
        var result = transcriptionService.startTranscription(testMeetingId, testConfig);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).startTranscription(testMeetingId, testConfig);
    }
    
    @Test
    @DisplayName("Should handle transcription start failure")
    void shouldHandleTranscriptionStartFailure() {
        // Given
        var error = new RuntimeException("Failed to start transcription");
        when(transcriptionService.startTranscription(eq(testMeetingId), any(TranscriptionConfig.class)))
            .thenReturn(Mono.error(error));
        
        // When
        var result = transcriptionService.startTranscription(testMeetingId, testConfig);
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Failed to start transcription"))
            .verify();
        
        verify(transcriptionService, times(1)).startTranscription(testMeetingId, testConfig);
    }
    
    @Test
    @DisplayName("Should stop transcription successfully")
    void shouldStopTranscriptionSuccessfully() {
        // Given
        when(transcriptionService.stopTranscription(testMeetingId)).thenReturn(Mono.empty());
        
        // When
        var result = transcriptionService.stopTranscription(testMeetingId);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).stopTranscription(testMeetingId);
    }
    
    @Test
    @DisplayName("Should handle transcription stop failure")
    void shouldHandleTranscriptionStopFailure() {
        // Given
        var error = new RuntimeException("Failed to stop transcription");
        when(transcriptionService.stopTranscription(testMeetingId)).thenReturn(Mono.error(error));
        
        // When
        var result = transcriptionService.stopTranscription(testMeetingId);
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Failed to stop transcription"))
            .verify();
        
        verify(transcriptionService, times(1)).stopTranscription(testMeetingId);
    }
    
    @Test
    @DisplayName("Should get transcription status successfully")
    void shouldGetTranscriptionStatusSuccessfully() {
        // Given
        var expectedStatus = TranscriptionService.TranscriptionStatus.ACTIVE;
        when(transcriptionService.getTranscriptionStatus(testMeetingId))
            .thenReturn(Mono.just(expectedStatus));
        
        // When
        var result = transcriptionService.getTranscriptionStatus(testMeetingId);
        
        // Then
        StepVerifier.create(result)
            .expectNext(expectedStatus)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).getTranscriptionStatus(testMeetingId);
    }
    
    @Test
    @DisplayName("Should handle transcription status not found")
    void shouldHandleTranscriptionStatusNotFound() {
        // Given
        when(transcriptionService.getTranscriptionStatus(testMeetingId))
            .thenReturn(Mono.empty());
        
        // When
        var result = transcriptionService.getTranscriptionStatus(testMeetingId);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).getTranscriptionStatus(testMeetingId);
    }
    
    @Test
    @DisplayName("Should handle transcription status retrieval error")
    void shouldHandleTranscriptionStatusRetrievalError() {
        // Given
        var error = new RuntimeException("Failed to get transcription status");
        when(transcriptionService.getTranscriptionStatus(testMeetingId))
            .thenReturn(Mono.error(error));
        
        // When
        var result = transcriptionService.getTranscriptionStatus(testMeetingId);
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Failed to get transcription status"))
            .verify();
        
        verify(transcriptionService, times(1)).getTranscriptionStatus(testMeetingId);
    }
    
    @Test
    @DisplayName("Should get transcription stream successfully")
    void shouldGetTranscriptionStreamSuccessfully() {
        // Given
        var expectedSegments = List.of(testSegment);
        when(transcriptionService.getTranscriptionStream(testMeetingId))
            .thenReturn(Flux.fromIterable(expectedSegments));
        
        // When
        var result = transcriptionService.getTranscriptionStream(testMeetingId);
        
        // Then
        StepVerifier.create(result)
            .expectNext(testSegment)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).getTranscriptionStream(testMeetingId);
    }
    
    @Test
    @DisplayName("Should handle empty transcription stream")
    void shouldHandleEmptyTranscriptionStream() {
        // Given
        when(transcriptionService.getTranscriptionStream(testMeetingId))
            .thenReturn(Flux.empty());
        
        // When
        var result = transcriptionService.getTranscriptionStream(testMeetingId);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).getTranscriptionStream(testMeetingId);
    }
    
    @Test
    @DisplayName("Should get all transcription segments successfully")
    void shouldGetAllTranscriptionSegmentsSuccessfully() {
        // Given
        var expectedSegments = List.of(testSegment);
        when(transcriptionService.getAllTranscriptionSegments(testMeetingId))
            .thenReturn(Flux.fromIterable(expectedSegments));
        
        // When
        var result = transcriptionService.getAllTranscriptionSegments(testMeetingId);
        
        // Then
        StepVerifier.create(result)
            .expectNext(testSegment)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).getAllTranscriptionSegments(testMeetingId);
    }
    
    @Test
    @DisplayName("Should handle empty all transcription segments")
    void shouldHandleEmptyAllTranscriptionSegments() {
        // Given
        when(transcriptionService.getAllTranscriptionSegments(testMeetingId))
            .thenReturn(Flux.empty());
        
        // When
        var result = transcriptionService.getAllTranscriptionSegments(testMeetingId);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).getAllTranscriptionSegments(testMeetingId);
    }
    
    @Test
    @DisplayName("Should process audio data successfully")
    void shouldProcessAudioDataSuccessfully() {
        // Given
        var audioData = new byte[]{1, 2, 3, 4, 5};
        var expectedSegments = List.of(testSegment);
        when(transcriptionService.processAudio(eq(testMeetingId), any(byte[].class)))
            .thenReturn(Flux.fromIterable(expectedSegments));
        
        // When
        var result = transcriptionService.processAudio(testMeetingId, audioData);
        
        // Then
        StepVerifier.create(result)
            .expectNext(testSegment)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).processAudio(testMeetingId, audioData);
    }
    
    @Test
    @DisplayName("Should handle empty audio data processing")
    void shouldHandleEmptyAudioDataProcessing() {
        // Given
        var emptyAudioData = new byte[0];
        when(transcriptionService.processAudio(eq(testMeetingId), any(byte[].class)))
            .thenReturn(Flux.empty());
        
        // When
        var result = transcriptionService.processAudio(testMeetingId, emptyAudioData);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).processAudio(testMeetingId, emptyAudioData);
    }
    
    @Test
    @DisplayName("Should handle null audio data processing")
    void shouldHandleNullAudioDataProcessing() {
        // Given
        when(transcriptionService.processAudio(eq(testMeetingId), isNull()))
            .thenReturn(Flux.empty());
        
        // When
        var result = transcriptionService.processAudio(testMeetingId, null);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).processAudio(testMeetingId, null);
    }
    
    @Test
    @DisplayName("Should set language successfully")
    void shouldSetLanguageSuccessfully() {
        // Given
        var language = "en-US";
        when(transcriptionService.setLanguage(testMeetingId, language)).thenReturn(Mono.empty());
        
        // When
        var result = transcriptionService.setLanguage(testMeetingId, language);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).setLanguage(testMeetingId, language);
    }
    
    @Test
    @DisplayName("Should handle language setting failure")
    void shouldHandleLanguageSettingFailure() {
        // Given
        var language = "en-US";
        var error = new RuntimeException("Failed to set language");
        when(transcriptionService.setLanguage(testMeetingId, language))
            .thenReturn(Mono.error(error));
        
        // When
        var result = transcriptionService.setLanguage(testMeetingId, language);
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Failed to set language"))
            .verify();
        
        verify(transcriptionService, times(1)).setLanguage(testMeetingId, language);
    }
    
    @Test
    @DisplayName("Should get language successfully")
    void shouldGetLanguageSuccessfully() {
        // Given
        var expectedLanguage = "en-US";
        when(transcriptionService.getLanguage(testMeetingId))
            .thenReturn(Mono.just(expectedLanguage));
        
        // When
        var result = transcriptionService.getLanguage(testMeetingId);
        
        // Then
        StepVerifier.create(result)
            .expectNext(expectedLanguage)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).getLanguage(testMeetingId);
    }
    
    @Test
    @DisplayName("Should handle language not found")
    void shouldHandleLanguageNotFound() {
        // Given
        when(transcriptionService.getLanguage(testMeetingId))
            .thenReturn(Mono.empty());
        
        // When
        var result = transcriptionService.getLanguage(testMeetingId);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).getLanguage(testMeetingId);
    }
    
    @Test
    @DisplayName("Should enable speaker diarization successfully")
    void shouldEnableSpeakerDiarizationSuccessfully() {
        // Given
        when(transcriptionService.setSpeakerDiarization(testMeetingId, true))
            .thenReturn(Mono.empty());
        
        // When
        var result = transcriptionService.setSpeakerDiarization(testMeetingId, true);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).setSpeakerDiarization(testMeetingId, true);
    }
    
    @Test
    @DisplayName("Should disable speaker diarization successfully")
    void shouldDisableSpeakerDiarizationSuccessfully() {
        // Given
        when(transcriptionService.setSpeakerDiarization(testMeetingId, false))
            .thenReturn(Mono.empty());
        
        // When
        var result = transcriptionService.setSpeakerDiarization(testMeetingId, false);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).setSpeakerDiarization(testMeetingId, false);
    }
    
    @Test
    @DisplayName("Should handle speaker diarization setting failure")
    void shouldHandleSpeakerDiarizationSettingFailure() {
        // Given
        var error = new RuntimeException("Failed to set speaker diarization");
        when(transcriptionService.setSpeakerDiarization(testMeetingId, anyBoolean()))
            .thenReturn(Mono.error(error));
        
        // When
        var result = transcriptionService.setSpeakerDiarization(testMeetingId, true);
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Failed to set speaker diarization"))
            .verify();
        
        verify(transcriptionService, times(1)).setSpeakerDiarization(testMeetingId, true);
    }
    
    @Test
    @DisplayName("Should check speaker diarization enabled successfully")
    void shouldCheckSpeakerDiarizationEnabledSuccessfully() {
        // Given
        when(transcriptionService.isSpeakerDiarizationEnabled(testMeetingId))
            .thenReturn(Mono.just(true));
        
        // When
        var result = transcriptionService.isSpeakerDiarizationEnabled(testMeetingId);
        
        // Then
        StepVerifier.create(result)
            .expectNext(true)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).isSpeakerDiarizationEnabled(testMeetingId);
    }
    
    @Test
    @DisplayName("Should check speaker diarization disabled successfully")
    void shouldCheckSpeakerDiarizationDisabledSuccessfully() {
        // Given
        when(transcriptionService.isSpeakerDiarizationEnabled(testMeetingId))
            .thenReturn(Mono.just(false));
        
        // When
        var result = transcriptionService.isSpeakerDiarizationEnabled(testMeetingId);
        
        // Then
        StepVerifier.create(result)
            .expectNext(false)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).isSpeakerDiarizationEnabled(testMeetingId);
    }
    
    @Test
    @DisplayName("Should set confidence threshold successfully")
    void shouldSetConfidenceThresholdSuccessfully() {
        // Given
        var threshold = 0.8;
        when(transcriptionService.setConfidenceThreshold(testMeetingId, threshold))
            .thenReturn(Mono.empty());
        
        // When
        var result = transcriptionService.setConfidenceThreshold(testMeetingId, threshold);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).setConfidenceThreshold(testMeetingId, threshold);
    }
    
    @Test
    @DisplayName("Should handle confidence threshold setting failure")
    void shouldHandleConfidenceThresholdSettingFailure() {
        // Given
        var threshold = 0.8;
        var error = new RuntimeException("Failed to set confidence threshold");
        when(transcriptionService.setConfidenceThreshold(testMeetingId, threshold))
            .thenReturn(Mono.error(error));
        
        // When
        var result = transcriptionService.setConfidenceThreshold(testMeetingId, threshold);
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Failed to set confidence threshold"))
            .verify();
        
        verify(transcriptionService, times(1)).setConfidenceThreshold(testMeetingId, threshold);
    }
    
    @Test
    @DisplayName("Should get confidence threshold successfully")
    void shouldGetConfidenceThresholdSuccessfully() {
        // Given
        var expectedThreshold = 0.8;
        when(transcriptionService.getConfidenceThreshold(testMeetingId))
            .thenReturn(Mono.just(expectedThreshold));
        
        // When
        var result = transcriptionService.getConfidenceThreshold(testMeetingId);
        
        // Then
        StepVerifier.create(result)
            .expectNext(expectedThreshold)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).getConfidenceThreshold(testMeetingId);
    }
    
    @Test
    @DisplayName("Should get transcription statistics successfully")
    void shouldGetTranscriptionStatisticsSuccessfully() {
        // Given
        when(transcriptionService.getTranscriptionStats(testMeetingId))
            .thenReturn(Mono.just(testStats));
        
        // When
        var result = transcriptionService.getTranscriptionStats(testMeetingId);
        
        // Then
        StepVerifier.create(result)
            .expectNext(testStats)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).getTranscriptionStats(testMeetingId);
    }
    
    @Test
    @DisplayName("Should handle transcription statistics not found")
    void shouldHandleTranscriptionStatisticsNotFound() {
        // Given
        when(transcriptionService.getTranscriptionStats(testMeetingId))
            .thenReturn(Mono.empty());
        
        // When
        var result = transcriptionService.getTranscriptionStats(testMeetingId);
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).getTranscriptionStats(testMeetingId);
    }
    
    @Test
    @DisplayName("Should export transcription successfully")
    void shouldExportTranscriptionSuccessfully() {
        // Given
        var format = TranscriptionService.ExportFormat.JSON;
        var expectedData = new byte[]{1, 2, 3, 4, 5};
        when(transcriptionService.exportTranscription(testMeetingId, format))
            .thenReturn(Mono.just(expectedData));
        
        // When
        var result = transcriptionService.exportTranscription(testMeetingId, format);
        
        // Then
        StepVerifier.create(result)
            .expectNext(expectedData)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).exportTranscription(testMeetingId, format);
    }
    
    @Test
    @DisplayName("Should handle transcription export failure")
    void shouldHandleTranscriptionExportFailure() {
        // Given
        var format = TranscriptionService.ExportFormat.TXT;
        var error = new RuntimeException("Failed to export transcription");
        when(transcriptionService.exportTranscription(testMeetingId, format))
            .thenReturn(Mono.error(error));
        
        // When
        var result = transcriptionService.exportTranscription(testMeetingId, format);
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> throwable.getMessage().equals("Failed to export transcription"))
            .verify();
        
        verify(transcriptionService, times(1)).exportTranscription(testMeetingId, format);
    }
    
    @Test
    @DisplayName("Should create transcription segment record correctly")
    void shouldCreateTranscriptionSegmentRecordCorrectly() {
        // When
        var segment = new TranscriptionSegment(
            testSegment.id(), testSegment.meetingId(), testSegment.startTime(),
            testSegment.endTime(), testSegment.text(), testSegment.confidence(),
            testSegment.speakerId(), testSegment.timestamp()
        );
        
        // Then
        assertEquals(testSegment.id(), segment.id());
        assertEquals(testSegment.meetingId(), segment.meetingId());
        assertEquals(testSegment.startTime(), segment.startTime());
        assertEquals(testSegment.endTime(), segment.endTime());
        assertEquals(testSegment.text(), segment.text());
        assertEquals(testSegment.confidence(), segment.confidence());
        assertEquals(testSegment.speakerId(), segment.speakerId());
        assertEquals(testSegment.timestamp(), segment.timestamp());
    }
    
    @Test
    @DisplayName("Should create transcription config record correctly")
    void shouldCreateTranscriptionConfigRecordCorrectly() {
        // When
        var config = new TranscriptionConfig(
            testConfig.language(), testConfig.enableSpeakerDiarization(),
            testConfig.confidenceThreshold(), testConfig.enablePunctuation(),
            testConfig.enableCapitalization(), testConfig.enableTimestamps(),
            testConfig.maxSpeakers(), testConfig.model()
        );
        
        // Then
        assertEquals(testConfig.language(), config.language());
        assertEquals(testConfig.enableSpeakerDiarization(), config.enableSpeakerDiarization());
        assertEquals(testConfig.confidenceThreshold(), config.confidenceThreshold());
        assertEquals(testConfig.enablePunctuation(), config.enablePunctuation());
        assertEquals(testConfig.enableCapitalization(), config.enableCapitalization());
        assertEquals(testConfig.enableTimestamps(), config.enableTimestamps());
        assertEquals(testConfig.maxSpeakers(), config.maxSpeakers());
        assertEquals(testConfig.model(), config.model());
    }
    
    @Test
    @DisplayName("Should create default transcription configuration")
    void shouldCreateDefaultTranscriptionConfiguration() {
        // When
        var config = TranscriptionService.TranscriptionConfig.defaultConfig();
        
        // Then
        assertEquals("en-US", config.language());
        assertTrue(config.enableSpeakerDiarization());
        assertEquals(0.7, config.confidenceThreshold());
        assertTrue(config.enablePunctuation());
        assertTrue(config.enableCapitalization());
        assertTrue(config.enableTimestamps());
        assertEquals(10, config.maxSpeakers());
        assertEquals("whisper-1", config.model());
    }
    
    @Test
    @DisplayName("Should create high quality transcription configuration")
    void shouldCreateHighQualityTranscriptionConfiguration() {
        // When
        var config = TranscriptionService.TranscriptionConfig.highQuality();
        
        // Then
        assertEquals("en-US", config.language());
        assertTrue(config.enableSpeakerDiarization());
        assertEquals(0.8, config.confidenceThreshold());
        assertTrue(config.enablePunctuation());
        assertTrue(config.enableCapitalization());
        assertTrue(config.enableTimestamps());
        assertEquals(20, config.maxSpeakers());
        assertEquals("whisper-1", config.model());
    }
    
    @Test
    @DisplayName("Should create fast transcription configuration")
    void shouldCreateFastTranscriptionConfiguration() {
        // When
        var config = TranscriptionService.TranscriptionConfig.fast();
        
        // Then
        assertEquals("en-US", config.language());
        assertFalse(config.enableSpeakerDiarization());
        assertEquals(0.6, config.confidenceThreshold());
        assertFalse(config.enablePunctuation());
        assertFalse(config.enableCapitalization());
        assertTrue(config.enableTimestamps());
        assertEquals(5, config.maxSpeakers());
        assertEquals("whisper-tiny", config.model());
    }
    
    @Test
    @DisplayName("Should create transcription statistics record correctly")
    void shouldCreateTranscriptionStatisticsRecordCorrectly() {
        // When
        var stats = new TranscriptionStats(
            testStats.meetingId(), testStats.totalSegments(), testStats.totalWords(),
            testStats.averageConfidence(), testStats.startTime(), testStats.endTime(),
            testStats.totalDuration(), testStats.speakerCount(), testStats.detectedLanguages()
        );
        
        // Then
        assertEquals(testStats.meetingId(), stats.meetingId());
        assertEquals(testStats.totalSegments(), stats.totalSegments());
        assertEquals(testStats.totalWords(), stats.totalWords());
        assertEquals(testStats.averageConfidence(), stats.averageConfidence());
        assertEquals(testStats.startTime(), stats.startTime());
        assertEquals(testStats.endTime(), stats.endTime());
        assertEquals(testStats.totalDuration(), stats.totalDuration());
        assertEquals(testStats.speakerCount(), stats.speakerCount());
        assertEquals(testStats.detectedLanguages(), stats.detectedLanguages());
    }
    
    @Test
    @DisplayName("Should calculate words per minute correctly")
    void shouldCalculateWordsPerMinuteCorrectly() {
        // When
        var wpm = testStats.getWordsPerMinute();
        
        // Then
        assertEquals(5.0, wpm, 0.01); // 50 words in 10 minutes = 5 wpm
    }
    
    @Test
    @DisplayName("Should handle words per minute with zero duration")
    void shouldHandleWordsPerMinuteWithZeroDuration() {
        // Given
        var statsWithZeroDuration = new TranscriptionStats(
            testMeetingId, 10, 50, 0.88,
            LocalDateTime.now(), LocalDateTime.now(),
            java.time.Duration.ZERO, 2, List.of("en-US")
        );
        
        // When
        var wpm = statsWithZeroDuration.getWordsPerMinute();
        
        // Then
        assertEquals(0.0, wpm);
    }
    
    @Test
    @DisplayName("Should calculate average segment duration correctly")
    void shouldCalculateAverageSegmentDurationCorrectly() {
        // When
        var avgDuration = testStats.getAverageSegmentDuration();
        
        // Then
        assertEquals(java.time.Duration.ofMinutes(1), avgDuration); // 10 minutes / 10 segments
    }
    
    @Test
    @DisplayName("Should handle average segment duration with zero segments")
    void shouldHandleAverageSegmentDurationWithZeroSegments() {
        // Given
        var statsWithZeroSegments = new TranscriptionStats(
            testMeetingId, 0, 0, 0.0,
            LocalDateTime.now(), LocalDateTime.now(),
            java.time.Duration.ofMinutes(10), 0, List.of()
        );
        
        // When
        var avgDuration = statsWithZeroSegments.getAverageSegmentDuration();
        
        // Then
        assertEquals(java.time.Duration.ZERO, avgDuration);
    }
    
    @ParameterizedTest
    @EnumSource(TranscriptionService.TranscriptionStatus.class)
    @DisplayName("Should handle all transcription status types")
    void shouldHandleAllTranscriptionStatusTypes(TranscriptionService.TranscriptionStatus status) {
        // Given
        when(transcriptionService.getTranscriptionStatus(testMeetingId))
            .thenReturn(Mono.just(status));
        
        // When
        var result = transcriptionService.getTranscriptionStatus(testMeetingId);
        
        // Then
        StepVerifier.create(result)
            .expectNext(status)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).getTranscriptionStatus(testMeetingId);
    }
    
    @ParameterizedTest
    @EnumSource(TranscriptionService.ExportFormat.class)
    @DisplayName("Should handle all export formats")
    void shouldHandleAllExportFormats(TranscriptionService.ExportFormat format) {
        // Given
        var expectedData = new byte[]{1, 2, 3};
        when(transcriptionService.exportTranscription(testMeetingId, format))
            .thenReturn(Mono.just(expectedData));
        
        // When
        var result = transcriptionService.exportTranscription(testMeetingId, format);
        
        // Then
        StepVerifier.create(result)
            .expectNext(expectedData)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).exportTranscription(testMeetingId, format);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"en-US", "es-ES", "fr-FR", "de-DE", "ja-JP", "zh-CN"})
    @DisplayName("Should handle various language codes")
    void shouldHandleVariousLanguageCodes(String language) {
        // Given
        when(transcriptionService.setLanguage(testMeetingId, language))
            .thenReturn(Mono.empty());
        when(transcriptionService.getLanguage(testMeetingId))
            .thenReturn(Mono.just(language));
        
        // When
        var setResult = transcriptionService.setLanguage(testMeetingId, language);
        var getResult = transcriptionService.getLanguage(testMeetingId);
        
        // Then
        StepVerifier.create(setResult)
            .verifyComplete();
        StepVerifier.create(getResult)
            .expectNext(language)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).setLanguage(testMeetingId, language);
        verify(transcriptionService, times(1)).getLanguage(testMeetingId);
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.5, 0.7, 0.9, 1.0})
    @DisplayName("Should handle various confidence thresholds")
    void shouldHandleVariousConfidenceThresholds(double threshold) {
        // Given
        when(transcriptionService.setConfidenceThreshold(testMeetingId, threshold))
            .thenReturn(Mono.empty());
        when(transcriptionService.getConfidenceThreshold(testMeetingId))
            .thenReturn(Mono.just(threshold));
        
        // When
        var setResult = transcriptionService.setConfidenceThreshold(testMeetingId, threshold);
        var getResult = transcriptionService.getConfidenceThreshold(testMeetingId);
        
        // Then
        StepVerifier.create(setResult)
            .verifyComplete();
        StepVerifier.create(getResult)
            .expectNext(threshold)
            .verifyComplete();
        
        verify(transcriptionService, times(1)).setConfidenceThreshold(testMeetingId, threshold);
        verify(transcriptionService, times(1)).getConfidenceThreshold(testMeetingId);
    }
    
    @Test
    @DisplayName("Should handle transcription segment with negative speaker ID")
    void shouldHandleTranscriptionSegmentWithNegativeSpeakerId() {
        // When
        var segment = new TranscriptionSegment(
            UUID.randomUUID(), testMeetingId, 0, 5000,
            "Test text", 0.9, -1, LocalDateTime.now()
        );
        
        // Then
        assertEquals(-1, segment.speakerId());
    }
    
    @Test
    @DisplayName("Should handle transcription segment with zero confidence")
    void shouldHandleTranscriptionSegmentWithZeroConfidence() {
        // When
        var segment = new TranscriptionSegment(
            UUID.randomUUID(), testMeetingId, 0, 5000,
            "Test text", 0.0, 0, LocalDateTime.now()
        );
        
        // Then
        assertEquals(0.0, segment.confidence());
    }
    
    @Test
    @DisplayName("Should handle transcription segment with perfect confidence")
    void shouldHandleTranscriptionSegmentWithPerfectConfidence() {
        // When
        var segment = new TranscriptionSegment(
            UUID.randomUUID(), testMeetingId, 0, 5000,
            "Test text", 1.0, 0, LocalDateTime.now()
        );
        
        // Then
        assertEquals(1.0, segment.confidence());
    }
    
    @Test
    @DisplayName("Should handle transcription segment with negative time values")
    void shouldHandleTranscriptionSegmentWithNegativeTimeValues() {
        // When
        var segment = new TranscriptionSegment(
            UUID.randomUUID(), testMeetingId, -1000, -500,
            "Test text", 0.8, 0, LocalDateTime.now()
        );
        
        // Then
        assertEquals(-1000, segment.startTime());
        assertEquals(-500, segment.endTime());
    }
    
    @Test
    @DisplayName("Should handle transcription segment with empty text")
    void shouldHandleTranscriptionSegmentWithEmptyText() {
        // When
        var segment = new TranscriptionSegment(
            UUID.randomUUID(), testMeetingId, 0, 5000,
            "", 0.8, 0, LocalDateTime.now()
        );
        
        // Then
        assertEquals("", segment.text());
    }
    
    @Test
    @DisplayName("Should handle transcription segment with null timestamp")
    void shouldHandleTranscriptionSegmentWithNullTimestamp() {
        // When
        var segment = new TranscriptionSegment(
            UUID.randomUUID(), testMeetingId, 0, 5000,
            "Test text", 0.8, 0, null
        );
        
        // Then
        assertNull(segment.timestamp());
    }
}