import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';

import { MatTableDataSource } from '@angular/material/table';
import { MatSort } from '@angular/material/sort';
import { SelectionModel } from '@angular/cdk/collections';

import { Meeting } from '../detection/detection.component';

@Component({
  selector: 'app-meeting-list-table',
  templateUrl: './meeting-list-table.component.html',
  styleUrls: ['./meeting-list-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MeetingListTableComponent {
  @Input() meetings: Meeting[] = [];
  @Input() selectedMeeting: Meeting | null = null;
  @Output() meetingSelect = new EventEmitter<Meeting>();

  // Table setup
  displayedColumns: string[] = [
    'title',
    'status',
    'startTime',
    'participantCount',
    'processId',
    'actions'
  ];
  
  dataSource = new MatTableDataSource<Meeting>([]);
  selection = new SelectionModel<Meeting>(false, []);

  constructor() { }

  ngOnChanges(): void {
    this.dataSource.data = this.meetings;
  }

  /**
   * Handles row selection.
   */
  selectRow(meeting: Meeting): void {
    this.meetingSelect.emit(meeting);
  }

  /**
   * Gets status color class.
   */
  getStatusColor(status: string): string {
    switch (status.toUpperCase()) {
      case 'ACTIVE':
        return 'status-active';
      case 'PAUSED':
        return 'status-paused';
      case 'ENDED':
        return 'status-ended';
      default:
        return 'status-unknown';
    }
  }

  /**
   * Gets status icon.
   */
  getStatusIcon(status: string): string {
    switch (status.toUpperCase()) {
      case 'ACTIVE':
        return 'play_circle';
      case 'PAUSED':
        return 'pause_circle';
      case 'ENDED':
        return 'stop_circle';
      default:
        return 'help_circle';
    }
  }

  /**
   * Checks if row is selected.
   */
  isRowSelected(meeting: Meeting): boolean {
    return this.selectedMeeting?.meetingId === meeting.meetingId;
  }

  /**
   * Applies sorting to data source.
   */
  applySorting(sort: MatSort): void {
    this.dataSource.sort = sort;
  }

  /**
   * Formats date string.
   */
  formatDate(dateString: string): string {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString();
  }

  /**
   * Gets short meeting ID.
   */
  getShortMeetingId(meetingId: string): string {
    return meetingId ? meetingId.substring(0, 8) + '...' : '-';
  }

  /**
   * Gets relative time.
   */
  getRelativeTime(startTime: string): string {
    if (!startTime) return '-';
    
    const start = new Date(startTime);
    const now = new Date();
    const diffMs = now.getTime() - start.getTime();
    
    const diffMinutes = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMinutes / 60);
    const diffDays = Math.floor(diffHours / 24);
    
    if (diffDays > 0) {
      return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
    } else if (diffHours > 0) {
      return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    } else if (diffMinutes > 0) {
      return `${diffMinutes} minute${diffMinutes > 1 ? 's' : ''} ago`;
    } else {
      return 'Just now';
    }
  }

  /**
   * Gets participant count display.
   */
  getParticipantDisplay(count: number): string {
    return `${count} participant${count !== 1 ? 's' : ''}`;
  }

  /**
   * Shows meeting details.
   */
  showDetails(meeting: Meeting): void {
    // This would open a dialog or navigate to details view
    console.log('Show details for:', meeting);
  }

  /**
   * Ends meeting.
   */
  endMeeting(meeting: Meeting): void {
    // This would call API to end the meeting
    console.log('End meeting:', meeting);
  }

  /**
   * Joins meeting.
   */
  joinMeeting(meeting: Meeting): void {
    // This would open Zoom or start transcription
    console.log('Join meeting:', meeting);
  }
}