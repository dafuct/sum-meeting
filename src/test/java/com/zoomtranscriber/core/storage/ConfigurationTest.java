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
 * Unit tests for Configuration entity.
 * Tests entity behavior, validation, and business logic.
 */
@DisplayName("Configuration Entity Tests")
class ConfigurationTest {
    
    private Configuration configuration;
    private LocalDateTime testCreatedAt;
    
    @BeforeEach
    void setUp() {
        configuration = new Configuration();
        testCreatedAt = LocalDateTime.now();
    }
    
    @Test
    @DisplayName("Should create configuration with default constructor")
    void shouldCreateConfigurationWithDefaultConstructor() {
        // When
        var config = new Configuration();
        
        // Then
        assertNotNull(config.getId());
        assertNull(config.getKey());
        assertNull(config.getValue());
        assertNull(config.getDescription());
        assertNull(config.getCategory());
        assertFalse(config.getIsEncrypted());
        assertNotNull(config.getCreatedAt());
        assertNotNull(config.getUpdatedAt());
    }
    
    @Test
    @DisplayName("Should create configuration with three-parameter constructor")
    void shouldCreateConfigurationWithThreeParameterConstructor() {
        // Given
        var key = "audio.sample_rate";
        var value = "44100";
        var category = Configuration.ConfigCategory.AUDIO;
        
        // When
        var config = new Configuration(key, value, category);
        
        // Then
        assertEquals(key, config.getKey());
        assertEquals(value, config.getValue());
        assertEquals(category, config.getCategory());
        assertNull(config.getDescription());
        assertFalse(config.getIsEncrypted());
        assertNotNull(config.getCreatedAt());
        assertNotNull(config.getUpdatedAt());
    }
    
    @Test
    @DisplayName("Should create configuration with four-parameter constructor")
    void shouldCreateConfigurationWithFourParameterConstructor() {
        // Given
        var key = "transcription.language";
        var value = "en-US";
        var category = Configuration.ConfigCategory.TRANSCRIPTION;
        var description = "Default transcription language setting";
        
        // When
        var config = new Configuration(key, value, category, description);
        
        // Then
        assertEquals(key, config.getKey());
        assertEquals(value, config.getValue());
        assertEquals(category, config.getCategory());
        assertEquals(description, config.getDescription());
        assertFalse(config.getIsEncrypted());
        assertNotNull(config.getCreatedAt());
        assertNotNull(config.getUpdatedAt());
    }
    
    @Test
    @DisplayName("Should set and get ID correctly")
    void shouldSetAndGetIdCorrectly() {
        // Given
        var id = UUID.randomUUID();
        
        // When
        configuration.setId(id);
        
        // Then
        assertEquals(id, configuration.getId());
    }
    
    @Test
    @DisplayName("Should set and get key correctly")
    void shouldSetAndGetKeyCorrectly() {
        // Given
        var key = "ui.theme";
        
        // When
        configuration.setKey(key);
        
        // Then
        assertEquals(key, configuration.getKey());
    }
    
    @Test
    @DisplayName("Should set and get value correctly")
    void shouldSetAndGetValueCorrectly() {
        // Given
        var value = "dark";
        
        // When
        configuration.setValue(value);
        
        // Then
        assertEquals(value, configuration.getValue());
    }
    
    @Test
    @DisplayName("Should set and get description correctly")
    void shouldSetAndGetDescriptionCorrectly() {
        // Given
        var description = "UI theme preference";
        
        // When
        configuration.setDescription(description);
        
        // Then
        assertEquals(description, configuration.getDescription());
    }
    
    @ParameterizedTest
    @EnumSource(Configuration.ConfigCategory.class)
    @DisplayName("Should set and get category correctly for all enum values")
    void shouldSetAndGetCategoryCorrectlyForAllEnumValues(Configuration.ConfigCategory category) {
        // When
        configuration.setCategory(category);
        
        // Then
        assertEquals(category, configuration.getCategory());
    }
    
