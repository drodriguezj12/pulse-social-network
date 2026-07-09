import { NgStyle } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { avatarStyle, initialOf } from './core/avatar';
import { WsService } from './core/ws.service';
import { ToastsComponent } from './shared/toasts.component';
import { AuthStore } from './stores/auth.store';
import { PostsStore } from './stores/posts.store';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NgStyle, ToastsComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  readonly auth = inject(AuthStore);
  readonly ws = inject(WsService);
  private readonly posts = inject(PostsStore);

  readonly avatarStyle = avatarStyle;
  readonly initialOf = initialOf;

  logout(): void {
    this.posts.reset();
    this.auth.logout();
  }
}
