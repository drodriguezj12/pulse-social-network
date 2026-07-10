import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Post } from '../../core/models';
import { TimeAgoPipe } from '../../core/time-ago.pipe';
import { WsService } from '../../core/ws.service';
import { AvatarComponent } from '../../shared/avatar.component';
import { PostsStore } from '../../stores/posts.store';

@Component({
  selector: 'app-feed',
  imports: [RouterLink, TimeAgoPipe, AvatarComponent],
  templateUrl: './feed.component.html',
  styleUrl: './feed.component.css',
})
export class FeedComponent implements OnInit {
  readonly store = inject(PostsStore);
  private readonly ws = inject(WsService);

  readonly skeletons = [0, 1, 2];

  /** Post ids whose counter is pulsing after a WebSocket like event. */
  readonly pulsing = signal<ReadonlySet<string>>(new Set());
  /** Post ids that just arrived over the WebSocket (slide-in animation). */
  readonly entering = signal<ReadonlySet<string>>(new Set());

  constructor() {
    effect(() => {
      const event = this.store.lastEvent();
      if (!event) {
        return;
      }
      this.flash(this.pulsing, event.postId, 500);
    });
    effect(() => {
      const arrival = this.store.lastNewPost();
      if (!arrival) {
        return;
      }
      this.flash(this.entering, arrival.postId, 700);
    });
  }

  ngOnInit(): void {
    this.ws.start();
    void this.store.loadFeed();
  }

  toggleLike(post: Post): void {
    void this.store.toggleLike(post);
  }

  private flash(target: typeof this.pulsing, id: string, ms: number): void {
    target.update(ids => new Set(ids).add(id));
    setTimeout(() => {
      target.update(ids => {
        const next = new Set(ids);
        next.delete(id);
        return next;
      });
    }, ms);
  }
}
