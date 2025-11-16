import { Component, Input, Output, EventEmitter, OnChanges, ChangeDetectionStrategy, ViewChild, ElementRef } from '@angular/core';
import { NgForOf, NgIf } from '@angular/common';

// Transcription interfaces
export interface TranscriptionSegment {
  id: string;
  meetingId: string;
  text: string;
  speakerId?: string;
  confidence: number;
  timestamp: string;
  isFinal: boolean;
  duration?: number;
  language?: string;
  wordCount?: number;
}

export interface TranscriptionSettings {
  showTimestamps: boolean;
  showSpeakers: boolean;
  showConfidence: boolean;
  fontSize: 'small' | 'medium' | 'large';
  autoScroll: boolean;
  highlightWords: boolean;
  wordHighlight: string;
}

@Component({
  selector: 'app-transcription-display',
  templateUrl: './transcription-display.component.html',
  styleUrls: ['./transcription-display.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TranscriptionDisplayComponent implements OnChanges {
  @Input() segments: TranscriptionSegment[] = [];
  @Input() settings: TranscriptionSettings = {
    showTimestamps: true,
    showSpeakers: true,
    showConfidence: false,
    fontSize: 'medium',
    autoScroll: true,
    highlightWords: false,
    wordHighlight: ''
  };
  @Input() isLoading = false;
  @Input() searchText = '';
  @Output() segmentClick = new EventEmitter<TranscriptionSegment>();
  @Output() segmentSelect = new EventEmitter<TranscriptionSegment>();
  @Output() segmentEdit = new EventEmitter<TranscriptionSegment>();

  @ViewChild('transcriptionContainer', { static: false }) transcriptionContainer!: ElementRef;

  // Derived properties
  filteredSegments: TranscriptionSegment[] = [];
  selectedSegment: TranscriptionSegment | null = null;

  constructor() { }

  ngOnChanges(): void {
    this.filterSegments();
    if (this.settings.autoScroll) {
      setTimeout(() => this.scrollToBottom(), 100);
    }
  }

  /**
   * Filters segments based on search text.
   */
  private filterSegments(): void {
    if (!this.searchText || this.searchText.trim() === '') {
      this.filteredSegments = [...this.segments];
    } else {
      const searchLower = this.searchText.toLowerCase();
      this.filteredSegments = this.segments.filter(segment =>
        segment.text.toLowerCase().includes(searchLower) ||
        (segment.speakerId && segment.speakerId.toLowerCase().includes(searchLower))
      );
    }
  }

  /**
   * Scrolls to bottom of transcription container.
   */
  scrollToBottom(): void {
    if (this.transcriptionContainer && this.transcriptionContainer.nativeElement) {
      const element = this.transcriptionContainer.nativeElement;
      element.scrollTop = element.scrollHeight;
    }
  }

  /**
   * Handles segment click.
   */
  onSegmentClick(segment: TranscriptionSegment, event: MouseEvent): void {
    event.preventDefault();
    this.segmentClick.emit(segment);
  }

  /**
   * Handles segment selection.
   */
  onSegmentSelect(segment: TranscriptionSegment, event: MouseEvent): void {
    if (event.ctrlKey || event.metaKey) {
      // Multi-select with Ctrl/Cmd key
      return;
    }

    this.selectedSegment = this.selectedSegment?.id === segment.id ? null : segment;
    this.segmentSelect.emit(this.selectedSegment || segment);
  }

  /**
   * Handles segment edit.
   */
  onSegmentEdit(segment: TranscriptionSegment, event: MouseEvent): void {
    if (event.detail === 2) { // Double click
      event.preventDefault();
      this.segmentEdit.emit(segment);
    }
  }

  /**
   * Highlights text based on search term.
   */
  highlightText(text: string): string {
    if (!this.settings.highlightWords || !this.settings.wordHighlight) {
      return text;
    }

    const regex = new RegExp(`(${this.escapeRegex(this.settings.wordHighlight)})`, 'gi');
    return text.replace(regex, '<mark>$1</mark>');
  }

  /**
   * Escapes regex special characters.
   */
  private escapeRegex(str: string): string {
    return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }

  /**
   * Gets formatted timestamp.
   */
  getFormattedTimestamp(timestamp: string): string {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { 
      hour: '2-digit', 
      minute: '2-digit', 
      second: '2-digit' 
    });
  }

  /**
   * Gets confidence display.
   */
  getConfidenceDisplay(confidence: number): string {
    return `${Math.round(confidence * 100)}%`;
  }

  /**
   * Gets confidence color.
   */
  getConfidenceColor(confidence: number): string {
    if (confidence >= 0.9) return '#4caf50'; // Green
    if (confidence >= 0.7) return '#ff9800'; // Orange
    if (confidence >= 0.5) return '#ff5722'; // Red
    return '#f44336'; // Dark red
  }

  /**
   * Gets speaker color.
   */
  getSpeakerColor(speakerId: string): string {
    if (!speakerId) return '#666';
    
    // Generate consistent color based on speaker ID
    const colors = [
      '#2196f3', '#4caf50', '#ff9800', '#9c27b0',
      '#f44336', '#009688', '#795548', '#607d8b',
      '#e91e63', '#00bcd4', '#8bc34a', '#ffc107'
    ];
    
    const hash = speakerId.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    return colors[hash % colors.length];
  }

  /**
   * Gets word count display.
   */
  getWordCountDisplay(segment: TranscriptionSegment): string {
    const words = segment.wordCount || segment.text.split(/\s+/).length;
    return `${words} word${words !== 1 ? 's' : ''}`;
  }

  /**
   * Gets segment duration display.
   */
  getDurationDisplay(segment: TranscriptionSegment): string {
    if (!segment.duration) return '';
    
    const seconds = Math.floor(segment.duration / 1000);
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    
    if (minutes > 0) {
      return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
    }
    return `0:${remainingSeconds.toString().padStart(2, '0')}`;
  }

  /**
   * Checks if segment matches search.
   */
  isSearchMatch(segment: TranscriptionSegment): boolean {
    if (!this.searchText || this.searchText.trim() === '') {
      return false;
    }
    
    const searchLower = this.searchText.toLowerCase();
    return segment.text.toLowerCase().includes(searchLower) ||
           (segment.speakerId && segment.speakerId.toLowerCase().includes(searchLower));
  }

  /**
   * Gets CSS classes for segment.
   */
  getSegmentClasses(segment: TranscriptionSegment): { [key: string]: boolean } {
    return {
      'segment': true,
      'segment-selected': this.selectedSegment?.id === segment.id,
      'segment-final': segment.isFinal,
      'segment-interim': !segment.isFinal,
      'segment-search-match': this.isSearchMatch(segment),
      [`font-size-${this.settings.fontSize}`]: true
    };
  }

  /**
   * Gets transcription statistics.
   */
  getStatistics(): { 
    totalSegments: number; 
    finalSegments: number; 
    totalWords: number; 
    totalTime: number; 
    averageConfidence: number;
    speakers: Set<string>;
  } {
    const finalSegments = this.segments.filter(s => s.isFinal);
    const totalWords = this.segments.reduce((sum, s) => sum + (s.wordCount || s.text.split(/\s+/).length), 0);
    const totalTime = this.segments.reduce((sum, s) => sum + (s.duration || 0), 0);
    const totalConfidence = this.segments.reduce((sum, s) => sum + s.confidence, 0);
    const speakers = new Set(this.segments.filter(s => s.speakerId).map(s => s.speakerId!));
    
    return {
      totalSegments: this.segments.length,
      finalSegments: finalSegments.length,
      totalWords,
      totalTime,
      averageConfidence: this.segments.length > 0 ? totalConfidence / this.segments.length : 0,
      speakers
    };
  }

  /**
   * Exports transcription data.
   */
  exportTranscription(): void {
    const data = {
      segments: this.segments,
      statistics: this.getStatistics(),
      exportedAt: new Date().toISOString()
    };
    
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `transcription-${new Date().toISOString().split('T')[0]}.json`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }

  /**
   * Copies transcription text to clipboard.
   */
  copyToClipboard(): void {
    const text = this.segments
      .filter(s => s.isFinal)
      .map(s => `${this.getFormattedTimestamp(s.timestamp)} ${s.speakerId ? `[${s.speakerId}]` : ''} ${s.text}`)
      .join('\n');
    
    navigator.clipboard.writeText(text).then(() => {
      console.log('Transcription copied to clipboard');
    }).catch(err => {
      console.error('Failed to copy transcription:', err);
    });
  }
}