import { NgStyle } from '@angular/common';
import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { avatarStyle, initialOf } from '../../core/avatar';
import { Post } from '../../core/models';
import { TimeAgoPipe } from '../../core/time-ago.pipe';
import { WsService } from '../../core/ws.service';
import { PostsStore } from '../../stores/posts.store';

@Component({
  selector: 'app-feed',
  imports: [NgStyle, RouterLink, TimeAgoPipe],
  templateUrl: './feed.component.html',
  styleUrl: './feed.component.css',
})
export class FeedComponent implements OnInit {
  readonly store = inject(PostsStore);
  private readonly ws = inject(WsService);

  readonly avatarStyle = avatarStyle;
  readonly initialOf = initialOf;
  readonly skeletons = [0, 1, 2];

  /** Post ids whose counter is pulsing after a WebSocket like event. */
  readonly pulsing = signal<ReadonlySet<string>>(new Set());

  constructor() {
    effect(() => {
      const event = this.store.lastEvent();
      if (!event) {
        return;
      }
      this.pulsing.update(ids => new Set(ids).add(event.postId));
      setTimeout(() => {
        this.pulsing.update(ids => {
          const next = new Set(ids);
          next.delete(event.postId);
          return next;
        });
      }, 500);
    });
  }

  ngOnInit(): void {
    this.ws.start();
    void this.store.loadFeed();
  }

  toggleLike(post: Post): void {
    void this.store.toggleLike(post);
  }
}
