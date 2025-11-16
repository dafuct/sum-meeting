import { Component } from '@angular/core';

@Component({
  selector: 'app-dashboard',
  template: `
    <div class="dashboard">
      <h2>Dashboard</h2>
      <p>Welcome to Zoom Transcriber Dashboard</p>
      <div class="dashboard-cards">
        <div class="card">
          <h3>Active Meetings</h3>
          <p class="metric">0</p>
        </div>
        <div class="card">
          <h3>Total Transcriptions</h3>
          <p class="metric">0</p>
        </div>
        <div class="card">
          <h3>Today's Summary</h3>
          <p class="metric">0</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .dashboard {
      padding: 2rem;
    }
    
    .dashboard-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 1.5rem;
      margin-top: 2rem;
    }
    
    .card {
      background: white;
      border-radius: 8px;
      padding: 1.5rem;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      text-align: center;
    }
    
    .card h3 {
      margin: 0 0 1rem 0;
      color: #666;
    }
    
    .metric {
      font-size: 2rem;
      font-weight: bold;
      color: #3f51b5;
      margin: 0;
    }
  `]
})
export class DashboardComponent {}