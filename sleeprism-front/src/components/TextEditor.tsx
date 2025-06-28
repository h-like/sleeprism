// src/components/TextEditor.tsx
import React from 'react';
import { EditorContent, useEditor } from '@tiptap/react';
import { Editor } from '@tiptap/core'; // Editor 타입 임포트
import { useTheme } from '../contexts/ThemeContext'; // 다크 모드 테마 컨텍스트 임포트

interface TextEditorProps {
  editor: Editor | null; // Tiptap Editor 인스턴스
  onImageButtonClick: () => void; // 이미지 업로드 버튼 클릭 핸들러
  isLoadingImage: boolean; // 이미지 업로드 중인지 여부
}

function TextEditor({ editor, onImageButtonClick, isLoadingImage }: TextEditorProps) {
  const { isDarkMode } = useTheme(); // 다크 모드 상태 가져오기

  if (!editor) {
    return null;
  }

  return (
    <div className={`tiptap-wrapper ${isDarkMode ? 'dark-mode' : 'light-mode'}`}>
      {/* Tiptap 에디터 툴바 */}
      <div className="tiptap-toolbar">
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleBold().run()}
          disabled={!editor.can().chain().focus().toggleBold().run()}
          className={`tiptap-button ${editor.isActive('bold') ? 'is-active' : ''}`}
        >
          Bold
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleItalic().run()}
          disabled={!editor.can().chain().focus().toggleItalic().run()}
          className={`tiptap-button ${editor.isActive('italic') ? 'is-active' : ''}`}
        >
          Italic
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleBulletList().run()}
          className={`tiptap-button ${editor.isActive('bulletList') ? 'is-active' : ''}`}
        >
          Bullet List
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleOrderedList().run()}
          className={`tiptap-button ${editor.isActive('orderedList') ? 'is-active' : ''}`}
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
          className={`tiptap-button ${editor.isActive('link') ? 'is-active' : ''}`}
        >
          Link
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().unsetLink().run()}
          disabled={!editor.isActive('link')}
          className="tiptap-button"
        >
          Unlink
        </button>
        <button
          type="button"
          onClick={onImageButtonClick}
          disabled={isLoadingImage}
          className={`tiptap-button ${isLoadingImage ? 'is-loading' : ''}`}
        >
          {isLoadingImage ? 'Uploading...' : 'Image'}
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().deleteSelection().run()}
          disabled={!editor.can().deleteSelection()}
          className="tiptap-button"
        >
          Delete Selection
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().undo().run()}
          disabled={!editor.can().undo()} // undo 기능 활성화 여부
          className="tiptap-button"
        >
          Undo
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().redo().run()}
          disabled={!editor.can().redo()} // redo 기능 활성화 여부
          className="tiptap-button"
        >
          Redo
        </button>
      </div>
      {/* Tiptap 에디터 본문 */}
      <EditorContent editor={editor} className="tiptap-editor-content" />
    </div>
  );
}

export default TextEditor;
