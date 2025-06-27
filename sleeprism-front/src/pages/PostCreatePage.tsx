// src/pages/PostCreatePage.tsx
import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';

// Tiptap 관련 임포트
import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Link from '@tiptap/extension-link';
import Image from '@tiptap/extension-image';

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

  // 백엔드 기본 URL 및 컨텍스트 경로 정의
  const BACKEND_BASE_URL = 'http://localhost:8080';
  const FILE_API_PATH_PREFIX = '/api/posts/files/'; // 파일 API의 기본 경로

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
          class: 'max-w-full h-auto rounded-md shadow-sm my-4', // 에디터 내 이미지 스타일
        },
      }),
    ],
    content: content,
    onUpdate: ({ editor }) => {
      setContent(editor.getHTML());
    },
    editorProps: {
      attributes: {
        class: 'prose prose-sm sm:prose lg:prose-lg xl:prose-xl focus:outline-none p-4 min-h-[300px] border border-gray-300 rounded-b-md shadow-sm',
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

  const MenuBar = ({ editor, onImageButtonClick, isLoadingImage }: { editor: any, onImageButtonClick: () => void, isLoadingImage: boolean }) => {
    if (!editor) {
      return null;
    }

    return (
      <div className="flex flex-wrap gap-2 p-2 border border-gray-300 rounded-t-md bg-gray-50">
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleBold().run()}
          disabled={!editor.can().chain().focus().toggleBold().run()}
          className={`px-2 py-1 rounded ${editor.isActive('bold') ? 'bg-indigo-500 text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300'}`}
        >
          Bold
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleItalic().run()}
          disabled={!editor.can().chain().focus().toggleItalic().run()}
          className={`px-2 py-1 rounded ${editor.isActive('italic') ? 'bg-indigo-500 text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300'}`}
        >
          Italic
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleBulletList().run()}
          className={`px-2 py-1 rounded ${editor.isActive('bulletList') ? 'bg-indigo-500 text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300'}`}
        >
          Bullet List
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleOrderedList().run()}
          className={`px-2 py-1 rounded ${editor.isActive('orderedList') ? 'bg-indigo-500 text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300'}`}
        >
          Ordered List
        </button>
        <button
          type="button"
          onClick={() => {
            const url = window.prompt('URL을 입력하세요:');
            if (url) {
              editor.chain().focus().setLink({ href: url }).run();
            }
          }}
          className={`px-2 py-1 rounded ${editor.isActive('link') ? 'bg-indigo-500 text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300'}`}
        >
          Link
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().unsetLink().run()}
          disabled={!editor.isActive('link')}
          className="px-2 py-1 rounded bg-gray-200 text-gray-700 hover:bg-gray-300"
        >
          Unlink
        </button>
        <button
          type="button"
          onClick={onImageButtonClick}
          disabled={isLoadingImage}
          className={`px-2 py-1 rounded ${isLoadingImage ? 'bg-gray-400 text-gray-600 cursor-not-allowed' : 'bg-gray-200 text-gray-700 hover:bg-gray-300'}`}
        >
          {isLoadingImage ? 'Uploading...' : 'Image'}
        </button>
        {/* FIX: 이미지 삭제 버튼 추가 - 현재 선택된 노드가 이미지 노드일 때만 활성화 (더 정교한 조건 필요) */}
        {/* Tiptap의 deleteSelection()은 현재 선택된 콘텐츠를 삭제합니다.
            이미지를 선택한 상태에서 이 버튼을 누르면 이미지가 삭제됩니다. */}
        <button
          type="button"
          onClick={() => editor.chain().focus().deleteSelection().run()}
          disabled={!editor.can().deleteSelection()} // 선택 영역이 있을 때만 활성화
          className="px-2 py-1 rounded bg-gray-200 text-gray-700 hover:bg-gray-300"
        >
          Delete Selection
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().undo().run()}
          className="px-2 py-1 rounded bg-gray-200 text-gray-700 hover:bg-gray-300"
        >
          Undo
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().redo().run()}
          className="px-2 py-1 rounded bg-gray-200 text-gray-700 hover:bg-gray-300"
        >
          Redo
        </button>
      </div>
    );
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
              <option value="DREAM_DIARY">꿈 다이어리</option>
              <option value="SLEEP_INFO">수면 정보</option>
              <option value="FREE_TALK">자유 게시판</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              내용
            </label>
            {/* Tiptap 에디터 툴바 */}
            <MenuBar
              editor={editor}
              onImageButtonClick={() => fileInputRef.current?.click()}
              isLoadingImage={isImageUploading}
            />
            {/* Tiptap 에디터 본문 */}
            <EditorContent editor={editor} className="mt-1 border border-gray-300 rounded-b-md shadow-sm focus-within:ring-indigo-500 focus-within:border-indigo-500" />
          </div>

          <input
            type="file"
            accept="image/*"
            ref={fileInputRef}
            onChange={handleImageUpload}
            className="hidden"
          />

          {error && <p className="mt-2 text-center text-sm text-red-600">{error}</p>}
          {successMessage && <p className="mt-2 text-center text-sm text-green-600">{successMessage}</p>}

          <div>
            <button
              type="submit"
              disabled={loading || isImageUploading}
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
