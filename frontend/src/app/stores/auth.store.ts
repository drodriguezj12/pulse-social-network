import { computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import {
  patchState,
  signalStore,
  withComputed,
  withHooks,
  withMethods,
  withState,
} from '@ngrx/signals';
import { firstValueFrom } from 'rxjs';
import { AuthApiService } from '../core/api/auth-api.service';
import { httpMessage } from '../core/http-error';
import { UserProfile } from '../core/models';
import { ToastService } from '../core/toast.service';

interface AuthState {
  user: UserProfile | null;
  token: string | null;
  loading: boolean;
  saving: boolean;
  error: string | null;
  /** Bumped after an avatar upload so img URLs skip the browser cache. */
  avatarVersion: number;
}

const TOKEN_KEY = 'pulse.token';
const USER_KEY = 'pulse.user';

/**
 * Session state as an NgRx SignalStore singleton (providedIn: 'root').
 * Components consume signals only (user, token, isAuthenticated, ...);
 * the JWT is persisted in localStorage so a reload keeps the session.
 */
export const AuthStore = signalStore(
  { providedIn: 'root' },
  withState<AuthState>({
    user: null,
    token: null,
    loading: false,
    saving: false,
    error: null,
    avatarVersion: 0,
  }),
  withComputed(({ token, user }) => ({
    isAuthenticated: computed(() => token() !== null),
    alias: computed(() => user()?.alias ?? ''),
  })),
  withMethods(store => {
    const api = inject(AuthApiService);
    const router = inject(Router);
    const toasts = inject(ToastService);
    return {
      async login(username: string, password: string): Promise<void> {
        patchState(store, { loading: true, error: null });
        try {
          const res = await firstValueFrom(api.login(username, password));
          localStorage.setItem(TOKEN_KEY, res.token);
          localStorage.setItem(USER_KEY, JSON.stringify(res.user));
          patchState(store, { user: res.user, token: res.token, loading: false });
          await router.navigateByUrl('/publicaciones');
        } catch (e) {
          patchState(store, {
            loading: false,
            error: httpMessage(e, 'No se pudo iniciar sesión'),
          });
        }
      },

      /** Re-reads the profile from GET /users/me (profile screen). */
      async refreshProfile(): Promise<void> {
        try {
          const user = await firstValueFrom(api.me());
          localStorage.setItem(USER_KEY, JSON.stringify(user));
          patchState(store, { user });
        } catch {
          // A 401 here is handled globally by the interceptor.
        }
      },

      /** Edits the public alias; username and real names stay immutable. */
      async updateProfile(alias: string): Promise<boolean> {
        patchState(store, { saving: true });
        try {
          const res = await firstValueFrom(api.updateProfile(alias));
          localStorage.setItem(TOKEN_KEY, res.token);
          localStorage.setItem(USER_KEY, JSON.stringify(res.user));
          patchState(store, { user: res.user, token: res.token, saving: false });
          toasts.success('Perfil actualizado');
          return true;
        } catch (e) {
          patchState(store, { saving: false });
          toasts.error(httpMessage(e, 'No se pudo actualizar el perfil'));
          return false;
        }
      },

      async uploadAvatar(image: File): Promise<void> {
        patchState(store, { saving: true });
        try {
          const user = await firstValueFrom(api.uploadAvatar(image));
          localStorage.setItem(USER_KEY, JSON.stringify(user));
          patchState(store, { user, saving: false, avatarVersion: Date.now() });
          toasts.success('Foto de perfil actualizada');
        } catch (e) {
          patchState(store, { saving: false });
          toasts.error(httpMessage(e, 'No se pudo subir la imagen'));
        }
      },

      restore(): void {
        const token = localStorage.getItem(TOKEN_KEY);
        const raw = localStorage.getItem(USER_KEY);
        if (token && raw) {
          patchState(store, { token, user: JSON.parse(raw) as UserProfile });
        }
      },

      logout(notice?: string): void {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
        patchState(store, { user: null, token: null, error: null });
        if (notice) {
          toasts.error(notice);
        }
        void router.navigateByUrl('/login');
      },

      clearError(): void {
        patchState(store, { error: null });
      },
    };
  }),
  withHooks({
    onInit(store) {
      store.restore();
    },
  }),
);
