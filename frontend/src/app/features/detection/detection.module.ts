import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MaterialModule } from '../../shared/material/material.module';
import { DatePipe } from '../../pipes/date.pipe';

import { DetectionComponent } from './detection.component';

const routes: Routes = [
  {
    path: '',
    component: DetectionComponent
  }
];

@NgModule({
  declarations: [
    DetectionComponent,
    DatePipe
  ],
  imports: [
    CommonModule,
    RouterModule.forChild(routes),
    FormsModule,
    MaterialModule
  ],
  exports: [
    DetectionComponent,
    DatePipe
  ]
})
export class DetectionModule {}