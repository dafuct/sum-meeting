import { Injectable } from '@angular/core';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';

// Notification types
export enum NotificationType {
  SUCCESS = 'success',
  ERROR = 'error',
  WARNING = 'warning',
  INFO = 'info'
}

// Notification interface
export interface Notification {
  message: string;
  type: NotificationType;
  duration?: number;
  action?: string;
  actionCallback?: () => void;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly DEFAULT_DURATION = 5000;
  private readonly LONG_DURATION = 10000;

  constructor(
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  /**
   * Shows a success notification.
   */
  showSuccess(message: string, duration?: number): void {
    this.show({
      message,
      type: NotificationType.SUCCESS,
      duration: duration || this.DEFAULT_DURATION
    });
  }

  /**
   * Shows an error notification.
   */
  showError(message: string, duration?: number): void {
    this.show({
      message,
      type: NotificationType.ERROR,
      duration: duration || this.LONG_DURATION
    });
  }

  /**
   * Shows a warning notification.
   */
  showWarning(message: string, duration?: number): void {
    this.show({
      message,
      type: NotificationType.WARNING,
      duration: duration || this.DEFAULT_DURATION
    });
  }

  /**
   * Shows an info notification.
   */
  showInfo(message: string, duration?: number): void {
    this.show({
      message,
      type: NotificationType.INFO,
      duration: duration || this.DEFAULT_DURATION
    });
  }

  /**
   * Shows a notification with an action.
   */
  showWithAction(
    message: string,
    action: string,
    actionCallback: () => void,
    type: NotificationType = NotificationType.INFO,
    duration?: number
  ): void {
    this.show({
      message,
      type,
      action,
      actionCallback,
      duration: duration || this.LONG_DURATION
    });
  }

  /**
   * Shows a custom notification.
   */
  show(notification: Notification): void {
    const config: MatSnackBarConfig = {
      duration: notification.duration || this.DEFAULT_DURATION,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: this.getPanelClass(notification.type)
    };

    const snackBarRef = this.snackBar.open(
      notification.message,
      notification.action,
      config
    );

    // Handle action callback
    if (notification.actionCallback) {
      snackBarRef.onAction().subscribe(() => {
        notification.actionCallback!();
      });
    }

    // Handle dismissal
    snackBarRef.afterDismissed().subscribe(() => {
      console.log('Notification dismissed:', notification.message);
    });
  }

  /**
   * Shows a confirmation dialog.
   */
  confirm(
    title: string,
    message: string,
    confirmText: string = 'Confirm',
    cancelText: string = 'Cancel'
  ): Promise<boolean> {
    return new Promise((resolve) => {
      // This would use a custom confirmation dialog component
      // For now, using browser's confirm
      const result = window.confirm(`${title}\n\n${message}`);
      resolve(result);
    });
  }

  /**
   * Shows an alert dialog.
   */
  alert(
    title: string,
    message: string,
    buttonText: string = 'OK'
  ): Promise<void> {
    return new Promise((resolve) => {
      // This would use a custom alert dialog component
      // For now, using browser's alert
      window.alert(`${title}\n\n${message}`);
      resolve();
    });
  }

  /**
   * Shows a toast notification (short message).
   */
  toast(message: string, type: NotificationType = NotificationType.INFO): void {
    this.show({
      message,
      type,
      duration: 3000 // Short duration for toast
    });
  }

  /**
   * Shows a persistent notification (no auto-dismiss).
   */
  persistent(message: string, type: NotificationType): void {
    this.show({
      message,
      type,
      duration: 0 // No auto-dismiss
    });
  }

  /**
   * Dismisses all active notifications.
   */
  dismissAll(): void {
    this.snackBar.dismiss();
  }

  /**
   * Shows progress notification.
   */
  showProgress(message: string, progress: number): void {
    this.show({
      message: `${message} (${progress}%)`,
      type: NotificationType.INFO,
      duration: this.LONG_DURATION
    });
  }

  /**
   * Shows success message for completed operation.
   */
  operationSuccess(operation: string): void {
    this.showSuccess(`${operation} completed successfully!`);
  }

  /**
   * Shows error message for failed operation.
   */
  operationFailed(operation: string, error?: string): void {
    const message = error ? 
      `${operation} failed: ${error}` : 
      `${operation} failed. Please try again.`;
    this.showError(message);
  }

  /**
   * Shows validation error messages.
   */
  showValidationErrors(errors: { [key: string]: string }): void {
    const messages = Object.values(errors);
    if (messages.length > 0) {
      this.showError(messages.join(', '));
    }
  }

  /**
   * Shows network error message.
   */
  networkError(): void {
    this.showError('Network error. Please check your internet connection.');
  }

  /**
   * Shows unauthorized error message.
   */
  unauthorized(): void {
    this.showError('You are not authorized to perform this action.');
  }

  /**
   * Shows generic error message.
   */
  genericError(): void {
    this.showError('An unexpected error occurred. Please try again later.');
  }

  /**
   * Gets CSS panel class for notification type.
   */
  private getPanelClass(type: NotificationType): string[] {
    switch (type) {
      case NotificationType.SUCCESS:
        return ['notification-success'];
      case NotificationType.ERROR:
        return ['notification-error'];
      case NotificationType.WARNING:
        return ['notification-warning'];
      case NotificationType.INFO:
        return ['notification-info'];
      default:
        return ['notification-default'];
    }
  }

  /**
   * Shows notification with custom HTML content.
   */
  showHtml(html: string, type: NotificationType = NotificationType.INFO, duration?: number): void {
    this.show({
      message: html,
      type,
      duration: duration || this.DEFAULT_DURATION
    });
  }

  /**
   * Shows notification with custom icon.
   */
  showWithIcon(
    message: string,
    icon: string,
    type: NotificationType = NotificationType.INFO,
    duration?: number
  ): void {
    const messageWithIcon = `${icon} ${message}`;
    this.show({
      message: messageWithIcon,
      type,
      duration: duration || this.DEFAULT_DURATION
    });
  }
}