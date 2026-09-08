import { NgStyle } from '@angular/common';
import { Component, computed, effect, input, signal } from '@angular/core';
import { avatarStyle, initialOf } from '../core/avatar';

/**
 * Users without a picture are remembered here, so a feed showing twenty posts
 * from five authors makes five avatar requests instead of twenty.
 */
const withoutAvatar = new Set<string>();

/**
 * Profile picture with a graceful fallback: it tries GET /users/{id}/avatar and,
 * when the user has no picture (404), shows the deterministic initial disc.
 * `bust` forces a fresh URL after the user changes their own picture.
 */
@Component({
  selector: 'app-avatar',
  imports: [NgStyle],
  template: `
    @if (showImage()) {
      <img class="avatar-img" [src]="src()" [alt]="alias()" (error)="onError()" />
    } @else {
      <span class="avatar-fallback" [ngStyle]="fallbackStyle()">{{ initial() }}</span>
    }
  `,
  styles: `
    :host { display: inline-flex; flex-shrink: 0; }
    .avatar-img,
    .avatar-fallback {
      width: var(--av-size, 2.5rem);
      height: var(--av-size, 2.5rem);
      border-radius: 50%;
    }
    .avatar-img { object-fit: cover; border: 1px solid oklch(1 0 0 / 0.15); }
    .avatar-fallback {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
      text-transform: uppercase;
      font-size: calc(var(--av-size, 2.5rem) * 0.4);
    }
  `,
})
export class AvatarComponent {
  readonly userId = input.required<string>();
  readonly alias = input.required<string>();
  readonly bust = input<number | null>(null);

  private readonly failed = signal(false);

  readonly showImage = computed(() =>
    !this.failed() && (this.bust() !== null || !withoutAvatar.has(this.userId())),
  );
  readonly src = computed(() =>
    `/users/${this.userId()}/avatar${this.bust() ? `?v=${this.bust()}` : ''}`,
  );
  readonly fallbackStyle = computed(() => avatarStyle(this.alias()));
  readonly initial = computed(() => initialOf(this.alias()));

  constructor() {
    // A different user, or a fresh upload, deserves a new attempt.
    effect(() => {
      const id = this.userId();
      const bust = this.bust();
      if (bust !== null) {
        withoutAvatar.delete(id);
      }
      this.failed.set(false);
    });
  }

  onError(): void {
    withoutAvatar.add(this.userId());
    this.failed.set(true);
  }
}
