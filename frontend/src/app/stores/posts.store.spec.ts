import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { PostsApiService } from '../core/api/posts-api.service';
import { LikeResponse, Post, PostPage } from '../core/models';
import { ToastService } from '../core/toast.service';
import { PostsStore } from './posts.store';

describe('PostsStore', () => {
  let api: jasmine.SpyObj<PostsApiService>;
  let toasts: jasmine.SpyObj<ToastService>;
  let store: InstanceType<typeof PostsStore>;

  const post: Post = {
    id: 'p1',
    authorId: 'u2',
    authorAlias: 'cgomez',
    message: 'hola',
    publishedAt: new Date().toISOString(),
    likeCount: 2,
    likedByMe: false,
    hasImage: false,
  };

  const page = (items: Post[], nextCursor: string | null = null): PostPage => ({
    items,
    nextCursor,
  });

  beforeEach(() => {
    api = jasmine.createSpyObj('PostsApiService', [
      'feed', 'create', 'like', 'unlike', 'delete', 'syncAuthorAlias',
    ]);
    toasts = jasmine.createSpyObj('ToastService', ['success', 'error']);
    TestBed.configureTestingModule({
      providers: [
        { provide: PostsApiService, useValue: api },
        { provide: ToastService, useValue: toasts },
      ],
    });
    store = TestBed.inject(PostsStore);
  });

  it('loadFeed stores the first page returned by the API', async () => {
    api.feed.and.returnValue(of(page([post])));

    await store.loadFeed();

    expect(store.posts()).toEqual([post]);
    expect(store.loading()).toBeFalse();
    expect(store.loaded()).toBeTrue();
    expect(store.hasMore()).toBeFalse();
  });

  it('loadMore appends the next page and follows the cursor until it runs out', async () => {
    const second: Post = { ...post, id: 'p2' };
    api.feed.and.returnValue(of(page([post], 'cursor-1')));
    await store.loadFeed();
    expect(store.hasMore()).toBeTrue();

    api.feed.and.returnValue(of(page([second])));
    await store.loadMore();

    expect(api.feed).toHaveBeenCalledWith('cursor-1', 10);
    expect(store.posts().map(p => p.id)).toEqual(['p1', 'p2']);
    expect(store.hasMore()).toBeFalse();
  });

  it('loadMore does nothing once the cursor is null', async () => {
    api.feed.and.returnValue(of(page([post])));
    await store.loadFeed();
    api.feed.calls.reset();

    await store.loadMore();

    expect(api.feed).not.toHaveBeenCalled();
  });

  it('loadMore never duplicates a post already in the feed', async () => {
    api.feed.and.returnValue(of(page([post], 'cursor-1')));
    await store.loadFeed();

    api.feed.and.returnValue(of(page([post, { ...post, id: 'p2' }])));
    await store.loadMore();

    expect(store.posts().map(p => p.id)).toEqual(['p1', 'p2']);
  });

  it('applyLikeEvent updates the matching post and sequences the event', async () => {
    api.feed.and.returnValue(of(page([post])));
    await store.loadFeed();

    store.applyLikeEvent({ postId: 'p1', likeCount: 7 });

    expect(store.posts()[0].likeCount).toBe(7);
    expect(store.lastEvent()?.postId).toBe('p1');
    expect(store.lastEvent()?.seq).toBe(1);

    store.applyLikeEvent({ postId: 'p1', likeCount: 8 });
    expect(store.lastEvent()?.seq).toBe(2);
  });

  it('toggleLike applies an optimistic update and reconciles with the server total', async () => {
    api.feed.and.returnValue(of(page([post])));
    await store.loadFeed();
    const response: LikeResponse = { postId: 'p1', likeCount: 3, likedByMe: true };
    api.like.and.returnValue(of(response));

    await store.toggleLike(store.posts()[0]);

    expect(api.like).toHaveBeenCalledWith('p1');
    expect(store.posts()[0].likeCount).toBe(3);
    expect(store.posts()[0].likedByMe).toBeTrue();
  });

  it('toggleLike reverts the optimistic update when the API fails', async () => {
    api.feed.and.returnValue(of(page([post])));
    await store.loadFeed();
    api.like.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })),
    );

    await store.toggleLike(store.posts()[0]);

    expect(store.posts()[0].likeCount).toBe(2);
    expect(store.posts()[0].likedByMe).toBeFalse();
    expect(toasts.error).toHaveBeenCalled();
  });

  it('applyNewPost prepends the broadcast post and ignores duplicates', async () => {
    api.feed.and.returnValue(of(page([post])));
    await store.loadFeed();
    const incoming: Post = { ...post, id: 'p2', message: 'nuevo en vivo' };

    store.applyNewPost(incoming);

    expect(store.posts().length).toBe(2);
    expect(store.posts()[0].id).toBe('p2');
    expect(store.lastNewPost()?.postId).toBe('p2');

    store.applyNewPost(incoming); // duplicate frame
    expect(store.posts().length).toBe(2);
  });

  it('applyDeletedPost removes a post deleted somewhere else', async () => {
    api.feed.and.returnValue(of(page([post, { ...post, id: 'p2' }])));
    await store.loadFeed();

    store.applyDeletedPost('p1');

    expect(store.posts().map(p => p.id)).toEqual(['p2']);
  });

  it('createPost reports success and failure through toasts', async () => {
    api.create.and.returnValue(of(post));
    expect(await store.createPost('hola')).toBeTrue();
    expect(toasts.success).toHaveBeenCalled();

    api.create.and.returnValue(throwError(() => new HttpErrorResponse({ status: 400 })));
    expect(await store.createPost('x')).toBeFalse();
    expect(toasts.error).toHaveBeenCalled();
  });

  it('createPost prepends the created post returned by the API', async () => {
    api.feed.and.returnValue(of(page([post])));
    await store.loadFeed();
    const created: Post = { ...post, id: 'p3', authorId: 'u1', message: 'mi post' };
    api.create.and.returnValue(of(created));

    expect(await store.createPost('mi post')).toBeTrue();

    expect(store.posts().map(p => p.id)).toEqual(['p3', 'p1']);
    expect(store.lastNewPost()?.postId).toBe('p3');
  });

  it('deletePost removes a deleted post from the feed', async () => {
    api.feed.and.returnValue(of(page([post])));
    api.delete.and.returnValue(of(void 0));
    await store.loadFeed();

    expect(await store.deletePost(post.id)).toBeTrue();

    expect(api.delete).toHaveBeenCalledWith('p1');
    expect(store.posts()).toEqual([]);
    expect(toasts.success).toHaveBeenCalledWith('Publicación eliminada');
  });

  it('syncAuthorAlias swallows failures: it is a cosmetic follow-up', async () => {
    api.syncAuthorAlias.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })),
    );

    await expectAsync(store.syncAuthorAlias()).toBeResolved();
    expect(toasts.error).not.toHaveBeenCalled();
  });
});
