import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../core/toast.service';
import { AvatarComponent } from '../../shared/avatar.component';
import { AuthStore } from '../../stores/auth.store';

const MAX_IMAGE_BYTES = 2 * 1024 * 1024;
const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

@Component({
  selector: 'app-profile',
  imports: [DatePipe, ReactiveFormsModule, AvatarComponent],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
})
export class ProfileComponent implements OnInit {
  readonly auth = inject(AuthStore);
  private readonly toasts = inject(ToastService);

  readonly editing = signal(false);

  readonly form = new FormGroup({
    firstName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(80)],
    }),
    lastName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(80)],
    }),
  });

  ngOnInit(): void {
    // Always re-read from GET /users/me so the screen shows live backend data.
    void this.auth.refreshProfile();
  }

  startEditing(): void {
    const user = this.auth.user();
    if (!user) {
      return;
    }
    this.form.setValue({ firstName: user.firstName, lastName: user.lastName });
    this.editing.set(true);
  }

  cancelEditing(): void {
    this.editing.set(false);
  }

  async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { firstName, lastName } = this.form.getRawValue();
    const saved = await this.auth.updateProfile(firstName.trim(), lastName.trim());
    if (saved) {
      this.editing.set(false);
    }
  }

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    if (!file) {
      return;
    }
    if (!IMAGE_TYPES.includes(file.type)) {
      this.toasts.error('Solo se permiten imágenes JPEG, PNG o WebP');
      return;
    }
    if (file.size > MAX_IMAGE_BYTES) {
      this.toasts.error('La imagen no puede superar 2MB');
      return;
    }
    void this.auth.uploadAvatar(file);
  }

  invalid(name: 'firstName' | 'lastName'): boolean {
    const control = this.form.controls[name];
    return control.invalid && control.touched;
  }
}
