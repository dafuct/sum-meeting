import { Component } from '@angular/core';

@Component({
  selector: 'app-detection',
  template: `
    <div class="detection">
      <h2>Meeting Detection</h2>
      <p>Monitor and detect Zoom meetings in real-time</p>
      
      <div class="detection-controls">
        <div class="status-indicator">
          <span class="status-label">Connection Status:</span>
          <span class="status-value" [class.connected]="isConnected">
            {{ isConnected ? 'Connected' : 'Disconnected' }}
          </span>
        </div>
        
        <div class="action-buttons">
          <button (click)="toggleMonitoring()" [disabled]="!isConnected">
            {{ isMonitoring ? 'Stop Monitoring' : 'Start Monitoring' }}
          </button>
          <button (click)="scanMeetings()" [disabled]="!isConnected">
            Scan Now
          </button>
        </div>
      </div>
      
      <div class="meeting-list" *ngIf="meetings.length > 0">
        <h3>Active Meetings ({{ meetings.length }})</h3>
        <div class="meeting-item" *ngFor="let meeting of meetings">
          <div class="meeting-info">
            <h4>{{ meeting.title || 'Untitled Meeting' }}</h4>
            <p>Status: {{ meeting.status }}</p>
            <p>Started: {{ meeting.startTime }}</p>
          </div>
          <div class="meeting-actions">
            <button (click)="joinMeeting(meeting)">Join</button>
            <button (click)="endMeeting(meeting)">End</button>
          </div>
        </div>
      </div>
      
      <div class="empty-state" *ngIf="meetings.length === 0">
        <p>No active meetings detected</p>
      </div>
    </div>
  `,
  styles: [`
    .detection {
      padding: 2rem;
    }
    
    .detection-controls {
      background: white;
      border-radius: 8px;
      padding: 1.5rem;
      margin-bottom: 2rem;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    }
    
    .status-indicator {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 1rem;
    }
    
    .status-value {
      font-weight: bold;
      color: #f44336;
    }
    
    .status-value.connected {
      color: #4caf50;
    }
    
    .action-buttons {
      display: flex;
      gap: 1rem;
    }
    
    .meeting-list {
      background: white;
      border-radius: 8px;
      padding: 1.5rem;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    }
    
    .meeting-item {
      border: 1px solid #e0e0e0;
      border-radius: 4px;
      padding: 1rem;
      margin-bottom: 1rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .meeting-info h4 {
      margin: 0 0 0.5rem 0;
    }
    
    .meeting-info p {
      margin: 0.25rem 0;
      color: #666;
    }
    
    .meeting-actions {
      display: flex;
      gap: 0.5rem;
    }
    
    .empty-state {
      text-align: center;
      padding: 2rem;
      color: #666;
    }
    
    button {
      background: #3f51b5;
      color: white;
      border: none;
      padding: 0.5rem 1rem;
      border-radius: 4px;
      cursor: pointer;
    }
    
    button:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
    
    button:hover:not(:disabled) {
      background: #303f9f;
    }
  `]
})
export class DetectionComponent {
  isConnected = false;
  isMonitoring = false;
  meetings: any[] = [];
  
  toggleMonitoring() {
    this.isMonitoring = !this.isMonitoring;
    // TODO: Implement monitoring logic
  }
  
  scanMeetings() {
    // TODO: Implement scanning logic
    console.log('Scanning for meetings...');
  }
  
  joinMeeting(meeting: any) {
    console.log('Joining meeting:', meeting);
    // TODO: Implement join logic
  }
  
  endMeeting(meeting: any) {
    console.log('Ending meeting:', meeting);
    // TODO: Implement end logic
  }
}