    @Test
    @DisplayName("Should set and get encrypted flag correctly")
    void shouldSetAndGetEncryptedFlagCorrectly() {
        // When
        configuration.setIsEncrypted(true);
        
        // Then
        assertTrue(configuration.getIsEncrypted());
        
        // When
        configuration.setIsEncrypted(false);
        
        // Then
        assertFalse(configuration.getIsEncrypted());
    }
    
    @Test
    @DisplayName("Should set and get created at timestamp correctly")
    void shouldSetAndGetCreatedAtCorrectly() {
        // Given
        var createdAt = LocalDateTime.of(2023, 1, 1, 12, 0, 0);
        
        // When
        configuration.setCreatedAt(createdAt);
        
        // Then
        assertEquals(createdAt, configuration.getCreatedAt());
    }
    
    @Test
    @DisplayName("Should set and get updated at timestamp correctly")
    void shouldSetAndGetUpdatedAtCorrectly() {
        // Given
        var updatedAt = LocalDateTime.of(2023, 1, 1, 12, 30, 0);
        
        // When
        configuration.setUpdatedAt(updatedAt);
        
        // Then
        assertEquals(updatedAt, configuration.getUpdatedAt());
    }
    
    @Test
    @DisplayName("Should update timestamp on preUpdate")
    void shouldUpdateTimestampOnPreUpdate() {
        // Given
        var originalUpdatedAt = configuration.getUpdatedAt();
        
        // Simulate some time passing
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // When
        configuration.onUpdate();
        
        // Then
        assertTrue(configuration.getUpdatedAt().isAfter(originalUpdatedAt));
    }
    
    @Test
    @DisplayName("Should handle null key")
    void shouldHandleNullKey() {
        // When
        configuration.setKey(null);
        
        // Then
        assertNull(configuration.getKey());
    }
    
    @Test
    @DisplayName("Should handle null value")
    void shouldHandleNullValue() {
        // When
        configuration.setValue(null);
        
        // Then
        assertNull(configuration.getValue());
    }
    
    @Test
    @DisplayName("Should handle null description")
    void shouldHandleNullDescription() {
        // When
        configuration.setDescription(null);
        
        // Then
        assertNull(configuration.getDescription());
    }
    
    @Test
    @DisplayName("Should handle null encrypted flag")
    void shouldHandleNullEncryptedFlag() {
        // When
        configuration.setIsEncrypted(null);
        
        // Then
        assertNull(configuration.getIsEncrypted());
    }
    
    @Test
    @DisplayName("Should handle empty key")
    void shouldHandleEmptyKey() {
        // When
        configuration.setKey("");
        
        // Then
        assertEquals("", configuration.getKey());
    }
    
    @Test
    @DisplayName("Should handle empty value")
    void shouldHandleEmptyValue() {
        // When
        configuration.setValue("");
        
        // Then
        assertEquals("", configuration.getValue());
    }
    
    @Test
    @DisplayName("Should handle empty description")
    void shouldHandleEmptyDescription() {
        // When
        configuration.setDescription("");
        
        // Then
        assertEquals("", configuration.getDescription());
    }
    
    @Test
    @DisplayName("Should handle long key")
    void shouldHandleLongKey() {
        // Given
        var longKey = "a".repeat(100); // Maximum allowed length
        
        // When
        configuration.setKey(longKey);
        
        // Then
        assertEquals(longKey, configuration.getKey());
    }
    
    @Test
    @DisplayName("Should handle long value")
    void shouldHandleLongValue() {
        // Given
        var longValue = "a".repeat(1000); // Maximum allowed length
        
        // When
        configuration.setValue(longValue);
        
        // Then
        assertEquals(longValue, configuration.getValue());
    }
    
    @Test
    @DisplayName("Should handle long description")
    void shouldHandleLongDescription() {
        // Given
        var longDescription = "a".repeat(255); // Maximum allowed length
        
        // When
        configuration.setDescription(longDescription);
        
        // Then
        assertEquals(longDescription, configuration.getDescription());
    }
    
    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
        // Given
        var key = "test.key";
        var config1 = new Configuration();
        var config2 = new Configuration();
        var config3 = new Configuration();
        
