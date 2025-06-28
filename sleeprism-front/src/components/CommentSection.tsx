// src/components/CommentSection.tsx
import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import type { CommentCreateRequestDTO, CommentResponseDTO, ImageUploadResponse } from '../type/comment';
import '../../public/css/CommentSection.css'; // 새로 생성할 CSS 파일 임포트

// JWT 토큰에서 사용자 ID를 디코딩하는 헬퍼 함수 (PostDetailPage와 동일)
const getUserIdFromToken = (): number | null => {
  const token = localStorage.getItem('jwtToken');
  if (!token) {
    return null;
  }
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(function (c) {
          return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        })
        .join('')
    );
    const decodedToken = JSON.parse(jsonPayload);
    const userIdRaw = decodedToken.userId || decodedToken.id || decodedToken.sub;
    const userId = typeof userIdRaw === 'number' ? userIdRaw : parseInt(userIdRaw as string, 10);
    return isNaN(userId) ? null : userId;
  } catch (e) {
    console.error('JWT 토큰 디코딩 중 오류 발생:', e);
    return null;
  }
};

interface CommentSectionProps {
  postId: number;
}

function CommentSection({ postId }: CommentSectionProps) {
  const [comments, setComments] = useState<CommentResponseDTO[]>([]);
  const [newCommentContent, setNewCommentContent] = useState<string>('');
  const [newCommentImageFile, setNewCommentImageFile] = useState<File | null>(null);
  const [newCommentImageUrl, setNewCommentImageUrl] = useState<string | null>(null); // 업로드된 이미지 URL
  const [loadingComments, setLoadingComments] = useState<boolean>(true);
  const [submittingComment, setSubmittingComment] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null); // 파일 입력 필드 참조

  const navigate = useNavigate();
  const currentUserId = getUserIdFromToken(); // 현재 로그인한 사용자 ID

  // 댓글 목록을 불러오는 함수
  const fetchComments = async () => {
    setLoadingComments(true);
    try {
      // 백엔드 API 호출: 특정 게시글의 댓글 조회
      const response = await fetch(`http://localhost:8080/api/comments/post/${postId}`);
      if (!response.ok) {
        throw new Error(`댓글을 불러오는데 실패했습니다: ${response.status}`);
      }
      const data: CommentResponseDTO[] = await response.json();
      // 최신 댓글이 위에 오도록 정렬 (선택 사항, 백엔드에서 정렬해도 됨)
      setComments(
        data.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      );
    } catch (e: any) {
      console.error('댓글 목록 가져오기 오류:', e);
      setError(e.message || '댓글을 불러오는데 실패했습니다.');
    } finally {
      setLoadingComments(false);
    }
  };

  useEffect(() => {
    fetchComments(); // 컴포넌트 마운트 시 댓글 불러오기
  }, [postId]); // postId가 변경될 때마다 댓글 재로딩

  // 이미지 파일 선택 핸들러
  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      // 이미지 파일만 허용
      if (!file.type.startsWith('image/')) {
        alert('이미지 파일 (JPG, PNG, GIF 등)만 첨부할 수 있습니다.');
        setNewCommentImageFile(null);
        setNewCommentImageUrl(null);
        if (fileInputRef.current) fileInputRef.current.value = ''; // 파일 선택 초기화
        return;
      }
      setNewCommentImageFile(file);
      // 미리보기 URL 생성
      setNewCommentImageUrl(URL.createObjectURL(file));
      setError(null);
    } else {
      setNewCommentImageFile(null);
      setNewCommentImageUrl(null);
    }
  };

  // 댓글 작성 제출 핸들러
  const handleCommentSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmittingComment(true);

    const token = localStorage.getItem('jwtToken');
    if (!token) {
      alert('댓글을 작성하려면 로그인이 필요합니다.');
      navigate('/login');
      setSubmittingComment(false);
      return;
    }

    if (!newCommentContent.trim() && !newCommentImageFile) {
      setError('댓글 내용 또는 이미지를 첨부해야 합니다.');
      setSubmittingComment(false);
      return;
    }

    let uploadedImageUrl: string | null = null;
    let uploadedImageType: string | null = null;

    try {
      // 1. 이미지 파일이 있으면 먼저 업로드
      if (newCommentImageFile) {
        const formData = new FormData();
        formData.append('image', newCommentImageFile); // PostController의 @RequestParam("image")와 일치

        // 게시글 이미지 업로드 엔드포인트 재사용
        const uploadResponse = await fetch('http://localhost:8080/api/posts/upload-image', {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`, // 이미지 업로드도 인증 필요
            // 'Content-Type': 'multipart/form-data' 헤더는 FormData 사용 시 자동으로 설정됩니다.
          },
          body: formData,
        });

        const uploadData: ImageUploadResponse = await uploadResponse.json();

        if (!uploadResponse.ok) {
          throw new Error(uploadData.message || '이미지 업로드에 실패했습니다.');
        }
        uploadedImageUrl = uploadData.url;
        uploadedImageType = newCommentImageFile.type.startsWith('image/') ? 'IMAGE' : 'UNKNOWN'; // GIF도 IMAGE로 간주
      }

      // 2. 댓글 생성 DTO 준비
      const commentRequest: CommentCreateRequestDTO = {
        postId: postId,
        parentCommentId: null, // 1단계 댓글
        content: newCommentContent.trim(),
        attachmentUrl: uploadedImageUrl,
        attachmentType: uploadedImageType,
      };

      // 3. 댓글 생성 API 호출
      const createResponse = await fetch('http://localhost:8080/api/comments', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(commentRequest),
      });

      const createData = await createResponse.json();

      if (!createResponse.ok) {
        throw new Error(createData.message || '댓글 작성에 실패했습니다.');
      }

      setNewCommentContent(''); // 폼 초기화
      setNewCommentImageFile(null);
      setNewCommentImageUrl(null);
      if (fileInputRef.current) fileInputRef.current.value = ''; // 파일 입력 필드 초기화

      fetchComments(); // 댓글 목록 새로고침
      alert('댓글이 성공적으로 작성되었습니다!');
    } catch (e: any) {
      console.error('댓글 작성 또는 이미지 업로드 중 오류 발생:', e);
      setError(e.message || '댓글 작성 중 알 수 없는 오류가 발생했습니다.');
    } finally {
      setSubmittingComment(false);
    }
  };

  // 댓글 수정 핸들러 (나중에 구현)
  const handleCommentEdit = (commentId: number) => {
    alert(`댓글 수정 기능 (ID: ${commentId})은 아직 구현되지 않았습니다.`);
    // navigate(`/comments/${commentId}/edit`);
  };

  // 댓글 삭제 핸들러
  const handleCommentDelete = async (commentId: number) => {
    if (!window.confirm('정말로 이 댓글을 삭제하시겠습니까?')) {
      return;
    }

    const token = localStorage.getItem('jwtToken');
    if (!token) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }

    try {
      const response = await fetch(`http://localhost:8080/api/comments/${commentId}`, {
        method: 'DELETE',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || `댓글 삭제에 실패했습니다: ${response.status}`);
      }

      alert('댓글이 성공적으로 삭제되었습니다!');
      fetchComments(); // 댓글 목록 새로고침
    } catch (e: any) {
      console.error('댓글 삭제 중 오류 발생:', e);
      setError(e.message || '댓글 삭제 중 오류가 발생했습니다.');
    }
  };

  return (
   
    <div className="comment-section-container">
      <h3 className="comment-section-title">댓글 ({comments.length})</h3>

      {/* 댓글 작성 폼 */}
      <article className="comment-form-article">
        <div className="comment-avatar-wrapper">
          {/* 아바타 이미지 (고정 또는 동적) */}
          <img className="comment-avatar" src="https://via.placeholder.com/48" alt="작성자 아바타" />
        </div>
        <div className="comment-input-wrapper">
          <form onSubmit={handleCommentSubmit}>
            <div className="comment-textarea-wrapper">
              <textarea
                id="commentContent"
                className="comment-textarea"
                rows={3}
                placeholder="댓글을 작성하세요."
                value={newCommentContent}
                onChange={(e) => setNewCommentContent(e.target.value)}
                required
              ></textarea>
              {/* 이미지 미리보기 */}
              {newCommentImageUrl && (
                <div className="image-preview-wrapper">
                  <img src={newCommentImageUrl} alt="첨부 이미지 미리보기" className="image-preview" />
                  <button
                    type="button"
                    onClick={() => {
                      setNewCommentImageFile(null);
                      setNewCommentImageUrl(null);
                      if (fileInputRef.current) fileInputRef.current.value = '';
                    }}
                    className="remove-image-button"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
                  </button>
                </div>
              )}
            </div>
            <div className="comment-form-actions">
              <label htmlFor="commentImage" className="image-upload-label">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-image"><rect width="18" height="18" x="3" y="3" rx="2" ry="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/></svg>
                <span>사진 추가</span>
              </label>
              <input
                type="file"
                id="commentImage"
                ref={fileInputRef}
                onChange={handleImageChange}
                accept="image/*"
                className="hidden"
              />
              <button
                type="submit"
                disabled={submittingComment}
                className="comment-submit-button"
              >
                {submittingComment ? '작성 중...' : '댓글 작성'}
              </button>
            </div>
          </form>
        </div>
      </article>

      {/* 댓글 목록 */}
      {loadingComments ? (
        <p className="text-center text-gray-600 mt-8">댓글을 불러오는 중...</p>
      ) : comments.length === 0 ? (
        <p className="text-center text-gray-600 mt-8">아직 댓글이 없습니다.</p>
      ) : (
        <div className="comments-list">
          {comments.map((comment) => (
            <article key={comment.id} className="comment-item">
              <footer className="comment-header">
                <div className="comment-author-info">
                  <p className="author-name">{comment.authorNickname}</p>
                  <p className="comment-date">
                    <time dateTime={comment.createdAt}>
                      {new Date(comment.createdAt).toLocaleString()}
                    </time>
                  </p>
                </div>
                {/* 본인 댓글에만 수정/삭제 버튼 표시 */}
                {currentUserId !== null && comment.originalAuthorId === currentUserId && (
                  <div className="comment-actions">
                    <button
                      onClick={() => handleCommentEdit(comment.id)}
                      className="comment-action-button"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-pencil"><path d="M17 3a2.85 2.85 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/><path d="m15 5 3 3"/></svg>
                      <span className="sr-only">수정</span>
                    </button>
                    <button
                      onClick={() => handleCommentDelete(comment.id)}
                      className="comment-action-button"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-trash-2"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/><line x1="10" x2="10" y1="11" y2="17"/><line x1="14" x2="14" y1="11" y2="17"/></svg>
                      <span className="sr-only">삭제</span>
                    </button>
                  </div>
                )}
              </footer>
              <div className="comment-content-body">
                {comment.attachmentUrl && (
                  <div className="comment-image-wrapper">
                    <img
                      src={`http://localhost:8080/${comment.attachmentUrl}`}
                      alt="첨부 이미지"
                      className="comment-image"
                    />
                  </div>
                )}
                <p className="comment-text">{comment.content}</p>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}

export default CommentSection;