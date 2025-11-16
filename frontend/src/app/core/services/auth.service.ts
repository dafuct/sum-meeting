import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, throwError, timer } from 'rxjs';
import { map, catchError, tap, finalize } from 'rxjs/operators';

import { environment } from '@environments/environment';
import { ApiService } from './api.service';
import { StorageService } from './storage.service';
import { NotificationService } from './notification.service';

// Interfaces
export interface LoginRequest {
  username: string;
  password: string;
  rememberMe?: boolean;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  username: string;
  roles: string[];
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface UserInfo {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
  lastLogin: string;
  createdAt: string;
}

export interface AuthState {
  isAuthenticated: boolean;
  user: UserInfo | null;
  token: string | null;
  isLoading: boolean;
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = `${environment.apiUrl}/api/auth`;
  private readonly TOKEN_KEY = 'auth_token';
  private readonly REFRESH_TOKEN_KEY = 'refresh_token';
  private readonly USER_KEY = 'user_info';

  // BehaviorSubjects for reactive state management
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  private userSubject = new BehaviorSubject<UserInfo | null>(null);
  private isLoadingSubject = new BehaviorSubject<boolean>(false);
  private errorSubject = new BehaviorSubject<string | null>(null);

  // Public observables
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  public user$ = this.userSubject.asObservable();
  public isLoading$ = this.isLoadingSubject.asObservable();
  public error$ = this.errorSubject.asObservable();

  // Token refresh timer
  private refreshTimer: any;

  constructor(
    private http: HttpClient,
    private apiService: ApiService,
    private storage: StorageService,
    private notification: NotificationService,
    private router: Router
  ) {
    this.initializeAuth();
  }

  /**
   * Initializes authentication state from storage.
   */
  private initializeAuth(): void {
    const token = this.storage.get(this.TOKEN_KEY);
    const user = this.storage.get(this.USER_KEY);

    if (token && user) {
      this.token = token;
      this.user = JSON.parse(user);
      this.isAuthenticated = true;
      this.scheduleTokenRefresh();
    }
  }

  /**
   * Logs in a user.
   */
  login(credentials: LoginRequest): Observable<LoginResponse> {
    this.isLoading = true;
    this.error = null;

    return this.http.post<LoginResponse>(`${this.API_URL}/login`, credentials).pipe(
      tap(response => {
        this.handleLoginSuccess(response, credentials.rememberMe);
      }),
      catchError(error => {
        this.handleAuthError('Login failed. Please check your credentials.');
        return throwError(() => error);
      }),
      finalize(() => {
        this.isLoading = false;
      })
    );
  }

  /**
   * Registers a new user.
   */
  register(userData: RegisterRequest): Observable<any> {
    this.isLoading = true;
    this.error = null;

    return this.http.post(`${this.API_URL}/register`, userData).pipe(
      tap(response => {
        this.notification.showSuccess('Registration successful! Please log in.');
        this.router.navigate(['/auth/login']);
      }),
      catchError(error => {
        this.handleAuthError('Registration failed. Please try again.');
        return throwError(() => error);
      }),
      finalize(() => {
        this.isLoading = false;
      })
    );
  }

  /**
   * Logs out the current user.
   */
  logout(): void {
    // Call logout endpoint to invalidate token on server
    this.http.post(`${this.API_URL}/logout`, { token: this.token }).subscribe({
      next: () => {
        console.log('Server logout successful');
      },
      error: (error) => {
        console.warn('Server logout failed:', error);
      }
    });

    this.clearAuth();
    this.router.navigate(['/auth/login']);
    this.notification.showInfo('You have been logged out.');
  }

  /**
   * Refreshes the access token.
   */
  refreshToken(): Observable<LoginResponse> {
    const refreshToken = this.storage.get(this.REFRESH_TOKEN_KEY);
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    return this.http.post<LoginResponse>(`${this.API_URL}/refresh`, { 
      refreshToken 
    }).pipe(
      tap(response => {
        this.handleTokenRefresh(response);
      }),
      catchError(error => {
        this.clearAuth();
        this.router.navigate(['/auth/login']);
        return throwError(() => error);
      })
    );
  }

  /**
   * Changes user password.
   */
  changePassword(currentPassword: string, newPassword: string): Observable<any> {
    return this.http.post(`${this.API_URL}/change-password`, {
      currentPassword,
      newPassword
    }).pipe(
      tap(() => {
        this.notification.showSuccess('Password changed successfully!');
      }),
      catchError(error => {
        this.notification.showError('Failed to change password. Please check your current password.');
        return throwError(() => error);
      })
    );
  }

