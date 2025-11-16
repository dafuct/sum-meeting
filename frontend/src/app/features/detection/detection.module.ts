import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MaterialModule } from '../../shared/material/material.module';
import { DatePipe } from '../../pipes/date.pipe';

import { DetectionComponent } from './detection.component';

@NgModule({
  declarations: [
    DetectionComponent
  ],
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MaterialModule,
    DatePipe
  ],
  exports: [
    DetectionComponent
  ]
})
export class DetectionModule {}