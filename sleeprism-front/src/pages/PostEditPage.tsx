// src/pages/PostEditPage.tsx
import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';

// Tiptap 관련 임포트: useEditor와 필요한 확장 기능들을 여기서 임포트합니다.
import { useEditor } from '@tiptap/react';
// import StarterKit from '@tiptap/extension-starter-kit';
import Link from '@tiptap/extension-link';
import Image from '@tiptap/extension-image';
import TextAlign from '@tiptap/extension-text-align';
import TextStyle from '@tiptap/extension-text-style';
import { Color } from '@tiptap/extension-color';
import Underline from '@tiptap/extension-underline';

// 공통 CSS 및 TextEditor 컴포넌트 임포트
import '../../public/css/LoginPage.css'; // 기존 로그인 페이지 스타일을 재활용
import TextEditor from '../components/TextEditor';
import StarterKit from '@tiptap/starter-kit';

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
  authorProfileImageUrl: string | null;
  createdAt: string;
  updatedAt: string;
  sellable: boolean;
  sold: boolean;
  likeCount: number;
  bookmarkCount: number;
}

// JWT 토큰에서 사용자 ID를 디코딩하는 헬퍼 함수 (PostDetailPage에서 가져옴)
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

// 백엔드 기본 URL (프로필 이미지, 첨부 파일 경로 구성용)
const BACKEND_BASE_URL = 'http://localhost:8080';

