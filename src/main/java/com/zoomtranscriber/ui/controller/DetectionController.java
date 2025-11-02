package com.zoomtranscriber.ui.controller;

import com.zoomtranscriber.core.detection.ZoomDetectionService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * JavaFX controller for meeting detection UI.
 * Manages the detection interface and displays meeting status information.
 */
@Component
public class DetectionController {
    
    private static final Logger logger = LoggerFactory.getLogger(DetectionController.class);
    private static final DateTimeFormatter TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private final ZoomDetectionService zoomDetectionService;
    
    // UI Components
    @FXML private VBox mainContainer;
    @FXML private HBox statusContainer;
    @FXML private Label statusLabel;
    @FXML private ToggleButton monitoringToggle;
    @FXML private Button refreshButton;
    @FXML private ProgressBar detectionProgressBar;
    @FXML private Label lastUpdateLabel;
    
    @FXML private TableView<MeetingTableRow> meetingsTable;
    @FXML private TableColumn<MeetingTableRow, String> meetingIdColumn;
    @FXML private TableColumn<MeetingTableRow, String> titleColumn;
    @FXML private TableColumn<MeetingTableRow, String> statusColumn;
    @FXML private TableColumn<MeetingTableRow, String> startTimeColumn;
    @FXML private TableColumn<MeetingTableRow, String> participantCountColumn;
    @FXML private TableColumn<MeetingTableRow, Void> actionsColumn;
    
    @FXML private TextArea eventLogArea;
    @FXML private Button clearLogButton;
    @FXML private CheckBox autoScrollCheckBox;
    
    // Reactive subscriptions
    private Disposable meetingEventsSubscription;
    private Disposable statusUpdateSubscription;
    
    // Data
    private final ObservableList<MeetingTableRow> meetingsData = FXCollections.observableArrayList();
    
    public DetectionController(ZoomDetectionService zoomDetectionService) {
        this.zoomDetectionService = zoomDetectionService;
    }
    
    @FXML
    public void initialize() {
        logger.info("Initializing DetectionController");
        
        setupTableColumns();
        setupEventHandlers();
        setupDataBindings();
        
        // Initialize UI state
        updateMonitoringStatus();
        updateLastUpdateTime();
        
        logger.info("DetectionController initialized successfully");
    }
    
