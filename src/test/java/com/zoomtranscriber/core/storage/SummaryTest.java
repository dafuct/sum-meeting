package com.zoomtranscriber.core.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Summary entity.
 * Tests entity behavior, validation, and business logic.
 */
@DisplayName("Summary Entity Tests")
class SummaryTest {
    
    private Summary summary;
    private MeetingSession meetingSession;
    private LocalDateTime testCreatedAt;
    
    @BeforeEach
    void setUp() {
        summary = new Summary();
        meetingSession = new MeetingSession("Test Meeting");
        meetingSession.setId(UUID.randomUUID());
        testCreatedAt = LocalDateTime.now();
    }
    
    @Test
    @DisplayName("Should create summary with default constructor")
    void shouldCreateSummaryWithDefaultConstructor() {
        // When
        var summary = new Summary();
        
        // Then
        assertNotNull(summary.getId());
        assertNull(summary.getMeetingSession());
        assertNull(summary.getSummaryType());
        assertNull(summary.getContent());
        assertNull(summary.getModelUsed());
        assertNull(summary.getProcessingTime());
        assertNotNull(summary.getCreatedAt());
    }
    
    @Test
    @DisplayName("Should create summary with parameterized constructor")
    void shouldCreateSummaryWithParameterizedConstructor() {
        // Given
        var content = "This is a test summary content.";
        var summaryType = Summary.SummaryType.FULL;
        
        // When
        var summary = new Summary(meetingSession, summaryType, content);
        
        // Then
        assertEquals(meetingSession, summary.getMeetingSession());
        assertEquals(summaryType, summary.getSummaryType());
        assertEquals(content, summary.getContent());
        assertNotNull(summary.getCreatedAt());
    }
    
    @Test
    @DisplayName("Should set and get ID correctly")
    void shouldSetAndGetIdCorrectly() {
        // Given
        var id = UUID.randomUUID();
        
        // When
        summary.setId(id);
        
        // Then
        assertEquals(id, summary.getId());
    }
    
    @Test
    @DisplayName("Should set and get meeting session correctly")
    void shouldSetAndGetMeetingSessionCorrectly() {
        // When
        summary.setMeetingSession(meetingSession);
        
        // Then
        assertEquals(meetingSession, summary.getMeetingSession());
    }
    
    @ParameterizedTest
    @EnumSource(Summary.SummaryType.class)
    @DisplayName("Should set and get summary type correctly for all enum values")
    void shouldSetAndGetSummaryTypeCorrectlyForAllEnumValues(Summary.SummaryType summaryType) {
        // When
        summary.setSummaryType(summaryType);
        
        // Then
        assertEquals(summaryType, summary.getSummaryType());
    }
    
    @Test
    @DisplayName("Should set and get content correctly")
    void shouldSetAndGetContentCorrectly() {
        // Given
        var content = "Meeting summary with key points and action items.";
        
        // When
        summary.setContent(content);
        
        // Then
        assertEquals(content, summary.getContent());
    }
    
    @Test
    @DisplayName("Should set and get model used correctly")
    void shouldSetAndGetModelUsedCorrectly() {
        // Given
        var modelUsed = "qwen2.5:0.5b";
        
        // When
        summary.setModelUsed(modelUsed);
        
        // Then
        assertEquals(modelUsed, summary.getModelUsed());
    }
    
    @Test
    @DisplayName("Should set and get processing time correctly")
    void shouldSetAndGetProcessingTimeCorrectly() {
        // Given
        var processingTime = "PT2M30S"; // ISO 8601 duration format
        
        // When
        summary.setProcessingTime(processingTime);
        
        // Then
        assertEquals(processingTime, summary.getProcessingTime());
    }
    
    @Test
    @DisplayName("Should set and get created at timestamp correctly")
    void shouldSetAndGetCreatedAtCorrectly() {
        // Given
        var createdAt = LocalDateTime.of(2023, 1, 1, 12, 0, 0);
        
        // When
        summary.setCreatedAt(createdAt);
        
        // Then
        assertEquals(createdAt, summary.getCreatedAt());
    }
    
    @Test
    @DisplayName("Should handle null meeting session")
    void shouldHandleNullMeetingSession() {
        // When
        summary.setMeetingSession(null);
        
        // Then
        assertNull(summary.getMeetingSession());
    }
    
    @Test
    @DisplayName("Should handle null content")
    void shouldHandleNullContent() {
        // When
        summary.setContent(null);
        
        // Then
        assertNull(summary.getContent());
    }
    