  /**
   * Requests password reset.
   */
  forgotPassword(email: string): Observable<any> {
    return this.http.post(`${this.API_URL}/forgot-password`, { email }).pipe(
      tap(() => {
        this.notification.showSuccess('Password reset link sent to your email!');
      }),
      catchError(error => {
        this.notification.showError('Failed to send password reset email.');
        return throwError(() => error);
      })
    );
  }

  /**
   * Resets password with token.
   */
  resetPassword(token: string, newPassword: string): Observable<any> {
    return this.http.post(`${this.API_URL}/reset-password`, {
      token,
      newPassword
    }).pipe(
      tap(() => {
        this.notification.showSuccess('Password reset successful! Please log in.');
        this.router.navigate(['/auth/login']);
      }),
      catchError(error => {
        this.notification.showError('Password reset failed or token expired.');
        return throwError(() => error);
      })
    );
  }

  /**
   * Gets current user information.
   */
  getCurrentUser(): Observable<UserInfo> {
    return this.http.get<UserInfo>(`${this.API_URL}/me`).pipe(
      tap(user => {
        this.user = user;
      })
    );
  }

  /**
   * Checks if user has specific role.
   */
  hasRole(role: string): boolean {
    const user = this.user;
    return user ? user.roles.includes(role) : false;
  }

  /**
   * Checks if user has any of the specified roles.
   */
  hasAnyRole(roles: string[]): boolean {
    const user = this.user;
    return user ? roles.some(role => user.roles.includes(role)) : false;
  }

  // Getters and setters
  get token(): string | null {
    return this.storage.get(this.TOKEN_KEY);
  }

  set token(token: string | null) {
    if (token) {
      this.storage.set(this.TOKEN_KEY, token);
      this.apiService.setAuthToken(token);
    } else {
      this.storage.remove(this.TOKEN_KEY);
      this.apiService.setAuthToken(null);
    }
  }

  get isAuthenticated(): boolean {
    return this.isAuthenticatedSubject.value;
  }

  set isAuthenticated(isAuthenticated: boolean) {
    this.isAuthenticatedSubject.next(isAuthenticated);
  }

  get user(): UserInfo | null {
    return this.userSubject.value;
  }

  set user(user: UserInfo | null) {
    this.userSubject.next(user);
    if (user) {
      this.storage.set(this.USER_KEY, JSON.stringify(user));
    } else {
      this.storage.remove(this.USER_KEY);
    }
  }

  get isLoading(): boolean {
    return this.isLoadingSubject.value;
  }

  set isLoading(isLoading: boolean) {
    this.isLoadingSubject.next(isLoading);
  }

  get error(): string | null {
    return this.errorSubject.value;
  }

  set error(error: string | null) {
    this.errorSubject.next(error);
  }

  // Private helper methods

  /**
   * Handles successful login.
   */
  private handleLoginSuccess(response: LoginResponse, rememberMe: boolean = false): void {
    this.token = response.accessToken;
    this.storage.set(this.REFRESH_TOKEN_KEY, response.refreshToken, rememberMe);
    this.isAuthenticated = true;
    
    // Get user info
    this.getCurrentUser().subscribe();
    this.notification.showSuccess(`Welcome back, ${response.username}!`);
    
    // Schedule token refresh
    this.scheduleTokenRefresh();
  }

  /**
   * Handles successful token refresh.
   */
  private handleTokenRefresh(response: LoginResponse): void {
    this.token = response.accessToken;
    this.scheduleTokenRefresh();
  }

  /**
   * Handles authentication errors.
   */
  private handleAuthError(message: string): void {
    this.error = message;
    this.notification.showError(message);
  }

  /**
   * Clears authentication state.
   */
  private clearAuth(): void {
    this.token = null;
    this.storage.remove(this.REFRESH_TOKEN_KEY);
    this.storage.remove(this.USER_KEY);
    this.isAuthenticated = false;
    this.user = null;
    this.error = null;
    
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
    }
  }

  /**
   * Schedules automatic token refresh.
   */
  private scheduleTokenRefresh(): void {
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
    }

    // Refresh token 5 minutes before it expires
    const tokenExpiry = this.getTokenExpiry();
    const refreshTime = Math.max(0, tokenExpiry - 5 * 60 * 1000);

    if (refreshTime > 0) {
      this.refreshTimer = setTimeout(() => {
        this.refreshToken().subscribe({
          error: () => {
            console.log('Auto refresh failed, user may need to re-login');
          }
        });
      }, refreshTime);
    }
  }

  /**
   * Gets token expiry time from JWT token.
   */
  private getTokenExpiry(): number {
    const token = this.token;
    if (!token) return 0;

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.exp * 1000 - Date.now(); // Return remaining time in ms
    } catch (error) {
      return 0;
    }
  }
}