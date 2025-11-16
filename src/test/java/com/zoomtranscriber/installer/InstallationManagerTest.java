package com.zoomtranscriber.installer;

import com.zoomtranscriber.core.storage.InstallationState;
import com.zoomtranscriber.core.storage.InstallationStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InstallationManager class.
 * Tests installation management, component installation, and status tracking.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InstallationManager Tests")
class InstallationManagerTest {
    
    @Mock
    private SystemChecker systemChecker;
    
    @Mock
    private DependencyInstaller dependencyInstaller;
    
    @Mock
    private InstallationStateRepository installationStateRepository;
    
    private InstallationManager installationManager;
    private SystemChecker.SystemCheckResult successSystemCheck;
    private SystemChecker.SystemCheckResult failureSystemCheck;
    private List<InstallationState> existingStates;
    private Consumer<String> progressCallback;
    
    @BeforeEach
    void setUp() {
        installationManager = new InstallationManager(
            systemChecker, dependencyInstaller, installationStateRepository
        );
        
        // Setup test data
        successSystemCheck = createSuccessSystemCheck();
        failureSystemCheck = createFailureSystemCheck();
        existingStates = createExistingInstallationStates();
        progressCallback = mock(Consumer.class);
        
        // Setup default mock behaviors
        when(systemChecker.checkSystem()).thenReturn(successSystemCheck);
        when(installationStateRepository.findByComponent(any()))
            .thenReturn(Optional.empty());
        when(installationStateRepository.save(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }
    
    @Test
    @DisplayName("Should get installation status successfully")
    void shouldGetInstallationStatusSuccessfully() {
        // Given
        when(installationStateRepository.findAll()).thenReturn(existingStates);
        
        // When
        var result = installationManager.getInstallationStatus();
        
        // Then
        assertNotNull(result);
        assertEquals(successSystemCheck, result.systemCheck());
        assertEquals(existingStates, result.databaseStates());
        verify(systemChecker, times(1)).checkSystem();
        verify(installationStateRepository, times(1)).findAll();
    }
    
    @Test
    @DisplayName("Should install Java component successfully")
    void shouldInstallJavaComponentSuccessfully() throws Exception {
        // Given
        var expectedResult = new DependencyInstaller.InstallationResult(
            InstallationState.Component.Java,
            InstallationState.InstallStatus.INSTALLED,
            "Java 17 installed",
            "17.0.2"
        );
        var future = CompletableFuture.completedFuture(expectedResult);
        
        when(dependencyInstaller.installJava(any(Consumer.class))).thenReturn(future);
        
        // When
        var result = installationManager.installComponent(
            InstallationState.Component.Java, progressCallback
        ).get();
        
        // Then
        assertEquals(expectedResult, result);
        verify(dependencyInstaller, times(1)).installJava(progressCallback);
        verify(installationStateRepository, times(1)).save(any(InstallationState.class));
        
        // Verify the saved state has correct properties
        var stateCaptor = ArgumentCaptor.forClass(InstallationState.class);
        verify(installationStateRepository).save(stateCaptor.capture());
        var savedState = stateCaptor.getValue();
        assertEquals(InstallationState.Component.Java, savedState.getComponent());
        assertEquals(InstallationState.InstallStatus.INSTALLED, savedState.getStatus());
        assertEquals("17.0.2", savedState.getVersion());
    }
    
    @Test
    @DisplayName("Should install Ollama component successfully")
    void shouldInstallOllamaComponentSuccessfully() throws Exception {
        // Given
        var expectedResult = new DependencyInstaller.InstallationResult(
            InstallationState.Component.Ollama,
            InstallationState.InstallStatus.INSTALLED,
            "Ollama installed",
            "0.1.42"
        );
        var future = CompletableFuture.completedFuture(expectedResult);
        
        when(dependencyInstaller.installOllama(any(Consumer.class))).thenReturn(future);
        
        // When
        var result = installationManager.installComponent(
            InstallationState.Component.Ollama, progressCallback
        ).get();
        
        // Then
        assertEquals(expectedResult, result);
        verify(dependencyInstaller, times(1)).installOllama(progressCallback);
        verify(installationStateRepository, times(1)).save(any(InstallationState.class));
    }
    
    @Test
    @DisplayName("Should install Model component successfully")
    void shouldInstallModelComponentSuccessfully() throws Exception {
        // Given
        var expectedResult = new DependencyInstaller.InstallationResult(
            InstallationState.Component.Model,
            InstallationState.InstallStatus.INSTALLED,
            "Model installed",
            "qwen2.5:0.5b"
        );
        var future = CompletableFuture.completedFuture(expectedResult);
        
        when(dependencyInstaller.installModel(any(Consumer.class))).thenReturn(future);
        
        // When
        var result = installationManager.installComponent(
            InstallationState.Component.Model, progressCallback
        ).get();
        
        // Then
        assertEquals(expectedResult, result);
        verify(dependencyInstaller, times(1)).installModel(progressCallback);
        verify(installationStateRepository, times(1)).save(any(InstallationState.class));
    }
    
    @Test
    @DisplayName("Should handle component installation failure")
    void shouldHandleComponentInstallationFailure() throws Exception {
        // Given
        var expectedResult = new DependencyInstaller.InstallationResult(
            InstallationState.Component.Java,
            InstallationState.InstallStatus.FAILED,
            "Java installation failed",
            null
        );
        var future = CompletableFuture.completedFuture(expectedResult);
        
        when(dependencyInstaller.installJava(any(Consumer.class))).thenReturn(future);
        
        // When
        var result = installationManager.installComponent(
            InstallationState.Component.Java, progressCallback
        ).get();
        
        // Then
        assertEquals(expectedResult, result);
        verify(dependencyInstaller, times(1)).installJava(progressCallback);
        verify(installationStateRepository, times(1)).save(any(InstallationState.class));
        
        var stateCaptor = ArgumentCaptor.forClass(InstallationState.class);
        verify(installationStateRepository).save(stateCaptor.capture());
        var savedState = stateCaptor.getValue();
        assertEquals(InstallationState.InstallStatus.FAILED, savedState.getStatus());
        assertNull(savedState.getVersion());
    }
    
    @Test
    @DisplayName("Should install all components successfully when all missing")
    void shouldInstallAllComponentsSuccessfullyWhenAllMissing() throws Exception {
        // Given
        when(systemChecker.checkSystem()).thenReturn(failureSystemCheck);
        
        var javaResult = new DependencyInstaller.InstallationResult(
            InstallationState.Component.Java,
            InstallationState.InstallStatus.INSTALLED,
            "Java installed",
            "17.0.2"
        );
        var ollamaResult = new DependencyInstaller.InstallationResult(
            InstallationState.Component.Ollama,
            InstallationState.InstallStatus.INSTALLED,
            "Ollama installed",
            "0.1.42"
        );
        var modelResult = new DependencyInstaller.InstallationResult(
            InstallationState.Component.Model,
            InstallationState.InstallStatus.INSTALLED,
            "Model installed",
            "qwen2.5:0.5b"
        );
        
        when(dependencyInstaller.installJava(any(Consumer.class)))
            .thenReturn(CompletableFuture.completedFuture(javaResult));
        when(dependencyInstaller.installOllama(any(Consumer.class)))
            .thenReturn(CompletableFuture.completedFuture(ollamaResult));
        when(dependencyInstaller.installModel(any(Consumer.class)))
            .thenReturn(CompletableFuture.completedFuture(modelResult));
        
        // When
        var result = installationManager.installAll(progressCallback).get();
        
        // Then
        assertEquals(modelResult, result); // Last result is returned
        verify(dependencyInstaller, times(1)).installJava(progressCallback);
        verify(dependencyInstaller, times(1)).installOllama(progressCallback);
        verify(dependencyInstaller, times(1)).installModel(progressCallback);
        verify(installationStateRepository, times(3)).save(any(InstallationState.class));
    }
    
    @Test
    @DisplayName("Should skip installation when all components already installed")
    void shouldSkipInstallationWhenAllComponentsAlreadyInstalled() throws Exception {
        // Given
        when(systemChecker.checkSystem()).thenReturn(successSystemCheck);
        
        // When
        var result = installationManager.installAll(progressCallback).get();
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals("All components installed", result.message());
        verify(progressCallback).accept("All components are already installed");
        verify(dependencyInstaller, never()).installJava(any());
        verify(dependencyInstaller, never()).installOllama(any());
        verify(dependencyInstaller, never()).installModel(any());
        verify(installationStateRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should update existing installation state during component installation")
    void shouldUpdateExistingInstallationStateDuringComponentInstallation() throws Exception {
        // Given
        var existingState = new InstallationState(InstallationState.Component.Java);
        existingState.setCreatedAt(LocalDateTime.now().minusDays(1));
        existingState.setStatus(InstallationState.InstallStatus.NOT_INSTALLED);
        
        when(installationStateRepository.findByComponent(InstallationState.Component.Java))
            .thenReturn(Optional.of(existingState));
        
        var expectedResult = new DependencyInstaller.InstallationResult(
            InstallationState.Component.Java,
            InstallationState.InstallStatus.INSTALLED,
            "Java updated",
            "17.0.3"
        );
        var future = CompletableFuture.completedFuture(expectedResult);
        
        when(dependencyInstaller.installJava(any(Consumer.class))).thenReturn(future);
        
        // When
        var result = installationManager.installComponent(
            InstallationState.Component.Java, progressCallback
        ).get();
        
        // Then
        assertEquals(expectedResult, result);
        
        var stateCaptor = ArgumentCaptor.forClass(InstallationState.class);
        verify(installationStateRepository).save(stateCaptor.capture());
        var savedState = stateCaptor.getValue();
        
        // Verify existing state was updated
        assertEquals(existingState.getId(), savedState.getId());
        assertEquals(existingState.getCreatedAt(), savedState.getCreatedAt());
        assertEquals(InstallationState.InstallStatus.INSTALLED, savedState.getStatus());
        assertEquals("17.0.3", savedState.getVersion());
    }
    
    @Test
    @DisplayName("Should check system readiness correctly when ready")
    void shouldCheckSystemReadinessCorrectlyWhenReady() {
        // Given
        when(systemChecker.checkSystem()).thenReturn(successSystemCheck);
        
        // When
        var result = installationManager.isSystemReady();
        
        // Then
        assertTrue(result);
        verify(systemChecker, times(1)).checkSystem();
    }
    
    @Test
    @DisplayName("Should check system readiness correctly when not ready")
    void shouldCheckSystemReadinessCorrectlyWhenNotReady() {
        // Given
        when(systemChecker.checkSystem()).thenReturn(failureSystemCheck);
        
        // When
        var result = installationManager.isSystemReady();
        
        // Then
        assertFalse(result);
        verify(systemChecker, times(1)).checkSystem();
    }
    
    @Test
    @DisplayName("Should get missing components correctly")
    void shouldGetMissingComponentsCorrectly() {
        // Given
        var missingComponents = List.of("Java", "Ollama", "Model");
        when(systemChecker.getMissingComponents()).thenReturn(missingComponents);
        
        // When
        var result = installationManager.getMissingComponents();
        
        // Then
        assertEquals(missingComponents, result);
        verify(systemChecker, times(1)).getMissingComponents();
    }
    
    @Test
    @DisplayName("Should refresh installation status successfully")
    void shouldRefreshInstallationStatusSuccessfully() {
        // Given
        when(systemChecker.checkSystem()).thenReturn(successSystemCheck);
        when(installationStateRepository.findByComponent(any()))
            .thenReturn(Optional.of(new InstallationState(InstallationState.Component.Java)));
        
        // When
        installationManager.refreshInstallationStatus();
        
        // Then
        verify(systemChecker, times(1)).checkSystem();
        verify(installationStateRepository, atLeastOnce()).findByComponent(any());
        verify(installationStateRepository, atLeastOnce()).save(any(InstallationState.class));
    }
    
    @Test
    @DisplayName("Should handle progress callback during installation")
    void shouldHandleProgressCallbackDuringInstallation() throws Exception {
        // Given
        var progressMessages = new AtomicReference<List<String>>();
        var capturingCallback = new Consumer<String>() {
            private final List<String> messages = new java.util.ArrayList<>();
            
            @Override
            public void accept(String message) {
                messages.add(message);
                progressMessages.set(messages);
            }
        };
        
        var expectedResult = new DependencyInstaller.InstallationResult(
            InstallationState.Component.Java,
            InstallationState.InstallStatus.INSTALLED,
            "Java installed",
            "17.0.2"
        );
        var future = CompletableFuture.completedFuture(expectedResult);
        
        when(dependencyInstaller.installJava(any(Consumer.class))).thenAnswer(invocation -> {
            var callback = (Consumer<String>) invocation.getArgument(0);
            callback.accept("Installing Java...");
            callback.accept("Java installation completed");
            return future;
        });
        
        // When
        installationManager.installComponent(InstallationState.Component.Java, capturingCallback).get();
        
        // Then
        var messages = progressMessages.get();
        assertNotNull(messages);
        assertTrue(messages.contains("Installing Java..."));
        assertTrue(messages.contains("Java installation completed"));
    }
    
    @Test
    @DisplayName("Should handle installation state with error information")
    void shouldHandleInstallationStateWithErrorInformation() throws Exception {
        // Given
        var expectedResult = new DependencyInstaller.InstallationResult(
            InstallationState.Component.Java,
            InstallationState.InstallStatus.FAILED,
            "Java installation failed",
            null
        );
        var future = CompletableFuture.completedFuture(expectedResult);
        
        when(dependencyInstaller.installJava(any(Consumer.class))).thenReturn(future);
        
        // When
        installationManager.installComponent(InstallationState.Component.Java, progressCallback).get();
        
        // Then
        var stateCaptor = ArgumentCaptor.forClass(InstallationState.class);
        verify(installationStateRepository).save(stateCaptor.capture());
        var savedState = stateCaptor.getValue();
        assertEquals(InstallationState.InstallStatus.FAILED, savedState.getStatus());
        assertNull(savedState.getVersion());
        assertNotNull(savedState.getUpdatedAt());
        assertNotNull(savedState.getLastChecked());
    }
    
    @ParameterizedTest
    @EnumSource(InstallationState.Component.class)
    @DisplayName("Should install all component types successfully")
    void shouldInstallAllComponentTypesSuccessfully(InstallationState.Component component) throws Exception {
        // Given
        var expectedResult = new DependencyInstaller.InstallationResult(
            component,
            InstallationState.InstallStatus.INSTALLED,
            "Component installed",
            "1.0.0"
        );
        var future = CompletableFuture.completedFuture(expectedResult);
        
        switch (component) {
            case Java -> when(dependencyInstaller.installJava(any(Consumer.class))).thenReturn(future);
            case Ollama -> when(dependencyInstaller.installOllama(any(Consumer.class))).thenReturn(future);
            case Model -> when(dependencyInstaller.installModel(any(Consumer.class))).thenReturn(future);
        }
        
        // When
        var result = installationManager.installComponent(component, progressCallback).get();
        
        // Then
        assertEquals(expectedResult, result);
        verify(installationStateRepository, times(1)).save(any(InstallationState.class));
        
        var stateCaptor = ArgumentCaptor.forClass(InstallationState.class);
        verify(installationStateRepository).save(stateCaptor.capture());
        var savedState = stateCaptor.getValue();
        assertEquals(component, savedState.getComponent());
        assertEquals(InstallationState.InstallStatus.INSTALLED, savedState.getStatus());
        assertEquals("1.0.0", savedState.getVersion());
    }
    
    @Test
    @DisplayName("Should handle concurrent component installation")
    void shouldHandleConcurrentComponentInstallation() throws Exception {
        // Given
        var javaResult = new DependencyInstaller.InstallationResult(
            InstallationState.Component.Java,
            InstallationState.InstallStatus.INSTALLED,
            "Java installed",
            "17.0.2"
        );
        var ollamaResult = new DependencyInstaller.InstallationResult(
            InstallationState.Component.Ollama,
            InstallationState.InstallStatus.INSTALLED,
            "Ollama installed",
            "0.1.42"
        );
        
        when(dependencyInstaller.installJava(any(Consumer.class)))
            .thenReturn(CompletableFuture.completedFuture(javaResult));
        when(dependencyInstaller.installOllama(any(Consumer.class)))
            .thenReturn(CompletableFuture.completedFuture(ollamaResult));
        
        // When
        var javaFuture = installationManager.installComponent(
            InstallationState.Component.Java, progressCallback
        );
        var ollamaFuture = installationManager.installComponent(
            InstallationState.Component.Ollama, progressCallback
        );
        
        var javaResultFinal = javaFuture.get();
        var ollamaResultFinal = ollamaFuture.get();
        
        // Then
        assertEquals(javaResult, javaResultFinal);
        assertEquals(ollamaResult, ollamaResultFinal);
        verify(installationStateRepository, times(2)).save(any(InstallationState.class));
    }
    
    @Test
    @DisplayName("Should handle null progress callback gracefully")
    void shouldHandleNullProgressCallbackGracefully() throws Exception {
        // Given
        var expectedResult = new DependencyInstaller.InstallationResult(
            InstallationState.Component.Java,
            InstallationState.InstallStatus.INSTALLED,
            "Java installed",
            "17.0.2"
        );
        var future = CompletableFuture.completedFuture(expectedResult);
        
        when(dependencyInstaller.installJava(isNull())).thenReturn(future);
        
        // When
        var result = installationManager.installComponent(
            InstallationState.Component.Java, null
        ).get();
        
        // Then
        assertEquals(expectedResult, result);
        verify(dependencyInstaller, times(1)).installJava(isNull());
        verify(installationStateRepository, times(1)).save(any(InstallationState.class));
    }
    
    @Test
    @DisplayName("Should handle system checker throwing exception")
    void shouldHandleSystemCheckerThrowingException() {
        // Given
        when(systemChecker.checkSystem()).thenThrow(new RuntimeException("System check failed"));
        
        // When
        var exception = assertThrows(RuntimeException.class, 
            () -> installationManager.getInstallationStatus());
        
        // Then
        assertEquals("System check failed", exception.getMessage());
        verify(systemChecker, times(1)).checkSystem();
    }
    
    @Test
    @DisplayName("Should handle dependency installer throwing exception")
    void shouldHandleDependencyInstallerThrowingException() throws Exception {
        // Given
        when(dependencyInstaller.installJava(any(Consumer.class)))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Installation failed")));
        
        // When
        var exception = assertThrows(Exception.class, 
            () -> installationManager.installComponent(
                InstallationState.Component.Java, progressCallback
            ).get());
        
        // Then
        assertEquals("Installation failed", exception.getCause().getMessage());
        verify(dependencyInstaller, times(1)).installJava(progressCallback);
    }
    
    @Test
    @DisplayName("Should handle repository save throwing exception")
    void shouldHandleRepositorySaveThrowingException() throws Exception {
        // Given
        var expectedResult = new DependencyInstaller.InstallationResult(
            InstallationState.Component.Java,
            InstallationState.InstallStatus.INSTALLED,
            "Java installed",
            "17.0.2"
        );
        var future = CompletableFuture.completedFuture(expectedResult);
        
        when(dependencyInstaller.installJava(any(Consumer.class))).thenReturn(future);
        when(installationStateRepository.save(any()))
            .thenThrow(new RuntimeException("Database save failed"));
        
        // When
        var exception = assertThrows(RuntimeException.class, 
            () -> installationManager.installComponent(
                InstallationState.Component.Java, progressCallback
            ).get());
        
        // Then
        assertEquals("Database save failed", exception.getMessage());
        verify(installationStateRepository, times(1)).save(any(InstallationState.class));
    }
    
    // Helper methods
    
    private SystemChecker.SystemCheckResult createSuccessSystemCheck() {
        var results = List.of(
            new SystemChecker.CheckResult(
                InstallationState.Component.Java,
                InstallationState.InstallStatus.INSTALLED,
                "17.0.2",
                null
            ),
            new SystemChecker.CheckResult(
                InstallationState.Component.Ollama,
                InstallationState.InstallStatus.INSTALLED,
                "0.1.42",
                null
            ),
            new SystemChecker.CheckResult(
                InstallationState.Component.Model,
                InstallationState.InstallStatus.INSTALLED,
                "qwen2.5:0.5b",
                null
            )
        );
        return new SystemChecker.SystemCheckResult(results);
    }
    
    private SystemChecker.SystemCheckResult createFailureSystemCheck() {
        var results = List.of(
            new SystemChecker.CheckResult(
                InstallationState.Component.Java,
                InstallationState.InstallStatus.NOT_INSTALLED,
                null,
                "Java not found"
            ),
            new SystemChecker.CheckResult(
                InstallationState.Component.Ollama,
                InstallationState.InstallStatus.NOT_INSTALLED,
                null,
                "Ollama not found"
            ),
            new SystemChecker.CheckResult(
                InstallationState.Component.Model,
                InstallationState.InstallStatus.NOT_INSTALLED,
                null,
                "Model not found"
            )
        );
        return new SystemChecker.SystemCheckResult(results);
    }
    
    private List<InstallationState> createExistingInstallationStates() {
        return List.of(
            createInstallationState(InstallationState.Component.Java, 
                InstallationState.InstallStatus.INSTALLED, "17.0.2"),
            createInstallationState(InstallationState.Component.Ollama, 
                InstallationState.InstallStatus.INSTALLED, "0.1.42"),
            createInstallationState(InstallationState.Component.Model, 
                InstallationState.InstallStatus.INSTALLED, "qwen2.5:0.5b")
        );
    }
    
    private InstallationState createInstallationState(InstallationState.Component component,
                                                   InstallationState.InstallStatus status,
                                                   String version) {
        var state = new InstallationState(component);
        state.setStatus(status);
        state.setVersion(version);
        state.setCreatedAt(LocalDateTime.now().minusDays(1));
        state.setUpdatedAt(LocalDateTime.now());
        state.setLastChecked(LocalDateTime.now());
        return state;
    }
}