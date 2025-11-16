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
 * Unit tests for InstallationState entity.
 * Tests entity behavior, validation, and business logic.
 */
@DisplayName("InstallationState Entity Tests")
class InstallationStateTest {
    
    private InstallationState installationState;
    private LocalDateTime testCreatedAt;
    
    @BeforeEach
    void setUp() {
        installationState = new InstallationState();
        testCreatedAt = LocalDateTime.now();
    }
    
    @Test
    @DisplayName("Should create installation state with default constructor")
    void shouldCreateInstallationStateWithDefaultConstructor() {
        // When
        var state = new InstallationState();
        
        // Then
        assertNotNull(state.getId());
        assertNull(state.getComponent());
        assertNull(state.getVersion());
        assertEquals(InstallationState.InstallStatus.NOT_INSTALLED, state.getStatus());
        assertNull(state.getInstallPath());
        assertNull(state.getLastChecked());
        assertNotNull(state.getCreatedAt());
        assertNotNull(state.getUpdatedAt());
    }
    
    @Test
    @DisplayName("Should create installation state with component constructor")
    void shouldCreateInstallationStateWithComponentConstructor() {
        // Given
        var component = InstallationState.Component.Java;
        
        // When
        var state = new InstallationState(component);
        
        // Then
        assertEquals(component, state.getComponent());
        assertEquals(InstallationState.InstallStatus.NOT_INSTALLED, state.getStatus());
        assertNotNull(state.getCreatedAt());
        assertNotNull(state.getUpdatedAt());
    }
    
    @Test
    @DisplayName("Should create installation state with component and status constructor")
    void shouldCreateInstallationStateWithComponentAndStatusConstructor() {
        // Given
        var component = InstallationState.Component.Ollama;
        var status = InstallationState.InstallStatus.INSTALLED;
        
        // When
        var state = new InstallationState(component, status);
        
        // Then
        assertEquals(component, state.getComponent());
        assertEquals(status, state.getStatus());
        assertNotNull(state.getCreatedAt());
        assertNotNull(state.getUpdatedAt());
    }
    
    @Test
    @DisplayName("Should set and get ID correctly")
    void shouldSetAndGetIdCorrectly() {
        // Given
        var id = UUID.randomUUID();
        
        // When
        installationState.setId(id);
        
        // Then
        assertEquals(id, installationState.getId());
    }
    
    @ParameterizedTest
    @EnumSource(InstallationState.Component.class)
    @DisplayName("Should set and get component correctly for all enum values")
    void shouldSetAndGetComponentCorrectlyForAllEnumValues(InstallationState.Component component) {
        // When
        installationState.setComponent(component);
        
        // Then
        assertEquals(component, installationState.getComponent());
    }
    
    @Test
    @DisplayName("Should set and get version correctly")
    void shouldSetAndGetVersionCorrectly() {
        // Given
        var version = "21.0.2";
        
        // When
        installationState.setVersion(version);
        
        // Then
        assertEquals(version, installationState.getVersion());
    }
    
    @ParameterizedTest
    @EnumSource(InstallationState.InstallStatus.class)
    @DisplayName("Should set and get status correctly for all enum values")
    void shouldSetAndGetStatusCorrectlyForAllEnumValues(InstallationState.InstallStatus status) {
        // When
        installationState.setStatus(status);
        
        // Then
        assertEquals(status, installationState.getStatus());
    }
    
    @Test
    @DisplayName("Should set and get install path correctly")
    void shouldSetAndGetInstallPathCorrectly() {
        // Given
        var installPath = "/usr/local/java";
        
        // When
        installationState.setInstallPath(installPath);
        
        // Then
        assertEquals(installPath, installationState.getInstallPath());
    }
    
    @Test
    @DisplayName("Should set and get last checked timestamp correctly")
    void shouldSetAndGetLastCheckedCorrectly() {
        // Given
        var lastChecked = LocalDateTime.of(2023, 6, 15, 14, 30, 0);
        
        // When
        installationState.setLastChecked(lastChecked);
        
        // Then
        assertEquals(lastChecked, installationState.getLastChecked());
    }
    
