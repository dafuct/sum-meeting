package com.zoomtranscriber.core.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
 * Unit tests for MeetingRepository.
 * Tests repository query methods, edge cases, and exception handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MeetingRepository Tests")
class MeetingRepositoryTest {

    @Mock
    private MeetingRepository meetingRepository;

    private MeetingSession testMeeting;
    private UUID testMeetingId;
    private LocalDateTime testStartTime;
    private LocalDateTime testEndTime;
    private Pageable testPageable;

    @BeforeEach
    void setUp() {
        testMeetingId = UUID.randomUUID();
        testStartTime = LocalDateTime.of(2023, 11, 4, 10, 0, 0);
        testEndTime = testStartTime.plusHours(1);
        testPageable = PageRequest.of(0, 10);

        testMeeting = new MeetingSession("Test Meeting");
        testMeeting.setId(testMeetingId);
        testMeeting.setStartTime(testStartTime);
        testMeeting.setEndTime(testEndTime);
        testMeeting.setStatus(MeetingSession.MeetingStatus.COMPLETED);
        testMeeting.setAudioFilePath("/path/to/audio.wav");
        testMeeting.setParticipantCount(5);
    }

    @Test
    @DisplayName("Should find meetings by status")
    void shouldFindMeetingsByStatus() {
        // Given
        var status = MeetingSession.MeetingStatus.COMPLETED;
        var expectedMeetings = List.of(testMeeting);
        when(meetingRepository.findByStatus(status)).thenReturn(expectedMeetings);

        // When
        var result = meetingRepository.findByStatus(status);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testMeeting, result.get(0));
        verify(meetingRepository, times(1)).findByStatus(status);
    }

    @Test
    @DisplayName("Should return empty list when no meetings found by status")
    void shouldReturnEmptyListWhenNoMeetingsFoundByStatus() {
        // Given
        var status = MeetingSession.MeetingStatus.ERROR;
        when(meetingRepository.findByStatus(status)).thenReturn(Collections.emptyList());

        // When
        var result = meetingRepository.findByStatus(status);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(meetingRepository, times(1)).findByStatus(status);
    }

    @Test
    @DisplayName("Should find meetings by status with pagination")
    void shouldFindMeetingsByStatusWithPagination() {
        // Given
        var status = MeetingSession.MeetingStatus.COMPLETED;
        var expectedPage = new PageImpl<>(List.of(testMeeting), testPageable, 1);
        when(meetingRepository.findByStatus(status, testPageable)).thenReturn(expectedPage);

        // When
        var result = meetingRepository.findByStatus(status, testPageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testMeeting, result.getContent().get(0));
        verify(meetingRepository, times(1)).findByStatus(status, testPageable);
    }

    @Test
    @DisplayName("Should find meetings by start time between dates")
    void shouldFindMeetingsByStartTimeBetweenDates() {
        // Given
        var startDate = testStartTime.minusDays(1);
        var endDate = testEndTime.plusDays(1);
        var expectedMeetings = List.of(testMeeting);
        when(meetingRepository.findByStartTimeBetween(startDate, endDate)).thenReturn(expectedMeetings);

        // When
        var result = meetingRepository.findByStartTimeBetween(startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testMeeting, result.get(0));
        verify(meetingRepository, times(1)).findByStartTimeBetween(startDate, endDate);
    }

    @Test
    @DisplayName("Should find meetings by start time between dates with pagination")
    void shouldFindMeetingsByStartTimeBetweenDatesWithPagination() {
        // Given
        var startDate = testStartTime.minusDays(1);
        var endDate = testEndTime.plusDays(1);
        var expectedPage = new PageImpl<>(List.of(testMeeting), testPageable, 1);
        when(meetingRepository.findByStartTimeBetween(startDate, endDate, testPageable)).thenReturn(expectedPage);

        // When
        var result = meetingRepository.findByStartTimeBetween(startDate, endDate, testPageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testMeeting, result.getContent().get(0));
        verify(meetingRepository, times(1)).findByStartTimeBetween(startDate, endDate, testPageable);
    }

    @Test
    @DisplayName("Should find meetings by filters with all parameters")
    void shouldFindMeetingsByFiltersWithAllParameters() {
        // Given
        var status = MeetingSession.MeetingStatus.COMPLETED;
        var startDate = testStartTime.minusDays(1);
        var endDate = testEndTime.plusDays(1);
        var expectedPage = new PageImpl<>(List.of(testMeeting), testPageable, 1);
        when(meetingRepository.findByFilters(status, startDate, endDate, testPageable)).thenReturn(expectedPage);

        // When
        var result = meetingRepository.findByFilters(status, startDate, endDate, testPageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testMeeting, result.getContent().get(0));
        verify(meetingRepository, times(1)).findByFilters(status, startDate, endDate, testPageable);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Test Meeting", "Standup", "Review"})
    @DisplayName("Should find meeting by title containing text (case insensitive)")
    void shouldFindMeetingByTitleContainingTextCaseInsensitive(String title) {
        // Given
        when(meetingRepository.findByTitleContainingIgnoreCase(title)).thenReturn(Optional.of(testMeeting));

        // When
        var result = meetingRepository.findByTitleContainingIgnoreCase(title);

        // Then
        if (title != null && !title.isEmpty()) {
            assertTrue(result.isPresent());
            assertEquals(testMeeting, result.get());
        }
        verify(meetingRepository, times(1)).findByTitleContainingIgnoreCase(title);
    }

    @Test
    @DisplayName("Should return empty optional when no meeting found by title")
    void shouldReturnEmptyOptionalWhenNoMeetingFoundByTitle() {
        // Given
        var nonExistentTitle = "Non-existent Meeting";
        when(meetingRepository.findByTitleContainingIgnoreCase(nonExistentTitle)).thenReturn(Optional.empty());

        // When
        var result = meetingRepository.findByTitleContainingIgnoreCase(nonExistentTitle);

        // Then
        assertFalse(result.isPresent());
        verify(meetingRepository, times(1)).findByTitleContainingIgnoreCase(nonExistentTitle);
    }

    @Test
    @DisplayName("Should find meetings with audio file path")
    void shouldFindMeetingsWithAudioFilePath() {
        // Given
        var expectedMeetings = List.of(testMeeting);
        when(meetingRepository.findByAudioFilePathIsNotNull()).thenReturn(expectedMeetings);

        // When
        var result = meetingRepository.findByAudioFilePathIsNotNull();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testMeeting, result.get(0));
        verify(meetingRepository, times(1)).findByAudioFilePathIsNotNull();
    }

    @ParameterizedTest
    @EnumSource(MeetingSession.MeetingStatus.class)
    @DisplayName("Should count meetings by status")
    void shouldCountMeetingsByStatus(MeetingSession.MeetingStatus status) {
        // Given
        var expectedCount = 5L;
        when(meetingRepository.countByStatus(status)).thenReturn(expectedCount);

        // When
        var result = meetingRepository.countByStatus(status);

        // Then
        assertEquals(expectedCount, result);
        verify(meetingRepository, times(1)).countByStatus(status);
    }

    @Test
    @DisplayName("Should find meetings by multiple statuses")
    void shouldFindMeetingsByMultipleStatuses() {
        // Given
        var statuses = List.of(MeetingSession.MeetingStatus.COMPLETED, MeetingSession.MeetingStatus.RECORDING);
        var expectedMeetings = List.of(testMeeting);
        when(meetingRepository.findByStatusIn(statuses)).thenReturn(expectedMeetings);

        // When
        var result = meetingRepository.findByStatusIn(statuses);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testMeeting, result.get(0));
        verify(meetingRepository, times(1)).findByStatusIn(statuses);
    }

    @Test
    @DisplayName("Should find recent meetings")
    void shouldFindRecentMeetings() {
        // Given
        var since = testStartTime.minusDays(7);
        var expectedMeetings = List.of(testMeeting);
        when(meetingRepository.findRecentMeetings(since)).thenReturn(expectedMeetings);

        // When
        var result = meetingRepository.findRecentMeetings(since);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testMeeting, result.get(0));
        verify(meetingRepository, times(1)).findRecentMeetings(since);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Test Meeting", "Unique Title"})
    @DisplayName("Should check if meeting exists by title")
    void shouldCheckIfMeetingExistsByTitle(String title) {
        // Given
        var expectedExists = title != null && !title.isEmpty();
        when(meetingRepository.existsByTitle(title)).thenReturn(expectedExists);

        // When
        var result = meetingRepository.existsByTitle(title);

        // Then
        assertEquals(expectedExists, result);
        verify(meetingRepository, times(1)).existsByTitle(title);
    }

    @Test
    @DisplayName("Should find all distinct statuses")
    void shouldFindAllDistinctStatuses() {
        // Given
        var expectedStatuses = Arrays.asList(MeetingSession.MeetingStatus.values());
        when(meetingRepository.findAllStatuses()).thenReturn(expectedStatuses);

        // When
        var result = meetingRepository.findAllStatuses();

        // Then
        assertNotNull(result);
        assertEquals(expectedStatuses.size(), result.size());
        assertTrue(result.containsAll(expectedStatuses));
        verify(meetingRepository, times(1)).findAllStatuses();
    }

    @Test
    @DisplayName("Should handle filters with null parameters")
    void shouldHandleFiltersWithNullParameters() {
        // Given
        var expectedPage = new PageImpl<>(Collections.emptyList(), testPageable, 0);
        when(meetingRepository.findByFilters(null, null, null, testPageable)).thenReturn(expectedPage);

        // When
        var result = meetingRepository.findByFilters(null, null, null, testPageable);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(meetingRepository, times(1)).findByFilters(null, null, null, testPageable);
    }

    @Test
    @DisplayName("Should handle filters with mixed null and non-null parameters")
    void shouldHandleFiltersWithMixedNullAndNonNullParameters() {
        // Given
        var status = MeetingSession.MeetingStatus.COMPLETED;
        var expectedPage = new PageImpl<>(List.of(testMeeting), testPageable, 1);
        when(meetingRepository.findByFilters(status, null, null, testPageable)).thenReturn(expectedPage);

        // When
        var result = meetingRepository.findByFilters(status, null, null, testPageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testMeeting, result.getContent().get(0));
        verify(meetingRepository, times(1)).findByFilters(status, null, null, testPageable);
    }

    @Test
    @DisplayName("Should handle empty status list for findByStatusIn")
    void shouldHandleEmptyStatusListForFindByStatusIn() {
        // Given
        var emptyStatuses = Collections.<MeetingSession.MeetingStatus>emptyList();
        when(meetingRepository.findByStatusIn(emptyStatuses)).thenReturn(Collections.emptyList());

        // When
        var result = meetingRepository.findByStatusIn(emptyStatuses);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(meetingRepository, times(1)).findByStatusIn(emptyStatuses);
    }

    @Test
    @DisplayName("Should handle pagination with zero page size")
    void shouldHandlePaginationWithZeroPageSize() {
        // Given
        var zeroSizePageable = PageRequest.of(0, 0);
        var expectedPage = new PageImpl<>(Collections.emptyList(), zeroSizePageable, 0);
        when(meetingRepository.findByStatus(MeetingSession.MeetingStatus.COMPLETED, zeroSizePageable))
                .thenReturn(expectedPage);

        // When
        var result = meetingRepository.findByStatus(MeetingSession.MeetingStatus.COMPLETED, zeroSizePageable);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(meetingRepository, times(1)).findByStatus(MeetingSession.MeetingStatus.COMPLETED, zeroSizePageable);
    }

    @Test
    @DisplayName("Should handle date range with same start and end dates")
    void shouldHandleDateRangeWithSameStartAndEndDates() {
        // Given
        var sameDate = testStartTime;
        var expectedMeetings = List.of(testMeeting);
        when(meetingRepository.findByStartTimeBetween(sameDate, sameDate)).thenReturn(expectedMeetings);

        // When
        var result = meetingRepository.findByStartTimeBetween(sameDate, sameDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testMeeting, result.get(0));
        verify(meetingRepository, times(1)).findByStartTimeBetween(sameDate, sameDate);
    }

    @Test
    @DisplayName("Should handle reversed date range")
    void shouldHandleReversedDateRange() {
        // Given
        var startDate = testEndTime;
        var endDate = testStartTime;
        when(meetingRepository.findByStartTimeBetween(startDate, endDate)).thenReturn(Collections.emptyList());

        // When
        var result = meetingRepository.findByStartTimeBetween(startDate, endDate);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(meetingRepository, times(1)).findByStartTimeBetween(startDate, endDate);
    }

    @Test
    @DisplayName("Should verify repository interface extends JpaRepository")
    void shouldVerifyRepositoryInterfaceExtendsJpaRepository() {
        // Given & When & Then
        assertTrue(meetingRepository instanceof JpaRepository);
        assertDoesNotThrow(() -> {
            // Test that all JpaRepository methods are available
            meetingRepository.save(testMeeting);
            meetingRepository.findById(testMeetingId);
            meetingRepository.findAll();
            meetingRepository.count();
            meetingRepository.delete(testMeeting);
        });
    }

    @Test
    @DisplayName("Should handle large number of meetings in results")
    void shouldHandleLargeNumberOfMeetingsInResults() {
        // Given
        var largeMeetingList = new ArrayList<MeetingSession>();
        for (int i = 0; i < 1000; i++) {
            var meeting = new MeetingSession("Meeting " + i);
            meeting.setId(UUID.randomUUID());
            largeMeetingList.add(meeting);
        }
        when(meetingRepository.findByStatus(MeetingSession.MeetingStatus.COMPLETED)).thenReturn(largeMeetingList);

        // When
        var result = meetingRepository.findByStatus(MeetingSession.MeetingStatus.COMPLETED);

        // Then
        assertNotNull(result);
        assertEquals(1000, result.size());
        verify(meetingRepository, times(1)).findByStatus(MeetingSession.MeetingStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should handle very long search strings")
    void shouldHandleVeryLongSearchStrings() {
        // Given
        var veryLongTitle = "a".repeat(1000);
        when(meetingRepository.findByTitleContainingIgnoreCase(veryLongTitle)).thenReturn(Optional.empty());

        // When
        var result = meetingRepository.findByTitleContainingIgnoreCase(veryLongTitle);

        // Then
        assertFalse(result.isPresent());
        verify(meetingRepository, times(1)).findByTitleContainingIgnoreCase(veryLongTitle);
    }

    @Test
    @DisplayName("Should handle special characters in search strings")
    void shouldHandleSpecialCharactersInSearchStrings() {
        // Given
        var titleWithSpecialChars = "Meeting & Review: Q4'23 \"Final\" (Draft)";
        when(meetingRepository.findByTitleContainingIgnoreCase(titleWithSpecialChars)).thenReturn(Optional.of(testMeeting));

        // When
        var result = meetingRepository.findByTitleContainingIgnoreCase(titleWithSpecialChars);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testMeeting, result.get());
        verify(meetingRepository, times(1)).findByTitleContainingIgnoreCase(titleWithSpecialChars);
    }
}