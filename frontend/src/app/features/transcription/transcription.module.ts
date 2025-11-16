import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { MaterialModule } from '@shared/material/material.module';
import { SharedModule } from '@shared/shared.module';

// Components
import { TranscriptionComponent } from './components/transcription/transcription.component';
import { TranscriptionDisplayComponent } from './components/transcription-display/transcription-display.component';
import { TranscriptionControlsComponent } from './components/transcription-controls/transcription-controls.component';
import { TranscriptionSettingsComponent } from './components/transcription-settings/transcription-settings.component';
import { TranscriptionSearchComponent } from './components/transcription-search/transcription-search.component';
import { TranscriptionExportComponent } from './components/transcription-export/transcription-export.component';

@NgModule({
  declarations: [
    TranscriptionComponent,
    TranscriptionDisplayComponent,
    TranscriptionControlsComponent,
    TranscriptionSettingsComponent,
    TranscriptionSearchComponent,
    TranscriptionExportComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule,
    MaterialModule,
    SharedModule
  ],
  exports: [
    TranscriptionComponent,
    TranscriptionDisplayComponent,
    TranscriptionControlsComponent,
    TranscriptionSettingsComponent,
    TranscriptionSearchComponent,
    TranscriptionExportComponent
  ]
})
export class TranscriptionModule { }