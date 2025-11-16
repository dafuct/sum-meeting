import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { map, catchError, tap } from 'rxjs/operators';

import { environment } from '@environments/environment';
import { NotificationService } from './notification.service';   

// API Response wrapper
export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  message?: string;
  error?: string;
}

// Pagination interface
export interface PaginationParams {
  page: number;
  size: number;
  sort?: string;
  order?: 'asc' | 'desc';
}

// Paginated response
export interface PaginatedResponse<T> {
  data: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly BASE_URL = environment.apiUrl;
  private authToken: string | null = null;
  
  // Loading state for requests
  private loadingSubject = new BehaviorSubject<boolean>(false);
  public loading$ = this.loadingSubject.asObservable();

  constructor(
    private http: HttpClient,
    private notification: NotificationService
  ) {}

  /**
   * Sets the authentication token for API requests.
   */
  setAuthToken(token: string | null): void {
    this.authToken = token;
  }

  /**
   * Gets the HTTP headers with authentication.
   */
  private getHeaders(includeContentType: boolean = true): HttpHeaders {
    let headers = new HttpHeaders();
    
    if (this.authToken) {
      headers = headers.set('Authorization', `Bearer ${this.authToken}`);
    }
    
    if (includeContentType) {
      headers = headers.set('Content-Type', 'application/json');
    }
    
    return headers;
  }

  /**
   * Handles HTTP errors.
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    console.error('API Error:', error);
    
    let errorMessage = 'An unexpected error occurred';
    
    if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = error.error.message;
    } else {
      // Server-side error
      switch (error.status) {
        case 400:
          errorMessage = 'Bad request. Please check your input.';
          break;
        case 401:
          errorMessage = 'Unauthorized. Please log in again.';
          break;
        case 403:
          errorMessage = 'Forbidden. You do not have permission to perform this action.';
          break;
        case 404:
          errorMessage = 'Resource not found.';
          break;
        case 409:
          errorMessage = 'Conflict. The resource already exists.';
          break;
        case 422:
          errorMessage = 'Validation error. Please check your input.';
          break;
        case 500:
          errorMessage = 'Internal server error. Please try again later.';
          break;
        case 503:
          errorMessage = 'Service unavailable. Please try again later.';
          break;
        default:
          errorMessage = error.error?.message || error.message || errorMessage;
      }
    }
    
    this.notification.showError(errorMessage);
    return throwError(() => error);
  }

  /**
   * Shows/hides loading indicator.
   */
  private setLoading(loading: boolean): void {
    this.loadingSubject.next(loading);
  }

  // HTTP Methods

  /**
   * Generic GET request.
   */
  get<T>(endpoint: string, params?: HttpParams): Observable<T> {
    this.setLoading(true);
    
    return this.http.get<T>(`${this.BASE_URL}${endpoint}`, {
      headers: this.getHeaders(false),
      params
    }).pipe(
      catchError(error => this.handleError(error)),
      tap(() => this.setLoading(false))
    );
  }

  /**
   * Generic POST request.
   */
  post<T>(endpoint: string, data?: any): Observable<T> {
    this.setLoading(true);
    
    return this.http.post<T>(`${this.BASE_URL}${endpoint}`, data, {
      headers: this.getHeaders()
    }).pipe(
      catchError(error => this.handleError(error)),
      tap(() => this.setLoading(false))
    );
  }

  /**
   * Generic PUT request.
   */
  put<T>(endpoint: string, data?: any): Observable<T> {
    this.setLoading(true);
    
    return this.http.put<T>(`${this.BASE_URL}${endpoint}`, data, {
      headers: this.getHeaders()
    }).pipe(
      catchError(error => this.handleError(error)),
      tap(() => this.setLoading(false))
    );
  }

  /**
   * Generic PATCH request.
   */
  patch<T>(endpoint: string, data?: any): Observable<T> {
    this.setLoading(true);
    
    return this.http.patch<T>(`${this.BASE_URL}${endpoint}`, data, {
      headers: this.getHeaders()
    }).pipe(
      catchError(error => this.handleError(error)),
      tap(() => this.setLoading(false))
    );
  }

