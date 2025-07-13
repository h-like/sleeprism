import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import type { CommentCreateRequestDTO, CommentResponseDTO } from '../type/comment'; // ImageUploadResponse는 이제 직접 사용하지 않음
import '../../public/css/CommentSection.css';
import '../../public/css/PostDetailPage.css'
import type { UserProfile } from '../service/UserService';

// JWT 토큰에서 사용자 ID를 디코딩하는 헬퍼 함수
interface DecodedToken {
  userId?: number;
  id?: number;
  sub?: string;
}

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
    const decodedToken: DecodedToken = JSON.parse(jsonPayload);
    const userIdRaw = decodedToken.userId || decodedToken.id || decodedToken.sub;
    const userId = typeof userIdRaw === 'number' ? userIdRaw : parseInt(userIdRaw as string, 10);
    return isNaN(userId) ? null : userId;
  } catch (e) {
    console.error('JWT 토큰 디코딩 중 오류 발생:', e);
    return null;
  }
};

// 백엔드 기본 URL
const BACKEND_BASE_URL = 'http://localhost:8080';

// URL 경로를 정리하는 헬퍼 함수 (중복 슬래시, 중복 경로 제거)
const cleanUrlPath = (path: string | null): string | null => {
    if (!path) return null;
    // 'comments//' 또는 '/files//files/' 등 중복 슬래시 및 특정 중복 경로 패턴 제거
    let cleaned = path.replace(/\/\/+/g, '/'); // 연속된 슬래시를 하나로 줄임
    cleaned = cleaned.replace(/\/api\/posts\/files\/files\//g, '/api/posts/files/'); // 특정 중복 패턴
    
    return cleaned;
};


interface CommentSectionProps {
  postId: number;
}

function CommentSection({ postId }: CommentSectionProps) {
  const [comments, setComments] = useState<CommentResponseDTO[]>([]);
  const [newCommentContent, setNewCommentContent] = useState<string>('');
  const [newCommentImageFile, setNewCommentImageFile] = useState<File | null>(null);
  const [newCommentImageUrl, setNewCommentImageUrl] = useState<string | null>(null); // 미리보기 URL
  const [loadingComments, setLoadingComments] = useState<boolean>(true);
  const [submittingComment, setSubmittingComment] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const navigate = useNavigate();
  const currentUserId = getUserIdFromToken();
  const [currentUserProfile, setCurrentUserProfile] = useState<UserProfile | null>(null);

  const fetchCurrentUserProfile = async () => {
    const userId = getUserIdFromToken();
    if (!userId) {
      console.log("현재 로그인된 사용자 ID가 없어 프로필을 가져오지 않습니다.");
      setCurrentUserProfile(null);
      return;
    }
    const token = localStorage.getItem('jwtToken');
    if (!token) {
      console.log("JWT 토큰이 없어 프로필을 가져오지 않습니다.");
      setCurrentUserProfile(null);
      return;
    }
    try {
      const response = await fetch(`${BACKEND_BASE_URL}/api/users/profile`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (response.ok) {
        const data: UserProfile = await response.json();
        setCurrentUserProfile(data);
      } else {
        console.error('현재 사용자 프로필을 가져오는데 실패했습니다.', response.status, await response.text());
        setCurrentUserProfile(null);
      }
    } catch (error) {
      console.error('현재 사용자 프로필을 가져오는 중 오류 발생:', error);
      setCurrentUserProfile(null);
    }
  };

  const fetchComments = async () => {
    setLoadingComments(true);
    try {
      const response = await fetch(`${BACKEND_BASE_URL}/api/comments/post/${postId}`);
      if (!response.ok) {
        throw new Error(`댓글을 불러오는데 실패했습니다: ${response.status}`);
      }
      const data: CommentResponseDTO[] = await response.json();
      setComments(data);
    } catch (e: any) {
      console.error('댓글 목록 가져오기 오류:', e);
      setError(e.message || '댓글을 불러오는데 실패했습니다.');
    } finally {
      setLoadingComments(false);
    }
  };

  useEffect(() => {
    fetchCurrentUserProfile();
    fetchComments();
  }, [postId]);

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      if (!file.type.startsWith('image/')) {
        alert('이미지 파일 (JPG, PNG, GIF 등)만 첨부할 수 있습니다.');
        setNewCommentImageFile(null);
        setNewCommentImageUrl(null);
        if (fileInputRef.current) fileInputRef.current.value = '';
        return;
      }
      setNewCommentImageFile(file);
      setNewCommentImageUrl(URL.createObjectURL(file));
      setError(null);
    } else {
      setNewCommentImageFile(null);
      setNewCommentImageUrl(null);
    }
  };

  const handleCommentSubmit = async (e: React.FormEvent, parentCommentId: number | null = null) => {
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
    
    // FormData 생성
    const formData = new FormData();
    // JSON 데이터를 Blob으로 변환하여 추가
    const commentRequestDto = {
      postId: postId,
      parentCommentId: parentCommentId,
      content: newCommentContent.trim(),
    };
    formData.append('requestDto', new Blob([JSON.stringify(commentRequestDto)], { type: 'application/json' }));

    // 이미지 파일이 있으면 FormData에 추가
    if (newCommentImageFile) {
      formData.append('attachmentFile', newCommentImageFile); // 백엔드의 @RequestPart("attachmentFile")과 일치
    }

    try {
      // 댓글 생성 API 호출 (multipart/form-data)
      const createResponse = await fetch(`${BACKEND_BASE_URL}/api/comments`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          // 'Content-Type': 'multipart/form-data' 헤더는 FormData 사용 시 자동으로 설정되므로 명시하지 않습니다.
        },
        body: formData,
      });

      const createData = await createResponse.json();

      if (!createResponse.ok) {
        throw new Error(createData.message || '댓글 작성에 실패했습니다.');
      }

      setNewCommentContent('');
      setNewCommentImageFile(null);
      setNewCommentImageUrl(null);
      if (fileInputRef.current) fileInputRef.current.value = '';

      fetchComments();
      alert('댓글이 성공적으로 작성되었습니다!');
    } catch (e: any) {
      console.error('댓글 작성 또는 이미지 업로드 중 오류 발생:', e);
      setError(e.message || '댓글 작성 중 알 수 없는 오류가 발생했습니다.');
    } finally {
      setSubmittingComment(false);
    }
  };

  const handleCommentEdit = (commentId: number) => {
    alert(`댓글 수정 기능 (ID: ${commentId})은 아직 구현되지 않았습니다.`);
  };

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
      const response = await fetch(`${BACKEND_BASE_URL}/api/comments/${commentId}`, {
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
      fetchComments();
    } catch (e: any) {
      console.error('댓글 삭제 중 오류 발생:', e);
      setError(e.message || '댓글 삭제 중 오류가 발생했습니다.');
    }
  };

  const [replyingTo, setReplyingTo] = useState<number | null>(null);

  const handleReplyClick = (commentId: number) => {
    setReplyingTo(replyingTo === commentId ? null : commentId);
    setNewCommentContent('');
    setNewCommentImageFile(null);
    setNewCommentImageUrl(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const renderComments = (commentsToRender: CommentResponseDTO[]) => {
    return (
      commentsToRender.map((comment) => (
        <React.Fragment key={comment.id}>
          <article className={`comment-item ${comment.parentCommentId ? 'reply-item' : ''}`}>
            <div className="comment-avatar-wrapper">
              <img
                className="comment-avatar"
                src={comment.authorProfileImageUrl 
                    ? `${BACKEND_BASE_URL}${cleanUrlPath(comment.authorProfileImageUrl)}` 
                    : "https://placehold.co/48x48/F0F0F0/ADADAD?text=User"}
                alt="작성자 아바타"
              />
            </div>
            <div className="comment-content-container">
              <footer className="comment-header">
                <div className="comment-author-info">
                  <p className="author-name">{comment.authorNickname}</p>
                  <p className="comment-date">
                    <time dateTime={comment.createdAt}>
                      {new Date(comment.createdAt).toLocaleString('ko-KR', {
                        year: 'numeric',
                        month: 'long',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit',
                        second: '2-digit',
                      })}
                    </time>
                  </p>
                </div>
                <div className="comment-actions">
                  {!comment.isDeleted && currentUserId && (
                    <button
                      onClick={() => handleReplyClick(comment.id)}
                      className="comment-action-button reply-button"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" x2="12" y1="15" y2="3"/></svg>
                      <span className="sr-only">답글</span>
                    </button>
                  )}
                  {currentUserId !== null && comment.authorId === currentUserId && (
                    <>
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
                    </>
                  )}
                </div>
              </footer>
              <div className="comment-content-body">
                {comment.attachmentUrl && (
                  <div className="comment-image-wrapper">
                    {/* 여기 URL 경로 수정: comment.attachmentUrl은 이미 'comments/uuid.jpg' 형태이므로 앞의 백엔드 경로만 붙입니다. */}
                    <img
                      src={`${BACKEND_BASE_URL}/api/comments/files/${comment.attachmentUrl}`} 
                      alt="첨부 이미지"
                      className="comment-image"
                    />
                  </div>
                )}
                <p className="comment-text">{comment.content}</p>
              </div>
            </div>
          </article>
          {replyingTo === comment.id && !comment.isDeleted && (
            <article className="reply-form-article">
                <div className="comment-avatar-wrapper">
                    <img 
                        className="comment-avatar" 
                        src={currentUserProfile?.profileImageUrl 
                            ? `${BACKEND_BASE_URL}${cleanUrlPath(currentUserProfile.profileImageUrl)}` 
                            : "https://placehold.co/48x48/F0F0F0/ADADAD?text=User"} 
                        alt="작성자 아바타" 
                    />
                </div>
                <div className="comment-input-wrapper">
                    <form onSubmit={(e) => handleCommentSubmit(e, comment.id)}>
                        <div className="comment-textarea-wrapper">
                            <textarea
                                id={`replyContent-${comment.id}`}
                                className="comment-textarea"
                                rows={2}
                                placeholder="대댓글을 작성하세요."
                                value={newCommentContent}
                                onChange={(e) => setNewCommentContent(e.target.value)}
                                disabled={!currentUserId || submittingComment}
                            ></textarea>
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
                        {error && <p className="error-message">{error}</p>}
                        <div className="comment-form-actions">
                            <label htmlFor={`replyImage-${comment.id}`} className={`image-upload-label ${!currentUserId ? 'disabled' : ''}`}>
                                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-image"><rect width="18" height="18" x="3" y="3" rx="2" ry="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/></svg>
                                <span>사진 추가</span>
                            </label>
                            <input
                                type="file"
                                id={`replyImage-${comment.id}`}
                                ref={fileInputRef}
                                onChange={handleImageChange}
                                accept="image/*"
                                className="hidden"
                                disabled={!currentUserId || submittingComment}
                            />
                            <button
                                type="submit"
                                disabled={!currentUserId || submittingComment || (!newCommentContent.trim() && !newCommentImageFile)}
                                className="comment-submit-button"
                            >
                                {submittingComment ? '작성 중...' : '답글 작성'}
                            </button>
                        </div>
                    </form>
                </div>
            </article>
          )}

          {/* 재귀 호출에서 comments.children 대신 comment.children 사용 */}
          {comment.children && comment.children.length > 0 && (
            <div className="replies-list">
              {renderComments(comment.children)} 
            </div>
          )}
        </React.Fragment>
      ))
    );
  };


  return (
    <div className="comment-section-container">
      <h3 className="comment-section-title">댓글 ({comments.length})</h3>

      <article className="comment-form-article">
        <div className="comment-avatar-wrapper">
          <img
            className="comment-avatar"
            src={currentUserProfile?.profileImageUrl 
                ? `${BACKEND_BASE_URL}${cleanUrlPath(currentUserProfile.profileImageUrl)}` 
                : "https://placehold.co/48x48/F0F0F0/ADADAD?text=User"}
            alt="작성자 아바타"
          />
        </div>
        <div className="comment-input-wrapper">
          <form onSubmit={(e) => handleCommentSubmit(e, null)}>
            <div className="comment-textarea-wrapper">
              <textarea
                id="commentContent"
                className="comment-textarea"
                rows={3}
                placeholder={currentUserId ? "댓글을 작성하세요." : "로그인 후 댓글을 작성할 수 있습니다."}
                value={newCommentContent}
                onChange={(e) => setNewCommentContent(e.target.value)}
                disabled={!currentUserId || submittingComment}
              ></textarea>
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
            {error && <p className="error-message">{error}</p>}
            <div className="comment-form-actions">
              <label htmlFor="commentImage" className={`image-upload-label ${!currentUserId ? 'disabled' : ''}`}>
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
                disabled={!currentUserId || submittingComment}
              />
              <button
                type="submit"
                disabled={!currentUserId || submittingComment || (!newCommentContent.trim() && !newCommentImageFile)}
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
          {renderComments(comments)}
        </div>
      )}
    </div>
  );
}

export default CommentSection;