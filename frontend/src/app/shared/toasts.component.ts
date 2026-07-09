import { Component, inject } from '@angular/core';
import { ToastService } from '../core/toast.service';

@Component({
  selector: 'app-toasts',
  template: `
    <div class="toast-stack" role="status" aria-live="polite">
      @for (toast of toasts.toasts(); track toast.id) {
        <div class="toast" [class.success]="toast.kind === 'success'" [class.error]="toast.kind === 'error'">
          @if (toast.kind === 'success') {
            <svg class="icon" width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M20 6 9 17l-5-5" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          } @else {
            <svg class="icon" width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2"/>
              <path d="M12 8v4m0 4h.01" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
            </svg>
          }
          <span>{{ toast.message }}</span>
          <button class="btn-icon" (click)="toasts.dismiss(toast.id)" aria-label="Cerrar notificación">✕</button>
        </div>
      }
    </div>
  `,
})
export class ToastsComponent {
  readonly toasts = inject(ToastService);
}
