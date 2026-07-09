import { Injectable, inject, signal } from '@angular/core';
import { RxStomp, RxStompState } from '@stomp/rx-stomp';
import { PostsStore } from '../stores/posts.store';
import { LikeEvent } from './models';

/**
 * STOMP over WebSocket against posts-service (/ws, proxied by nginx or the
 * dev proxy). Subscribes to /topic/likes and feeds every broadcast into the
 * PostsStore, so like totals move on screen without reloading.
 */
@Injectable({ providedIn: 'root' })
export class WsService {
  private readonly postsStore = inject(PostsStore);
  private readonly rxStomp = new RxStomp();
  private started = false;

  /** Drives the "live" heartbeat dot in the header. */
  readonly connected = signal(false);

  start(): void {
    if (this.started) {
      return;
    }
    this.started = true;

    const protocol = location.protocol === 'https:' ? 'wss' : 'ws';
    this.rxStomp.configure({
      brokerURL: `${protocol}://${location.host}/ws`,
      reconnectDelay: 3000,
    });

    this.rxStomp.connectionState$.subscribe(state =>
      this.connected.set(state === RxStompState.OPEN),
    );
    this.rxStomp.watch('/topic/likes').subscribe(message =>
      this.postsStore.applyLikeEvent(JSON.parse(message.body) as LikeEvent),
    );

    this.rxStomp.activate();
  }
}
