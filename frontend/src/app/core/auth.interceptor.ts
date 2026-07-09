import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthStore } from '../stores/auth.store';

/**
 * Attaches the JWT to every outgoing request and closes the session
 * globally when the backend answers 401 (expired/invalid token).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthStore);
  const token = auth.token();
  const request = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(request).pipe(
    catchError((err: unknown) => {
      if (
        err instanceof HttpErrorResponse &&
        err.status === 401 &&
        !req.url.includes('/auth/login')
      ) {
        auth.logout('Tu sesión expiró. Inicia sesión de nuevo.');
      }
      return throwError(() => err);
    }),
  );
};