    @Test
    @DisplayName("Should handle null model used")
    void shouldHandleNullModelUsed() {
        // When
        summary.setModelUsed(null);
        
        // Then
        assertNull(summary.getModelUsed());
    }
    
    @Test
    @DisplayName("Should handle null processing time")
    void shouldHandleNullProcessingTime() {
        // When
        summary.setProcessingTime(null);
        
        // Then
        assertNull(summary.getProcessingTime());
    }
    
    @Test
    @DisplayName("Should handle empty content")
    void shouldHandleEmptyContent() {
        // When
        summary.setContent("");
        
        // Then
        assertEquals("", summary.getContent());
    }
    
    @Test
    @DisplayName("Should handle empty model used")
    void shouldHandleEmptyModelUsed() {
        // When
        summary.setModelUsed("");
        
        // Then
        assertEquals("", summary.getModelUsed());
    }
    
    @Test
    @DisplayName("Should handle empty processing time")
    void shouldHandleEmptyProcessingTime() {
        // When
        summary.setProcessingTime("");
        
        // Then
        assertEquals("", summary.getProcessingTime());
    }
    
    @Test
    @DisplayName("Should handle long content")
    void shouldHandleLongContent() {
        // Given
        var longContent = "A".repeat(25000); // 25000 characters
        
        // When
        summary.setContent(longContent);
        
        // Then
        assertEquals(longContent, summary.getContent());
    }
    
