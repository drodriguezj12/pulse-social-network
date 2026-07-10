export interface UserProfile {
  id: string;
  username: string;
  firstName: string;
  lastName: string;
  birthDate: string;
  alias: string;
  hasAvatar: boolean;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresInMs: number;
  user: UserProfile;
}

export interface Post {
  id: string;
  authorId: string;
  authorAlias: string;
  message: string;
  publishedAt: string;
  likeCount: number;
  likedByMe: boolean;
  hasImage: boolean;
}

export interface LikeResponse {
  postId: string;
  likeCount: number;
  likedByMe: boolean;
}

/** Broadcast on /topic/likes by posts-service. */
export interface LikeEvent {
  postId: string;
  likeCount: number;
}

/** Error body produced by both services' @RestControllerAdvice. */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
