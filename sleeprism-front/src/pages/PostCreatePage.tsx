// src/pages/PostCreatePage.tsx
import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';

// Tiptap 관련 임포트
import { useEditor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Link from '@tiptap/extension-link';
import Image from '@tiptap/extension-image';

// 공통 CSS 및 TextEditor 컴포넌트 임포트
import '../../public/css/LoginPage.css'; // 공통 스타일 재사용
import TextEditor from '../components/TextEditor'; // 새로 생성한 TextEditor 컴포넌트

function PostCreatePage() {
  const [title, setTitle] = useState<string>('');
  const [category, setCategory] = useState<string>('DREAM_DIARY');
  const [content, setContent] = useState<string>('');
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [isImageUploading, setIsImageUploading] = useState<boolean>(false);

  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null); // 타입 명시

  // 백엔드 기본 URL 정의
  const BACKEND_BASE_URL = 'http://localhost:8080';

  const editor = useEditor({
    extensions: [
      StarterKit,
      Link.configure({
        openOnClick: false,
        autolink: true,
      }),
      Image.configure({
        inline: true,
        allowBase64: false,
        HTMLAttributes: {
          class: 'tiptap-image', // 에디터 내 이미지 스타일을 위한 새로운 클래스
        },
      }),
    ],
    content: content,
    onUpdate: ({ editor }) => {
      setContent(editor.getHTML());
    },
    // editorProps는 이제 TextEditor 컴포넌트 내부에서 설정하는 것이 좋습니다.
    // 하지만 Tiptap의 기본 스타일을 오버라이드하기 위해 일부 속성을 유지합니다.
    editorProps: {
      attributes: {
        class: 'prose prose-sm sm:prose lg:prose-lg xl:prose-xl focus:outline-none',
      },
    },
  });

  useEffect(() => {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
      console.warn('게시글을 작성하려면 로그인이 필요합니다.');
      navigate('/login');
    }
  }, [navigate]);

  const handleImageUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('image', file);

    setIsImageUploading(true);
    setError(null);

    try {
      const token = localStorage.getItem('jwtToken');
      // 백엔드 upload-image 엔드포인트는 /api/posts/upload-image
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

      // FIX: 에디터에 삽입할 이미지 URL에 컨텍스트 경로를 포함하여 절대 경로로 만듭니다.
      // 백엔드에서 '/api/posts/files/...' 형식의 URL이 반환되므로, 그 앞에 전체 백엔드 URL을 붙입니다.
      const imageUrlForEditor = `${BACKEND_BASE_URL}${data.url}`;
      editor?.chain().focus().setImage({ src: imageUrlForEditor }).run();
      setSuccessMessage('이미지가 성공적으로 업로드되었습니다.');
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
      let contentToSave = editor?.getHTML() || '';
      // FIX: 데이터베이스에 저장하기 전에, 에디터 내의 절대 경로 URL을 다시 상대 경로로 변환
      // http://localhost:8080/api/posts/files/ -> /api/posts/files/
      // 이렇게 해야 백엔드 HtmlSanitizer가 올바르게 처리합니다.
      const absoluteUrlPrefix = `${BACKEND_BASE_URL}`;
      contentToSave = contentToSave.replace(new RegExp(absoluteUrlPrefix, 'g'), '');
      
      const response = await fetch(`${BACKEND_BASE_URL}/api/posts`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({ title, category, content: contentToSave }),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || '게시글 작성에 실패했습니다.');
      }

      setSuccessMessage('게시글이 성공적으로 작성되었습니다!');
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
    <div className="main-container"> 
      <div className="post-create-wraper">
        <h1 className="login-title">
          새 게시글 작성
        </h1>
        <form className="post-form" onSubmit={handleSubmit}> {/* space-y-6 -> login-form */}
          <div>
            <label htmlFor="title" className="text-1 block text-sm font-medium mb-1"> {/* text-gray-700 -> text-1 */}
              제목
            </label>
            <input
              type="text"
              id="title"
              name="title"
              required
              className="post-input my-post-title" 
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="게시글 제목을 입력하세요."
            />
          </div>

          <div className="select-wrapper">
            <label htmlFor="category" className="text-1 block text-sm font-medium mb-1">
                카테고리
            </label>
            <select
                id="category"
                name="category"
                required
                className="post-input"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
            >
                <option value="DREAM_DIARY">꿈 다이어리</option>
                <option value="SLEEP_INFO">수면 정보</option>
                <option value="FREE_TALK">자유 게시판</option>
            </select>
            </div>

          <div>
            <label className="text-1 block text-sm font-medium mb-1"> {/* text-gray-700 -> text-1 */}
              내용
            </label>
            {/* Tiptap 에디터 컴포넌트 사용 */}
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

          {error && <p className="login-error-message">{error}</p>}  {/* mt-2 text-center ... -> login-error-message */}
          {successMessage && <p className="login-success-message">{successMessage}</p>} {/* mt-2 text-center ... -> login-success-message */}

          <div>
            <button
              type="submit"
              disabled={loading || isImageUploading}
              className="login-button" 
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
