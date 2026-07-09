import { Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { PostsStore } from '../../stores/posts.store';

const MAX_LENGTH = 500;

@Component({
  selector: 'app-create-post',
  imports: [ReactiveFormsModule],
  templateUrl: './create-post.component.html',
  styleUrl: './create-post.component.css',
})
export class CreatePostComponent {
  readonly store = inject(PostsStore);
  private readonly router = inject(Router);

  readonly maxLength = MAX_LENGTH;
  readonly message = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(MAX_LENGTH)],
  });

  get remaining(): number {
    return MAX_LENGTH - this.message.value.length;
  }

  get showError(): boolean {
    return this.message.invalid && this.message.touched;
  }

  async submit(): Promise<void> {
    const value = this.message.value.trim();
    if (!value) {
      this.message.markAsTouched();
      return;
    }
    const created = await this.store.createPost(value);
    if (created) {
      this.message.reset();
      await this.router.navigateByUrl('/publicaciones');
    }
  }
}
