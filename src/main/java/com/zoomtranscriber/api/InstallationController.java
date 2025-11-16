package com.zoomtranscriber.api;

import com.zoomtranscriber.core.storage.InstallationState;
import com.zoomtranscriber.core.storage.InstallationStateRepository;
import com.zoomtranscriber.installer.InstallationManager;
import com.zoomtranscriber.installer.SystemChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController("apiInstallationController")
@RequestMapping("/api/v1/system/installation")
@CrossOrigin(origins = "*")
public class InstallationController {
    
    private static final Logger logger = LoggerFactory.getLogger(InstallationController.class);
    
    private final InstallationManager installationManager;
    private final InstallationStateRepository installationStateRepository;
    
    public InstallationController(InstallationManager installationManager,
                              InstallationStateRepository installationStateRepository) {
        this.installationManager = installationManager;
        this.installationStateRepository = installationStateRepository;
    }
    
    @GetMapping
    public ResponseEntity<List<InstallationState>> getInstallationStatus() {
        try {
            logger.info("Getting installation status");
            var status = installationManager.getInstallationStatus();
            var states = status.databaseStates();
            
            return ResponseEntity.ok(states);
        } catch (Exception e) {
            logger.error("Error getting installation status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping
    public ResponseEntity<InstallationState> installComponent(@RequestBody InstallRequest request) {
        try {
            logger.info("Installing component: {}", request.component());
            
            var progressCallback = new java.util.function.Consumer<String>() {
                @Override
                public void accept(String message) {
                    logger.info("Installation progress: {}", message);
                }
            };
            
            var future = installationManager.installComponent(
                    InstallationState.Component.valueOf(request.component()),
                    progressCallback
            );
            
            var result = future.get();
            
            if (result.isSuccess()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(
                        createInstallationStateFromResult(request.component(), result)
                );
            } else {
                return ResponseEntity.badRequest().body(
                        createInstallationStateFromResult(request.component(), result)
                );
            }
        } catch (Exception e) {
            logger.error("Error installing component", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/install-all")
    public ResponseEntity<Map<String, Object>> installAllComponents() {
        try {
            logger.info("Starting full installation");
            
            var progressCallback = new java.util.function.Consumer<String>() {
                @Override
                public void accept(String message) {
                    logger.info("Installation progress: {}", message);
                }
            };
            
            var future = installationManager.installAll(progressCallback);
            var result = future.get();
            
            if (result.isSuccess()) {
                return ResponseEntity.accepted().body(Map.of(
                        "message", "Installation started successfully",
                        "status", "INSTALLING"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Installation failed: " + result.error(),
                        "status", "ERROR"
                ));
            }
        } catch (Exception e) {
            logger.error("Error installing all components", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Installation error: " + e.getMessage(),
                    "status", "ERROR"
            ));
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<List<InstallationState>> refreshInstallationStatus() {
        try {
            logger.info("Refreshing installation status");
            installationManager.refreshInstallationStatus();
            
            var status = installationManager.getInstallationStatus();
            var states = status.databaseStates();
            
            return ResponseEntity.ok(states);
        } catch (Exception e) {
            logger.error("Error refreshing installation status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkSystem() {
        try {
            logger.info("Checking system requirements");
            var systemCheck = installationManager.getInstallationStatus();
            
            return ResponseEntity.ok(Map.of(
                    "isReady", systemCheck.isFullyInstalled(),
                    "systemCheck", systemCheck.systemCheck().results(),
                    "missingComponents", systemCheck.getMissingComponents()
            ));
        } catch (Exception e) {
            logger.error("Error checking system", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "System check error: " + e.getMessage(),
                    "isReady", false
            ));
        }
    }
    
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> isSystemReady() {
        try {
            var isReady = installationManager.isSystemReady();
            return ResponseEntity.ok(Map.of(
                    "isReady", isReady,
                    "message", isReady ? "System is ready for use" : "System setup required"
            ));
        } catch (Exception e) {
            logger.error("Error checking system readiness", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Readiness check error: " + e.getMessage(),
                    "isReady", false
            ));
        }
    }
    
    @GetMapping("/component/{component}")
    public ResponseEntity<InstallationState> getComponentStatus(@PathVariable String component) {
        try {
            var componentEnum = InstallationState.Component.valueOf(component.toUpperCase());
            var state = installationStateRepository.findByComponent(componentEnum);
            
            return state.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error getting component status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/component/{component}")
    public ResponseEntity<Void> resetComponentStatus(@PathVariable String component) {
        try {
            var componentEnum = InstallationState.Component.valueOf(component.toUpperCase());
            var state = installationStateRepository.findByComponent(componentEnum);
            
            if (state.isPresent()) {
                installationStateRepository.delete(state.get());
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error resetting component status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private InstallationState createInstallationStateFromResult(String component, 
                                                         com.zoomtranscriber.installer.DependencyInstaller.InstallationResult result) {
        var state = new InstallationState(InstallationState.Component.valueOf(component));
        state.setStatus(result.status());
        state.setVersion(result.version());
        state.setUpdatedAt(java.time.LocalDateTime.now());
        state.setLastChecked(java.time.LocalDateTime.now());
        
        return state;
    }
    
    public record InstallRequest(
            String component
    ) {
        public boolean isValid() {
            try {
                InstallationState.Component.valueOf(component.toUpperCase());
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }
}