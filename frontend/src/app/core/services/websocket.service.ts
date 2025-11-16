import { Injectable } from '@angular/core';
import { Observable, Subject, fromEvent } from 'rxjs';
import { filter, map, takeUntil } from 'rxjs/operators';

// WebSocket message types
export interface WebSocketMessage {
  type: string;
  data: any;
  timestamp: Date;
}

// Connection status
export enum ConnectionStatus {
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  DISCONNECTED = 'DISCONNECTED',
  ERROR = 'ERROR'
}

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private socket: WebSocket | null = null;
  private url: string = 'ws://localhost:8080/ws';
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 1000; // Start with 1 second
  private maxReconnectDelay = 30000; // Max 30 seconds
  private reconnectTimer: any;

  // Subjects for different message types
  private connectionStatus$ = new Subject<ConnectionStatus>();
  private messages$ = new Subject<WebSocketMessage>();
  private transcriptionSegments$ = new Subject<any>();
  private meetingEvents$ = new Subject<any>();
  private summaryProgress$ = new Subject<any>();

  // Connection status observable
  public connectionStatus = this.connectionStatus$.asObservable();
  public messages = this.messages$.asObservable();
  public transcriptionSegments = this.transcriptionSegments$.asObservable();
  public meetingEvents = this.meetingEvents$.asObservable();
  public summaryProgress = this.summaryProgress$.asObservable();

  constructor() {
    this.connect();
  }

  /**
   * Connects to WebSocket server.
   */
  connect(): void {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      return;
    }

    this.connectionStatus$.next(ConnectionStatus.CONNECTING);

    try {
      this.socket = new WebSocket(this.url);
      this.setupSocketListeners();
    } catch (error) {
      console.error('WebSocket connection error:', error);
      this.connectionStatus$.next(ConnectionStatus.ERROR);
      this.scheduleReconnect();
    }
  }

  /**
   * Disconnects from WebSocket server.
   */
  disconnect(): void {
    this.clearReconnectTimer();
    
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
    
    this.connectionStatus$.next(ConnectionStatus.DISCONNECTED);
  }

  /**
   * Sends a message to the WebSocket server.
   */
  send(type: string, data: any): void {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      console.warn('WebSocket is not connected. Message not sent:', { type, data });
      return;
    }

    const message: WebSocketMessage = {
      type,
      data,
      timestamp: new Date()
    };

    this.socket.send(JSON.stringify(message));
  }

  /**
   * Sends connection message.
   */
  sendConnection(clientId: string): void {
    this.send('connect', { clientId });
  }

  /**
   * Sends start meeting monitoring request.
   */
  startMeetingMonitoring(clientId: string, meetingId?: string): void {
    this.send('meetings/start-monitoring', {
      clientId,
      meetingId: meetingId || null
    });
  }

  /**
   * Sends stop meeting monitoring request.
   */
  stopMeetingMonitoring(clientId: string, meetingId?: string): void {
    this.send('meetings/stop-monitoring', {
      clientId,
      meetingId: meetingId || null
    });
  }

  /**
   * Sends start transcription request.
   */
  startTranscription(clientId: string, meetingId: string, config: any): void {
    this.send('transcription/start', {
      clientId,
      meetingId,
      ...config
    });
  }

  /**
   * Sends stop transcription request.
   */
  stopTranscription(clientId: string, meetingId: string): void {
    this.send('transcription/stop', {
      clientId,
      meetingId
    });
  }

  /**
   * Sends summary generation request.
   */
  generateSummary(clientId: string, meetingId: string, summaryType: string, customPrompt?: string): void {
    this.send('summary/generate', {
      clientId,
      meetingId,
      summaryType,
      customPrompt
    });
  }

  /**
   * Sets up WebSocket event listeners.
   */
  private setupSocketListeners(): void {
    if (!this.socket) return;

    this.socket.onopen = (event) => {
      console.log('WebSocket connected:', event);
      this.connectionStatus$.next(ConnectionStatus.CONNECTED);
      this.reconnectAttempts = 0;
      this.reconnectDelay = 1000;
    };

    this.socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        this.handleMessage(message);
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error);
      }
    };

    this.socket.onclose = (event) => {
      console.log('WebSocket disconnected:', event);
      this.connectionStatus$.next(ConnectionStatus.DISCONNECTED);
      
      if (!event.wasClean && this.reconnectAttempts < this.maxReconnectAttempts) {
        this.scheduleReconnect();
      }
    };

    this.socket.onerror = (event) => {
      console.error('WebSocket error:', event);
      this.connectionStatus$.next(ConnectionStatus.ERROR);
    };
  }

  /**
   * Handles incoming WebSocket messages.
   */
  private handleMessage(message: any): void {
    const webSocketMessage: WebSocketMessage = {
      type: message.type || 'unknown',
      data: message.data || message,
      timestamp: new Date()
    };

    this.messages$.next(webSocketMessage);

    // Route to specific message handlers
    switch (webSocketMessage.type) {
      case 'transcription_segment':
        this.transcriptionSegments$.next(webSocketMessage.data);
        break;
      case 'meeting_event':
        this.meetingEvents$.next(webSocketMessage.data);
        break;
      case 'summary_progress':
        this.summaryProgress$.next(webSocketMessage.data);
        break;
      default:
        console.log('Unhandled WebSocket message:', webSocketMessage);
    }
  }

  /**
   * Schedules reconnection attempt.
   */
  private scheduleReconnect(): void {
    this.clearReconnectTimer();
    
    const delay = Math.min(this.reconnectDelay, this.maxReconnectDelay);
    
    console.log(`Scheduling reconnect attempt ${this.reconnectAttempts + 1} in ${delay}ms`);
    
    this.reconnectTimer = setTimeout(() => {
      this.reconnectAttempts++;
      this.reconnectDelay = this.reconnectDelay * 2; // Exponential backoff
      this.connect();
    }, delay);
  }

  /**
   * Clears reconnection timer.
   */
  private clearReconnectTimer(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  /**
   * Creates a typed observable for specific message types.
   */
  createTypedObservable<T>(messageType: string): Observable<T> {
    return this.messages$.pipe(
      filter(message => message.type === messageType),
      map(message => message.data as T)
    );
  }

  /**
   * Gets messages for specific meeting.
   */
  getMeetingTranscription(meetingId: string): Observable<any> {
    return this.transcriptionSegments$.pipe(
      filter(segment => segment.meetingId === meetingId)
    );
  }

  /**
   * Gets events for specific meeting.
   */
  getMeetingEvents(meetingId: string): Observable<any> {
    return this.meetingEvents$.pipe(
      filter(event => event.meetingId === meetingId)
    );
  }

  /**
   * Gets summary progress for specific meeting.
   */
  getSummaryProgress(meetingId: string): Observable<any> {
    return this.summaryProgress$.pipe(
      filter(progress => progress.meetingId === meetingId)
    );
  }

  /**
   * Checks if WebSocket is connected.
   */
  isConnected(): boolean {
    return this.socket !== null && this.socket.readyState === WebSocket.OPEN;
  }

  /**
   * Gets connection status as enum value.
   */
  getConnectionStatus(): ConnectionStatus {
    return this.socket ? 
      this.socket.readyState === WebSocket.OPEN ? ConnectionStatus.CONNECTED :
      this.socket.readyState === WebSocket.CONNECTING ? ConnectionStatus.CONNECTING :
      this.socket.readyState === WebSocket.CLOSING ? ConnectionStatus.DISCONNECTED :
      ConnectionStatus.DISCONNECTED
    : ConnectionStatus.DISCONNECTED;
  }
}