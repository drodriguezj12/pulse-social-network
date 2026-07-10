import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthApiService } from '../../core/api/auth-api.service';
import { UserProfile } from '../../core/models';
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
  private readonly api = inject(AuthApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toasts = inject(ToastService);

  readonly editing = signal(false);
  readonly profile = signal<UserProfile | null>(null);
  readonly isOwnProfile = computed(() => {
    const current = this.auth.user();
    const viewed = this.profile();
    return !!current && !!viewed && current.id === viewed.id;
  });

  readonly form = new FormGroup({
    alias: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(50)],
    }),
  });

  ngOnInit(): void {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        void this.loadProfile(params.get('id'));
      });
  }

  startEditing(): void {
    const user = this.profile();
    if (!user || !this.isOwnProfile()) {
      return;
    }
    this.form.setValue({ alias: user.alias });
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
    const { alias } = this.form.getRawValue();
    const saved = await this.auth.updateProfile(alias.trim());
    if (saved) {
      this.profile.set(this.auth.user());
      this.editing.set(false);
    }
  }

  async onAvatarSelected(event: Event): Promise<void> {
    if (!this.isOwnProfile()) {
      return;
    }
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
    await this.auth.uploadAvatar(file);
    this.profile.set(this.auth.user());
  }

  invalid(name: 'alias'): boolean {
    const control = this.form.controls[name];
    return control.invalid && control.touched;
  }

  private async loadProfile(userId: string | null): Promise<void> {
    this.editing.set(false);
    try {
      if (!userId || userId === this.auth.user()?.id) {
        await this.auth.refreshProfile();
        this.profile.set(this.auth.user());
        return;
      }
      this.profile.set(await firstValueFrom(this.api.user(userId)));
    } catch (e) {
      this.toasts.error('No se pudo cargar el perfil');
    }
  }
}
