import { Routes } from '@angular/router';
import { anonymousGuard, authGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [anonymousGuard],
    loadComponent: () =>
      import('./features/login/login.component').then(m => m.LoginComponent),
  },
  {
    path: 'publicaciones',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/feed/feed.component').then(m => m.FeedComponent),
  },
  {
    path: 'crear',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/create-post/create-post.component').then(
        m => m.CreatePostComponent,
      ),
  },
  {
    path: 'perfil',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/profile/profile.component').then(m => m.ProfileComponent),
  },
  { path: '', pathMatch: 'full', redirectTo: 'publicaciones' },
  { path: '**', redirectTo: 'publicaciones' },
];
