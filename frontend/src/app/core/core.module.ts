import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { ReactiveFormsModule } from '@angular/forms';

// Core Services
import { AuthService } from './services/auth.service';
import { ApiService } from './services/api.service';
import { WebSocketService } from './services/websocket.service';
import { StorageService } from './services/storage.service';
import { NotificationService } from './services/notification.service';

// Guards
import { AuthGuard } from './guards/auth.guard';
import { RoleGuard } from './guards/role.guard';

// Interceptors
import { AuthInterceptor } from './interceptors/auth.interceptor';
import { ErrorInterceptor } from './interceptors/error.interceptor';

@NgModule({
  imports: [
    CommonModule,
    HttpClientModule,
    ReactiveFormsModule
  ],
  providers: [
    // Services
    AuthService,
    ApiService,
    WebSocketService,
    StorageService,
    NotificationService,
    
    // Guards
    AuthGuard,
    RoleGuard,
    
    // Interceptors
    AuthInterceptor,
    ErrorInterceptor
  ]
})
export class CoreModule {
  constructor() {
    console.log('CoreModule initialized');
  }
}