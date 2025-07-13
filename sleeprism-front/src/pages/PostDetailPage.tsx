import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import CommentSection from '../components/CommentSection';
import SaleRequestModal from '../components/SaleRequestModal';
import { createOrGetSingleChatRoom } from '../service/ChatService';
import DreamInterpretationModal from '../components/DreamInterpretationModal';
import '../../public/css/PostDetailPage.css'; // 새로운 CSS 파일을 사용할 예정입니다.
import ScrollToTopButton from '../components/ScrollToTopButton';

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
  authorProfileImageUrl: string | null; // 프로필 이미지 URL 추가 (백엔드 DTO에 있어야 함)
  createdAt: string;
  updatedAt: string;
  sellable: boolean;
  sold: boolean;
  likeCount: number; // 좋아요 수 추가
  bookmarkCount: number; // 북마크 수 추가
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
      // FIX: src가 이미 backendBaseUrl로 시작하면 아무것도 하지 않습니다.
      // HtmlSanitizer 수정으로 DB에 이미 절대 경로가 저장되기 때문입니다.
      if (src.startsWith(backendBaseUrl)) {
        return; 
      }
      // 그 외의 경우 (상대 경로인 경우)에만 backendBaseUrl을 붙입니다.
      // (이 로직은 현재 상황에서는 거의 실행되지 않을 것입니다.
      // 백엔드에서 상대 경로를 반환하고 Sanitizer가 이를 절대 경로로 만들지 않는 경우에 유용합니다.)
      else if (src.startsWith('/')) { 
        img.setAttribute('src', backendBaseUrl + src);
      }
    }
  });

  return doc.body.innerHTML;
};

// 백엔드 기본 URL (프로필 이미지, 첨부 파일 경로 구성용)
const BACKEND_BASE_URL = 'http://localhost:8080';

