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
  loadingMore: boolean;
  creating: boolean;
  loaded: boolean;
  /** Cursor for the next page; null once the whole feed has been read. */
  nextCursor: string | null;
  /** Last WebSocket like event, sequenced so the feed can pulse the counter. */
  lastEvent: (LikeEvent & { seq: number }) | null;
  /** Last post that arrived over the WebSocket, so the feed can animate it in. */
  lastNewPost: { postId: string; seq: number } | null;
}

const PAGE_SIZE = 10;

/**
 * Feed state as an NgRx SignalStore singleton (providedIn: 'root').
 * REST pages and WebSocket events converge here; components only read
 * signals, so one broadcast updates every open screen at once.
 */
export const PostsStore = signalStore(
  { providedIn: 'root' },
  withState<PostsState>({
    posts: [],
    loading: false,
    loadingMore: false,
    creating: false,
    loaded: false,
    nextCursor: null,
    lastEvent: null,
    lastNewPost: null,
  }),
  withComputed(({ posts, loading, loaded, nextCursor }) => ({
    isEmpty: computed(() => loaded() && !loading() && posts().length === 0),
    hasMore: computed(() => nextCursor() !== null),
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
      /** Loads the first page, replacing whatever the feed currently holds. */
      async loadFeed(): Promise<void> {
        patchState(store, { loading: true });
        try {
          const page = await firstValueFrom(api.feed(null, PAGE_SIZE));
          patchState(store, {
            posts: page.items,
            nextCursor: page.nextCursor,
            loading: false,
            loaded: true,
          });
        } catch (e) {
          patchState(store, { loading: false, loaded: true });
          toasts.error(httpMessage(e, 'No se pudieron cargar las publicaciones'));
        }
      },

      /** Appends the next page. Safe to call repeatedly: it no-ops while busy. */
      async loadMore(): Promise<void> {
        const cursor = store.nextCursor();
        if (!cursor || store.loadingMore() || store.loading()) {
          return;
        }
        patchState(store, { loadingMore: true });
        try {
          const page = await firstValueFrom(api.feed(cursor, PAGE_SIZE));
          const known = new Set(store.posts().map(p => p.id));
          patchState(store, {
            posts: [...store.posts(), ...page.items.filter(p => !known.has(p.id))],
            nextCursor: page.nextCursor,
            loadingMore: false,
          });
        } catch (e) {
          patchState(store, { loadingMore: false });
          toasts.error(httpMessage(e, 'No se pudieron cargar más publicaciones'));
        }
      },

      async createPost(message: string, image?: File | null): Promise<boolean> {
        patchState(store, { creating: true });
        try {
          const created = await firstValueFrom(api.create(message, image));
          this.applyNewPost(created);
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

      async deletePost(postId: string): Promise<boolean> {
        try {
          await firstValueFrom(api.delete(postId));
          this.applyDeletedPost(postId);
          toasts.success('Publicación eliminada');
          return true;
        } catch (e) {
          toasts.error(httpMessage(e, 'No se pudo eliminar la publicación'));
          return false;
        }
      },

      /** Called after renaming the profile, so old posts show the new alias. */
      async syncAuthorAlias(): Promise<void> {
        try {
          await firstValueFrom(api.syncAuthorAlias());
        } catch {
          // Cosmetic: the next feed load will still show the new alias on new posts.
        }
      },

      /** Entry point for WebSocket like broadcasts (see WsService). */
      applyLikeEvent(event: LikeEvent): void {
        replacePost(event.postId, { likeCount: event.likeCount });
        patchState(store, { lastEvent: { ...event, seq: ++eventSeq } });
      },

      /**
       * Entry point for WebSocket new-post broadcasts. Prepends the post so
       * every open feed grows in real time. Duplicate frames are ignored, so
       * the creator can also receive the broadcast after the REST response.
       */
      applyNewPost(post: Post): void {
        if (store.posts().some(p => p.id === post.id)) {
          return; // duplicate frame guard
        }
        patchState(store, {
          posts: [post, ...store.posts()],
          lastNewPost: { postId: post.id, seq: ++eventSeq },
        });
      },

      /** Entry point for WebSocket deletions: drop the card wherever it is open. */
      applyDeletedPost(postId: string): void {
        patchState(store, {
          posts: store.posts().filter(post => post.id !== postId),
        });
      },

      reset(): void {
        patchState(store, {
          posts: [],
          loaded: false,
          nextCursor: null,
          lastEvent: null,
          lastNewPost: null,
        });
      },
    };
  }),
);
