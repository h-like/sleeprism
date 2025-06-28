import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import CommentSection from '../components/CommentSection';
import SaleRequestModal from '../components/SaleRequestModal';
import { createOrGetSingleChatRoom } from '../service/ChatService';
import '../../public/css/PostDetailPage.css'; // 새로운 CSS 파일을 사용할 예정입니다.

// Post 데이터의 타입 정의 (백엔드 PostResponseDTO와 일치해야 합니다)
interface PostDetail {
  id: number;
  title: string;
  content: string;
  category: string;
  viewCount: number;
  deleted: boolean;
  authorNickname: string;
  originalAuthorId: number; // 게시글 원본 작성자 ID
  createdAt: string;
  updatedAt: string;
  sellable: boolean;
  sold: boolean;
}

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

// 이미지 URL에 백엔드 컨텍스트 경로를 추가하는 헬퍼 함수
const convertImageUrlsWithContextPath = (htmlContent: string): string => {
  if (!htmlContent) return '';

  const parser = new DOMParser();
  const doc = parser.parseFromString(htmlContent, 'text/html');
  const images = doc.querySelectorAll('img');

  images.forEach((img) => {
    let src = img.getAttribute('src');
    if (src) {
      const backendBaseUrl = 'http://localhost:8080';
      const apiPathSegment = '/api/posts/files/';
      if (src.startsWith(`${backendBaseUrl}/`)) {
        return;
      } else if (src.startsWith(backendBaseUrl + apiPathSegment)) {
        img.setAttribute('src', src.replace(backendBaseUrl, backendBaseUrl));
      } else if (src.startsWith(apiPathSegment)) {
        img.setAttribute('src', backendBaseUrl + src);
      }
    }
  });

  return doc.body.innerHTML;
};