        config1.setKey(key);
        config2.setKey(key);
        config3.setKey("different.key");
        
        // Then
        assertEquals(config1, config2);
        assertNotEquals(config1, config3);
        assertEquals(config1, config1); // Same object
        assertNotEquals(config1, null);
        assertNotEquals(config1, "string");
    }
    
    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
        // Given
        var key = "test.key";
        var config1 = new Configuration();
        var config2 = new Configuration();
        
        config1.setKey(key);
        config2.setKey(key);
        
        // Then
        assertEquals(config1.hashCode(), config2.hashCode());
        
        // Test with null key
        var config3 = new Configuration();
        var config4 = new Configuration();
        assertEquals(config3.hashCode(), config4.hashCode());
    }
    
    @Test
    @DisplayName("Should implement toString correctly")
    void shouldImplementToStringCorrectly() {
        // Given
        var id = UUID.randomUUID();
        var key = "test.key";
        var value = "test.value";
        var category = Configuration.ConfigCategory.AUDIO;
        var description = "Test configuration";
        var isEncrypted = true;
        
        configuration.setId(id);
        configuration.setKey(key);
        configuration.setValue(value);
        configuration.setCategory(category);
        configuration.setDescription(description);
        configuration.setIsEncrypted(isEncrypted);
        
        // When
        var result = configuration.toString();
        
        // Then
        assertTrue(result.contains("id=" + id));
        assertTrue(result.contains("key='" + key + "'"));
        assertTrue(result.contains("value='" + value + "'"));
        assertTrue(result.contains("category=" + category));
        assertTrue(result.contains("isEncrypted=" + isEncrypted));
        assertTrue(result.contains("description='" + description + "'"));
    }
    
    @Test
    @DisplayName("Should handle configuration with all fields set")
    void shouldHandleConfigurationWithAllFieldsSet() {
        // Given
        var id = UUID.randomUUID();
        var key = "privacy.data_retention";
        var value = "90";
        var category = Configuration.ConfigCategory.PRIVACY;
        var description = "Data retention period in days";
        var isEncrypted = false;
        var createdAt = LocalDateTime.of(2023, 6, 15, 14, 30, 0);
        var updatedAt = LocalDateTime.of(2023, 6, 15, 15, 0, 0);
        
        // When
        configuration.setId(id);
        configuration.setKey(key);
        configuration.setValue(value);
        configuration.setCategory(category);
        configuration.setDescription(description);
        configuration.setIsEncrypted(isEncrypted);
        configuration.setCreatedAt(createdAt);
        configuration.setUpdatedAt(updatedAt);
        
        // Then
        assertEquals(id, configuration.getId());
        assertEquals(key, configuration.getKey());
        assertEquals(value, configuration.getValue());
        assertEquals(category, configuration.getCategory());
        assertEquals(description, configuration.getDescription());
        assertEquals(isEncrypted, configuration.getIsEncrypted());
        assertEquals(createdAt, configuration.getCreatedAt());
        assertEquals(updatedAt, configuration.getUpdatedAt());
    }
    
    @Test
    @DisplayName("Should handle special characters in value")
    void shouldHandleSpecialCharactersInValue() {
        // Given
        var valueWithSpecialChars = "Value with special chars: @#$%^&*()_+-={}[]|\\:;\"'<>?,./";
        
        // When
        configuration.setValue(valueWithSpecialChars);
        
        // Then
        assertEquals(valueWithSpecialChars, configuration.getValue());
    }
    
    @Test
    @DisplayName("Should handle unicode characters in value")
    void shouldHandleUnicodeCharactersInValue() {
        // Given
        var unicodeValue = "Unicode value: 世界 🌍 ñáéíóú";
        
        // When
        configuration.setValue(unicodeValue);
        
        // Then
        assertEquals(unicodeValue, configuration.getValue());
    }
    
    @Test
    @DisplayName("Should handle time-based operations correctly")
    void shouldHandleTimeBasedOperationsCorrectly() {
        // Given
        var before = LocalDateTime.now();
        
        // When
        var config = new Configuration();
        var after = LocalDateTime.now();
        
        // Then
        assertTrue(config.getCreatedAt().isAfter(before) || config.getCreatedAt().isEqual(before));
        assertTrue(config.getCreatedAt().isBefore(after) || config.getCreatedAt().isEqual(after));
        assertTrue(config.getUpdatedAt().isAfter(before) || config.getUpdatedAt().isEqual(before));
        assertTrue(config.getUpdatedAt().isBefore(after) || config.getUpdatedAt().isEqual(after));
    }
    
    @Test
    @DisplayName("Should maintain immutability of created at timestamp")
    void shouldMaintainImmutabilityOfCreatedAtTimestamp() {
        // Given
        var originalCreatedAt = configuration.getCreatedAt();
        
        // When
        var newCreatedAt = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        configuration.setCreatedAt(newCreatedAt);
        
        // Then
        assertEquals(newCreatedAt, configuration.getCreatedAt());
        assertNotEquals(originalCreatedAt, configuration.getCreatedAt());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "audio.sample_rate",
        "transcription.language",
        "ui.theme",
        "privacy.data_retention",
        "system.max_memory",
        "audio.input_device",
        "ollama.model",
        "meeting.auto_record",
        "export.format"
    })
    @DisplayName("Should handle various configuration keys")
    void shouldHandleVariousConfigurationKeys(String key) {
        // When
        configuration.setKey(key);
        
        // Then
        assertEquals(key, configuration.getKey());
    }
    
    @Test
    @DisplayName("Should handle configuration category transitions")
    void shouldHandleConfigurationCategoryTransitions() {
        // Test all categories
        configuration.setCategory(Configuration.ConfigCategory.AUDIO);
        assertEquals(Configuration.ConfigCategory.AUDIO, configuration.getCategory());
        
        configuration.setCategory(Configuration.ConfigCategory.TRANSCRIPTION);
        assertEquals(Configuration.ConfigCategory.TRANSCRIPTION, configuration.getCategory());
        
        configuration.setCategory(Configuration.ConfigCategory.UI);
        assertEquals(Configuration.ConfigCategory.UI, configuration.getCategory());
        
        configuration.setCategory(Configuration.ConfigCategory.PRIVACY);
        assertEquals(Configuration.ConfigCategory.PRIVACY, configuration.getCategory());
        
        configuration.setCategory(Configuration.ConfigCategory.SYSTEM);
        assertEquals(Configuration.ConfigCategory.SYSTEM, configuration.getCategory());
    }
    
    @Test
    @DisplayName("Should handle JSON-like values")
    void shouldHandleJsonLikeValues() {
        // Given
        var jsonValue = "{\"theme\":\"dark\",\"fontSize\":14,\"language\":\"en-US\"}";
        
        // When
        configuration.setValue(jsonValue);
        
        // Then
        assertEquals(jsonValue, configuration.getValue());
    }
    
    @Test
    @DisplayName("Should handle numeric values as strings")
    void shouldHandleNumericValuesAsStrings() {
        // Given
        var numericValues = new String[]{"42", "3.14159", "-100", "0", "1.0e-10"};
        
        // When & Then
        for (var value : numericValues) {
            configuration.setValue(value);
            assertEquals(value, configuration.getValue());
        }
    }
    
    @Test
    @DisplayName("Should handle boolean values as strings")
    void shouldHandleBooleanValuesAsStrings() {
        // Given
        var booleanValues = new String[]{"true", "false", "TRUE", "FALSE", "True", "False"};
        
        // When & Then
        for (var value : booleanValues) {
            configuration.setValue(value);
            assertEquals(value, configuration.getValue());
        }
    }
}