    /**
     * Sets up table columns with proper bindings.
     */
    private void setupTableColumns() {
        meetingIdColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().meetingId().toString().substring(0, 8) + "..."));
        
        titleColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().title()));
        
        statusColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().status()));
        
        startTimeColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().startTime().format(TIME_FORMATTER)));
        
        participantCountColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(
                String.valueOf(data.getValue().participantCount())));
        
        // Add action buttons to actions column
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button detailsButton = new Button("Details");
            private final Button endButton = new Button("End");
            private final HBox buttonBox = new HBox(5, detailsButton, endButton);
            
            {
                detailsButton.setOnAction(event -> showMeetingDetails(getTableRow().getItem()));
                endButton.setOnAction(event -> endMeeting(getTableRow().getItem()));
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(buttonBox);
                }
            }
        });
        
        meetingsTable.setItems(meetingsData);
    }
    
    /**
     * Sets up event handlers for UI components.
     */
    private void setupEventHandlers() {
        monitoringToggle.setOnAction(event -> toggleMonitoring());
        refreshButton.setOnAction(event -> refreshMeetings());
        clearLogButton.setOnAction(event -> clearEventLog());
        
        // Auto-scroll checkbox
        autoScrollCheckBox.setSelected(true);
    }
    
    /**
     * Sets up data bindings and reactive streams.
     */
    private void setupDataBindings() {
        // Subscribe to meeting events
        meetingEventsSubscription = zoomDetectionService.getMeetingEvents()
            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
            .subscribe(this::handleMeetingEvent, 
                error -> logger.error("Error in meeting events stream", error));
        
        // Periodic status updates
        statusUpdateSubscription = reactor.core.publisher.Flux.interval(java.time.Duration.ofSeconds(5))
            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
            .subscribe(tick -> {
                Platform.runLater(this::updateMeetingsList);
                Platform.runLater(this::updateLastUpdateTime);
            });
    }
    
    /**
     * Toggles meeting monitoring on/off.
     */
    private void toggleMonitoring() {
        var isStarting = monitoringToggle.isSelected();
        
        if (isStarting) {
            startMonitoring();
        } else {
            stopMonitoring();
        }
    }
    
    /**
     * Starts meeting monitoring.
     */
    private void startMonitoring() {
        logger.info("Starting meeting monitoring");
        
        zoomDetectionService.startMonitoring()
            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
            .subscribe(
                result -> Platform.runLater(() -> {
                    monitoringToggle.setText("Stop Monitoring");
                    statusLabel.setText("Monitoring Active");
                    statusLabel.setStyle("-fx-text-fill: green;");
                    logEvent("Meeting monitoring started");
                }),
                error -> Platform.runLater(() -> {
                    logger.error("Failed to start monitoring", error);
                    monitoringToggle.setSelected(false);
                    statusLabel.setText("Monitoring Failed");
                    statusLabel.setStyle("-fx-text-fill: red;");
                    logEvent("Failed to start monitoring: " + error.getMessage());
                })
            );
    }
    
    /**
     * Stops meeting monitoring.
     */
    private void stopMonitoring() {
        logger.info("Stopping meeting monitoring");
        
        zoomDetectionService.stopMonitoring()
            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
            .subscribe(
                result -> Platform.runLater(() -> {
                    monitoringToggle.setText("Start Monitoring");
                    statusLabel.setText("Monitoring Stopped");
                    statusLabel.setStyle("-fx-text-fill: orange;");
                    logEvent("Meeting monitoring stopped");
                }),
                error -> Platform.runLater(() -> {
                    logger.error("Failed to stop monitoring", error);
                    statusLabel.setText("Stop Failed");
                    statusLabel.setStyle("-fx-text-fill: red;");
                    logEvent("Failed to stop monitoring: " + error.getMessage());
                })
            );
    }
    
    /**
     * Refreshes the meetings list.
     */
    private void refreshMeetings() {
        logger.debug("Refreshing meetings list");
        
        zoomDetectionService.triggerDetectionScan()
            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
            .subscribe(
                result -> Platform.runLater(() -> {
                    updateMeetingsList();
                    logEvent("Detection scan completed");
                }),
                error -> Platform.runLater(() -> {
                    logger.error("Detection scan failed", error);
                    logEvent("Detection scan failed: " + error.getMessage());
                })
            );
    }
    
    /**
     * Updates the monitoring status display.
     */
    private void updateMonitoringStatus() {
        var isMonitoring = zoomDetectionService.isMonitoring();
        
        Platform.runLater(() -> {
            monitoringToggle.setSelected(isMonitoring);
            if (isMonitoring) {
                monitoringToggle.setText("Stop Monitoring");
                statusLabel.setText("Monitoring Active");
                statusLabel.setStyle("-fx-text-fill: green;");
            } else {
                monitoringToggle.setText("Start Monitoring");
                statusLabel.setText("Monitoring Inactive");
                statusLabel.setStyle("-fx-text-fill: gray;");
            }
        });
    }
    
    /**
     * Updates the meetings list with current data.
     */
    private void updateMeetingsList() {
        zoomDetectionService.getActiveMeetings()
            .collectList()
            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
            .subscribe(
                meetings -> Platform.runLater(() -> {
                    meetingsData.clear();
                    meetings.stream()
                        .map(MeetingTableRow::from)
                        .forEach(meetingsData::add);
                }),
                error -> logger.error("Error updating meetings list", error)
            );
    }
    
    /**
     * Updates the last update time display.
     */
    private void updateLastUpdateTime() {
        Platform.runLater(() -> {
            lastUpdateLabel.setText("Last updated: " + 
                LocalDateTime.now().format(TIME_FORMATTER));
        });
    }
    
    /**
     * Handles a meeting event.
     * 
     * @param event the meeting event
     */
    private void handleMeetingEvent(ZoomDetectionService.MeetingEvent event) {
        Platform.runLater(() -> {
            var eventType = switch (event.eventType()) {
                case MEETING_STARTED -> "Meeting Started";
                case MEETING_ENDED -> "Meeting Ended";
                case MEETING_PAUSED -> "Meeting Paused";
                case MEETING_RESUMED -> "Meeting Resumed";
                case PARTICIPANT_JOINED -> "Participant Joined";
                case PARTICIPANT_LEFT -> "Participant Left";
            };
            
            logEvent(String.format("%s: %s (%s)", 
                eventType, 
                event.windowTitle() != null ? event.windowTitle() : "Unknown",
                event.meetingId().toString().substring(0, 8)));
            
            updateMeetingsList();
        });
    }
    
    /**
     * Shows details for a specific meeting.
     * 
     * @param meeting the meeting to show details for
     */
    private void showMeetingDetails(MeetingTableRow meeting) {
        if (meeting == null) return;
        
        zoomDetectionService.getCurrentMeetingState(meeting.meetingId())
            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
            .subscribe(
                state -> Platform.runLater(() -> {
                    var alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Meeting Details");
                    alert.setHeaderText("Meeting: " + state.title());
                    
                    var content = new StringBuilder();
                    content.append("Meeting ID: ").append(state.meetingId()).append("\n");
                    content.append("Title: ").append(state.title()).append("\n");
                    content.append("Status: ").append(state.status()).append("\n");
                    content.append("Start Time: ").append(state.startTime()).append("\n");
                    if (state.endTime() != null) {
                        content.append("End Time: ").append(state.endTime()).append("\n");
                    }
                    content.append("Process ID: ").append(state.processId()).append("\n");
                    content.append("Participants: ").append(state.participantCount()).append("\n");
                    content.append("Last Updated: ").append(state.lastUpdated()).append("\n");
                    
                    alert.setContentText(content.toString());
                    alert.showAndWait();
                }),
                error -> logger.error("Error getting meeting details", error)
            );
    }
    
    /**
     * Ends a meeting.
     * 
     * @param meeting the meeting to end
     */
    private void endMeeting(MeetingTableRow meeting) {
        if (meeting == null) return;
        
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("End Meeting");
        alert.setHeaderText("End Meeting: " + meeting.title());
        alert.setContentText("Are you sure you want to end this meeting?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                logEvent("Manually ended meeting: " + meeting.title());
                // In a real implementation, this would trigger meeting end logic
            }
        });
    }
    
    /**
     * Logs an event to the event log area.
     * 
     * @param message the event message
     */
    private void logEvent(String message) {
        var timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        var logEntry = String.format("[%s] %s\n", timestamp, message);
        
        eventLogArea.appendText(logEntry);
        
        if (autoScrollCheckBox.isSelected()) {
            // Auto-scroll to bottom
            eventLogArea.setScrollTop(Double.MAX_VALUE);
        }
    }
    
    /**
     * Clears the event log.
     */
    private void clearEventLog() {
        eventLogArea.clear();
        logEvent("Event log cleared");
    }
    
    /**
     * Cleans up resources when the controller is destroyed.
     */
    public void cleanup() {
        logger.info("Cleaning up DetectionController");
        
        if (meetingEventsSubscription != null && !meetingEventsSubscription.isDisposed()) {
            meetingEventsSubscription.dispose();
        }
        
        if (statusUpdateSubscription != null && !statusUpdateSubscription.isDisposed()) {
            statusUpdateSubscription.dispose();
        }
        
        zoomDetectionService.stopMonitoring()
            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
            .subscribe();
    }
    
    /**
     * Data class for table rows.
     */
    public record MeetingTableRow(
        UUID meetingId,
        String title,
        String status,
        LocalDateTime startTime,
        int participantCount
    ) {
        static MeetingTableRow from(ZoomDetectionService.MeetingState state) {
            return new MeetingTableRow(
                state.meetingId(),
                state.title(),
                state.status().toString(),
                state.startTime(),
                state.participantCount()
            );
        }
    }
}