function PostDetailPage() {
  const { postId } = useParams<{ postId: string }>();
  const navigate = useNavigate();

  const [post, setPost] = useState<PostDetail | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);
  const [isSaleRequestModalOpen, setIsSaleRequestModalOpen] = useState<boolean>(false);

  const refreshPostData = async () => {
    setLoading(true);
    try {
      const response = await fetch(`http://localhost:8080/api/posts/${postId}`);
      if (!response.ok) {
        if (response.status === 404) {
          throw new Error('게시글을 찾을 수 없습니다.');
        }
        const errorData = await response.json();
        throw new Error(
          errorData.message || `게시글을 불러오는데 실패했습니다: ${response.status}`
        );
      }
      const data: PostDetail = await response.json();
      setPost(data);
      console.log('Debug: Post data refreshed. sellable:', data.sellable, 'sold:', data.sold);
    } catch (e: any) {
      console.error('게시글 상세 정보를 새로고침하는 중 오류 발생:', e);
      setError(e.message || '게시글을 불러오는데 실패했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    setCurrentUserId(getUserIdFromToken());
    refreshPostData();
  }, [postId]);

  const handleEdit = () => {
    if (post) {
      // TODO: 게시글 수정 페이지로 이동하는 로직 구현
      console.log('게시글 수정 기능은 아직 구현되지 않았습니다.');
    }
  };

  const handleDelete = async () => {
    const confirmDelete = window.confirm('정말로 이 게시글을 삭제하시겠습니까?');
    if (!post || !confirmDelete) {
      return;
    }

    const token = localStorage.getItem('jwtToken');
    if (!token) {
      console.log('로그인이 필요합니다.');
      navigate('/login');
      return;
    }

    try {
      const response = await fetch(`http://localhost:8080/api/posts/${post.id}`, {
        method: 'DELETE',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || `게시글 삭제에 실패했습니다: ${response.status}`);
      }

      console.log('게시글이 성공적으로 삭제되었습니다!');
      navigate('/posts');
    } catch (e: any) {
      console.error('게시글 삭제 중 오류 발생:', e);
      setError(e.message || '게시글 삭제 중 오류가 발생했습니다.');
    }
  };

  const handleOpenSaleRequestModal = () => {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
      console.log('판매 요청을 하려면 로그인이 필요합니다.');
      navigate('/login');
      return;
    }
    setIsSaleRequestModalOpen(true);
  };

  const handleCloseSaleRequestModal = () => {
    setIsSaleRequestModalOpen(false);
  };

  const handleStartChat = async () => {
    if (!post || !currentUserId) {
      alert('로그인이 필요하거나 게시글 정보가 없습니다.');
      return;
    }
    if (currentUserId === post.originalAuthorId) {
      alert('자신에게 채팅을 시작할 수 없습니다.');
      return;
    }

    try {
      const chatRoom = await createOrGetSingleChatRoom(post.originalAuthorId);
      alert(`${post.authorNickname}님과의 채팅방으로 이동합니다.`);
      navigate(`/chat/${chatRoom.id}`);
    } catch (err: any) {
      console.error('채팅방 생성 또는 조회 실패:', err);
      alert(`채팅 시작 실패: ${err.message}`);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-100 p-4 font-inter">
        <p className="text-xl text-gray-700">게시글을 불러오는 중...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-red-100 p-4 rounded-lg shadow-md font-inter">
        <p className="text-xl text-red-700">{error}</p>
      </div>
    );
  }

  if (!post) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-100 p-4 font-inter">
        <p className="text-xl text-gray-700">게시글이 존재하지 않습니다.</p>
      </div>
    );
  }

  const isAuthor = currentUserId !== null && post.originalAuthorId === currentUserId;
  const showSaleRequestButton = !isAuthor && post.sellable && !post.sold;
  const renderedContent = post ? convertImageUrlsWithContextPath(post.content) : '';

  return (   
    <div className="main-container">
      <div className="post-detail-wrapper">
        <div className="post-header-section">
          <div className="post-author-info">
            <div className="author-avatar">
              <img src="/default-avatar.png" alt="Author Avatar" />
            </div>
            <div className="author-details">
              <span className="author-nickname">{post.authorNickname}</span>
              <span className="post-date">
                {new Date(post.createdAt).toLocaleDateString('ko-KR', {
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric',
                })}
              </span>
            </div>
          </div>
          <div className="post-actions-top">
            {/* 상단 버튼 그룹 */}
            {isAuthor && (
              <>
                <button onClick={handleEdit} className="action-button edit-button">
                  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-pencil-line"><path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/><path d="m15 5 3 3"/></svg>
                  <span>수정</span>
                </button>
                <button onClick={handleDelete} className="action-button delete-button">
                  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-trash-2"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/><line x1="10" x2="10" y1="11" y2="17"/><line x1="14" x2="14" y1="11" y2="17"/></svg>
                  <span>삭제</span>
                </button>
              </>
            )}
            {!isAuthor && currentUserId && (
              <button onClick={handleStartChat} className="action-button chat-button">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-message-circle-code"><path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z"/><path d="m10 10-2 2 2 2"/><path d="m14 14 2-2-2-2"/></svg>
                <span>1:1 채팅</span>
              </button>
            )}
          </div>
        </div>

        <h1 className="post-detail-title">{post.title}</h1>
        <div className="post-meta-tags">
          <span className="meta-tag category-tag">{post.category}</span>
          <span className="meta-tag view-count">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-eye"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></svg>
            <span>{post.viewCount}</span>
          </span>
          {post.sold && <span className="meta-tag status-tag sold">판매 완료</span>}
          {!post.sellable && <span className="meta-tag status-tag un-sellable">판매 불가</span>}
        </div>

        <div
          className="post-content-body"
          dangerouslySetInnerHTML={{ __html: renderedContent }}
        />

        <div className="post-actions-bottom">
          {showSaleRequestButton && (
            <button
              onClick={handleOpenSaleRequestModal}
              className="action-button primary-button"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-badge-dollar-sign mr-1"><path d="M3.34 18.27a10 10 0 1 1 17.32 0"/><path d="M16 16.27a2 2 0 0 0 0-4H8"/><path d="M12 8v8"/><path d="M8 12h8"/></svg>
              <span>판매 요청하기</span>
            </button>
          )}
          <button
            onClick={() => navigate('/posts')}
            className="action-button secondary-button"
          >
            <span>목록으로 돌아가기</span>
          </button>
        </div>
        {/* // 짧은 구분선 */}
        <hr style={{
          backgroundColor: '#d6d6d6',
          height: '1px',
          border: 'none'
        }} />
        {/* 댓글 섹션 */}
        {post.id && <CommentSection postId={post.id} />}
      </div>

      {/* 판매 요청 모달 */}
      {isSaleRequestModalOpen && post && (
        <SaleRequestModal
          postId={post.id}
          postTitle={post.title}
          onClose={handleCloseSaleRequestModal}
          onSuccess={refreshPostData}
        />
      )}
    </div>
    
  );
}

export default PostDetailPage;