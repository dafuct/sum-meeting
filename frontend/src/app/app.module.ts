import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { AppRoutingModule } from './app-routing.module';
import { SharedModule } from './shared/shared.module';
import { MaterialModule } from './shared/material/material.module';

// Feature Modules
import { DashboardModule } from './features/dashboard/dashboard.module';
import { DetectionModule } from './features/detection/detection.module';
import { AuthenticationModule } from './features/authentication/authentication.module';

// Store
import { StoreModule } from '@ngrx/store';
import { StoreDevtoolsModule } from '@ngrx/store-devtools';
import { EffectsModule } from '@ngrx/effects';
import { StoreRouterConnectingModule } from '@ngrx/router-store';

// Root Component
import { AppComponent } from './app.component';

// Environment
import { environment } from '../environments/environment';
import { reducers } from './core/store/reducers';

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    AppRoutingModule,
    SharedModule,
    MaterialModule,
    DashboardModule,
    DetectionModule,
    AuthenticationModule,

    // NGRX Store
    StoreModule.forRoot(reducers, { metaReducers: [] }),
    StoreDevtoolsModule.instrument({ 
      maxAge: 25, 
      logOnly: !environment.production 
    }),
    StoreRouterConnectingModule.forRoot(),
    EffectsModule.forRoot([])
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }