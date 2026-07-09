import { computed, inject } from '@angular/core';
import {
  patchState,
  signalStore,
  withComputed,
  withMethods,
  withState,
} from '@ngrx/signals';
import { firstValueFrom } from 'rxjs';
import { PostsApiService } from '../core/api/posts-api.service';
import { httpMessage } from '../core/http-error';
import { LikeEvent, Post } from '../core/models';
import { ToastService } from '../core/toast.service';

interface PostsState {
  posts: Post[];
  loading: boolean;
  creating: boolean;
  loaded: boolean;
  /** Last WebSocket like event, sequenced so the feed can pulse the counter. */
  lastEvent: (LikeEvent & { seq: number }) | null;
}

/**
 * Feed state as an NgRx SignalStore singleton (providedIn: 'root').
 * REST responses and WebSocket like events converge here; components
 * only read signals, so a broadcast updates every open screen at once.
 */
export const PostsStore = signalStore(
  { providedIn: 'root' },
  withState<PostsState>({
    posts: [],
    loading: false,
    creating: false,
    loaded: false,
    lastEvent: null,
  }),
  withComputed(({ posts, loading, loaded }) => ({
    isEmpty: computed(() => loaded() && !loading() && posts().length === 0),
  })),
  withMethods(store => {
    const api = inject(PostsApiService);
    const toasts = inject(ToastService);
    let eventSeq = 0;

    const replacePost = (postId: string, patch: Partial<Post>) =>
      patchState(store, {
        posts: store.posts().map(p => (p.id === postId ? { ...p, ...patch } : p)),
      });

    return {
      async loadFeed(): Promise<void> {
        patchState(store, { loading: true });
        try {
          const posts = await firstValueFrom(api.feed());
          patchState(store, { posts, loading: false, loaded: true });
        } catch (e) {
          patchState(store, { loading: false, loaded: true });
          toasts.error(httpMessage(e, 'No se pudieron cargar las publicaciones'));
        }
      },

      async createPost(message: string): Promise<boolean> {
        patchState(store, { creating: true });
        try {
          await firstValueFrom(api.create(message));
          patchState(store, { creating: false });
          toasts.success('Publicación creada');
          return true;
        } catch (e) {
          patchState(store, { creating: false });
          toasts.error(httpMessage(e, 'No se pudo crear la publicación'));
          return false;
        }
      },

      /** Optimistic like/unlike, reconciled with the stored-procedure total. */
      async toggleLike(post: Post): Promise<void> {
        const wasLiked = post.likedByMe;
        replacePost(post.id, {
          likedByMe: !wasLiked,
          likeCount: post.likeCount + (wasLiked ? -1 : 1),
        });
        try {
          const res = await firstValueFrom(
            wasLiked ? api.unlike(post.id) : api.like(post.id),
          );
          replacePost(post.id, { likedByMe: res.likedByMe, likeCount: res.likeCount });
        } catch (e) {
          replacePost(post.id, { likedByMe: wasLiked, likeCount: post.likeCount });
          toasts.error(httpMessage(e, 'No se pudo registrar el like'));
        }
      },

      /** Entry point for WebSocket broadcasts (see WsService). */
      applyLikeEvent(event: LikeEvent): void {
        replacePost(event.postId, { likeCount: event.likeCount });
        patchState(store, { lastEvent: { ...event, seq: ++eventSeq } });
      },

      reset(): void {
        patchState(store, { posts: [], loaded: false, lastEvent: null });
      },
    };
  }),
);
