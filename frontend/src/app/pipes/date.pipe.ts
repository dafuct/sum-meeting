import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'date'
})
export class DatePipe implements PipeTransform {
  transform(value: string | Date, format?: string): string {
    if (!value) return '';

    const date = typeof value === 'string' ? new Date(value) : value;

    if (format === 'short') {
      return date.toLocaleTimeString([], {
        hour: '2-digit',
        minute: '2-digit'
      });
    }

    return date.toLocaleString();
  }
}