    @Test
    @DisplayName("Should set and get created at timestamp correctly")
    void shouldSetAndGetCreatedAtCorrectly() {
        // Given
        var createdAt = LocalDateTime.of(2023, 1, 1, 12, 0, 0);
        
        // When
        installationState.setCreatedAt(createdAt);
        
        // Then
        assertEquals(createdAt, installationState.getCreatedAt());
    }
    
    @Test
    @DisplayName("Should set and get updated at timestamp correctly")
    void shouldSetAndGetUpdatedAtCorrectly() {
        // Given
        var updatedAt = LocalDateTime.of(2023, 1, 1, 12, 30, 0);
        
        // When
        installationState.setUpdatedAt(updatedAt);
        
        // Then
        assertEquals(updatedAt, installationState.getUpdatedAt());
    }
    
    @Test
    @DisplayName("Should update timestamp and last checked on preUpdate")
    void shouldUpdateTimestampAndLastCheckedOnPreUpdate() {
        // Given
        var originalUpdatedAt = installationState.getUpdatedAt();
        var originalLastChecked = installationState.getLastChecked();
        
        // Simulate some time passing
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // When
        installationState.onUpdate();
        
        // Then
        assertTrue(installationState.getUpdatedAt().isAfter(originalUpdatedAt));
        assertTrue(installationState.getLastChecked().isAfter(originalLastChecked));
    }
    
    @Test
    @DisplayName("Should handle null version")
    void shouldHandleNullVersion() {
        // When
        installationState.setVersion(null);
        
        // Then
        assertNull(installationState.getVersion());
    }
    
    @Test
    @DisplayName("Should handle null install path")
    void shouldHandleNullInstallPath() {
        // When
        installationState.setInstallPath(null);
        
        // Then
        assertNull(installationState.getInstallPath());
    }
    
    @Test
    @DisplayName("Should handle null last checked")
    void shouldHandleNullLastChecked() {
        // When
        installationState.setLastChecked(null);
        
        // Then
        assertNull(installationState.getLastChecked());
    }
    
    @Test
    @DisplayName("Should handle empty version")
    void shouldHandleEmptyVersion() {
        // When
        installationState.setVersion("");
        
        // Then
        assertEquals("", installationState.getVersion());
    }
    
    @Test
    @DisplayName("Should handle empty install path")
    void shouldHandleEmptyInstallPath() {
        // When
        installationState.setInstallPath("");
        
        // Then
        assertEquals("", installationState.getInstallPath());
    }
    
    @Test
    @DisplayName("Should handle long version")
    void shouldHandleLongVersion() {
        // Given
        var longVersion = "a".repeat(50); // Maximum allowed length
        
        // When
        installationState.setVersion(longVersion);
        
        // Then
        assertEquals(longVersion, installationState.getVersion());
    }
    
