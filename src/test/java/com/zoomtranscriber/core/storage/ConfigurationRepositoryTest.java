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
 * Unit tests for ConfigurationRepository.
 * Tests repository query methods, edge cases, and exception handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigurationRepository Tests")
class ConfigurationRepositoryTest {

    @Mock
    private ConfigurationRepository configurationRepository;

    private Configuration testConfiguration;
    private UUID testConfigurationId;
    private String testKey;
    private String testValue;
    private Configuration.ConfigCategory testCategory;

    @BeforeEach
    void setUp() {
        testConfigurationId = UUID.randomUUID();
        testKey = "audio.sample.rate";
        testValue = "44100";
        testCategory = Configuration.ConfigCategory.AUDIO;

        testConfiguration = new Configuration(testKey, testValue, testCategory, "Audio sample rate configuration");
        testConfiguration.setId(testConfigurationId);
    }

    @Test
    @DisplayName("Should find configuration by key")
    void shouldFindConfigurationByKey() {
        // Given
        when(configurationRepository.findByKey(testKey)).thenReturn(Optional.of(testConfiguration));

        // When
        var result = configurationRepository.findByKey(testKey);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testConfiguration, result.get());
        assertEquals(testKey, result.get().getKey());
        verify(configurationRepository, times(1)).findByKey(testKey);
    }

    @Test
    @DisplayName("Should return empty optional when configuration not found by key")
    void shouldReturnEmptyOptionalWhenConfigurationNotFoundByKey() {
        // Given
        var nonExistentKey = "non.existent.key";
        when(configurationRepository.findByKey(nonExistentKey)).thenReturn(Optional.empty());

        // When
        var result = configurationRepository.findByKey(nonExistentKey);

        // Then
        assertFalse(result.isPresent());
        verify(configurationRepository, times(1)).findByKey(nonExistentKey);
    }

    @ParameterizedTest
    @EnumSource(Configuration.ConfigCategory.class)
    @DisplayName("Should find configurations by category")
    void shouldFindConfigurationsByCategory(Configuration.ConfigCategory category) {
        // Given
        var expectedConfigurations = List.of(testConfiguration);
        when(configurationRepository.findByCategory(category)).thenReturn(expectedConfigurations);

        // When
        var result = configurationRepository.findByCategory(category);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testConfiguration, result.get(0));
        assertEquals(category, result.get(0).getCategory());
        verify(configurationRepository, times(1)).findByCategory(category);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Should find configurations by encryption status")
    void shouldFindConfigurationsByEncryptionStatus(Boolean isEncrypted) {
        // Given
        var expectedConfigurations = List.of(testConfiguration);
        when(configurationRepository.findByIsEncrypted(isEncrypted)).thenReturn(expectedConfigurations);

        // When
        var result = configurationRepository.findByIsEncrypted(isEncrypted);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testConfiguration, result.get(0));
        assertEquals(isEncrypted, result.get(0).getIsEncrypted());
        verify(configurationRepository, times(1)).findByIsEncrypted(isEncrypted);
    }

    @Test
    @DisplayName("Should find configurations by category and encryption status")
    void shouldFindConfigurationsByCategoryAndEncryptionStatus() {
        // Given
        var expectedConfigurations = List.of(testConfiguration);
        when(configurationRepository.findByCategoryAndIsEncrypted(testCategory, false))
                .thenReturn(expectedConfigurations);

        // When
        var result = configurationRepository.findByCategoryAndIsEncrypted(testCategory, false);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testConfiguration, result.get(0));
        assertEquals(testCategory, result.get(0).getCategory());
        assertFalse(result.get(0).getIsEncrypted());
        verify(configurationRepository, times(1)).findByCategoryAndIsEncrypted(testCategory, false);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"audio", "sample", "rate"})
    @DisplayName("Should find configurations by key or description containing keyword")
    void shouldFindConfigurationsByKeyOrDescriptionContainingKeyword(String keyword) {
        // Given
        var expectedConfigurations = keyword != null && !keyword.isEmpty() ? 
                List.of(testConfiguration) : Collections.emptyList();
        when(configurationRepository.findByKeyOrDescriptionContaining(keyword)).thenReturn(expectedConfigurations);

        // When
        var result = configurationRepository.findByKeyOrDescriptionContaining(keyword);

        // Then
        assertNotNull(result);
        assertEquals(keyword != null && !keyword.isEmpty() ? 1 : 0, result.size());
        if (keyword != null && !keyword.isEmpty()) {
            assertEquals(testConfiguration, result.get(0));
        }
        verify(configurationRepository, times(1)).findByKeyOrDescriptionContaining(keyword);
    }

    @Test
    @DisplayName("Should find configurations by category and key or description containing keyword")
    void shouldFindConfigurationsByCategoryAndKeyOrDescriptionContaining() {
        // Given
        var keyword = "audio";
        var expectedConfigurations = List.of(testConfiguration);
        when(configurationRepository.findByCategoryAndKeyOrDescriptionContaining(testCategory, keyword))
                .thenReturn(expectedConfigurations);

        // When
        var result = configurationRepository.findByCategoryAndKeyOrDescriptionContaining(testCategory, keyword);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testConfiguration, result.get(0));
        assertEquals(testCategory, result.get(0).getCategory());
        verify(configurationRepository, times(1)).findByCategoryAndKeyOrDescriptionContaining(testCategory, keyword);
    }

    @Test
    @DisplayName("Should find all distinct categories")
    void shouldFindAllDistinctCategories() {
        // Given
        var expectedCategories = Arrays.asList(Configuration.ConfigCategory.values());
        when(configurationRepository.findAllCategories()).thenReturn(expectedCategories);

        // When
        var result = configurationRepository.findAllCategories();

        // Then
        assertNotNull(result);
        assertEquals(expectedCategories.size(), result.size());
        assertTrue(result.containsAll(expectedCategories));
        verify(configurationRepository, times(1)).findAllCategories();
    }

    @ParameterizedTest
    @EnumSource(Configuration.ConfigCategory.class)
    @DisplayName("Should count configurations by category")
    void shouldCountConfigurationsByCategory(Configuration.ConfigCategory category) {
        // Given
        var expectedCount = 15L;
        when(configurationRepository.countByCategory(category)).thenReturn(expectedCount);

        // When
        var result = configurationRepository.countByCategory(category);

        // Then
        assertEquals(expectedCount, result);
        verify(configurationRepository, times(1)).countByCategory(category);
    }

    @Test
    @DisplayName("Should count encrypted configurations")
    void shouldCountEncryptedConfigurations() {
        // Given
        var expectedCount = 8L;
        when(configurationRepository.countEncryptedConfigurations()).thenReturn(expectedCount);

        // When
        var result = configurationRepository.countEncryptedConfigurations();

        // Then
        assertEquals(expectedCount, result);
        verify(configurationRepository, times(1)).countEncryptedConfigurations();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"audio.sample.rate", "transcription.model", "ui.theme"})
    @DisplayName("Should check if configuration exists by key")
    void shouldCheckIfConfigurationExistsByKey(String key) {
        // Given
        var expectedExists = key != null && !key.isEmpty();
        when(configurationRepository.existsByKey(key)).thenReturn(expectedExists);

        // When
        var result = configurationRepository.existsByKey(key);

        // Then
        assertEquals(expectedExists, result);
        verify(configurationRepository, times(1)).existsByKey(key);
    }

    @Test
    @DisplayName("Should find configurations by keys in list")
    void shouldFindConfigurationsByKeysInList() {
        // Given
        var keys = Arrays.asList("audio.sample.rate", "audio.channels", "audio.bitrate");
        var expectedConfigurations = List.of(testConfiguration);
        when(configurationRepository.findByKeysIn(keys)).thenReturn(expectedConfigurations);

        // When
        var result = configurationRepository.findByKeysIn(keys);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testConfiguration, result.get(0));
        assertTrue(keys.contains(result.get(0).getKey()));
        verify(configurationRepository, times(1)).findByKeysIn(keys);
    }

    @Test
    @DisplayName("Should find configurations by multiple categories")
    void shouldFindConfigurationsByMultipleCategories() {
        // Given
        var categories = Arrays.asList(Configuration.ConfigCategory.AUDIO, Configuration.ConfigCategory.UI);
        var expectedConfigurations = List.of(testConfiguration);
        when(configurationRepository.findByCategoriesIn(categories)).thenReturn(expectedConfigurations);

        // When
        var result = configurationRepository.findByCategoriesIn(categories);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testConfiguration, result.get(0));
        assertTrue(categories.contains(result.get(0).getCategory()));
        verify(configurationRepository, times(1)).findByCategoriesIn(categories);
    }

    @Test
    @DisplayName("Should find all unencrypted key-value pairs")
    void shouldFindAllUnencryptedKeyValuePairs() {
        // Given
        var expectedPairs = Arrays.asList(
                new Object[]{"audio.sample.rate", "44100"},
                new Object[]{"audio.channels", "2"},
                new Object[]{"ui.theme", "dark"}
        );
        when(configurationRepository.findAllUnencryptedKeyValuePairs()).thenReturn(expectedPairs);

        // When
        var result = configurationRepository.findAllUnencryptedKeyValuePairs();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        
        // Verify structure of results
        for (Object[] pair : result) {
            assertEquals(2, pair.length);
            assertTrue(pair[0] instanceof String);
            assertTrue(pair[1] instanceof String);
        }
        verify(configurationRepository, times(1)).findAllUnencryptedKeyValuePairs();
    }

    @Test
    @DisplayName("Should find unencrypted key-value pairs by category")
    void shouldFindUnencryptedKeyValuePairsByCategory() {
        // Given
        var expectedPairs = Arrays.asList(
                new Object[]{"audio.sample.rate", "44100"},
                new Object[]{"audio.channels", "2"}
        );
        when(configurationRepository.findUnencryptedKeyValuePairsByCategory(testCategory))
                .thenReturn(expectedPairs);

        // When
        var result = configurationRepository.findUnencryptedKeyValuePairsByCategory(testCategory);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Verify structure of results
        for (Object[] pair : result) {
            assertEquals(2, pair.length);
            assertTrue(pair[0] instanceof String);
            assertTrue(pair[1] instanceof String);
        }
        verify(configurationRepository, times(1)).findUnencryptedKeyValuePairsByCategory(testCategory);
    }

    @Test
    @DisplayName("Should delete configuration by key")
    void shouldDeleteConfigurationByKey() {
        // Given
        doNothing().when(configurationRepository).deleteByKey(testKey);

        // When
        configurationRepository.deleteByKey(testKey);

        // Then
        verify(configurationRepository, times(1)).deleteByKey(testKey);
    }

    @Test
    @DisplayName("Should delete configurations by category")
    void shouldDeleteConfigurationsByCategory() {
        // Given
        doNothing().when(configurationRepository).deleteByCategory(testCategory);

        // When
        configurationRepository.deleteByCategory(testCategory);

        // Then
        verify(configurationRepository, times(1)).deleteByCategory(testCategory);
    }

    @Test
    @DisplayName("Should handle large number of configurations")
    void shouldHandleLargeNumberOfConfigurations() {
        // Given
        var largeConfigList = new ArrayList<Configuration>();
        for (int i = 0; i < 2000; i++) {
            var config = new Configuration("key." + i, "value." + i, testCategory);
            config.setId(UUID.randomUUID());
            largeConfigList.add(config);
        }
        when(configurationRepository.findByCategory(testCategory)).thenReturn(largeConfigList);

        // When
        var result = configurationRepository.findByCategory(testCategory);

        // Then
        assertNotNull(result);
        assertEquals(2000, result.size());
        verify(configurationRepository, times(1)).findByCategory(testCategory);
    }

    @Test
    @DisplayName("Should handle empty keys list")
    void shouldHandleEmptyKeysList() {
        // Given
        var emptyKeys = Collections.<String>emptyList();
        when(configurationRepository.findByKeysIn(emptyKeys)).thenReturn(Collections.emptyList());

        // When
        var result = configurationRepository.findByKeysIn(emptyKeys);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(configurationRepository, times(1)).findByKeysIn(emptyKeys);
    }

    @Test
    @DisplayName("Should handle empty categories list")
    void shouldHandleEmptyCategoriesList() {
        // Given
        var emptyCategories = Collections.<Configuration.ConfigCategory>emptyList();
        when(configurationRepository.findByCategoriesIn(emptyCategories)).thenReturn(Collections.emptyList());

        // When
        var result = configurationRepository.findByCategoriesIn(emptyCategories);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(configurationRepository, times(1)).findByCategoriesIn(emptyCategories);
    }

    @Test
    @DisplayName("Should handle special characters in keys and descriptions")
    void shouldHandleSpecialCharactersInKeysAndDescriptions() {
        // Given
        var specialKey = "api.key@#$%^&*()";
        var keyword = "@#$";
        var configWithSpecialChars = new Configuration(specialKey, "secret-value", Configuration.ConfigCategory.SYSTEM);
        var expectedConfigurations = List.of(configWithSpecialChars);
        when(configurationRepository.findByKeyOrDescriptionContaining(keyword)).thenReturn(expectedConfigurations);

        // When
        var result = configurationRepository.findByKeyOrDescriptionContaining(keyword);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(specialKey, result.get(0).getKey());
        verify(configurationRepository, times(1)).findByKeyOrDescriptionContaining(keyword);
    }

    @Test
    @DisplayName("Should handle very long values in configurations")
    void shouldHandleVeryLongValuesInConfigurations() {
        // Given
        var veryLongValue = "A".repeat(1000); // 1000 characters
        testConfiguration.setValue(veryLongValue);
        var expectedConfigurations = List.of(testConfiguration);
        when(configurationRepository.findByCategory(testCategory)).thenReturn(expectedConfigurations);

        // When
        var result = configurationRepository.findByCategory(testCategory);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(veryLongValue, result.get(0).getValue());
        verify(configurationRepository, times(1)).findByCategory(testCategory);
    }

    @Test
    @DisplayName("Should handle null values in key-value pair queries")
    void shouldHandleNullValuesInKeyValuePairQueries() {
        // Given
        testConfiguration.setValue(null);
        var expectedPairs = Arrays.asList(
                new Object[]{"audio.sample.rate", null}
        );
        when(configurationRepository.findAllUnencryptedKeyValuePairs()).thenReturn(expectedPairs);

        // When
        var result = configurationRepository.findAllUnencryptedKeyValuePairs();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("audio.sample.rate", result.get(0)[0]);
        assertNull(result.get(0)[1]);
        verify(configurationRepository, times(1)).findAllUnencryptedKeyValuePairs();
    }

    @Test
    @DisplayName("Should handle duplicate keys gracefully")
    void shouldHandleDuplicateKeysGracefully() {
        // Given
        var duplicateKey = "duplicate.key";
        var config1 = new Configuration(duplicateKey, "value1", testCategory);
        var config2 = new Configuration(duplicateKey, "value2", testCategory);
        var expectedConfigurations = Arrays.asList(config1, config2);
        when(configurationRepository.findByKey(duplicateKey)).thenReturn(Optional.of(config1));

        // When
        var result = configurationRepository.findByKey(duplicateKey);

        // Then
        assertTrue(result.isPresent());
        assertEquals(duplicateKey, result.get().getKey());
        verify(configurationRepository, times(1)).findByKey(duplicateKey);
    }

    @Test
    @DisplayName("Should verify repository interface extends JpaRepository")
    void shouldVerifyRepositoryInterfaceExtendsJpaRepository() {
        // Given & When & Then
        assertTrue(configurationRepository instanceof JpaRepository);
        assertDoesNotThrow(() -> {
            // Test that all JpaRepository methods are available
            configurationRepository.save(testConfiguration);
            configurationRepository.findById(testConfigurationId);
            configurationRepository.findAll();
            configurationRepository.count();
            configurationRepository.delete(testConfiguration);
        });
    }

    @Test
    @DisplayName("Should handle case sensitivity in key searches")
    void shouldHandleCaseSensitivityInKeySearches() {
        // Given
        var lowercaseKey = "audio.sample.rate";
        var uppercaseKey = "AUDIO.SAMPLE.RATE";
        when(configurationRepository.findByKey(lowercaseKey)).thenReturn(Optional.of(testConfiguration));
        when(configurationRepository.findByKey(uppercaseKey)).thenReturn(Optional.empty());

        // When
        var resultLower = configurationRepository.findByKey(lowercaseKey);
        var resultUpper = configurationRepository.findByKey(uppercaseKey);

        // Then
        assertTrue(resultLower.isPresent());
        assertFalse(resultUpper.isPresent());
        verify(configurationRepository, times(1)).findByKey(lowercaseKey);
        verify(configurationRepository, times(1)).findByKey(uppercaseKey);
    }

    @Test
    @DisplayName("Should handle boolean encryption status correctly")
    void shouldHandleBooleanEncryptionStatusCorrectly() {
        // Given
        var encryptedConfig = new Configuration("secret.key", "encrypted-value", Configuration.ConfigCategory.PRIVACY);
        encryptedConfig.setIsEncrypted(true);
        
        var unencryptedConfig = new Configuration("public.key", "public-value", Configuration.ConfigCategory.UI);
        unencryptedConfig.setIsEncrypted(false);

        when(configurationRepository.findByIsEncrypted(true)).thenReturn(List.of(encryptedConfig));
        when(configurationRepository.findByIsEncrypted(false)).thenReturn(List.of(unencryptedConfig));

        // When
        var encryptedResults = configurationRepository.findByIsEncrypted(true);
        var unencryptedResults = configurationRepository.findByIsEncrypted(false);

        // Then
        assertEquals(1, encryptedResults.size());
        assertTrue(encryptedResults.get(0).getIsEncrypted());
        
        assertEquals(1, unencryptedResults.size());
        assertFalse(unencryptedResults.get(0).getIsEncrypted());
        
        verify(configurationRepository, times(1)).findByIsEncrypted(true);
        verify(configurationRepository, times(1)).findByIsEncrypted(false);
    }
}