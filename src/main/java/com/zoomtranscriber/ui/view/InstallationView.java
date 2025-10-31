package com.zoomtranscriber.ui.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class InstallationView {
    
    private static final Logger logger = LoggerFactory.getLogger(InstallationView.class);
    
    private final ApplicationContext applicationContext;
    
    public InstallationView(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    public void show() {
        try {
            logger.info("Loading installation view");
            
            var loader = new FXMLLoader(getClass().getResource("/fxml/installation-view.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            
            Parent root = loader.load();
            
            var stage = new Stage();
            stage.setTitle("Zoom Transcriber - Installation");
            stage.setScene(new Scene(root, 800, 600));
            stage.setResizable(false);
            stage.show();
            
            logger.info("Installation view displayed");
        } catch (IOException e) {
            logger.error("Failed to load installation view", e);
        }
    }
    
    public void showAndWait() throws Exception {
        logger.info("Loading installation view (modal)");
        
        var loader = new FXMLLoader(getClass().getResource("/fxml/installation-view.fxml"));
        loader.setControllerFactory(applicationContext::getBean);
        
        Parent root = loader.load();
        
        var stage = new Stage();
        stage.setTitle("Zoom Transcriber - Installation");
        stage.setScene(new Scene(root, 800, 600));
        stage.setResizable(false);
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        
        stage.showAndWait();
        
        logger.info("Installation view closed");
    }
    
    public void close() {
        var currentStage = getCurrentStage();
        if (currentStage != null) {
            currentStage.close();
            logger.info("Installation view closed");
        }
    }
    
    private Stage getCurrentStage() {
        var scenes = javafx.stage.Window.getWindows();
        if (!scenes.isEmpty()) {
            var window = scenes.iterator().next();
            if (window instanceof Stage) {
                return (Stage) window;
            }
        }
        return null;
    }
    
    public boolean isShowing() {
        var currentStage = getCurrentStage();
        return currentStage != null && currentStage.isShowing();
    }
    
    public void setTitle(String title) {
        var currentStage = getCurrentStage();
        if (currentStage != null) {
            currentStage.setTitle("Zoom Transcriber - " + title);
        }
    }
    
    public void setProgress(double progress) {
        var controller = getController();
        if (controller != null) {
            controller.setProgress(progress);
        }
    }
    
    public void setStatus(String status) {
        var controller = getController();
        if (controller != null) {
            controller.setStatus(status);
        }
    }
    
    public void appendLog(String message) {
        var controller = getController();
        if (controller != null) {
            controller.appendLog(message);
        }
    }
    
    private com.zoomtranscriber.ui.controller.InstallationController getController() {
        try {
            var loader = new FXMLLoader(getClass().getResource("/fxml/installation-view.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            loader.load();
            return loader.getController();
        } catch (IOException e) {
            logger.error("Failed to get installation controller", e);
            return null;
        }
    }
}