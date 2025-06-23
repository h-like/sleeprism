export interface CommentResponseDTO {
  id: number;
  postId: number;
  parentCommentId: number | null; // 1단계 댓글이므로 일단 null로 사용됩니다.
  content: string;
  attachmentUrl: string | null; // 첨부 이미지 URL
  attachmentType: string | null; // 첨부 파일 타입 (예: "IMAGE", "GIF")
  authorNickname: string;
  originalAuthorId: number; // 댓글 작성자 ID (수정/삭제 권한 확인용)
  createdAt: string; // ISO 8601 string
  updatedAt: string; // ISO 8601 string
}

export interface CommentCreateRequestDTO {
  postId: number;
  parentCommentId: number | null; // 1단계 댓글이므로 null
  content: string;
  attachmentUrl?: string | null; // 선택적 첨부 URL
  attachmentType?: string | null; // 선택적 첨부 타입
}

export interface CommentUpdateRequestDTO {
  content: string;
  attachmentUrl?: string | null;
  attachmentType?: string | null;
}

// 이미지 업로드 응답 DTO
export interface ImageUploadResponse {
  url: string; // 백엔드에서 반환하는 이미지 URL
}