function PostEditPage() {
  const { postId } = useParams<{ postId: string }>(); // URL 파라미터에서 postId 가져오기
  const navigate = useNavigate();

  const [title, setTitle] = useState<string>('');
  const [category, setCategory] = useState<string>('DREAM_DIARY');
  const [content, setContent] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [isImageUploading, setIsImageUploading] = useState<boolean>(false);
  const [initialContentLoaded, setInitialContentLoaded] = useState<boolean>(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        heading: {
          levels: [1, 2, 3],
        },
      }),
      Link.configure({
        openOnClick: false,
        autolink: true,
        linkOnPaste: true,
      }),
      Image.configure({
        inline: true,
        allowBase64: false,
        HTMLAttributes: {
          class: 'tiptap-image',
        },
      }),
      TextAlign.configure({
        types: ['heading', 'paragraph'],
      }),
      TextStyle,
      Color,
      Underline,
    ],
    content: '', // 초기 content는 빈 문자열로 설정하고, useEffect에서 불러온 데이터로 업데이트
    onUpdate: ({ editor }) => {
      setContent(editor.getHTML());
    },
    editorProps: {
      attributes: {
        class: 'prose prose-sm sm:prose lg:prose-lg xl:prose-xl focus:outline-none',
      },
    },
  });

  // 게시글 데이터 불러오기
  useEffect(() => {
    const fetchPost = async () => {
      try {
        const token = localStorage.getItem('jwtToken');
        if (!token) {
          alert('로그인이 필요합니다.');
          navigate('/login');
          return;
        }

        const response = await fetch(`${BACKEND_BASE_URL}/api/posts/${postId}`, {
          headers: {
            'Authorization': `Bearer ${token}`,
          },
        });

        if (!response.ok) {
          if (response.status === 404) {
            throw new Error('게시글을 찾을 수 없습니다.');
          }
          const errorData = await response.json();
          throw new Error(errorData.message || '게시글을 불러오는데 실패했습니다.');
        }

        const postData: PostDetail = await response.json();
        const currentUserId = getUserIdFromToken();

        // 현재 로그인한 사용자가 게시글 작성자인지 확인
        if (currentUserId !== postData.originalAuthorId) {
          alert('이 게시글을 수정할 권한이 없습니다.');
          navigate(`/posts/${postId}`); // 권한 없으면 상세 페이지로 리다이렉트
          return;
        }

        setTitle(postData.title);
        setCategory(postData.category);
        setContent(postData.content); // 불러온 HTML 콘텐츠를 상태에 저장

        // 에디터가 준비되면 콘텐츠 설정
        if (editor) {
          editor.commands.setContent(postData.content);
          setInitialContentLoaded(true); // 초기 콘텐츠 로드 완료
        }

      } catch (e: any) {
        console.error('게시글 불러오기 오류:', e);
        setError(e.message || '게시글을 불러오는 중 오류가 발생했습니다.');
      } finally {
        setLoading(false);
      }
    };

    if (postId && editor) { // postId와 editor가 모두 준비되었을 때만 fetch
      fetchPost();
    }
  }, [postId, navigate, editor]); // editor를 의존성 배열에 추가

  // 에디터가 준비되고 초기 콘텐츠가 로드된 후에만 onUpdate 콜백이 실행되도록 제어
  // 이렇게 하면 초기 로드 시 불필요한 onUpdate 호출을 방지할 수 있습니다.
  useEffect(() => {
    if (editor && initialContentLoaded) {
      editor.on('update', ({ editor }) => {
        setContent(editor.getHTML());
      });
    }
    // 클린업 함수: 컴포넌트 언마운트 시 이벤트 리스너 제거
    return () => {
      if (editor) {
        editor.off('update');
      }
    };
  }, [editor, initialContentLoaded]);


  const handleImageUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('image', file);

    setIsImageUploading(true);
    setError(null);

    try {
      const token = localStorage.getItem('jwtToken');
      const response = await fetch(`${BACKEND_BASE_URL}/api/posts/upload-image`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
        body: formData,
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || '이미지 업로드에 실패했습니다.');
      }

      // 백엔드에서 반환되는 URL을 그대로 에디터에 삽입합니다.
      const imageUrlForEditor = `${BACKEND_BASE_URL}${data.url}`;
      
      editor?.chain().focus().setImage({ src: imageUrlForEditor }).run();
      // setSuccessMessage('이미지가 성공적으로 업로드되었습니다.'); // 수정 페이지에서는 별도 메시지 불필요
    } catch (err: any) {
      console.error('이미지 업로드 오류:', err);
      setError(err.message || '이미지 업로드 중 알 수 없는 오류가 발생했습니다.');
    } finally {
      setIsImageUploading(false);
      if (event.target) {
        event.target.value = ''; // 파일 input 초기화
      }
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    // setSuccessMessage(null); // 수정 페이지에서는 별도 메시지 불필요
    setLoading(true);

    const token = localStorage.getItem('jwtToken');
    if (!token) {
      setError('로그인 정보가 없습니다. 다시 로그인해주세요.');
      setLoading(false);
      navigate('/login');
      return;
    }

    try {
      const response = await fetch(`${BACKEND_BASE_URL}/api/posts/${postId}`, {
        method: 'PUT', // PUT 요청으로 변경
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({ title, category, content }), // content는 이미 에디터에서 가져온 HTML
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || '게시글 수정에 실패했습니다.');
      }

      alert('게시글이 성공적으로 수정되었습니다!'); // 수정 완료 메시지
      setTimeout(() => {
        navigate(`/posts/${postId}`); // 수정 후 상세 페이지로 이동
      }, 500);

    } catch (e: any) {
      console.error('게시글 수정 중 오류 발생:', e);
      setError(e.message || '알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setLoading(false);
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

  // 에디터가 아직 로드되지 않았거나 초기 콘텐츠가 설정되지 않았다면 로딩 상태 유지
  if (!editor || !initialContentLoaded) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-100 p-4 font-inter">
        <p className="text-xl text-gray-700">에디터 로딩 중...</p>
      </div>
    );
  }

  return (
    <div className="login-container">
      <div className="login-card create-post-card">
        <h1 className="login-title">
          게시글 수정
        </h1>
        <form className="login-form" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="title" className="text-1 block text-sm font-medium mb-1">
              제목
            </label>
            <input
              type="text"
              id="title"
              name="title"
              required
              className="login-input"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="게시글 제목을 입력하세요."
            />
          </div>

          <div>
            <label htmlFor="category" className="text-1 block text-sm font-medium mb-1">
              카테고리
            </label>
            <select
              id="category"
              name="category"
              required
              className="login-input"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
            >
              <option value="DREAM_DIARY">꿈 다이어리</option>
              <option value="SLEEP_INFO">수면 정보</option>
              <option value="FREE_TALK">자유 게시판</option>
            </select>
          </div>

          <div>
            <label className="text-1 block text-sm font-medium mb-1">
              내용
            </label>
            <TextEditor
              editor={editor}
              onImageButtonClick={() => fileInputRef.current?.click()}
              isLoadingImage={isImageUploading}
            />
          </div>

          <input
            type="file"
            accept="image/*"
            ref={fileInputRef}
            onChange={handleImageUpload}
            className="hidden"
          />

          {error && <p className="login-error-message">{error}</p>}

          <div>
            <button
              type="submit"
              disabled={loading || isImageUploading}
              className="login-button"
            >
              {loading ? '수정 중...' : '게시글 수정'}
            </button>
            <button
              type="button"
              onClick={() => navigate(`/posts/${postId}`)}
              disabled={loading || isImageUploading}
              className="login-button secondary-button mt-2"
            >
              취소
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default PostEditPage;
