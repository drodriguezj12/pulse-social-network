import { NgStyle } from '@angular/common';
import { Component, computed, effect, input, signal } from '@angular/core';
import { avatarStyle, initialOf } from '../core/avatar';

/**
 * Profile picture with graceful fallback: tries GET /users/{id}/avatar and,
 * when the user has no picture (404), shows the deterministic initial disc.
 * `bust` forces a fresh URL after the user changes their own picture.
 */
@Component({
  selector: 'app-avatar',
  imports: [NgStyle],
  template: `
    @if (!failed()) {
      <img class="avatar-img" [src]="src()" [alt]="alias()" (error)="failed.set(true)" />
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

  readonly failed = signal(false);

  readonly src = computed(() =>
    `/users/${this.userId()}/avatar${this.bust() ? `?v=${this.bust()}` : ''}`,
  );
  readonly fallbackStyle = computed(() => avatarStyle(this.alias()));
  readonly initial = computed(() => initialOf(this.alias()));

  constructor() {
    // A new user or a fresh upload deserves a new attempt at loading the image.
    effect(() => {
      this.userId();
      this.bust();
      this.failed.set(false);
    });
  }
}
