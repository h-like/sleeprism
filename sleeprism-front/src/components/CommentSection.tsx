// src/components/CommentSection.tsx
import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import type { CommentCreateRequestDTO, CommentResponseDTO, ImageUploadResponse } from '../type/comment';
// import { CommentResponseDTO, CommentCreateRequestDTO, ImageUploadResponse } from '../types/comment'; // DTO 인터페이스 임포트

// JWT 토큰에서 사용자 ID를 디코딩하는 헬퍼 함수 (PostDetailPage와 동일)
const getUserIdFromToken = (): number | null => {
  const token = localStorage.getItem('jwtToken');
  if (!token) {
    return null;
  }
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
      return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));
    const decodedToken = JSON.parse(jsonPayload);
    const userIdRaw = decodedToken.userId || decodedToken.id || decodedToken.sub;
    const userId = typeof userIdRaw === 'number' ? userIdRaw : parseInt(userIdRaw as string, 10);
    return isNaN(userId) ? null : userId;
  } catch (e) {
    console.error("JWT 토큰 디코딩 중 오류 발생:", e);
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
      const response = await fetch(`http://localhost:8080/sleeprism/api/comments/post/${postId}`);
      if (!response.ok) {
        throw new Error(`댓글을 불러오는데 실패했습니다: ${response.status}`);
      }
      const data: CommentResponseDTO[] = await response.json();
      // 최신 댓글이 위에 오도록 정렬 (선택 사항, 백엔드에서 정렬해도 됨)
      setComments(data.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()));
    } catch (e: any) {
      console.error("댓글 목록 가져오기 오류:", e);
      setError(e.message || "댓글을 불러오는데 실패했습니다.");
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
        const uploadResponse = await fetch('http://localhost:8080/sleeprism/api/posts/upload-image', {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`, // 이미지 업로드도 인증 필요
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
      const createResponse = await fetch('http://localhost:8080/sleeprism/api/comments', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
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
      const response = await fetch(`http://localhost:8080/sleeprism/api/comments/${commentId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
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
    <div className="mt-12 p-6 bg-gray-100 rounded-lg shadow-inner border border-gray-200">
      <h2 className="text-2xl font-semibold text-gray-700 mb-6">댓글</h2>

      {/* 댓글 작성 폼 */}
      <form onSubmit={handleCommentSubmit} className="space-y-4 mb-8">
        <div>
          <label htmlFor="commentContent" className="sr-only">댓글 내용</label>
          <textarea
            id="commentContent"
            className="w-full p-3 border border-gray-300 rounded-md focus:ring-indigo-500 focus:border-indigo-500"
            rows={3}
            placeholder="댓글을 작성하세요."
            value={newCommentContent}
            onChange={(e) => setNewCommentContent(e.target.value)}
          ></textarea>
        </div>
        
        {/* 이미지 첨부 필드 */}
        <div className="flex items-center space-x-4">
          <label htmlFor="commentImage" className="cursor-pointer px-4 py-2 bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300 transition duration-200">
            이미지 첨부 (1장)
          </label>
          <input
            type="file"
            id="commentImage"
            ref={fileInputRef}
            onChange={handleImageChange}
            accept="image/*" // 이미지 파일만 선택 가능
            className="hidden" // 실제 input은 숨김
          />
          {newCommentImageUrl && (
            <div className="relative w-24 h-24 border border-gray-300 rounded-md overflow-hidden">
              <img src={newCommentImageUrl} alt="첨부 이미지 미리보기" className="w-full h-full object-cover" />
              <button
                type="button"
                onClick={() => {
                  setNewCommentImageFile(null);
                  setNewCommentImageUrl(null);
                  if (fileInputRef.current) fileInputRef.current.value = '';
                }}
                className="absolute top-1 right-1 bg-red-500 text-white rounded-full p-1 text-xs leading-none"
              >
                X
              </button>
            </div>
          )}
          {newCommentImageFile && <span className="text-sm text-gray-600">{newCommentImageFile.name}</span>}
        </div>

        {error && <p className="text-red-500 text-sm mt-2">{error}</p>}

        <button
          type="submit"
          disabled={submittingComment}
          className="px-6 py-2 bg-indigo-600 text-white font-semibold rounded-md shadow-md hover:bg-indigo-700 transition duration-300 transform hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {submittingComment ? '댓글 작성 중...' : '댓글 작성'}
        </button>
      </form>

      {/* 댓글 목록 */}
      {loadingComments ? (
        <p className="text-center text-gray-600">댓글을 불러오는 중...</p>
      ) : comments.length === 0 ? (
        <p className="text-center text-gray-600">아직 댓글이 없습니다.</p>
      ) : (
        <div className="space-y-6">
          {comments.map((comment) => (
            <div key={comment.id} className="bg-white p-5 rounded-lg shadow-sm border border-gray-100">
              <div className="flex justify-between items-start mb-2">
                <p className="text-gray-800 font-semibold">{comment.authorNickname}</p>
                <div className="text-sm text-gray-500">
                  {new Date(comment.createdAt).toLocaleString()}
                </div>
              </div>
              
              {/* 이미지/GIF 먼저 표시 */}
              {comment.attachmentUrl && (
                <div className="mb-4">
                  <img 
                    src={`http://localhost:8080/sleeprism${comment.attachmentUrl}`} // 백엔드 URL과 Context Path 결합
                    alt="첨부 이미지" 
                    className="max-w-xs sm:max-w-sm lg:max-w-md h-auto rounded-md shadow-md"
                  />
                </div>
              )}

              <p className="text-gray-700 leading-relaxed mb-4">{comment.content}</p>

              {/* 본인 댓글에만 수정/삭제 버튼 표시 */}
              {currentUserId !== null && comment.originalAuthorId === currentUserId && (
                <div className="flex space-x-3 text-sm">
                  <button
                    onClick={() => handleCommentEdit(comment.id)}
                    className="text-blue-600 hover:underline flex items-center"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-pencil mr-1"><path d="M17 3a2.85 2.85 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/><path d="m15 5 3 3"/></svg>
                    수정
                  </button>
                  <button
                    onClick={() => handleCommentDelete(comment.id)}
                    className="text-red-600 hover:underline flex items-center"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-trash-2 mr-1"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/><line x1="10" x2="10" y1="11" y2="17"/><line x1="14" x2="14" y1="11" y2="17"/></svg>
                    삭제
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default CommentSection;
