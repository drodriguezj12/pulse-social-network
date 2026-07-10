import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { PostsApiService } from '../core/api/posts-api.service';
import { LikeResponse, Post } from '../core/models';
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
  };

  beforeEach(() => {
    api = jasmine.createSpyObj('PostsApiService', ['feed', 'create', 'like', 'unlike']);
    toasts = jasmine.createSpyObj('ToastService', ['success', 'error']);
    TestBed.configureTestingModule({
      providers: [
        { provide: PostsApiService, useValue: api },
        { provide: ToastService, useValue: toasts },
      ],
    });
    store = TestBed.inject(PostsStore);
  });

  it('loadFeed stores the posts returned by the API', async () => {
    api.feed.and.returnValue(of([post]));

    await store.loadFeed();

    expect(store.posts()).toEqual([post]);
    expect(store.loading()).toBeFalse();
    expect(store.loaded()).toBeTrue();
  });

  it('applyLikeEvent updates the matching post and sequences the event', async () => {
    api.feed.and.returnValue(of([post]));
    await store.loadFeed();

    store.applyLikeEvent({ postId: 'p1', likeCount: 7 });

    expect(store.posts()[0].likeCount).toBe(7);
    expect(store.lastEvent()?.postId).toBe('p1');
    expect(store.lastEvent()?.seq).toBe(1);

    store.applyLikeEvent({ postId: 'p1', likeCount: 8 });
    expect(store.lastEvent()?.seq).toBe(2);
  });

  it('toggleLike applies an optimistic update and reconciles with the server total', async () => {
    api.feed.and.returnValue(of([post]));
    await store.loadFeed();
    const response: LikeResponse = { postId: 'p1', likeCount: 3, likedByMe: true };
    api.like.and.returnValue(of(response));

    await store.toggleLike(store.posts()[0]);

    expect(api.like).toHaveBeenCalledWith('p1');
    expect(store.posts()[0].likeCount).toBe(3);
    expect(store.posts()[0].likedByMe).toBeTrue();
  });

  it('toggleLike reverts the optimistic update when the API fails', async () => {
    api.feed.and.returnValue(of([post]));
    await store.loadFeed();
    api.like.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })),
    );

    await store.toggleLike(store.posts()[0]);

    expect(store.posts()[0].likeCount).toBe(2);
    expect(store.posts()[0].likedByMe).toBeFalse();
    expect(toasts.error).toHaveBeenCalled();
  });

  it('createPost reports success and failure through toasts', async () => {
    api.create.and.returnValue(of(post));
    expect(await store.createPost('hola')).toBeTrue();
    expect(toasts.success).toHaveBeenCalled();

    api.create.and.returnValue(throwError(() => new HttpErrorResponse({ status: 400 })));
    expect(await store.createPost('x')).toBeFalse();
    expect(toasts.error).toHaveBeenCalled();
  });
});
