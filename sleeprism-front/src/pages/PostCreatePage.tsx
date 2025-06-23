// src/pages/PostCreatePage.tsx
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

function PostCreatePage() {
  const [title, setTitle] = useState<string>('');
  const [category, setCategory] = useState<string>('DREAM_DIARY'); // 기본 카테고리 설정
  const [content, setContent] = useState<string>('');
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);

  const navigate = useNavigate();

  // 컴포넌트 로드 시 로그인 상태 확인
  useEffect(() => {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
      alert('게시글을 작성하려면 로그인이 필요합니다.');
      navigate('/login'); // 토큰이 없으면 로그인 페이지로 리다이렉트
    }
  }, [navigate]); // navigate가 변경될 때마다 실행 (컴포넌트 마운트 시 한 번)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMessage(null);
    setLoading(true);

    const token = localStorage.getItem('jwtToken');
    if (!token) {
      setError('로그인 정보가 없습니다. 다시 로그인해주세요.');
      setLoading(false);
      navigate('/login');
      return;
    }

    try {
      const response = await fetch('http://localhost:8080/sleeprism/api/posts', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`, // JWT 토큰을 Authorization 헤더에 포함
        },
        body: JSON.stringify({ title, category, content }),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || '게시글 작성에 실패했습니다.');
      }

      setSuccessMessage('게시글이 성공적으로 작성되었습니다!');
      // 작성 후 게시글 상세 페이지 또는 목록 페이지로 이동
      // 생성된 게시글의 ID를 받아서 상세 페이지로 이동하는 것이 좋습니다.
      // 백엔드 PostResponseDTO에 id가 포함되어 있으므로 data.id를 사용합니다.
      setTimeout(() => {
        navigate(`/posts/${data.id}`); 
      }, 1500);

    } catch (e: any) {
      console.error('게시글 작성 중 오류 발생:', e);
      setError(e.message || '알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center py-10 px-4 sm:px-6 lg:px-8 font-inter">
      <div className="max-w-3xl w-full bg-white p-8 rounded-xl shadow-lg border border-gray-200">
        <h1 className="text-3xl font-bold text-gray-800 text-center mb-8">새 게시글 작성</h1>
        <form className="space-y-6" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="title" className="block text-sm font-medium text-gray-700">
              제목
            </label>
            <input
              type="text"
              id="title"
              name="title"
              required
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="게시글 제목을 입력하세요."
            />
          </div>

          <div>
            <label htmlFor="category" className="block text-sm font-medium text-gray-700">
              카테고리
            </label>
            <select
              id="category"
              name="category"
              required
              className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
            >
              {/* 백엔드 PostCategory enum 값과 일치해야 합니다. */}
              <option value="DREAM_DIARY">꿈 다이어리</option>
              <option value="FREE_TALK">자유 게시판</option>
              {/* 다른 카테고리가 있다면 여기에 추가 */}
            </select>
          </div>

          <div>
            <label htmlFor="content" className="block text-sm font-medium text-gray-700">
              내용
            </label>
            {/* Quill.js는 나중에 연동. 일단 textarea 사용 */}
            <textarea
              id="content"
              name="content"
              rows={10}
              required
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="꿈 내용을 자세히 작성해주세요."
            ></textarea>
          </div>

          {error && <p className="mt-2 text-center text-sm text-red-600">{error}</p>}
          {successMessage && <p className="mt-2 text-center text-sm text-green-600">{successMessage}</p>}

          <div>
            <button
              type="submit"
              disabled={loading}
              className="w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-700 hover:to-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition duration-300 transform hover:scale-105"
            >
              {loading ? '작성 중...' : '게시글 작성'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default PostCreatePage;
