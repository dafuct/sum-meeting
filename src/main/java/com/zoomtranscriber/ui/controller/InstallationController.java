package com.zoomtranscriber.ui.controller;

import com.zoomtranscriber.installer.InstallationManager;
import com.zoomtranscriber.installer.SystemChecker;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class InstallationController {
    
    private static final Logger logger = LoggerFactory.getLogger(InstallationController.class);
    
    private final InstallationManager installationManager;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private ProgressBar progressBar;
    
    @FXML
    private TextArea logArea;
    
    @FXML
    private ListView<String> componentsListView;
    
    @FXML
    private Button checkButton;
    
    @FXML
    private Button installButton;
    
    @FXML
    private Button refreshButton;
    
    private ObservableList<String> componentStatuses;
    
    public InstallationController(InstallationManager installationManager) {
        this.installationManager = installationManager;
        this.componentStatuses = FXCollections.observableArrayList();
    }
    
    @FXML
    public void initialize() {
        logger.info("Initializing InstallationController");
        
        componentsListView.setItems(componentStatuses);
        progressBar.setProgress(0.0);
        
        refreshInstallationStatus();
    }
    
    @FXML
    private void handleCheckAction() {
        logger.info("Checking installation status");
        setUIState(false, "Checking system...");
        
        CompletableFuture.runAsync(() -> {
            try {
                var status = installationManager.getInstallationStatus();
                Platform.runLater(() -> {
                    updateComponentList(status.systemCheck().results());
                    setUIState(true, "System check completed");
                });
            } catch (Exception e) {
                logger.error("Error checking installation status", e);
                Platform.runLater(() -> setUIState(true, "Error: " + e.getMessage()));
            }
        });
    }
    
    @FXML
    private void handleInstallAction() {
        logger.info("Starting installation");
        setUIState(false, "Installing components...");
        
        var progressCallback = new java.util.function.Consumer<String>() {
            @Override
            public void accept(String message) {
                Platform.runLater(() -> {
                    logArea.appendText("[INFO] " + message + "\n");
                    progressBar.setProgress(Math.min(progressBar.getProgress() + 0.1, 0.9));
                });
            }
        };
        
        installationManager.installAll(progressCallback)
                .whenComplete((result, throwable) -> {
                    Platform.runLater(() -> {
                        if (throwable != null) {
                            logger.error("Installation failed", throwable);
                            logArea.appendText("[ERROR] Installation failed: " + throwable.getMessage() + "\n");
                            setUIState(true, "Installation failed");
                            progressBar.setProgress(0.0);
                        } else {
                            logger.info("Installation completed: {}", result.status());
                            logArea.appendText("[SUCCESS] Installation completed\n");
                            setUIState(true, "Installation completed");
                            progressBar.setProgress(1.0);
                        }
                        
                        refreshInstallationStatus();
                    });
                });
    }
    
    @FXML
    private void handleRefreshAction() {
        logger.info("Refreshing installation status");
        setUIState(false, "Refreshing...");
        
        CompletableFuture.runAsync(() -> {
            try {
                installationManager.refreshInstallationStatus();
                var status = installationManager.getInstallationStatus();
                
                Platform.runLater(() -> {
                    updateComponentList(status.systemCheck().results());
                    setUIState(true, "Status refreshed");
                });
            } catch (Exception e) {
                logger.error("Error refreshing installation status", e);
                Platform.runLater(() -> setUIState(true, "Error: " + e.getMessage()));
            }
        });
    }
    
    private void refreshInstallationStatus() {
        CompletableFuture.runAsync(() -> {
            try {
                var status = installationManager.getInstallationStatus();
                Platform.runLater(() -> updateComponentList(status.systemCheck().results()));
            } catch (Exception e) {
                logger.error("Error refreshing status", e);
            }
        });
    }
    
    private void updateComponentList(List<SystemChecker.ComponentCheckResult> results) {
        componentStatuses.clear();
        
        for (var result : results) {
            var status = String.format("%s: %s %s", 
                    result.component(),
                    result.status(),
                    result.version() != null ? "(" + result.version() + ")" : ""
            );
            
            if (result.error() != null) {
                status += " - " + result.error();
            }
            
            componentStatuses.add(status);
        }
        
        var isReady = results.stream().allMatch(SystemChecker.ComponentCheckResult::isInstalled);
        if (isReady) {
            statusLabel.setText("All components installed");
            progressBar.setProgress(1.0);
            installButton.setDisable(true);
        } else {
            var missingCount = (int) results.stream()
                    .filter(result -> result.status() == com.zoomtranscriber.core.storage.InstallationState.InstallStatus.NOT_INSTALLED)
                    .count();
            statusLabel.setText(missingCount + " components missing");
            installButton.setDisable(false);
        }
    }
    
    private void setUIState(boolean enabled, String message) {
        checkButton.setDisable(!enabled);
        installButton.setDisable(!enabled);
        refreshButton.setDisable(!enabled);
        statusLabel.setText(message);
        
        if (enabled) {
            progressBar.setProgress(-1.0);
        } else {
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        }
    }
    
    public void appendLog(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }
    
    public void setProgress(double progress) {
        Platform.runLater(() -> progressBar.setProgress(progress));
    }
    
    public void setStatus(String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }
}