    @Test
    @DisplayName("Should handle maximum allowed content length")
    void shouldHandleMaximumAllowedContentLength() {
        // Given
        var maxContent = "A".repeat(50000); // Maximum allowed length
        
        // When
        summary.setContent(maxContent);
        
        // Then
        assertEquals(maxContent, summary.getContent());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"PT30S", "PT1M", "PT2M30S", "PT5M", "PT10M"})
    @DisplayName("Should handle various ISO 8601 processing time formats")
    void shouldHandleVariousISO8601ProcessingTimeFormats(String processingTime) {
        // When
        summary.setProcessingTime(processingTime);
        
        // Then
        assertEquals(processingTime, summary.getProcessingTime());
    }
    
    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
        // Given
        var id = UUID.randomUUID();
        var summary1 = new Summary();
        var summary2 = new Summary();
        var summary3 = new Summary();
        
        summary1.setId(id);
        summary2.setId(id);
        summary3.setId(UUID.randomUUID());
        
        // Then
        assertEquals(summary1, summary2);
        assertNotEquals(summary1, summary3);
        assertEquals(summary1, summary1); // Same object
        assertNotEquals(summary1, null);
        assertNotEquals(summary1, "string");
    }
    
    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
        // Given
        var id = UUID.randomUUID();
        var summary1 = new Summary();
        var summary2 = new Summary();
        
        summary1.setId(id);
        summary2.setId(id);
        
        // Then
        assertEquals(summary1.hashCode(), summary2.hashCode());
        
        // Test with null ID
        var summary3 = new Summary();
        var summary4 = new Summary();
        assertEquals(summary3.hashCode(), summary4.hashCode());
    }
    
    @Test
    @DisplayName("Should implement toString correctly")
    void shouldImplementToStringCorrectly() {
        // Given
        var id = UUID.randomUUID();
        var content = "Test summary content";
        var modelUsed = "qwen2.5:0.5b";
        var processingTime = "PT2M30S";
        
        summary.setId(id);
        summary.setMeetingSession(meetingSession);
        summary.setSummaryType(Summary.SummaryType.FULL);
        summary.setContent(content);
        summary.setModelUsed(modelUsed);
        summary.setProcessingTime(processingTime);
        
        // When
        var result = summary.toString();
        
        // Then
        assertTrue(result.contains("id=" + id));
        assertTrue(result.contains("meetingSessionId=" + meetingSession.getId()));
        assertTrue(result.contains("summaryType=" + Summary.SummaryType.FULL));
        assertTrue(result.contains("modelUsed='" + modelUsed + "'"));
        assertTrue(result.contains("processingTime='" + processingTime + "'"));
        assertTrue(result.contains("createdAt=" + summary.getCreatedAt()));
    }
    
    @Test
    @DisplayName("Should handle summary with all fields set")
    void shouldHandleSummaryWithAllFieldsSet() {
        // Given
        var id = UUID.randomUUID();
        var content = "Complete summary with all details";
        var modelUsed = "qwen2.5:0.5b";
        var processingTime = "PT3M15S";
        var createdAt = LocalDateTime.of(2023, 6, 15, 14, 30, 0);
        
        // When
        summary.setId(id);
        summary.setMeetingSession(meetingSession);
        summary.setSummaryType(Summary.SummaryType.KEY_POINTS);
        summary.setContent(content);
        summary.setModelUsed(modelUsed);
        summary.setProcessingTime(processingTime);
        summary.setCreatedAt(createdAt);
        
        // Then
        assertEquals(id, summary.getId());
        assertEquals(meetingSession, summary.getMeetingSession());
        assertEquals(Summary.SummaryType.KEY_POINTS, summary.getSummaryType());
        assertEquals(content, summary.getContent());
        assertEquals(modelUsed, summary.getModelUsed());
        assertEquals(processingTime, summary.getProcessingTime());
        assertEquals(createdAt, summary.getCreatedAt());
    }
    
    @Test
    @DisplayName("Should handle special characters in content")
    void shouldHandleSpecialCharactersInContent() {
        // Given
        var contentWithSpecialChars = "Summary with special chars: @#$%^&*()_+-={}[]|\\:;\"'<>?,./";
        
        // When
        summary.setContent(contentWithSpecialChars);
        
        // Then
        assertEquals(contentWithSpecialChars, summary.getContent());
    }
    
    @Test
    @DisplayName("Should handle unicode characters in content")
    void shouldHandleUnicodeCharactersInContent() {
        // Given
        var unicodeContent = "Summary with unicode: 世界 🌍 ñáéíóú";
        
        // When
        summary.setContent(unicodeContent);
        
        // Then
        assertEquals(unicodeContent, summary.getContent());
    }
    
    @Test
    @DisplayName("Should handle different model names")
    void shouldHandleDifferentModelNames() {
        // Given
        var modelNames = new String[]{
            "qwen2.5:0.5b",
            "llama3:8b",
            "mistral:7b",
            "custom-model-v1.0",
            "model-with-dashes_and_underscores"
        };
        
        // When & Then
        for (var modelName : modelNames) {
            summary.setModelUsed(modelName);
            assertEquals(modelName, summary.getModelUsed());
        }
    }
    
    @Test
    @DisplayName("Should handle time-based operations correctly")
    void shouldHandleTimeBasedOperationsCorrectly() {
        // Given
        var before = LocalDateTime.now();
        
        // When
        var summary = new Summary();
        var after = LocalDateTime.now();
        
        // Then
        assertTrue(summary.getCreatedAt().isAfter(before) || summary.getCreatedAt().isEqual(before));
        assertTrue(summary.getCreatedAt().isBefore(after) || summary.getCreatedAt().isEqual(after));
    }
    
    @Test
    @DisplayName("Should maintain immutability of created at timestamp")
    void shouldMaintainImmutabilityOfCreatedAtTimestamp() {
        // Given
        var originalCreatedAt = summary.getCreatedAt();
        
        // When
        var newCreatedAt = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        summary.setCreatedAt(newCreatedAt);
        
        // Then
        assertEquals(newCreatedAt, summary.getCreatedAt());
        assertNotEquals(originalCreatedAt, summary.getCreatedAt());
    }
    
    @Test
    @DisplayName("Should handle summary type transitions")
    void shouldHandleSummaryTypeTransitions() {
        // Test all summary types
        summary.setSummaryType(Summary.SummaryType.FULL);
        assertEquals(Summary.SummaryType.FULL, summary.getSummaryType());
        
        summary.setSummaryType(Summary.SummaryType.KEY_POINTS);
        assertEquals(Summary.SummaryType.KEY_POINTS, summary.getSummaryType());
        
        summary.setSummaryType(Summary.SummaryType.DECISIONS);
        assertEquals(Summary.SummaryType.DECISIONS, summary.getSummaryType());
        
        summary.setSummaryType(Summary.SummaryType.ACTION_ITEMS);
        assertEquals(Summary.SummaryType.ACTION_ITEMS, summary.getSummaryType());
    }
    
    @Test
    @DisplayName("Should handle content with line breaks")
    void shouldHandleContentWithLineBreaks() {
        // Given
        var contentWithLineBreaks = "Line 1\nLine 2\r\nLine 3\rLine 4";
        
        // When
        summary.setContent(contentWithLineBreaks);
        
        // Then
        assertEquals(contentWithLineBreaks, summary.getContent());
    }
    
    @Test
    @DisplayName("Should handle content with tabs and whitespace")
    void shouldHandleContentWithTabsAndWhitespace() {
        // Given
        var contentWithTabs = "Content with\ttabs\tand   multiple   spaces";
        
        // When
        summary.setContent(contentWithTabs);
        
        // Then
        assertEquals(contentWithTabs, summary.getContent());
    }
}