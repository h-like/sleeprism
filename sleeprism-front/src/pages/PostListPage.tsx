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

function PostListPage() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchPosts = async () => {
      try {
        // 백엔드 API 호출 URL은 현재 성공한 http://localhost:8080/sleeprism/api/posts 로 유지
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
            {posts.map((post) => (
              <div
                key={post.id}
                className="bg-gray-50 p-6 rounded-lg shadow-sm border border-gray-100 cursor-pointer hover:bg-gray-100 transition duration-200 ease-in-out transform hover:-translate-y-1"
                onClick={() => navigate(`/posts/${post.id}`)}
              >
                <h2 className="text-2xl font-semibold text-gray-800 mb-2">{post.title}</h2>
                <p className="text-gray-600 text-sm mb-3">
                  작성자: <span className="font-medium text-gray-700">{post.authorNickname}</span> | {/* 필드명 수정! */}
                  카테고리: <span className="font-medium text-gray-700">{post.category}</span> |
                  조회수: <span className="font-medium text-gray-700">{post.viewCount}</span> |
                  작성일: <span className="font-medium text-gray-700">{new Date(post.createdAt).toLocaleDateString()}</span>
                </p>
                <div 
                  className="text-gray-700 leading-relaxed text-md prose prose-blue max-w-none line-clamp-3"
                  dangerouslySetInnerHTML={{ __html: post.content }} 
                />
                 <div className="text-right mt-4 text-sm text-blue-600 font-medium hover:underline">더 보기</div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default PostListPage;
