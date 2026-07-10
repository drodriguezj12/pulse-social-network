import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { anonymousGuard, authGuard } from './auth.guard';
import { AuthStore } from '../stores/auth.store';

describe('route guards', () => {
  const route = {} as ActivatedRouteSnapshot;
  const state = {} as RouterStateSnapshot;

  function setup(isAuthenticated: boolean) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthStore, useValue: { isAuthenticated: () => isAuthenticated } },
      ],
    });
  }

  afterEach(() => TestBed.resetTestingModule());

  it('authGuard allows navigation when authenticated', () => {
    setup(true);
    const result = TestBed.runInInjectionContext(() => authGuard(route, state));
    expect(result).toBeTrue();
  });

  it('authGuard redirects to /login when anonymous', () => {
    setup(false);
    const result = TestBed.runInInjectionContext(() => authGuard(route, state)) as UrlTree;
    expect(result.toString()).toBe('/login');
  });

  it('anonymousGuard redirects authenticated users to the feed', () => {
    setup(true);
    const result = TestBed.runInInjectionContext(() => anonymousGuard(route, state)) as UrlTree;
    expect(result.toString()).toBe('/publicaciones');
  });

  it('anonymousGuard allows anonymous users into /login', () => {
    setup(false);
    const result = TestBed.runInInjectionContext(() => anonymousGuard(route, state));
    expect(result).toBeTrue();
  });
});
