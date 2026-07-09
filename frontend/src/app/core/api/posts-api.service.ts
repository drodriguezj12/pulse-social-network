import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { LikeResponse, Post } from '../models';

/** posts-service endpoints (routed by nginx / dev proxy, same as AuthApiService). */
@Injectable({ providedIn: 'root' })
export class PostsApiService {
  private readonly http = inject(HttpClient);

  feed(): Observable<Post[]> {
    return this.http.get<Post[]>('/posts');
  }

  create(message: string): Observable<Post> {
    return this.http.post<Post>('/posts', { message });
  }

  like(postId: string): Observable<LikeResponse> {
    return this.http.post<LikeResponse>(`/posts/${postId}/likes`, null);
  }

  unlike(postId: string): Observable<LikeResponse> {
    return this.http.delete<LikeResponse>(`/posts/${postId}/likes`);
  }
}
