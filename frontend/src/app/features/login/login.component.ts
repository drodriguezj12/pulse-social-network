import { Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthStore } from '../../stores/auth.store';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  readonly auth = inject(AuthStore);

  /** Seeded accounts (see db/seed.sql); all share the demo password. */
  readonly demoUsers = ['mariana', 'carlos', 'valentina', 'andres', 'daniela'];
  readonly demoPassword = 'Pulse2026!';

  readonly form = new FormGroup({
    username: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  invalid(name: 'username' | 'password'): boolean {
    const control = this.form.controls[name];
    return control.invalid && control.touched;
  }

  fill(username: string): void {
    this.auth.clearError();
    this.form.setValue({ username, password: this.demoPassword });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { username, password } = this.form.getRawValue();
    void this.auth.login(username.trim(), password);
  }
}
