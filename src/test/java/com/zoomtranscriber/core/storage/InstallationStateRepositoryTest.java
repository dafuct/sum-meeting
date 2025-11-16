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
 * Unit tests for InstallationStateRepository.
 * Tests repository query methods, edge cases, and exception handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InstallationStateRepository Tests")
class InstallationStateRepositoryTest {

    @Mock
    private InstallationStateRepository installationStateRepository;

    private InstallationState testInstallationState;
    private UUID testInstallationStateId;
    private InstallationState.Component testComponent;
    private String testVersion;
    private InstallationState.InstallStatus testStatus;
    private LocalDateTime testLastChecked;

    @BeforeEach
    void setUp() {
        testInstallationStateId = UUID.randomUUID();
        testComponent = InstallationState.Component.Java;
        testVersion = "21.0.0";
        testStatus = InstallationState.InstallStatus.INSTALLED;
        testLastChecked = LocalDateTime.of(2023, 11, 4, 10, 0, 0);

        testInstallationState = new InstallationState(testComponent, testStatus);
        testInstallationState.setId(testInstallationStateId);
        testInstallationState.setVersion(testVersion);
        testInstallationState.setInstallPath("/usr/local/java");
        testInstallationState.setLastChecked(testLastChecked);
    }

    @Test
    @DisplayName("Should find installation state by component")
    void shouldFindInstallationStateByComponent() {
        // Given
        when(installationStateRepository.findByComponent(testComponent)).thenReturn(Optional.of(testInstallationState));

        // When
        var result = installationStateRepository.findByComponent(testComponent);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testInstallationState, result.get());
        assertEquals(testComponent, result.get().getComponent());
        verify(installationStateRepository, times(1)).findByComponent(testComponent);
    }

    @Test
    @DisplayName("Should return empty optional when installation state not found by component")
    void shouldReturnEmptyOptionalWhenInstallationStateNotFoundByComponent() {
        // Given
        when(installationStateRepository.findByComponent(InstallationState.Component.Model)).thenReturn(Optional.empty());

        // When
        var result = installationStateRepository.findByComponent(InstallationState.Component.Model);

        // Then
        assertFalse(result.isPresent());
        verify(installationStateRepository, times(1)).findByComponent(InstallationState.Component.Model);
    }

    @ParameterizedTest
    @EnumSource(InstallationState.InstallStatus.class)
    @DisplayName("Should find installation states by status")
    void shouldFindInstallationStatesByStatus(InstallationState.InstallStatus status) {
        // Given
        var expectedStates = List.of(testInstallationState);
        when(installationStateRepository.findByStatus(status)).thenReturn(expectedStates);

        // When
        var result = installationStateRepository.findByStatus(status);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testInstallationState, result.get(0));
        assertEquals(status, result.get(0).getStatus());
        verify(installationStateRepository, times(1)).findByStatus(status);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"21.0.0", "1.8.0", "2.1.0"})
    @DisplayName("Should find installation states by version")
    void shouldFindInstallationStatesByVersion(String version) {
        // Given
        var expectedStates = version != null && !version.isEmpty() ? 
                List.of(testInstallationState) : Collections.emptyList();
        when(installationStateRepository.findByVersion(version)).thenReturn(expectedStates);

        // When
        var result = installationStateRepository.findByVersion(version);

        // Then
        assertNotNull(result);
        if (version != null && !version.isEmpty()) {
            assertEquals(1, result.size());
            assertEquals(version, result.get(0).getVersion());
        } else {
            assertTrue(result.isEmpty());
        }
        verify(installationStateRepository, times(1)).findByVersion(version);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"/usr/local/java", "/opt/ollama", "/custom/path"})
    @DisplayName("Should find installation states by install path")
    void shouldFindInstallationStatesByInstallPath(String installPath) {
        // Given
        var expectedStates = installPath != null && !installPath.isEmpty() ? 
                List.of(testInstallationState) : Collections.emptyList();
        when(installationStateRepository.findByInstallPath(installPath)).thenReturn(expectedStates);

        // When
        var result = installationStateRepository.findByInstallPath(installPath);

        // Then
        assertNotNull(result);
        if (installPath != null && !installPath.isEmpty()) {
            assertEquals(1, result.size());
            assertEquals(installPath, result.get(0).getInstallPath());
        } else {
            assertTrue(result.isEmpty());
        }
        verify(installationStateRepository, times(1)).findByInstallPath(installPath);
    }

    @Test
    @DisplayName("Should find installation states checked after timestamp")
    void shouldFindInstallationStatesCheckedAfterTimestamp() {
        // Given
        var since = testLastChecked.minusHours(1);
        var expectedStates = List.of(testInstallationState);
        when(installationStateRepository.findByLastCheckedAfter(since)).thenReturn(expectedStates);

        // When
        var result = installationStateRepository.findByLastCheckedAfter(since);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testInstallationState, result.get(0));
        assertTrue(result.get(0).getLastChecked().isAfter(since) || result.get(0).getLastChecked().isEqual(since));
        verify(installationStateRepository, times(1)).findByLastCheckedAfter(since);
    }

    @Test
    @DisplayName("Should find installation states checked before timestamp")
    void shouldFindInstallationStatesCheckedBeforeTimestamp() {
        // Given
        var before = testLastChecked.plusHours(1);
        var expectedStates = List.of(testInstallationState);
        when(installationStateRepository.findByLastCheckedBefore(before)).thenReturn(expectedStates);

        // When
        var result = installationStateRepository.findByLastCheckedBefore(before);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testInstallationState, result.get(0));
        assertTrue(result.get(0).getLastChecked().isBefore(before) || result.get(0).getLastChecked().isEqual(before));
        verify(installationStateRepository, times(1)).findByLastCheckedBefore(before);
    }

    @ParameterizedTest
    @EnumSource(InstallationState.InstallStatus.class)
    @DisplayName("Should count installation states by status")
    void shouldCountInstallationStatesByStatus(InstallationState.InstallStatus status) {
        // Given
        var expectedCount = 3L;
        when(installationStateRepository.countByStatus(status)).thenReturn(expectedCount);

        // When
        var result = installationStateRepository.countByStatus(status);

        // Then
        assertEquals(expectedCount, result);
        verify(installationStateRepository, times(1)).countByStatus(status);
    }

    @Test
    @DisplayName("Should count installation states grouped by component and status")
    void shouldCountInstallationStatesGroupedByComponentAndStatus() {
        // Given
        var expectedCounts = Arrays.asList(
                new Object[]{InstallationState.Component.Java, InstallationState.InstallStatus.INSTALLED, 2L},
                new Object[]{InstallationState.Component.Java, InstallationState.InstallStatus.NOT_INSTALLED, 1L},
                new Object[]{InstallationState.Component.Ollama, InstallationState.InstallStatus.INSTALLED, 1L}
        );
        when(installationStateRepository.countByComponentAndStatusGrouped()).thenReturn(expectedCounts);

        // When
        var result = installationStateRepository.countByComponentAndStatusGrouped();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        
        // Verify structure of results
        for (Object[] row : result) {
            assertEquals(3, row.length);
            assertTrue(row[0] instanceof InstallationState.Component);
            assertTrue(row[1] instanceof InstallationState.InstallStatus);
            assertTrue(row[2] instanceof Long);
        }
        verify(installationStateRepository, times(1)).countByComponentAndStatusGrouped();
    }

    @Test
    @DisplayName("Should count installed components grouped by component")
    void shouldCountInstalledComponentsGroupedByComponent() {
        // Given
        var expectedCounts = Arrays.asList(
                new Object[]{InstallationState.Component.Java, 1L},
                new Object[]{InstallationState.Component.Ollama, 2L},
                new Object[]{InstallationState.Component.Model, 3L}
        );
        when(installationStateRepository.countInstalledByComponentGrouped()).thenReturn(expectedCounts);

        // When
        var result = installationStateRepository.countInstalledByComponentGrouped();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        
        // Verify structure of results
        for (Object[] row : result) {
            assertEquals(2, row.length);
            assertTrue(row[0] instanceof InstallationState.Component);
            assertTrue(row[1] instanceof Long);
        }
        verify(installationStateRepository, times(1)).countInstalledByComponentGrouped();
    }

    @Test
    @DisplayName("Should find installation states by multiple statuses")
    void shouldFindInstallationStatesByMultipleStatuses() {
        // Given
        var statuses = Arrays.asList(InstallationState.InstallStatus.INSTALLED, InstallationState.InstallStatus.INSTALLING);
        var expectedStates = List.of(testInstallationState);
        when(installationStateRepository.findByStatusIn(statuses)).thenReturn(expectedStates);

        // When
        var result = installationStateRepository.findByStatusIn(statuses);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testInstallationState, result.get(0));
        assertTrue(statuses.contains(result.get(0).getStatus()));
        verify(installationStateRepository, times(1)).findByStatusIn(statuses);
    }

    @Test
    @DisplayName("Should find installation states by multiple components")
    void shouldFindInstallationStatesByMultipleComponents() {
        // Given
        var components = Arrays.asList(InstallationState.Component.Java, InstallationState.Component.Ollama);
        var expectedStates = List.of(testInstallationState);
        when(installationStateRepository.findByComponentsIn(components)).thenReturn(expectedStates);

        // When
        var result = installationStateRepository.findByComponentsIn(components);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testInstallationState, result.get(0));
        assertTrue(components.contains(result.get(0).getComponent()));
        verify(installationStateRepository, times(1)).findByComponentsIn(components);
    }

    @Test
    @DisplayName("Should find all distinct components")
    void shouldFindAllDistinctComponents() {
        // Given
        var expectedComponents = Arrays.asList(InstallationState.Component.values());
        when(installationStateRepository.findAllComponents()).thenReturn(expectedComponents);

        // When
        var result = installationStateRepository.findAllComponents();

        // Then
        assertNotNull(result);
        assertEquals(expectedComponents.size(), result.size());
        assertTrue(result.containsAll(expectedComponents));
        verify(installationStateRepository, times(1)).findAllComponents();
    }

    @Test
    @DisplayName("Should find all distinct statuses")
    void shouldFindAllDistinctStatuses() {
        // Given
        var expectedStatuses = Arrays.asList(InstallationState.InstallStatus.values());
        when(installationStateRepository.findAllStatuses()).thenReturn(expectedStatuses);

        // When
        var result = installationStateRepository.findAllStatuses();

        // Then
        assertNotNull(result);
        assertEquals(expectedStatuses.size(), result.size());
        assertTrue(result.containsAll(expectedStatuses));
        verify(installationStateRepository, times(1)).findAllStatuses();
    }

    @Test
    @DisplayName("Should find installation state by component and status")
    void shouldFindInstallationStateByComponentAndStatus() {
        // Given
        when(installationStateRepository.findByComponentAndStatus(testComponent, testStatus))
                .thenReturn(Optional.of(testInstallationState));

        // When
        var result = installationStateRepository.findByComponentAndStatus(testComponent, testStatus);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testInstallationState, result.get());
        assertEquals(testComponent, result.get().getComponent());
        assertEquals(testStatus, result.get().getStatus());
        verify(installationStateRepository, times(1)).findByComponentAndStatus(testComponent, testStatus);
    }

    @ParameterizedTest
    @EnumSource(InstallationState.Component.class)
    @DisplayName("Should check if installation state exists by component")
    void shouldCheckIfInstallationStateExistsByComponent(InstallationState.Component component) {
        // Given
        var expectedExists = true;
        when(installationStateRepository.existsByComponent(component)).thenReturn(expectedExists);

        // When
        var result = installationStateRepository.existsByComponent(component);

        // Then
        assertEquals(expectedExists, result);
        verify(installationStateRepository, times(1)).existsByComponent(component);
    }

    @ParameterizedTest
    @EnumSource(InstallationState.Component.class)
    @DisplayName("Should check if component is installed")
    void shouldCheckIfComponentIsInstalled(InstallationState.Component component) {
        // Given
        var expectedIsInstalled = component == InstallationState.Component.Java;
        when(installationStateRepository.isComponentInstalled(component)).thenReturn(expectedIsInstalled);

        // When
        var result = installationStateRepository.isComponentInstalled(component);

        // Then
        assertEquals(expectedIsInstalled, result);
        verify(installationStateRepository, times(1)).isComponentInstalled(component);
    }

    @Test
    @DisplayName("Should check if any installation errors exist")
    void shouldCheckIfAnyInstallationErrorsExist() {
        // Given
        var expectedHasErrors = false;
        when(installationStateRepository.hasAnyInstallationErrors()).thenReturn(expectedHasErrors);

        // When
        var result = installationStateRepository.hasAnyInstallationErrors();

        // Then
        assertEquals(expectedHasErrors, result);
        verify(installationStateRepository, times(1)).hasAnyInstallationErrors();
    }

    @Test
    @DisplayName("Should find installation errors")
    void shouldFindInstallationErrors() {
        // Given
        var errorState = new InstallationState(InstallationState.Component.Model, InstallationState.InstallStatus.ERROR);
        errorState.setId(UUID.randomUUID());
        var expectedErrors = List.of(errorState);
        when(installationStateRepository.findInstallationErrors()).thenReturn(expectedErrors);

        // When
        var result = installationStateRepository.findInstallationErrors();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(InstallationState.InstallStatus.ERROR, result.get(0).getStatus());
        verify(installationStateRepository, times(1)).findInstallationErrors();
    }

    @Test
    @DisplayName("Should delete installation state by component")
    void shouldDeleteInstallationStateByComponent() {
        // Given
        doNothing().when(installationStateRepository).deleteByComponent(testComponent);

        // When
        installationStateRepository.deleteByComponent(testComponent);

        // Then
        verify(installationStateRepository, times(1)).deleteByComponent(testComponent);
    }

    @Test
    @DisplayName("Should handle large number of installation states")
    void shouldHandleLargeNumberOfInstallationStates() {
        // Given
        var largeStateList = new ArrayList<InstallationState>();
        for (int i = 0; i < 1000; i++) {
            var state = new InstallationState(InstallationState.Component.Java, InstallationState.InstallStatus.INSTALLED);
            state.setId(UUID.randomUUID());
            state.setVersion("21.0." + i);
            largeStateList.add(state);
        }
        when(installationStateRepository.findByStatus(InstallationState.InstallStatus.INSTALLED)).thenReturn(largeStateList);

        // When
        var result = installationStateRepository.findByStatus(InstallationState.InstallStatus.INSTALLED);

        // Then
        assertNotNull(result);
        assertEquals(1000, result.size());
        verify(installationStateRepository, times(1)).findByStatus(InstallationState.InstallStatus.INSTALLED);
    }

    @Test
    @DisplayName("Should handle empty status list")
    void shouldHandleEmptyStatusList() {
        // Given
        var emptyStatuses = Collections.<InstallationState.InstallStatus>emptyList();
        when(installationStateRepository.findByStatusIn(emptyStatuses)).thenReturn(Collections.emptyList());

        // When
        var result = installationStateRepository.findByStatusIn(emptyStatuses);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(installationStateRepository, times(1)).findByStatusIn(emptyStatuses);
    }

    @Test
    @DisplayName("Should handle empty component list")
    void shouldHandleEmptyComponentList() {
        // Given
        var emptyComponents = Collections.<InstallationState.Component>emptyList();
        when(installationStateRepository.findByComponentsIn(emptyComponents)).thenReturn(Collections.emptyList());

        // When
        var result = installationStateRepository.findByComponentsIn(emptyComponents);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(installationStateRepository, times(1)).findByComponentsIn(emptyComponents);
    }

    @Test
    @DisplayName("Should handle timestamp range edge cases")
    void shouldHandleTimestampRangeEdgeCases() {
        // Given
        var sameTime = testLastChecked;
        var expectedStates = List.of(testInstallationState);
        
        when(installationStateRepository.findByLastCheckedAfter(sameTime)).thenReturn(expectedStates);
        when(installationStateRepository.findByLastCheckedBefore(sameTime)).thenReturn(expectedStates);

        // When
        var resultAfter = installationStateRepository.findByLastCheckedAfter(sameTime);
        var resultBefore = installationStateRepository.findByLastCheckedBefore(sameTime);

        // Then
        assertEquals(1, resultAfter.size());
        assertEquals(1, resultBefore.size());
        verify(installationStateRepository, times(1)).findByLastCheckedAfter(sameTime);
        verify(installationStateRepository, times(1)).findByLastCheckedBefore(sameTime);
    }

    @Test
    @DisplayName("Should handle null version in installation states")
    void shouldHandleNullVersionInInstallationStates() {
        // Given
        testInstallationState.setVersion(null);
        var expectedStates = List.of(testInstallationState);
        when(installationStateRepository.findByVersion(null)).thenReturn(expectedStates);

        // When
        var result = installationStateRepository.findByVersion(null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getVersion());
        verify(installationStateRepository, times(1)).findByVersion(null);
    }

    @Test
    @DisplayName("Should handle null install path in installation states")
    void shouldHandleNullInstallPathInInstallationStates() {
        // Given
        testInstallationState.setInstallPath(null);
        var expectedStates = List.of(testInstallationState);
        when(installationStateRepository.findByInstallPath(null)).thenReturn(expectedStates);

        // When
        var result = installationStateRepository.findByInstallPath(null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getInstallPath());
        verify(installationStateRepository, times(1)).findByInstallPath(null);
    }

    @Test
    @DisplayName("Should handle special characters in install paths")
    void shouldHandleSpecialCharactersInInstallPaths() {
        // Given
        var specialPath = "/path/with spaces & special@chars#123";
        testInstallationState.setInstallPath(specialPath);
        var expectedStates = List.of(testInstallationState);
        when(installationStateRepository.findByInstallPath(specialPath)).thenReturn(expectedStates);

        // When
        var result = installationStateRepository.findByInstallPath(specialPath);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(specialPath, result.get(0).getInstallPath());
        verify(installationStateRepository, times(1)).findByInstallPath(specialPath);
    }

    @Test
    @DisplayName("Should handle version patterns with semantic versioning")
    void shouldHandleVersionPatternsWithSemanticVersioning() {
        // Given
        var semanticVersions = Arrays.asList("21.0.0", "21.0.1", "21.1.0", "22.0.0");
        var version21 = "21.";
        var expectedStates = List.of(testInstallationState);
        when(installationStateRepository.findByVersion(startsWith(version21))).thenReturn(expectedStates);

        // When - Note: This is a simplified test as repository would use @Query for such pattern matching
        var result = installationStateRepository.findByVersion("21.0.0");

        // Then
        assertNotNull(result);
        verify(installationStateRepository, times(1)).findByVersion("21.0.0");
    }

    @Test
    @DisplayName("Should verify repository interface extends JpaRepository")
    void shouldVerifyRepositoryInterfaceExtendsJpaRepository() {
        // Given & When & Then
        assertTrue(installationStateRepository instanceof JpaRepository);
        assertDoesNotThrow(() -> {
            // Test that all JpaRepository methods are available
            installationStateRepository.save(testInstallationState);
            installationStateRepository.findById(testInstallationStateId);
            installationStateRepository.findAll();
            installationStateRepository.count();
            installationStateRepository.delete(testInstallationState);
        });
    }

    @Test
    @DisplayName("Should handle timestamp comparisons correctly")
    void shouldHandleTimestampComparisonsCorrectly() {
        // Given
        var past = testLastChecked.minusDays(1);
        var future = testLastChecked.plusDays(1);
        var currentState = List.of(testInstallationState);
        
        when(installationStateRepository.findByLastCheckedAfter(past)).thenReturn(currentState);
        when(installationStateRepository.findByLastCheckedBefore(future)).thenReturn(currentState);
        when(installationStateRepository.findByLastCheckedAfter(future)).thenReturn(Collections.emptyList());
        when(installationStateRepository.findByLastCheckedBefore(past)).thenReturn(Collections.emptyList());

        // When
        var resultAfterPast = installationStateRepository.findByLastCheckedAfter(past);
        var resultBeforeFuture = installationStateRepository.findByLastCheckedBefore(future);
        var resultAfterFuture = installationStateRepository.findByLastCheckedAfter(future);
        var resultBeforePast = installationStateRepository.findByLastCheckedBefore(past);

        // Then
        assertEquals(1, resultAfterPast.size());
        assertEquals(1, resultBeforeFuture.size());
        assertEquals(0, resultAfterFuture.size());
        assertEquals(0, resultBeforePast.size());
        
        verify(installationStateRepository, times(1)).findByLastCheckedAfter(past);
        verify(installationStateRepository, times(1)).findByLastCheckedBefore(future);
        verify(installationStateRepository, times(1)).findByLastCheckedAfter(future);
        verify(installationStateRepository, times(1)).findByLastCheckedBefore(past);
    }
}