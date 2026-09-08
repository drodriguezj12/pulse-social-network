import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { LikeResponse, Post, PostPage } from '../models';

/** posts-service endpoints (routed by nginx / dev proxy, same as AuthApiService). */
@Injectable({ providedIn: 'root' })
export class PostsApiService {
  private readonly http = inject(HttpClient);

  /**
   * @param cursor `nextCursor` from the previous page, or null for the first one
   */
  feed(cursor: string | null = null, limit = 10): Observable<PostPage> {
    let params = new HttpParams().set('limit', limit);
    if (cursor) {
      params = params.set('cursor', cursor);
    }
    return this.http.get<PostPage>('/posts', { params });
  }

  create(message: string, image?: File | null): Observable<Post> {
    if (image) {
      const form = new FormData();
      form.append('message', message);
      form.append('image', image);
      return this.http.post<Post>('/posts', form);
    }
    return this.http.post<Post>('/posts', { message });
  }

  like(postId: string): Observable<LikeResponse> {
    return this.http.post<LikeResponse>(`/posts/${postId}/likes`, null);
  }

  unlike(postId: string): Observable<LikeResponse> {
    return this.http.delete<LikeResponse>(`/posts/${postId}/likes`);
  }

  delete(postId: string): Observable<void> {
    return this.http.delete<void>(`/posts/${postId}`);
  }

  /** Realigns the alias denormalized on the caller's posts after a rename. */
  syncAuthorAlias(): Observable<void> {
    return this.http.post<void>('/posts/author-alias', null);
  }
}
