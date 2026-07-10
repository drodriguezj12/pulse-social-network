import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { LoginResponse, UserProfile } from '../models';

/**
 * auth-service endpoints. Paths are relative: nginx (Docker) or the Angular
 * dev proxy route /auth and /users to auth-service, so the SPA needs no
 * per-environment base URL and no CORS.
 */
@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/auth/login', { username, password });
  }

  me(): Observable<UserProfile> {
    return this.http.get<UserProfile>('/users/me');
  }

  updateProfile(firstName: string, lastName: string): Observable<UserProfile> {
    return this.http.put<UserProfile>('/users/me', { firstName, lastName });
  }

  uploadAvatar(image: File): Observable<UserProfile> {
    const form = new FormData();
    form.append('image', image);
    return this.http.put<UserProfile>('/users/me/avatar', form);
  }
}
