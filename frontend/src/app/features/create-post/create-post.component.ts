import { Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastService } from '../../core/toast.service';
import { PostsStore } from '../../stores/posts.store';

const MAX_LENGTH = 500;
const MAX_IMAGE_BYTES = 2 * 1024 * 1024;
const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

@Component({
  selector: 'app-create-post',
  imports: [ReactiveFormsModule],
  templateUrl: './create-post.component.html',
  styleUrl: './create-post.component.css',
})
export class CreatePostComponent {
  readonly store = inject(PostsStore);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastService);

  readonly maxLength = MAX_LENGTH;
  readonly message = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(MAX_LENGTH)],
  });

  readonly imageFile = signal<File | null>(null);
  readonly imagePreview = signal<string | null>(null);

  get remaining(): number {
    return MAX_LENGTH - this.message.value.length;
  }

  get showError(): boolean {
    return this.message.invalid && this.message.touched;
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = ''; // allow picking the same file again later
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
    this.clearImage();
    this.imageFile.set(file);
    this.imagePreview.set(URL.createObjectURL(file));
  }

  clearImage(): void {
    const url = this.imagePreview();
    if (url) {
      URL.revokeObjectURL(url);
    }
    this.imageFile.set(null);
    this.imagePreview.set(null);
  }

  async submit(event?: SubmitEvent): Promise<void> {
    event?.preventDefault();
    const value = this.message.value.trim();
    if (!value) {
      this.message.markAsTouched();
      return;
    }
    const created = await this.store.createPost(value, this.imageFile());
    if (created) {
      this.message.reset();
      this.clearImage();
      await this.router.navigateByUrl('/publicaciones');
    }
  }
}
