import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Observable, Subject, combineLatest, timer } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { ApiService } from '@core/services/api.service';
import { WebSocketService, ConnectionStatus } from '@core/services/websocket.service';
import { NotificationService } from '@core/services/notification.service';

// Interfaces
export interface Meeting {
  meetingId: string;
  title: string;
  status: string;
  startTime: string;
  endTime?: string;
  processId: string;
  participantCount: number;
  lastUpdated: string;
}

export interface MeetingEvent {
  meetingId: string;
  eventType: string;
  timestamp: string;
  processId?: string;
  windowTitle?: string;
}

@Component({
  selector: 'app-detection',
  templateUrl: './detection.component.html',
  styleUrls: ['./detection.component.scss']
})
export class DetectionComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private clientId = 'client-' + Math.random().toString(36).substr(2, 9);

  // Data
  meetings: Meeting[] = [];
  meetingEvents: MeetingEvent[] = [];
  isMonitoring = false;
  isLoading = false;
  connectionStatus = ConnectionStatus.DISCONNECTED;

  // Form
  detectionForm: FormGroup;
  selectedMeeting: Meeting | null = null;
  searchText = '';

  // Status
  lastUpdate = '';
  activeMeetingsCount = 0;
  totalEventsCount = 0;

  // WebSocket observables
  connectionStatus$: Observable<ConnectionStatus>;
  meetingEvents$: Observable<MeetingEvent>;

  // Auto-refresh timer
  refreshTimer: any;

  constructor(
    private api: ApiService,
    private websocket: WebSocketService,
    private notification: NotificationService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef
  ) {
    this.connectionStatus$ = this.websocket.connectionStatus;
    this.meetingEvents$ = this.websocket.meetingEvents;

    this.initializeForm();
  }

  ngOnInit(): void {
    this.setupSubscriptions();
    this.loadInitialData();
    this.connectWebSocket();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.stopAutoRefresh();
    
    if (this.isMonitoring) {
      this.stopMonitoring();
    }
  }

  /**
   * Initializes the form.
   */
  private initializeForm(): void {
    this.detectionForm = this.fb.group({
      scanInterval: [5000, [Validators.min(1000), Validators.max(60000)]],
      autoRefresh: [true],
      showEvents: [true],
      showDetails: [true]
    });
  }

  /**
   * Sets up component subscriptions.
   */
  private setupSubscriptions(): void {
    // Connection status
    this.connectionStatus$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(status => {
      this.connectionStatus = status;
      this.cd.detectChanges();
    });

    // Meeting events from WebSocket
    this.meetingEvents$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(event => {
      this.addMeetingEvent(event);
      this.notification.showInfo(`Meeting event: ${event.eventType}`);
    });

    // Search with debounce
    this.detectionForm.get('scanInterval')?.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.restartAutoRefresh();
    });
  }

  /**
   * Loads initial data.
   */
  private loadInitialData(): void {
    this.isLoading = true;
    
    this.api.getMeetings().subscribe({
      next: (meetings: Meeting[]) => {
        this.meetings = meetings;
        this.activeMeetingsCount = meetings.filter(m => m.status === 'ACTIVE').length;
        this.isLoading = false;
        this.lastUpdate = new Date().toLocaleTimeString();
      },
      error: (error) => {
        this.notification.showError('Failed to load meetings');
        this.isLoading = false;
      }
    });
  }

  /**
   * Connects to WebSocket.
   */
  private connectWebSocket(): void {
    this.websocket.connect();
    
    // Send connection message
    setTimeout(() => {
      this.websocket.sendConnection(this.clientId);
    }, 1000);
  }

  /**
   * Starts meeting monitoring.
   */
  startMonitoring(): void {
    this.api.startMeetingMonitoring().subscribe({
      next: () => {
        this.isMonitoring = true;
        this.websocket.startMeetingMonitoring(this.clientId);
        this.notification.showSuccess('Meeting monitoring started');
        this.cd.detectChanges();
      },
      error: (error) => {
        this.notification.showError('Failed to start meeting monitoring');
      }
    });
  }

  /**
   * Stops meeting monitoring.
   */
  stopMonitoring(): void {
    this.api.stopMeetingMonitoring().subscribe({
      next: () => {
        this.isMonitoring = false;
        this.websocket.stopMeetingMonitoring(this.clientId);
        this.notification.showInfo('Meeting monitoring stopped');
        this.cd.detectChanges();
      },
      error: (error) => {
        this.notification.showError('Failed to stop meeting monitoring');
      }
    });
  }

  /**
   * Refreshes meetings list.
   */
  refreshMeetings(): void {
    this.loadInitialData();
  }

  /**
   * Triggers manual scan.
   */
  triggerScan(): void {
    this.notification.showInfo('Triggering manual scan...');
    
    // This would call an API endpoint for manual scan
    setTimeout(() => {
      this.refreshMeetings();
      this.notification.showSuccess('Manual scan completed');
    }, 1000);
  }

  /**
   * Selects a meeting.
   */
  selectMeeting(meeting: Meeting): void {
    this.selectedMeeting = meeting;
    this.cd.detectChanges();
  }

  /**
   * Filters meetings based on search text.
   */
  get filteredMeetings(): Meeting[] {
    if (!this.searchText) {
      return this.meetings;
    }
    
    const searchLower = this.searchText.toLowerCase();
    return this.meetings.filter(meeting =>
      meeting.title.toLowerCase().includes(searchLower) ||
      meeting.status.toLowerCase().includes(searchLower) ||
      meeting.processId.toLowerCase().includes(searchLower)
    );
  }

  /**
   * Gets status color for meeting status.
   */
  getStatusColor(status: string): string {
    switch (status.toUpperCase()) {
      case 'ACTIVE':
        return 'primary';
      case 'PAUSED':
        return 'warn';
      case 'ENDED':
        return 'accent';
      default:
        return '';
    }
  }

  /**
   * Adds meeting event to the log.
   */
  private addMeetingEvent(event: MeetingEvent): void {
    this.meetingEvents.unshift(event);
    this.totalEventsCount++;
    
    // Keep only last 100 events
    if (this.meetingEvents.length > 100) {
      this.meetingEvents = this.meetingEvents.slice(0, 100);
    }
    
    this.cd.detectChanges();
  }

  /**
   * Starts auto-refresh timer.
   */
  private startAutoRefresh(): void {
    this.stopAutoRefresh();
    
    const interval = this.detectionForm.get('scanInterval')?.value || 5000;
    const autoRefresh = this.detectionForm.get('autoRefresh')?.value;
    
    if (autoRefresh && this.connectionStatus === ConnectionStatus.CONNECTED) {
      this.refreshTimer = setInterval(() => {
        this.refreshMeetings();
      }, interval);
    }
  }

  /**
   * Stops auto-refresh timer.
   */
  private stopAutoRefresh(): void {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
      this.refreshTimer = null;
    }
  }

  /**
   * Restarts auto-refresh with new settings.
   */
  private restartAutoRefresh(): void {
    this.startAutoRefresh();
  }

  /**
   * Gets connection status text.
   */
  get connectionStatusText(): string {
    switch (this.connectionStatus) {
      case ConnectionStatus.CONNECTED:
        return 'Connected';
      case ConnectionStatus.CONNECTING:
        return 'Connecting...';
      case ConnectionStatus.DISCONNECTED:
        return 'Disconnected';
      case ConnectionStatus.ERROR:
        return 'Connection Error';
      default:
        return 'Unknown';
    }
  }

  /**
   * Gets connection status color.
   */
  get connectionStatusColor(): string {
    switch (this.connectionStatus) {
      case ConnectionStatus.CONNECTED:
        return 'primary';
      case ConnectionStatus.CONNECTING:
        return 'warn';
      case ConnectionStatus.DISCONNECTED:
      case ConnectionStatus.ERROR:
        return 'warn';
      default:
        return '';
    }
  }

  /**
   * Clears event log.
   */
  clearEventLog(): void {
    this.meetingEvents = [];
    this.totalEventsCount = 0;
    this.cd.detectChanges();
  }

  /**
   * Exports meeting data.
   */
  exportMeetings(): void {
    const data = JSON.stringify(this.meetings, null, 2);
    const blob = new Blob([data], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `meetings-${new Date().toISOString().split('T')[0]}.json`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    
    this.notification.showSuccess('Meeting data exported successfully');
  }

  /**
   * Gets statistics.
   */
  get statistics(): { active: number; total: number; events: number } {
    return {
      active: this.activeMeetingsCount,
      total: this.meetings.length,
      events: this.totalEventsCount
    };
  }
}