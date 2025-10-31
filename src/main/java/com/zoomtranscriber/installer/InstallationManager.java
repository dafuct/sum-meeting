package com.zoomtranscriber.installer;

import com.zoomtranscriber.core.storage.InstallationState;
import com.zoomtranscriber.core.storage.InstallationStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class InstallationManager {
    
    private static final Logger logger = LoggerFactory.getLogger(InstallationManager.class);
    
    private final SystemChecker systemChecker;
    private final DependencyInstaller dependencyInstaller;
    private final InstallationStateRepository installationStateRepository;
    
    public InstallationManager(SystemChecker systemChecker, 
                          DependencyInstaller dependencyInstaller,
                          InstallationStateRepository installationStateRepository) {
        this.systemChecker = systemChecker;
        this.dependencyInstaller = dependencyInstaller;
        this.installationStateRepository = installationStateRepository;
    }
    
    public InstallationStatus getInstallationStatus() {
        var systemCheck = systemChecker.checkSystem();
        var dbStates = installationStateRepository.findAll();
        
        return new InstallationStatus(systemCheck, dbStates);
    }
    
    public CompletableFuture<DependencyInstaller.InstallationResult> installComponent(InstallationState.Component component, 
                                                         java.util.function.Consumer<String> progressCallback) {
        logger.info("Starting installation of component: {}", component);
        
        return switch (component) {
            case Java -> installJava(progressCallback);
            case Ollama -> installOllama(progressCallback);
            case Model -> installModel(progressCallback);
        };
    }
    
    public CompletableFuture<DependencyInstaller.InstallationResult> installAll(java.util.function.Consumer<String> progressCallback) {
        logger.info("Starting full system installation");
        
        var systemCheck = systemChecker.checkSystem();
        var missingComponents = systemCheck.getFailedChecks().stream()
                .map(check -> check.component())
                .toList();
        
        if (missingComponents.isEmpty()) {
            progressCallback.accept("All components are already installed");
            return CompletableFuture.completedFuture(
                new DependencyInstaller.InstallationResult(null, InstallationState.InstallStatus.INSTALLED, "All components installed", null)
            );
        }
        
        return installComponentsSequentially(missingComponents, progressCallback);
    }
    
    private CompletableFuture<DependencyInstaller.InstallationResult> installJava(java.util.function.Consumer<String> progressCallback) {
        progressCallback.accept("Installing Java...");
        
        return dependencyInstaller.installJava(progressCallback)
                .thenApply(result -> {
                    updateInstallationState(InstallationState.Component.Java, result);
                    return result;
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("Java installation failed", throwable);
                    } else {
                        logger.info("Java installation completed: {}", result.status());
                    }
                });
    }
    
    private CompletableFuture<DependencyInstaller.InstallationResult> installOllama(java.util.function.Consumer<String> progressCallback) {
        progressCallback.accept("Installing Ollama...");
        
        return dependencyInstaller.installOllama(progressCallback)
                .thenApply(result -> {
                    updateInstallationState(InstallationState.Component.Ollama, result);
                    return result;
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("Ollama installation failed", throwable);
                    } else {
                        logger.info("Ollama installation completed: {}", result.status());
                    }
                });
    }
    
    private CompletableFuture<DependencyInstaller.InstallationResult> installModel(java.util.function.Consumer<String> progressCallback) {
        progressCallback.accept("Installing Qwen model...");
        
        return dependencyInstaller.installModel(progressCallback)
                .thenApply(result -> {
                    updateInstallationState(InstallationState.Component.Model, result);
                    return result;
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("Model installation failed", throwable);
                    } else {
                        logger.info("Model installation completed: {}", result.status());
                    }
                });
    }
    
    private CompletableFuture<DependencyInstaller.InstallationResult> installComponentsSequentially(
            List<InstallationState.Component> components,
            java.util.function.Consumer<String> progressCallback) {
        
        CompletableFuture<DependencyInstaller.InstallationResult> future = CompletableFuture.completedFuture(
                new DependencyInstaller.InstallationResult(null, InstallationState.InstallStatus.INSTALLED, "Starting installation", null)
        );
        
        for (var component : components) {
            future = future.thenCompose(result -> installComponent(component, progressCallback));
        }
        
        return future;
    }
    
    private void updateInstallationState(InstallationState.Component component, DependencyInstaller.InstallationResult result) {
        var existingState = installationStateRepository.findByComponent(component);
        
        var state = existingState.orElseGet(() -> {
            var newState = new InstallationState(component);
            newState.setCreatedAt(LocalDateTime.now());
            return newState;
        });
        
        state.setStatus(result.status());
        state.setVersion(result.version());
        state.setUpdatedAt(LocalDateTime.now());
        state.setLastChecked(LocalDateTime.now());
        
        if (result.error() != null) {
            logger.error("Installation error for {}: {}", component, result.error());
        }
        
        installationStateRepository.save(state);
    }
    
    public boolean isSystemReady() {
        var systemCheck = systemChecker.checkSystem();
        return systemCheck.isAllInstalled();
    }
    
    public List<String> getMissingComponents() {
        return systemChecker.getMissingComponents();
    }
    
    public void refreshInstallationStatus() {
        logger.info("Refreshing installation status");
        
        var systemCheck = systemChecker.checkSystem();
        
        for (var checkResult : systemCheck.results()) {
            updateInstallationState(checkResult.component(), 
                    new DependencyInstaller.InstallationResult(
                            checkResult.component(),
                            checkResult.status(),
                            checkResult.version(),
                            checkResult.error()
                    ));
        }
    }
    
    public record InstallationStatus(
            SystemChecker.SystemCheckResult systemCheck,
            List<InstallationState> databaseStates
    ) {
        public boolean isFullyInstalled() {
            return systemCheck.isAllInstalled();
        }
        
        public List<InstallationState> getInstalledComponents() {
            return databaseStates.stream()
                    .filter(state -> state.getStatus() == InstallationState.InstallStatus.INSTALLED)
                    .toList();
        }
        
        public List<InstallationState> getMissingComponents() {
            return databaseStates.stream()
                    .filter(state -> state.getStatus() != InstallationState.InstallStatus.INSTALLED)
                    .toList();
        }
    }
}