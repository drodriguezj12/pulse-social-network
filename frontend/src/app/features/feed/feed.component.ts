import {
  Component,
  ElementRef,
  NgZone,
  OnInit,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { Post } from '../../core/models';
import { TimeAgoPipe } from '../../core/time-ago.pipe';
import { WsService } from '../../core/ws.service';
import { AvatarComponent } from '../../shared/avatar.component';
import { AuthStore } from '../../stores/auth.store';
import { PostsStore } from '../../stores/posts.store';

@Component({
  selector: 'app-feed',
  imports: [RouterLink, TimeAgoPipe, AvatarComponent],
  templateUrl: './feed.component.html',
  styleUrl: './feed.component.css',
})
export class FeedComponent implements OnInit {
  readonly store = inject(PostsStore);
  readonly auth = inject(AuthStore);
  private readonly ws = inject(WsService);
  private readonly zone = inject(NgZone);

  readonly skeletons = [0, 1, 2];

  /** Post ids whose counter is pulsing after a WebSocket like event. */
  readonly pulsing = signal<ReadonlySet<string>>(new Set());
  /** Post ids that just arrived over the WebSocket (slide-in animation). */
  readonly entering = signal<ReadonlySet<string>>(new Set());

  /** Element at the end of the list that triggers loading the next page. */
  private readonly sentinel = viewChild<ElementRef<HTMLElement>>('sentinel');

  constructor() {
    effect(() => {
      const event = this.store.lastEvent();
      if (event) {
        this.flash(this.pulsing, event.postId, 500);
      }
    });

    effect(() => {
      const arrival = this.store.lastNewPost();
      if (arrival) {
        this.flash(this.entering, arrival.postId, 700);
      }
    });

    // Infinite scroll: observe the sentinel whenever it is on screen. The
    // "Cargar más" button below it keeps the feature usable without the
    // observer (keyboard navigation, or a browser that lacks the API).
    effect(onCleanup => {
      const element = this.sentinel()?.nativeElement;
      if (!element || typeof IntersectionObserver === 'undefined') {
        return;
      }
      const observer = new IntersectionObserver(
        entries => {
          if (entries.some(entry => entry.isIntersecting)) {
            this.zone.run(() => void this.store.loadMore());
          }
        },
        { rootMargin: '400px' },   // start fetching before the user hits the end
      );
      observer.observe(element);
      onCleanup(() => observer.disconnect());
    });
  }

  ngOnInit(): void {
    this.ws.start();
    void this.store.loadFeed();
  }

  toggleLike(post: Post): void {
    void this.store.toggleLike(post);
  }

  deletePost(post: Post): void {
    void this.store.deletePost(post.id);
  }

  loadMore(): void {
    void this.store.loadMore();
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