    @Test
    @DisplayName("Should handle long install path")
    void shouldHandleLongInstallPath() {
        // Given
        var longInstallPath = "/very/long/installation/path/that/exceeds/normal/length/limits/and/contains/many/subdirectories";
        
        // When
        installationState.setInstallPath(longInstallPath);
        
        // Then
        assertEquals(longInstallPath, installationState.getInstallPath());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "21.0.2",
        "1.0.0",
        "2.1.3-beta",
        "3.0.0-rc1",
        "latest",
        "v1.2.3",
        "21.0.2+35"
    })
    @DisplayName("Should handle various version formats")
    void shouldHandleVariousVersionFormats(String version) {
        // When
        installationState.setVersion(version);
        
        // Then
        assertEquals(version, installationState.getVersion());
    }
    
    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
        // Given
        var component = InstallationState.Component.Java;
        var state1 = new InstallationState();
        var state2 = new InstallationState();
        var state3 = new InstallationState();
        
        state1.setComponent(component);
        state2.setComponent(component);
        state3.setComponent(InstallationState.Component.Ollama);
        
        // Then
        assertEquals(state1, state2);
        assertNotEquals(state1, state3);
        assertEquals(state1, state1); // Same object
        assertNotEquals(state1, null);
        assertNotEquals(state1, "string");
    }
    
    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
        // Given
        var component = InstallationState.Component.Java;
        var state1 = new InstallationState();
        var state2 = new InstallationState();
        
        state1.setComponent(component);
        state2.setComponent(component);
        
        // Then
        assertEquals(state1.hashCode(), state2.hashCode());
        
        // Test with null component
        var state3 = new InstallationState();
        var state4 = new InstallationState();
        assertEquals(state3.hashCode(), state4.hashCode());
    }
    
    @Test
    @DisplayName("Should implement toString correctly")
    void shouldImplementToStringCorrectly() {
        // Given
        var id = UUID.randomUUID();
        var component = InstallationState.Component.Java;
        var version = "21.0.2";
        var status = InstallationState.InstallStatus.INSTALLED;
        var installPath = "/usr/local/java";
        var lastChecked = LocalDateTime.of(2023, 6, 15, 14, 30, 0);
        
        installationState.setId(id);
        installationState.setComponent(component);
        installationState.setVersion(version);
        installationState.setStatus(status);
        installationState.setInstallPath(installPath);
        installationState.setLastChecked(lastChecked);
        
        // When
        var result = installationState.toString();
        
        // Then
        assertTrue(result.contains("id=" + id));
        assertTrue(result.contains("component=" + component));
        assertTrue(result.contains("version='" + version + "'"));
        assertTrue(result.contains("status=" + status));
        assertTrue(result.contains("installPath='" + installPath + "'"));
        assertTrue(result.contains("lastChecked=" + lastChecked));
    }
    
    @Test
    @DisplayName("Should handle installation state with all fields set")
    void shouldHandleInstallationStateWithAllFieldsSet() {
        // Given
        var id = UUID.randomUUID();
        var component = InstallationState.Component.Model;
        var version = "qwen2.5:0.5b";
        var status = InstallationState.InstallStatus.INSTALLING;
        var installPath = "/home/user/.ollama/models";
        var lastChecked = LocalDateTime.of(2023, 6, 15, 14, 30, 0);
        var createdAt = LocalDateTime.of(2023, 6, 15, 10, 0, 0);
        var updatedAt = LocalDateTime.of(2023, 6, 15, 15, 0, 0);
        
        // When
        installationState.setId(id);
        installationState.setComponent(component);
        installationState.setVersion(version);
        installationState.setStatus(status);
        installationState.setInstallPath(installPath);
        installationState.setLastChecked(lastChecked);
        installationState.setCreatedAt(createdAt);
        installationState.setUpdatedAt(updatedAt);
        
        // Then
        assertEquals(id, installationState.getId());
        assertEquals(component, installationState.getComponent());
        assertEquals(version, installationState.getVersion());
        assertEquals(status, installationState.getStatus());
        assertEquals(installPath, installationState.getInstallPath());
        assertEquals(lastChecked, installationState.getLastChecked());
        assertEquals(createdAt, installationState.getCreatedAt());
        assertEquals(updatedAt, installationState.getUpdatedAt());
    }
    
    @Test
    @DisplayName("Should handle component transitions")
    void shouldHandleComponentTransitions() {
        // Test all components
        installationState.setComponent(InstallationState.Component.Java);
        assertEquals(InstallationState.Component.Java, installationState.getComponent());
        
        installationState.setComponent(InstallationState.Component.Ollama);
        assertEquals(InstallationState.Component.Ollama, installationState.getComponent());
        
        installationState.setComponent(InstallationState.Component.Model);
        assertEquals(InstallationState.Component.Model, installationState.getComponent());
    }
    
    @Test
    @DisplayName("Should handle status transitions")
    void shouldHandleStatusTransitions() {
        // Test typical installation flow
        installationState.setStatus(InstallationState.InstallStatus.NOT_INSTALLED);
        assertEquals(InstallationState.InstallStatus.NOT_INSTALLED, installationState.getStatus());
        
        installationState.setStatus(InstallationState.InstallStatus.INSTALLING);
        assertEquals(InstallationState.InstallStatus.INSTALLING, installationState.getStatus());
        
        installationState.setStatus(InstallationState.InstallStatus.INSTALLED);
        assertEquals(InstallationState.InstallStatus.INSTALLED, installationState.getStatus());
        
        installationState.setStatus(InstallationState.InstallStatus.ERROR);
        assertEquals(InstallationState.InstallStatus.ERROR, installationState.getStatus());
    }
    
    @Test
    @DisplayName("Should handle time-based operations correctly")
    void shouldHandleTimeBasedOperationsCorrectly() {
        // Given
        var before = LocalDateTime.now();
        
        // When
        var state = new InstallationState();
        var after = LocalDateTime.now();
        
        // Then
        assertTrue(state.getCreatedAt().isAfter(before) || state.getCreatedAt().isEqual(before));
        assertTrue(state.getCreatedAt().isBefore(after) || state.getCreatedAt().isEqual(after));
        assertTrue(state.getUpdatedAt().isAfter(before) || state.getUpdatedAt().isEqual(before));
        assertTrue(state.getUpdatedAt().isBefore(after) || state.getUpdatedAt().isEqual(after));
    }
    
    @Test
    @DisplayName("Should maintain immutability of created at timestamp")
    void shouldMaintainImmutabilityOfCreatedAtTimestamp() {
        // Given
        var originalCreatedAt = installationState.getCreatedAt();
        
        // When
        var newCreatedAt = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        installationState.setCreatedAt(newCreatedAt);
        
        // Then
        assertEquals(newCreatedAt, installationState.getCreatedAt());
        assertNotEquals(originalCreatedAt, installationState.getCreatedAt());
    }
    
    @Test
    @DisplayName("Should handle different install path formats")
    void shouldHandleDifferentInstallPathFormats() {
        // Given
        var installPaths = new String[]{
            "/usr/local/java",
            "C:\\Program Files\\Java",
            "/home/user/.ollama",
            "/opt/ollama",
            "./local/install",
            "~/Applications/ZoomTranscriber.app"
        };
        
        // When & Then
        for (var installPath : installPaths) {
            installationState.setInstallPath(installPath);
            assertEquals(installPath, installationState.getInstallPath());
        }
    }
    
    @Test
    @DisplayName("Should handle special characters in install path")
    void shouldHandleSpecialCharactersInInstallPath() {
        // Given
        var pathWithSpecialChars = "/path/with-special_chars_123/and.dots";
        
        // When
        installationState.setInstallPath(pathWithSpecialChars);
        
        // Then
        assertEquals(pathWithSpecialChars, installationState.getInstallPath());
    }
    
    @Test
    @DisplayName("Should handle relative and absolute paths")
    void shouldHandleRelativeAndAbsolutePaths() {
        // Given
        var relativePath = "./local/install";
        var absolutePath = "/usr/local/bin";
        
        // When
        installationState.setInstallPath(relativePath);
        
        // Then
        assertEquals(relativePath, installationState.getInstallPath());
        
        // When
        installationState.setInstallPath(absolutePath);
        
        // Then
        assertEquals(absolutePath, installationState.getInstallPath());
    }
    
    @Test
    @DisplayName("Should handle semantic versioning")
    void shouldHandleSemanticVersioning() {
        // Given
        var semanticVersions = new String[]{
            "1.0.0",
            "1.1.0",
            "1.1.5",
            "2.0.0",
            "2.1.3",
            "21.0.0",
            "21.0.1",
            "21.0.2"
        };
        
        // When & Then
        for (var version : semanticVersions) {
            installationState.setVersion(version);
            assertEquals(version, installationState.getVersion());
        }
    }
}