  /**
   * Generic DELETE request.
   */
  delete<T>(endpoint: string): Observable<T> {
    this.setLoading(true);
    
    return this.http.delete<T>(`${this.BASE_URL}${endpoint}`, {
      headers: this.getHeaders()
    }).pipe(
      catchError(error => this.handleError(error)),
      tap(() => this.setLoading(false))
    );
  }

  /**
   * File upload.
   */
  upload<T>(endpoint: string, file: File, additionalData?: any): Observable<T> {
    this.setLoading(true);
    
    const formData = new FormData();
    formData.append('file', file);
    
    if (additionalData) {
      Object.keys(additionalData).forEach(key => {
        formData.append(key, additionalData[key]);
      });
    }
    
    return this.http.post<T>(`${this.BASE_URL}${endpoint}`, formData, {
      headers: new HttpHeaders({
        'Authorization': this.authToken ? `Bearer ${this.authToken}` : ''
        // Don't set Content-Type header for FormData - browser will set it with boundary
      })
    }).pipe(
      catchError(error => this.handleError(error)),
      tap(() => this.setLoading(false))
    );
  }

  /**
   * Download file.
   */
  download(endpoint: string, filename?: string): Observable<Blob> {
    this.setLoading(true);
    
    return this.http.get(`${this.BASE_URL}${endpoint}`, {
      headers: this.getHeaders(false),
      responseType: 'blob'
    }).pipe(
      map(blob => {
        // Create download link and trigger download
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename || 'download';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
        return blob;
      }),
      catchError(error => this.handleError(error)),
      tap(() => this.setLoading(false))
    );
  }

  /**
   * Paginated GET request.
   */
  getPaginated<T>(endpoint: string, pagination: PaginationParams): Observable<PaginatedResponse<T>> {
    const params = new HttpParams()
      .set('page', pagination.page.toString())
      .set('size', pagination.size.toString())
      .set('sort', pagination.sort || 'id')
      .set('order', pagination.order || 'desc');
    
    return this.get<ApiResponse<PaginatedResponse<T>>>(endpoint, params).pipe(
      map(response => response.data!)
    );
  }

  // Specific API methods for different endpoints

  // Meeting Detection API
  getMeetings() {
    return this.get('/api/meetings/active');
  }

  startMeetingMonitoring() {
    return this.post('/api/meetings/monitoring/start');
  }

  stopMeetingMonitoring() {
    return this.post('/api/meetings/monitoring/stop');
  }

  // Transcription API
  startTranscription(meetingId: string, config: any) {
    return this.post('/api/transcription/start', {
      meetingId,
      ...config
    });
  }

  stopTranscription(meetingId: string) {
    return this.post('/api/transcription/stop', { meetingId });
  }

  getTranscriptionSegments(meetingId: string, pagination?: PaginationParams) {
    const params = pagination ? new HttpParams()
      .set('page', pagination.page.toString())
      .set('size', pagination.size.toString()) : undefined;
    
    return this.get(`/api/transcription/${meetingId}/segments`, params);
  }

  // Summary API
  generateSummary(meetingId: string, summaryType: string, customPrompt?: string) {
    return this.post('/api/summary/generate', {
      meetingId,
      summaryType,
      customPrompt
    });
  }

  getSummary(meetingId: string, summaryType: string) {
    return this.get(`/api/summary/${meetingId}/type/${summaryType}`);
  }

  // Configuration API
  getConfiguration() {
    return this.get('/api/v1/configuration');
  }

  updateAudioConfig(config: any) {
    return this.put('/api/v1/configuration/audio', config);
  }

  updateOllamaConfig(config: any) {
    return this.put('/api/v1/configuration/ollama', config);
  }

  // Health check
  healthCheck() {
    return this.get('/api/health');
  }
}