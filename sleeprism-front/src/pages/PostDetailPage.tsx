import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import CommentSection from '../components/CommentSection';
import SaleRequestModal from '../components/SaleRequestModal';
import { createOrGetSingleChatRoom } from '../service/ChatService';
// import { createOrGetSingleChatRoom } from '../services/ChatService'; // ChatService에서 함수 임포트

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
// ChatRoomList 및 ChatWindow와 동일하게 통일 (authUtils에서 import)
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
    const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
      return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));

    const decodedToken: DecodedToken = JSON.parse(jsonPayload);
    const userIdRaw = decodedToken.userId || decodedToken.id || decodedToken.sub;
    const userId = typeof userIdRaw === 'number' ? userIdRaw : parseInt(userIdRaw as string, 10);
    return isNaN(userId) ? null : userId;
  } catch (e) {
    console.error("JWT 토큰 디코딩 중 오류 발생:", e);
    return null;
  }
};

// FIX: 이미지 URL에 백엔드 컨텍스트 경로를 추가하는 헬퍼 함수 (기존과 동일)
const convertImageUrlsWithContextPath = (htmlContent: string): string => {
  if (!htmlContent) return '';

  const parser = new DOMParser();
  const doc = parser.parseFromString(htmlContent, 'text/html');
  const images = doc.querySelectorAll('img');

  images.forEach(img => {
    let src = img.getAttribute('src');
    if (src) {
      const backendBaseUrl = 'http://localhost:8080';
    
      const apiPathSegment = '/api/posts/files/';

      if (src.startsWith(`${backendBaseUrl}/`)) {
        return; // 이미 올바른 경로이므로 변경하지 않습니다.
      } else if (src.startsWith(backendBaseUrl + apiPathSegment)) {
        img.setAttribute('src', src.replace(backendBaseUrl, backendBaseUrl ));
      } else if (src.startsWith(apiPathSegment)) {
        img.setAttribute('src', backendBaseUrl +  src);
      }
    }
  });

  // document.documentElement.innerHTML 대신, <body> 내부의 HTML만 반환합니다.
  // 왜냐하면 <html>, <head> 태그는 필요 없고, 콘텐츠만 렌더링하기 위함입니다.
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
        throw new Error(errorData.message || `게시글을 불러오는데 실패했습니다: ${response.status}`);
      }
      const data: PostDetail = await response.json();
      setPost(data);
      console.log("Debug: Post data refreshed. sellable:", data.sellable, "sold:", data.sold);
    } catch (e: any) {
      console.error("게시글 상세 정보를 새로고침하는 중 오류 발생:", e);
      setError(e.message || "게시글을 불러오는데 실패했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setLoading(false);
    }
  };


  useEffect(() => {
    setCurrentUserId(getUserIdFromToken());
    refreshPostData();
  }, [postId]);

  // 수정 버튼 클릭 핸들러
  const handleEdit = () => {
    if (post) {
      console.log('게시글 수정 기능은 아직 구현되지 않았습니다.');
    }
  };

  // 삭제 버튼 클릭 핸들러
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
          'Authorization': `Bearer ${token}`,
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

  // 판매 요청 모달 열기
  const handleOpenSaleRequestModal = () => {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
      console.log('판매 요청을 하려면 로그인이 필요합니다.');
      navigate('/login');
      return;
    }
    setIsSaleRequestModalOpen(true);
  };

  // 판매 요청 모달 닫기
  const handleCloseSaleRequestModal = () => {
    setIsSaleRequestModalOpen(false);
  };

  // 1:1 채팅 시작 핸들러
  const handleStartChat = async () => {
    if (!post || !currentUserId) {
      alert("로그인이 필요하거나 게시글 정보가 없습니다.");
      return;
    }
    if (currentUserId === post.originalAuthorId) {
      alert("자신에게 채팅을 시작할 수 없습니다.");
      return;
    }

    try {
      const chatRoom = await createOrGetSingleChatRoom(post.originalAuthorId);
      alert(`${post.authorNickname}님과의 채팅방으로 이동합니다.`);
      navigate(`/chat/${chatRoom.id}`); // 생성되거나 조회된 채팅방으로 이동
    } catch (err: any) {
      console.error("채팅방 생성 또는 조회 실패:", err);
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

  // 현재 로그인한 사용자가 게시글 작성자인지 확인
  const isAuthor = currentUserId !== null &&
                       typeof post.originalAuthorId === 'number' &&
                       post.originalAuthorId === currentUserId;

  // 판매 요청 버튼 표시 조건:
  const showSaleRequestButton = !isAuthor && post.sellable && !post.sold;

  // FIX: post.content의 이미지 URL에 컨텍스트 경로를 추가하여 렌더링합니다.
  const renderedContent = post ? convertImageUrlsWithContextPath(post.content) : '';

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center py-10 px-4 sm:px-6 lg:px-8 font-inter">
      <div className="max-w-4xl w-full bg-white p-8 rounded-xl shadow-lg border border-gray-200">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold text-gray-800">{post.title}</h1>
          <div className="flex space-x-3">
            {/* 수정/삭제 버튼은 작성자에게만 보임 */}
            {isAuthor && (
              <>
                <button
                  onClick={handleEdit}
                  className="px-4 py-2 bg-blue-500 text-white font-semibold rounded-lg shadow-md hover:bg-blue-600 transition duration-300 transform hover:scale-105 flex items-center"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-pencil-line mr-1"><path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/><path d="m15 5 3 3"/></svg>
                  수정
                </button>
                <button
                  onClick={handleDelete}
                  className="px-4 py-2 bg-red-500 text-white font-semibold rounded-lg shadow-md hover:bg-red-600 transition duration-300 transform hover:scale-105 flex items-center"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-trash-2 mr-1"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/><line x1="10" x2="10" y1="11" y2="17"/><line x1="14" x2="14" y1="11" y2="17"/></svg>
                  삭제
                </button>
              </>
            )}
            {showSaleRequestButton && (
              <button
                onClick={handleOpenSaleRequestModal}
                className="px-4 py-2 bg-green-500 text-white font-semibold rounded-lg shadow-md hover:bg-green-600 transition duration-300 transform hover:scale-105 flex items-center"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-badge-dollar-sign mr-1"><path d="M3.34 18.27a10 10 0 1 1 17.32 0"/><path d="M16 16.27a2 2 0 0 0 0-4H8"/><path d="M12 8v8"/><path d="M8 12h8"/></svg>
                판매 요청
              </button>
            )}
            {/* 1:1 채팅 시작 버튼 (게시글 작성자가 아니고, 로그인되어 있다면) */}
            {!isAuthor && currentUserId && (
              <button
                onClick={handleStartChat}
                className="px-4 py-2 bg-purple-500 text-white font-semibold rounded-lg shadow-md hover:bg-purple-600 transition duration-300 transform hover:scale-105 flex items-center"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-message-circle-code"><path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z"/><path d="m10 10-2 2 2 2"/><path d="m14 14 2-2-2-2"/></svg>
                1:1 채팅
              </button>
            )}
          </div>
        </div>

        <p className="text-gray-600 text-sm mb-4">
          작성자: <span className="font-medium text-gray-700">{post.authorNickname}</span> |
          카테고리: <span className="font-medium text-gray-700">{post.category}</span> |
          조회수: <span className="font-medium text-gray-700">{post.viewCount}</span> |
          작성일: <span className="font-medium text-gray-700">{new Date(post.createdAt).toLocaleDateString()}</span>
          {post.sold && <span className="ml-3 px-2 py-1 bg-gray-300 text-gray-700 text-xs font-semibold rounded-full">판매 완료</span>}
          {!post.sellable && <span className="ml-3 px-2 py-1 bg-red-300 text-red-700 text-xs font-semibold rounded-full">판매 불가</span>}
        </p>

        <div
          className="text-gray-800 leading-relaxed text-lg prose prose-blue max-w-none border-t border-b border-gray-200 py-6"
          dangerouslySetInnerHTML={{ __html: renderedContent }}
        />
        <style>
          {`
          .prose img {
            display: block !important;
            max-width: 100% !important;
            height: auto !important;
            margin-left: auto !important;
            margin-right: auto !important;
            object-fit: contain !important;
          }
          `}
        </style>

        <div className="mt-8">
          <button
            onClick={() => navigate('/posts')}
            className="px-6 py-2 bg-gray-200 text-gray-800 font-semibold rounded-lg shadow-md hover:bg-gray-300 transition duration-300 transform hover:scale-105"
          >
            목록으로 돌아가기
          </button>
        </div>

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
