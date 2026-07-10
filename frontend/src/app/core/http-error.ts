import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from './models';

/** Extracts the backend's ApiError message when present, else a fallback. */
export function httpMessage(err: unknown, fallback: string): string {
  if (err instanceof HttpErrorResponse) {
    const api = err.error as Partial<ApiError> | null;
    if (api && typeof api.message === 'string' && api.message.length > 0) {
      return api.message;
    }
    if (err.status === 0) {
      return 'No hay conexión con el servidor';
    }
    if (err.status === 413) {
      return 'La imagen no puede superar 2MB';
    }
  }
  return fallback;
}
