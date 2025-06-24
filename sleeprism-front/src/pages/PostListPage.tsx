// src/pages/PostListPage.tsx
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

// Post 데이터의 타입 정의 (백엔드 PostResponseDTO와 일치하도록 수정)
interface Post {
  id: number;
  title: string;
  content: string; // content도 백엔드 DTO에 있으니 명확히 추가
  category: string;
  viewCount: number;
  isDeleted: boolean; // DTO에 있으니 추가
  authorNickname: string; // <-- originalAuthorNickname 대신 authorNickname으로 변경
  createdAt: string; // LocalDateTime은 일반적으로 ISO 8601 형식의 문자열로 넘어옵니다.
  updatedAt: string; // DTO에 있으니 추가
}

// 이미지 URL을 파싱하고 절대 경로로 변환하는 헬퍼 함수 (PostDetailPage와 유사)
const extractAndConvertFirstImageUrl = (htmlContent: string): string | null => {
  if (!htmlContent) return null;

  const parser = new DOMParser();
  const doc = parser.parseFromString(htmlContent, 'text/html');
  const imgElement = doc.querySelector('img'); // 첫 번째 img 태그만 선택

  if (imgElement) {
    let src = imgElement.getAttribute('src');
    if (src) {
      // 백엔드 기본 URL 및 컨텍스트 경로 정의 (PostCreatePage와 동일하게)
      const BACKEND_BASE_URL = 'http://localhost:8080';
      const BACKEND_CONTEXT_PATH = '/sleeprism';
      const FILE_API_PATH_PREFIX = '/api/posts/files/';

      // 1. 이미 'http://localhost:8080/sleeprism/' 컨텍스트 경로가 붙어있는지 확인
      if (src.startsWith(`${BACKEND_BASE_URL}${BACKEND_CONTEXT_PATH}/`)) {
        return src; // 이미 올바른 절대 경로이므로 그대로 반환
      }
      // 2. 'http://localhost:8080/'으로 시작하지만 '/sleeprism'이 없는 경우 (DB 저장된 방식)
      else if (src.startsWith(BACKEND_BASE_URL + FILE_API_PATH_PREFIX)) {
        // 'http://localhost:8080/api/posts/files/' -> 'http://localhost:8080/sleeprism/api/posts/files/'
        return src.replace(BACKEND_BASE_URL, BACKEND_BASE_URL + BACKEND_CONTEXT_PATH);
      }
      // 3. '/api/posts/files/'로 시작하는 상대 경로인 경우 (이전 HTML Sanitizer 버전)
      else if (src.startsWith(FILE_API_PATH_PREFIX)) {
        // '/api/posts/files/' (상대 경로) -> 'http://localhost:8080/sleeprism/api/posts/files/'
        return `${BACKEND_BASE_URL}${BACKEND_CONTEXT_PATH}${src}`;
      }
    }
  }
  return null; // 이미지가 없거나 src가 유효하지 않은 경우
};

// FIX: HTML에서 순수 텍스트를 추출하고 길이 제한을 적용하는 헬퍼 함수
const getPlainTextSummary = (htmlContent: string, maxLength: number = 150): string => {
  if (!htmlContent) return '';
  const parser = new DOMParser();
  const doc = parser.parseFromString(htmlContent, 'text/html');
  const textContent = doc.body.textContent || ''; // 모든 HTML 태그를 제거하고 텍스트만 추출

  // 내용이 길면 잘라내고 "..." 추가
  if (textContent.length > maxLength) {
    return textContent.substring(0, textContent.lastIndexOf(' ', maxLength)) + '...';
  }
  return textContent;
};


function PostListPage() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchPosts = async () => {
      try {
        const response = await fetch('http://localhost:8080/sleeprism/api/posts');

        if (!response.ok) {
          const errorText = await response.text();
          console.error(`HTTP error! Status: ${response.status}, Response: ${errorText}`);
          throw new Error(`게시글을 불러오는데 실패했습니다: ${response.status} ${response.statusText}`);
        }
        
        const data: Post[] = await response.json();
        setPosts(data);
      } catch (e: any) {
        console.error("게시글 목록을 가져오는 중 오류 발생:", e);
        setError(e.message || "알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
      } finally {
        setLoading(false);
      }
    };

    fetchPosts();
  }, []);

  const handleCreateNewPost = () => {
    navigate('/posts/new');
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

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center py-10 px-4 sm:px-6 lg:px-8 font-inter">
      <div className="max-w-4xl w-full bg-white p-8 rounded-xl shadow-lg border border-gray-200">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold text-gray-800">꿈 해몽 게시글</h1>
          <button
            onClick={handleCreateNewPost}
            className="px-6 py-2 bg-gradient-to-r from-purple-500 to-indigo-600 text-white font-semibold rounded-lg shadow-md hover:from-purple-600 hover:to-indigo-700 transition duration-300 transform hover:scale-105"
          >
            새 게시글 작성
          </button>
        </div>

        {posts.length === 0 ? (
          <p className="text-center text-gray-600 text-lg py-10">아직 작성된 게시글이 없습니다.</p>
        ) : (
          <div className="space-y-6">
            {posts.map((post) => {
              const thumbnailUrl = extractAndConvertFirstImageUrl(post.content); // 첫 번째 이미지 URL 추출
              const summaryText = getPlainTextSummary(post.content, 150); // HTML 제거 후 텍스트 요약
              
              return (
                <div
                  key={post.id}
                  className="bg-gray-50 p-6 rounded-lg shadow-sm border border-gray-100 cursor-pointer hover:bg-gray-100 transition duration-200 ease-in-out transform hover:-translate-y-1 flex items-start space-x-4" // flex container로 변경
                  onClick={() => navigate(`/posts/${post.id}`)}
                >
                  {thumbnailUrl && (
                    <div className="flex-shrink-0 w-24 h-24 sm:w-32 sm:h-32 rounded-lg overflow-hidden border border-gray-200">
                      <img
                        src={thumbnailUrl}
                        alt="게시글 썸네일"
                        className="w-full h-full object-cover" // 이미지가 컨테이너를 채우도록
                      />
                    </div>
                  )}
                  <div className="flex-grow"> {/* 텍스트 컨텐츠가 남은 공간을 채우도록 */}
                    <h2 className="text-2xl font-semibold text-gray-800 mb-2">{post.title}</h2>
                    <p className="text-gray-600 text-sm mb-3">
                      작성자: <span className="font-medium text-gray-700">{post.authorNickname}</span> |
                      카테고리: <span className="font-medium text-gray-700">{post.category}</span> |
                      조회수: <span className="font-medium text-gray-700">{post.viewCount}</span> |
                      작성일: <span className="font-medium text-gray-700">{new Date(post.createdAt).toLocaleDateString()}</span>
                    </p>
                    {/* FIX: dangerouslySetInnerHTML 대신 텍스트 요약 사용 */}
                    <p className="text-gray-700 leading-relaxed text-md line-clamp-3">
                      {summaryText}
                    </p>
                    <div className="text-right mt-4 text-sm text-blue-600 font-medium hover:underline">더 보기</div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

export default PostListPage;
