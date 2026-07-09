import { Pipe, PipeTransform } from '@angular/core';

/** Relative time in Spanish: "ahora", "hace 5 min", "hace 3 h", "hace 2 días". */
@Pipe({ name: 'timeAgo' })
export class TimeAgoPipe implements PipeTransform {
  transform(iso: string): string {
    const seconds = Math.floor((Date.now() - new Date(iso).getTime()) / 1000);
    if (seconds < 45) return 'ahora';
    if (seconds < 3600) return `hace ${Math.max(1, Math.floor(seconds / 60))} min`;
    if (seconds < 86400) return `hace ${Math.floor(seconds / 3600)} h`;
    const days = Math.floor(seconds / 86400);
    return days === 1 ? 'hace 1 día' : `hace ${days} días`;
  }
}