function PostDetailPage() {
  const { postId } = useParams<{ postId: string }>();
  const navigate = useNavigate();
  const location = useLocation(); // useLocation 훅 추가

  const [post, setPost] = useState<PostDetail | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);
  const [isSaleRequestModalOpen, setIsSaleRequestModalOpen] = useState<boolean>(false);

  // 좋아요 및 북마크 상태 추가
  const [isLiked, setIsLiked] = useState<boolean>(false);
  const [isBookmarked, setIsBookmarked] = useState<boolean>(false);
  const [likeCount, setLikeCount] = useState<number>(0);
  const [bookmarkCount, setBookmarkCount] = useState<number>(0);

   // 꿈 해몽 모달의 상태를 관리하기 위한 state 추가
  const [isInterpretationModalOpen, setIsInterpretationModalOpen] = useState<boolean>(false);

  // 게시글 데이터 및 좋아요/북마크 상태를 새로고침하는 함수
  const refreshPostData = useCallback(async () => {
    setLoading(true);
    try {
      // 게시글 상세 정보 가져오기
      const postResponse = await fetch(`${BACKEND_BASE_URL}/api/posts/${postId}`);
      if (!postResponse.ok) {
        if (postResponse.status === 404) {
          throw new Error('게시글을 찾을 수 없습니다.');
        }
        const errorData = await postResponse.json();
        throw new Error(
          errorData.message || `게시글을 불러오는데 실패했습니다: ${postResponse.status}`
        );
      }
      const postData: PostDetail = await postResponse.json();
      setPost(postData);
      setLikeCount(postData.likeCount); // 초기 좋아요 수 설정
      setBookmarkCount(postData.bookmarkCount); // 초기 북마크 수 설정
      
      // 디버깅: 게시글 데이터 및 프로필 이미지 URL 확인
      console.log('게시글 데이터:', postData);
      console.log('게시글 작성자 프로필 이미지 URL:', postData.authorProfileImageUrl);

      // 로그인된 사용자라면 좋아요 및 북마크 상태 가져오기
      const userId = getUserIdFromToken();
      setCurrentUserId(userId);

      if (userId) {
        const token = localStorage.getItem('jwtToken');
        if (token) {
          // 좋아요 상태 확인
          const likeStatusResponse = await fetch(`${BACKEND_BASE_URL}/api/posts/${postId}/like/status`, {
            headers: { Authorization: `Bearer ${token}` },
          });
          if (likeStatusResponse.ok) {
            const likeStatusData = await likeStatusResponse.json();
            setIsLiked(likeStatusData.isLiked);
          } else {
            console.warn('좋아요 상태를 가져오는데 실패했습니다.', likeStatusResponse.status);
            setIsLiked(false); // 실패 시 좋아요 안 된 상태로 간주
          }

          // 북마크 상태 확인
          const bookmarkStatusResponse = await fetch(`${BACKEND_BASE_URL}/api/posts/${postId}/bookmark/status`, {
            headers: { Authorization: `Bearer ${token}` },
          });
          if (bookmarkStatusResponse.ok) {
            const bookmarkStatusData = await bookmarkStatusResponse.json();
            setIsBookmarked(bookmarkStatusData.isBookmarked);
          } else {
            console.warn('북마크 상태를 가져오는데 실패했습니다.', bookmarkStatusResponse.status);
            setIsBookmarked(false); // 실패 시 북마크 안 된 상태로 간주
          }
        }
      }
    } catch (e: any) {
      console.error('게시글 상세 정보를 새로고침하는 중 오류 발생:', e);
      setError(e.message || '게시글을 불러오는데 실패했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setLoading(false);
    }
  }, [postId]); // postId가 변경될 때마다 함수 재생성

  useEffect(() => {
    refreshPostData();
  }, [postId, refreshPostData]);

  const handleEdit = () => {
    if (post) {
    navigate(`/posts/${post.id}/edit`); // 수정 페이지로 이동
  }
  };

  const handleDelete = async () => {
    const confirmDelete = window.confirm('정말로 이 게시글을 삭제하시겠습니까?');
    if (!post || !confirmDelete) {
      return;
    }

    const token = localStorage.getItem('jwtToken');
    if (!token) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }

    try {
      const response = await fetch(`${BACKEND_BASE_URL}/api/posts/${post.id}`, {
        method: 'DELETE',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || `게시글 삭제에 실패했습니다: ${response.status}`);
      }

      alert('게시글이 성공적으로 삭제되었습니다!');
      navigate('/posts');
    } catch (e: any) {
      console.error('게시글 삭제 중 오류 발생:', e);
      setError(e.message || '게시글 삭제 중 오류가 발생했습니다.');
    }
  };

  const handleOpenSaleRequestModal = () => {
    console.log("'판매 요청하기' 버튼 클릭됨!"); // <-- 이 로그가 찍히는지 확인
    const token = localStorage.getItem('jwtToken');
    if (!token) {
      alert('판매 요청을 하려면 로그인이 필요합니다.');
      navigate('/login');
      return;
    }
    setIsSaleRequestModalOpen(true);
     console.log('isSaleRequestModalOpen 상태를 true로 변경 시도!');
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
      
       navigate(`/chat/${chatRoom.id}`, { 
      state: { 
        roomName: post.authorNickname 
      } 
    });
    
    } catch (err: any) {
      console.error('채팅방 생성 또는 조회 실패:', err);
      alert(`채팅 시작 실패: ${err.message}`);
    }
  };

  // 좋아요 토글 기능
  const handleLikeToggle = async () => {
    if (!currentUserId) {
      alert('좋아요 기능을 이용하려면 로그인이 필요합니다.');
      navigate('/login');
      return;
    }
    if (!post) return;

    const token = localStorage.getItem('jwtToken');
    if (!token) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }

    try {
      const response = await fetch(`${BACKEND_BASE_URL}/api/posts/${post.id}/like`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || `좋아요 토글 실패: ${response.status}`);
      }

      const result = await response.json();
      setIsLiked(result.liked);
      setLikeCount((prevCount) => (result.liked ? prevCount + 1 : prevCount - 1));
      console.log('좋아요 토글 성공:', result.liked);
    } catch (e: any) {
      console.error('좋아요 토글 중 오류 발생:', e);
      alert(`좋아요 처리 실패: ${e.message}`);
    }
  };

  // 북마크 토글 기능
  const handleBookmarkToggle = async () => {
    if (!currentUserId) {
      alert('북마크 기능을 이용하려면 로그인이 필요합니다.');
      navigate('/login');
      return;
    }
    if (!post) return;

    const token = localStorage.getItem('jwtToken');
    if (!token) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }

    try {
      const response = await fetch(`${BACKEND_BASE_URL}/api/posts/${post.id}/bookmark`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || `북마크 토글 실패: ${response.status}`);
      }

      const result = await response.json();
      setIsBookmarked(result.bookmarked);
      setBookmarkCount((prevCount) => (result.bookmarked ? prevCount + 1 : prevCount - 1));
      console.log('북마크 토글 성공:', result.bookmarked);
    } catch (e: any) {
      console.error('북마크 토글 중 오류 발생:', e);
      alert(`북마크 처리 실패: ${e.message}`);
    }
  };

  // "목록으로 돌아가기" 버튼 핸들러
  const handleBackToList = () => {
    // PostListPage에서 전달받은 상태를 사용하여 URL을 구성
    const state = location.state as { tab?: string; category?: string[]; period?: string } | undefined;
    const queryParams = new URLSearchParams();

    if (state) {
      if (state.tab) {
        queryParams.append('tab', state.tab);
      }
      if (state.tab === 'latest' && state.category && state.category.length > 0) {
        queryParams.append('category', state.category.join(','));
      }
      if (state.tab === 'popular' && state.period && state.period !== 'all_time') {
        queryParams.append('period', state.period);
      }
    }
    
    navigate(`/posts?${queryParams.toString()}`);
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
  // FIX: convertImageUrlsWithContextPath 함수가 이제 필요하지 않으므로 직접 content를 사용합니다.
  const renderedContent = post.content; 

   // 꿈 해몽 버튼을 보여줄지 결정하는 조건 (예: 카테고리가 '꿈'일 경우)
  const showInterpretationButton = post.category === 'DREAM_DIARY'; // 백엔드 카테고리 값에 따라 'DREAM', '꿈' 등으로 변경
 console.log('--- 렌더링 정보 ---');
  console.log('현재 post 데이터:', post);
  console.log('모달 열림 상태 (isSaleRequestModalOpen):', isSaleRequestModalOpen); // <-- 이 로그 추가


  return ( 
    <div className="main-container">
      <ScrollToTopButton />
      <div className="post-detail-wrapper">
        <div className="post-header-section">
          <div className="post-author-info"  onClick={() => navigate(`/chat`)} style={{ cursor: 'pointer' }}>
            <div className="author-avatar">
              {/* 백엔드에서 authorProfileImageUrl을 제공하면 사용, 아니면 placeholder 사용 */}
              <img 
                src={post.authorProfileImageUrl ? `${BACKEND_BASE_URL}${post.authorProfileImageUrl}` : "https://placehold.co/48x48/F0F0F0/ADADAD?text=User"} 
                alt="Author Avatar" 
              />
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
          {/* 좋아요/북마크 버튼 및 개수 표시 */}
          {currentUserId && ( // 로그인한 사용자에게만 표시
            <>
              <button onClick={handleLikeToggle} className={`meta-tag like-button ${isLiked ? 'liked' : ''}`}>
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill={isLiked ? "#dc2626" : "none"} stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-heart">
                  <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5L12 22Z"/>
                </svg>
                <span>{likeCount}</span>
              </button>
              <button onClick={handleBookmarkToggle} className={`meta-tag bookmark-button ${isBookmarked ? 'bookmarked' : ''}`}>
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill={isBookmarked ? "#2563eb" : "none"} stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-bookmark">
                  <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"/>
                </svg>
                <span>{bookmarkCount}</span>
              </button>
            </>
          )}

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

           {/* --- [추가] 꿈 해몽하기 버튼 --- */}
          {showInterpretationButton && currentUserId && (
            <button
              onClick={() => setIsInterpretationModalOpen(true)}
              className="action-button dream-button" // 새로운 스타일을 위한 클래스
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-wand-sparkles mr-1"><path d="m21.64 3.64-1.28-1.28a1.21 1.21 0 0 0-1.72 0L11.8 9.2a1.21 1.21 0 0 0 0 1.72l8.84 8.84a1.21 1.21 0 0 0 1.72 0l1.28-1.28a1.21 1.21 0 0 0 0-1.72Z"/><path d="m14 7 3 3"/><path d="M5 6v4"/><path d="M19 14v4"/><path d="M10 2v2"/><path d="M7 8H3"/><path d="M21 16h-4"/><path d="M11 3H9"/><path d="M18 21v-2"/><path d="M13 17H9"/></svg>
              <span>AI 꿈 해몽하기</span>
            </button>
          )}

          <button
            onClick={handleBackToList} // 수정된 핸들러 사용
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

       {/* --- [추가] 조건부로 꿈 해몽 모달 렌더링 --- */}
      {isInterpretationModalOpen && post && (
        <DreamInterpretationModal
          postId={post.id}
          onClose={() => setIsInterpretationModalOpen(false)}
        />
      )}
    </div>
    
  );
}

export default PostDetailPage;
