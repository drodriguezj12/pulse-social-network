import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { WsService } from './core/ws.service';
import { AvatarComponent } from './shared/avatar.component';
import { ToastsComponent } from './shared/toasts.component';
import { AuthStore } from './stores/auth.store';
import { PostsStore } from './stores/posts.store';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AvatarComponent, ToastsComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  readonly auth = inject(AuthStore);
  readonly ws = inject(WsService);
  private readonly posts = inject(PostsStore);

  logout(): void {
    this.posts.reset();
    this.auth.logout();
  }
}
