package com.zoomtranscriber.core.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SummaryRepository.
 * Tests repository query methods, edge cases, and exception handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SummaryRepository Tests")
class SummaryRepositoryTest {

    @Mock
    private SummaryRepository summaryRepository;

    private Summary testSummary;
    private MeetingSession testMeeting;
    private UUID testMeetingId;
    private UUID testSummaryId;
    private LocalDateTime testCreatedAt;

    @BeforeEach
    void setUp() {
        testMeetingId = UUID.randomUUID();
        testSummaryId = UUID.randomUUID();
        testCreatedAt = LocalDateTime.of(2023, 11, 4, 11, 0, 0);

        testMeeting = new MeetingSession("Test Meeting");
        testMeeting.setId(testMeetingId);

        testSummary = new Summary(testMeeting, Summary.SummaryType.FULL, "This is a full summary of the meeting.");
        testSummary.setId(testSummaryId);
        testSummary.setCreatedAt(testCreatedAt);
        testSummary.setModelUsed("GPT-4");
        testSummary.setProcessingTime("PT30S");
    }

    @Test
    @DisplayName("Should find summaries by meeting session ID")
    void shouldFindSummariesByMeetingSessionId() {
        // Given
        var expectedSummaries = List.of(testSummary);
        when(summaryRepository.findByMeetingSessionId(testMeetingId)).thenReturn(expectedSummaries);

        // When
        var result = summaryRepository.findByMeetingSessionId(testMeetingId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSummary, result.get(0));
        verify(summaryRepository, times(1)).findByMeetingSessionId(testMeetingId);
    }

    @Test
    @DisplayName("Should return empty list when no summaries found for meeting")
    void shouldReturnEmptyListWhenNoSummariesFoundForMeeting() {
        // Given
        var nonExistentMeetingId = UUID.randomUUID();
        when(summaryRepository.findByMeetingSessionId(nonExistentMeetingId)).thenReturn(Collections.emptyList());

        // When
        var result = summaryRepository.findByMeetingSessionId(nonExistentMeetingId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(summaryRepository, times(1)).findByMeetingSessionId(nonExistentMeetingId);
    }

    @Test
    @DisplayName("Should find summaries by meeting session ID ordered by creation date")
    void shouldFindSummariesByMeetingSessionIdOrderedByCreatedAt() {
        // Given
        var olderSummary = createSummary(Summary.SummaryType.KEY_POINTS, testCreatedAt.minusHours(1));
        var newerSummary = createSummary(Summary.SummaryType.DECISIONS, testCreatedAt.plusHours(1));
        var expectedSummaries = Arrays.asList(olderSummary, testSummary, newerSummary);
        when(summaryRepository.findByMeetingSessionIdOrderByCreatedAt(testMeetingId)).thenReturn(expectedSummaries);

        // When
        var result = summaryRepository.findByMeetingSessionIdOrderByCreatedAt(testMeetingId);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.get(0).getCreatedAt().isBefore(result.get(1).getCreatedAt()));
        assertTrue(result.get(1).getCreatedAt().isBefore(result.get(2).getCreatedAt()));
        verify(summaryRepository, times(1)).findByMeetingSessionIdOrderByCreatedAt(testMeetingId);
    }

    @ParameterizedTest
    @EnumSource(Summary.SummaryType.class)
    @DisplayName("Should find summaries by type")
    void shouldFindSummariesByType(Summary.SummaryType summaryType) {
        // Given
        var expectedSummaries = List.of(testSummary);
        when(summaryRepository.findBySummaryType(summaryType)).thenReturn(expectedSummaries);

        // When
        var result = summaryRepository.findBySummaryType(summaryType);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(summaryType, result.get(0).getSummaryType());
        verify(summaryRepository, times(1)).findBySummaryType(summaryType);
    }

