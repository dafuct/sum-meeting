import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-container">
      <div class="login-form">
        <h2>Login</h2>
        <p>Sign in to your Zoom Transcriber account</p>
        
        <form (ngSubmit)="onSubmit()" #loginForm="ngForm">
          <div class="form-group">
            <label for="email">Email</label>
            <input 
              type="email" 
              id="email" 
              name="email" 
              ngModel 
              required 
              email 
              #email="ngModel"
              placeholder="Enter your email">
            <div class="error" *ngIf="email.errors && email.touched">
              Please enter a valid email
            </div>
          </div>
          
          <div class="form-group">
            <label for="password">Password</label>
            <input 
              type="password" 
              id="password" 
              name="password" 
              ngModel 
              required 
              #password="ngModel"
              placeholder="Enter your password">
            <div class="error" *ngIf="password.errors && password.touched">
              Password is required
            </div>
          </div>
          
          <div class="form-actions">
            <button type="submit" [disabled]="loginForm.invalid">
              Login
            </button>
          </div>
        </form>
        
        <div class="auth-links">
          <p>Don't have an account? <a href="#" (click)="goToRegister()">Register</a></p>
          <p><a href="#" (click)="goToForgotPassword()">Forgot password?</a></p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }
    
    .login-form {
      background: white;
      border-radius: 8px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
      padding: 2rem;
      width: 100%;
      max-width: 400px;
    }
    
    .login-form h2 {
      text-align: center;
      color: #3f51b5;
      margin: 0 0 0.5rem 0;
    }
    
    .login-form > p {
      text-align: center;
      color: #666;
      margin: 0 0 2rem 0;
    }
    
    .form-group {
      margin-bottom: 1.5rem;
    }
    
    .form-group label {
      display: block;
      margin-bottom: 0.5rem;
      font-weight: 500;
      color: #333;
    }
    
    .form-group input {
      width: 100%;
      padding: 0.75rem;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 1rem;
      box-sizing: border-box;
    }
    
    .form-group input:focus {
      outline: none;
      border-color: #3f51b5;
      box-shadow: 0 0 0 2px rgba(63, 81, 181, 0.2);
    }
    
    .error {
      color: #f44336;
      font-size: 0.875rem;
      margin-top: 0.25rem;
    }
    
    .form-actions {
      margin-top: 2rem;
    }
    
    .form-actions button {
      width: 100%;
      background: #3f51b5;
      color: white;
      border: none;
      padding: 0.75rem;
      border-radius: 4px;
      font-size: 1rem;
      cursor: pointer;
      transition: background-color 0.2s;
    }
    
    .form-actions button:hover:not(:disabled) {
      background: #303f9f;
    }
    
    .form-actions button:disabled {
      background: #ccc;
      cursor: not-allowed;
    }
    
    .auth-links {
      margin-top: 1.5rem;
      text-align: center;
    }
    
    .auth-links p {
      margin: 0.5rem 0;
      color: #666;
    }
    
    .auth-links a {
      color: #3f51b5;
      text-decoration: none;
    }
    
    .auth-links a:hover {
      text-decoration: underline;
    }
  `]
})
export class LoginComponent {
  constructor(private router: Router) {}
  
  onSubmit() {
    console.log('Login submitted');
    // TODO: Implement authentication logic
    this.router.navigate(['/dashboard']);
  }
  
  goToRegister() {
    // TODO: Navigate to registration
    console.log('Navigate to register');
  }
  
  goToForgotPassword() {
    // TODO: Navigate to forgot password
    console.log('Navigate to forgot password');
  }
}