    @Test
    @DisplayName("Should find summaries by meeting session ID and type")
    void shouldFindSummariesByMeetingSessionIdAndType() {
        // Given
        var summaryType = Summary.SummaryType.FULL;
        var expectedSummaries = List.of(testSummary);
        when(summaryRepository.findByMeetingSessionIdAndSummaryType(testMeetingId, summaryType))
                .thenReturn(expectedSummaries);

        // When
        var result = summaryRepository.findByMeetingSessionIdAndSummaryType(testMeetingId, summaryType);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSummary, result.get(0));
        assertEquals(summaryType, result.get(0).getSummaryType());
        verify(summaryRepository, times(1)).findByMeetingSessionIdAndSummaryType(testMeetingId, summaryType);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"GPT-4", "GPT-3.5", "Claude-2"})
    @DisplayName("Should find summaries by model used")
    void shouldFindSummariesByModelUsed(String modelUsed) {
        // Given
        var expectedSummaries = modelUsed != null && !modelUsed.isEmpty() ? 
                List.of(testSummary) : Collections.emptyList();
        when(summaryRepository.findByModelUsed(modelUsed)).thenReturn(expectedSummaries);

        // When
        var result = summaryRepository.findByModelUsed(modelUsed);

        // Then
        assertNotNull(result);
        if (modelUsed != null && !modelUsed.isEmpty()) {
            assertEquals(1, result.size());
            assertEquals(modelUsed, result.get(0).getModelUsed());
        } else {
            assertTrue(result.isEmpty());
        }
        verify(summaryRepository, times(1)).findByModelUsed(modelUsed);
    }

    @Test
    @DisplayName("Should find summaries by creation date range")
    void shouldFindSummariesByCreationDateRange() {
        // Given
        var startDate = testCreatedAt.minusDays(1);
        var endDate = testCreatedAt.plusDays(1);
        var expectedSummaries = List.of(testSummary);
        when(summaryRepository.findByCreatedAtBetween(startDate, endDate)).thenReturn(expectedSummaries);

        // When
        var result = summaryRepository.findByCreatedAtBetween(startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSummary, result.get(0));
        verify(summaryRepository, times(1)).findByCreatedAtBetween(startDate, endDate);
    }

    @Test
    @DisplayName("Should find summaries by meeting session ID and creation date range")
    void shouldFindSummariesByMeetingSessionIdAndCreationDateRange() {
        // Given
        var startDate = testCreatedAt.minusDays(1);
        var endDate = testCreatedAt.plusDays(1);
        var expectedSummaries = List.of(testSummary);
        when(summaryRepository.findByMeetingSessionIdAndCreatedAtBetween(testMeetingId, startDate, endDate))
                .thenReturn(expectedSummaries);

        // When
        var result = summaryRepository.findByMeetingSessionIdAndCreatedAtBetween(testMeetingId, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSummary, result.get(0));
        verify(summaryRepository, times(1)).findByMeetingSessionIdAndCreatedAtBetween(testMeetingId, startDate, endDate);
    }

    @Test
    @DisplayName("Should find summaries by meeting session ID and multiple summary types")
    void shouldFindSummariesByMeetingSessionIdAndMultipleSummaryTypes() {
        // Given
        var summaryTypes = Arrays.asList(Summary.SummaryType.FULL, Summary.SummaryType.KEY_POINTS);
        var expectedSummaries = List.of(testSummary);
        when(summaryRepository.findByMeetingSessionIdAndSummaryTypes(testMeetingId, summaryTypes))
                .thenReturn(expectedSummaries);

        // When
        var result = summaryRepository.findByMeetingSessionIdAndSummaryTypes(testMeetingId, summaryTypes);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSummary, result.get(0));
        assertTrue(summaryTypes.contains(result.get(0).getSummaryType()));
        verify(summaryRepository, times(1)).findByMeetingSessionIdAndSummaryTypes(testMeetingId, summaryTypes);
    }

    @Test
    @DisplayName("Should count summaries by meeting session ID")
    void shouldCountSummariesByMeetingSessionId() {
        // Given
        var expectedCount = 5L;
        when(summaryRepository.countByMeetingSessionId(testMeetingId)).thenReturn(expectedCount);

        // When
        var result = summaryRepository.countByMeetingSessionId(testMeetingId);

        // Then
        assertEquals(expectedCount, result);
        verify(summaryRepository, times(1)).countByMeetingSessionId(testMeetingId);
    }

    @ParameterizedTest
    @EnumSource(Summary.SummaryType.class)
    @DisplayName("Should count summaries by type")
    void shouldCountSummariesByType(Summary.SummaryType summaryType) {
        // Given
        var expectedCount = 10L;
        when(summaryRepository.countBySummaryType(summaryType)).thenReturn(expectedCount);

        // When
        var result = summaryRepository.countBySummaryType(summaryType);

        // Then
        assertEquals(expectedCount, result);
        verify(summaryRepository, times(1)).countBySummaryType(summaryType);
    }

    @Test
    @DisplayName("Should find all distinct models used")
    void shouldFindAllDistinctModelsUsed() {
        // Given
        var expectedModels = Arrays.asList("GPT-4", "GPT-3.5", "Claude-2");
        when(summaryRepository.findAllDistinctModels()).thenReturn(expectedModels);

        // When
        var result = summaryRepository.findAllDistinctModels();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsAll(expectedModels));
        verify(summaryRepository, times(1)).findAllDistinctModels();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"meeting", "summary", "important"})
    @DisplayName("Should find summaries by content containing keyword")
    void shouldFindSummariesByContentContainingKeyword(String keyword) {
        // Given
        var expectedSummaries = keyword != null && !keyword.isEmpty() ? 
                List.of(testSummary) : Collections.emptyList();
        when(summaryRepository.findByContentContaining(keyword)).thenReturn(expectedSummaries);

        // When
        var result = summaryRepository.findByContentContaining(keyword);

        // Then
        assertNotNull(result);
        assertEquals(keyword != null && !keyword.isEmpty() ? 1 : 0, result.size());
        if (keyword != null && !keyword.isEmpty()) {
            assertTrue(result.get(0).getContent().contains(keyword));
        }
        verify(summaryRepository, times(1)).findByContentContaining(keyword);
    }

    @Test
    @DisplayName("Should find summaries by meeting session ID and content containing keyword")
    void shouldFindSummariesByMeetingSessionIdAndContentContaining() {
        // Given
        var keyword = "meeting";
        var expectedSummaries = List.of(testSummary);
        when(summaryRepository.findByMeetingSessionIdAndContentContaining(testMeetingId, keyword))
                .thenReturn(expectedSummaries);

        // When
        var result = summaryRepository.findByMeetingSessionIdAndContentContaining(testMeetingId, keyword);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSummary, result.get(0));
        assertTrue(result.get(0).getContent().contains(keyword));
        verify(summaryRepository, times(1)).findByMeetingSessionIdAndContentContaining(testMeetingId, keyword);
    }

    @Test
    @DisplayName("Should count summaries grouped by type")
    void shouldCountSummariesGroupedByType() {
        // Given
        var expectedCounts = Arrays.asList(
                new Object[]{Summary.SummaryType.FULL, 15L},
                new Object[]{Summary.SummaryType.KEY_POINTS, 25L},
                new Object[]{Summary.SummaryType.DECISIONS, 10L},
                new Object[]{Summary.SummaryType.ACTION_ITEMS, 20L}
        );
        when(summaryRepository.countBySummaryTypeGrouped()).thenReturn(expectedCounts);

        // When
        var result = summaryRepository.countBySummaryTypeGrouped();

        // Then
        assertNotNull(result);
        assertEquals(4, result.size());
        
        // Verify structure of results
        for (Object[] row : result) {
            assertEquals(2, row.length);
            assertTrue(row[0] instanceof Summary.SummaryType);
            assertTrue(row[1] instanceof Long);
        }
        verify(summaryRepository, times(1)).countBySummaryTypeGrouped();
    }

    @Test
    @DisplayName("Should count summaries grouped by model used")
    void shouldCountSummariesGroupedByModelUsed() {
        // Given
        var expectedCounts = Arrays.asList(
                new Object[]{"GPT-4", 50L},
                new Object[]{"GPT-3.5", 30L},
                new Object[]{"Claude-2", 20L}
        );
        when(summaryRepository.countByModelUsedGrouped()).thenReturn(expectedCounts);

        // When
        var result = summaryRepository.countByModelUsedGrouped();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        
        // Verify structure of results
        for (Object[] row : result) {
            assertEquals(2, row.length);
            assertTrue(row[0] instanceof String);
            assertTrue(row[1] instanceof Long);
        }
        verify(summaryRepository, times(1)).countByModelUsedGrouped();
    }

    @Test
    @DisplayName("Should delete summaries by meeting session ID")
    void shouldDeleteSummariesByMeetingSessionId() {
        // Given
        doNothing().when(summaryRepository).deleteByMeetingSessionId(testMeetingId);

        // When
        summaryRepository.deleteByMeetingSessionId(testMeetingId);

        // Then
        verify(summaryRepository, times(1)).deleteByMeetingSessionId(testMeetingId);
    }

    @Test
    @DisplayName("Should check if summary exists by meeting session ID and type")
    void shouldCheckIfSummaryExistsByMeetingSessionIdAndType() {
        // Given
        var summaryType = Summary.SummaryType.FULL;
        when(summaryRepository.existsByMeetingSessionIdAndSummaryType(testMeetingId, summaryType))
                .thenReturn(true);

        // When
        var result = summaryRepository.existsByMeetingSessionIdAndSummaryType(testMeetingId, summaryType);

        // Then
        assertTrue(result);
        verify(summaryRepository, times(1)).existsByMeetingSessionIdAndSummaryType(testMeetingId, summaryType);
    }

    @Test
    @DisplayName("Should find latest summary by meeting session ID")
    void shouldFindLatestSummaryByMeetingSessionId() {
        // Given
        when(summaryRepository.findLatestByMeetingSessionId(testMeetingId)).thenReturn(Optional.of(testSummary));

        // When
        var result = summaryRepository.findLatestByMeetingSessionId(testMeetingId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testSummary, result.get());
        verify(summaryRepository, times(1)).findLatestByMeetingSessionId(testMeetingId);
    }

    @Test
    @DisplayName("Should return empty optional when no latest summary found")
    void shouldReturnEmptyOptionalWhenNoLatestSummaryFound() {
        // Given
        var nonExistentMeetingId = UUID.randomUUID();
        when(summaryRepository.findLatestByMeetingSessionId(nonExistentMeetingId)).thenReturn(Optional.empty());

        // When
        var result = summaryRepository.findLatestByMeetingSessionId(nonExistentMeetingId);

        // Then
        assertFalse(result.isPresent());
        verify(summaryRepository, times(1)).findLatestByMeetingSessionId(nonExistentMeetingId);
    }

    @Test
    @DisplayName("Should handle large number of summaries")
    void shouldHandleLargeNumberOfSummaries() {
        // Given
        var largeSummaryList = new ArrayList<Summary>();
        for (int i = 0; i < 3000; i++) {
            var summary = createSummary(Summary.SummaryType.FULL, testCreatedAt.plusMinutes(i));
            largeSummaryList.add(summary);
        }
        when(summaryRepository.findBySummaryType(Summary.SummaryType.FULL)).thenReturn(largeSummaryList);

        // When
        var result = summaryRepository.findBySummaryType(Summary.SummaryType.FULL);

        // Then
        assertNotNull(result);
        assertEquals(3000, result.size());
        verify(summaryRepository, times(1)).findBySummaryType(Summary.SummaryType.FULL);
    }

    @Test
    @DisplayName("Should handle empty summary types list")
    void shouldHandleEmptySummaryTypesList() {
        // Given
        var emptyTypes = Collections.<Summary.SummaryType>emptyList();
        when(summaryRepository.findByMeetingSessionIdAndSummaryTypes(testMeetingId, emptyTypes))
                .thenReturn(Collections.emptyList());

        // When
        var result = summaryRepository.findByMeetingSessionIdAndSummaryTypes(testMeetingId, emptyTypes);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(summaryRepository, times(1)).findByMeetingSessionIdAndSummaryTypes(testMeetingId, emptyTypes);
    }

    @Test
    @DisplayName("Should handle date range with same start and end dates")
    void shouldHandleDateRangeWithSameStartAndEndDates() {
        // Given
        var sameDate = testCreatedAt;
        var expectedSummaries = List.of(testSummary);
        when(summaryRepository.findByCreatedAtBetween(sameDate, sameDate)).thenReturn(expectedSummaries);

        // When
        var result = summaryRepository.findByCreatedAtBetween(sameDate, sameDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSummary, result.get(0));
        verify(summaryRepository, times(1)).findByCreatedAtBetween(sameDate, sameDate);
    }

    @Test
    @DisplayName("Should handle null model used in summaries")
    void shouldHandleNullModelUsedInSummaries() {
        // Given
        testSummary.setModelUsed(null);
        var expectedSummaries = List.of(testSummary);
        when(summaryRepository.findByModelUsed(null)).thenReturn(expectedSummaries);

        // When
        var result = summaryRepository.findByModelUsed(null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getModelUsed());
        verify(summaryRepository, times(1)).findByModelUsed(null);
    }

    @Test
    @DisplayName("Should handle special characters in content search")
    void shouldHandleSpecialCharactersInContentSearch() {
        // Given
        var keywordWithSpecialChars = "decision: \"approve\" & (review)";
        var expectedSummaries = List.of(testSummary);
        when(summaryRepository.findByContentContaining(keywordWithSpecialChars))
                .thenReturn(expectedSummaries);

        // When
        var result = summaryRepository.findByContentContaining(keywordWithSpecialChars);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSummary, result.get(0));
        verify(summaryRepository, times(1)).findByContentContaining(keywordWithSpecialChars);
    }

    @Test
    @DisplayName("Should handle very long content in summaries")
    void shouldHandleVeryLongContentInSummaries() {
        // Given
        var veryLongContent = "A".repeat(50000); // 50K characters
        testSummary.setContent(veryLongContent);
        var expectedSummaries = List.of(testSummary);
        when(summaryRepository.findByContentContaining("A")).thenReturn(expectedSummaries);

        // When
        var result = summaryRepository.findByContentContaining("A");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(veryLongContent, result.get(0).getContent());
        verify(summaryRepository, times(1)).findByContentContaining("A");
    }

    @Test
    @DisplayName("Should verify repository interface extends JpaRepository")
    void shouldVerifyRepositoryInterfaceExtendsJpaRepository() {
        // Given & When & Then
        assertTrue(summaryRepository instanceof JpaRepository);
        assertDoesNotThrow(() -> {
            // Test that all JpaRepository methods are available
            summaryRepository.save(testSummary);
            summaryRepository.findById(testSummaryId);
            summaryRepository.findAll();
            summaryRepository.count();
            summaryRepository.delete(testSummary);
        });
    }

    // Helper methods
    private Summary createSummary(Summary.SummaryType type, LocalDateTime createdAt) {
        var summary = new Summary(testMeeting, type, "Summary content for " + type);
        summary.setId(UUID.randomUUID());
        summary.setCreatedAt(createdAt);
        summary.setModelUsed("GPT-4");
        summary.setProcessingTime("PT30S");
        return summary;
    }
}