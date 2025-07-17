// src/components/TextEditor.tsx
import React from 'react';
import { EditorContent } from '@tiptap/react'; // useEditor는 여기서 사용하지 않으므로 제거
import { Editor } from '@tiptap/core'; // Editor 타입 임포트
import { useTheme } from '../contexts/ThemeContext'; // 다크 모드 테마 컨텍스트 임포트
import '../styles/TiptopEditor.css'; // CSS 파일 임포트
import '@fortawesome/fontawesome-free/css/all.min.css';


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
        {/* 기본 서식 그룹 */}
        <div className="toolbar-group">
          <button
            type="button"
            onClick={() => editor.chain().focus().toggleBold().run()}
            disabled={!editor.can().chain().focus().toggleBold().run()}
            className={`tiptap-button ${editor.isActive('bold') ? 'is-active' : ''}`}
          >
            <i className="fas fa-bold"></i> {/* Font Awesome 아이콘 */}
          </button>
          <button
            type="button"
            onClick={() => editor.chain().focus().toggleItalic().run()}
            disabled={!editor.can().chain().focus().toggleItalic().run()}
            className={`tiptap-button ${editor.isActive('italic') ? 'is-active' : ''}`}
          >
            <i className="fas fa-italic"></i>
          </button>
          <button
            type="button"
            onClick={() => editor.chain().focus().toggleStrike().run()}
            disabled={!editor.can().chain().focus().toggleStrike().run()}
            className={`tiptap-button ${editor.isActive('strike') ? 'is-active' : ''}`}
          >
            <i className="fas fa-strikethrough"></i>
          </button>
        </div>

        {/* 헤딩 및 블록 서식 그룹 */}
        <div className="toolbar-group">
          <button
            type="button"
            onClick={() => editor.chain().focus().toggleHeading({ level: 1 }).run()}
            className={`tiptap-button ${editor.isActive('heading', { level: 1 }) ? 'is-active' : ''}`}
          >
            H1
          </button>
          <button
            type="button"
            onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
            className={`tiptap-button ${editor.isActive('heading', { level: 2 }) ? 'is-active' : ''}`}
          >
            H2
          </button>
          <button
            type="button"
            onClick={() => editor.chain().focus().toggleHeading({ level: 3 }).run()}
            className={`tiptap-button ${editor.isActive('heading', { level: 3 }) ? 'is-active' : ''}`}
          >
            H3
          </button>
          <button
            type="button"
            onClick={() => editor.chain().focus().toggleBlockquote().run()}
            className={`tiptap-button ${editor.isActive('blockquote') ? 'is-active' : ''}`}
          >
            <i className="fas fa-quote-right"></i>
          </button>
          <button
            type="button"
            onClick={() => editor.chain().focus().toggleCodeBlock().run()}
            className={`tiptap-button ${editor.isActive('codeBlock') ? 'is-active' : ''}`}
          >
            <i className="fas fa-code"></i>
          </button>
          <button
            type="button"
            onClick={() => editor.chain().focus().setHorizontalRule().run()}
            className="tiptap-button"
          >
            <i className="fas fa-minus"></i> {/* 수평선 */}
          </button>
        </div>

        {/* 리스트 그룹 */}
        <div className="toolbar-group">
          <button
            type="button"
            onClick={() => editor.chain().focus().toggleBulletList().run()}
            className={`tiptap-button ${editor.isActive('bulletList') ? 'is-active' : ''}`}
          >
            <i className="fas fa-list-ul"></i>
          </button>
          <button
            type="button"
            onClick={() => editor.chain().focus().toggleOrderedList().run()}
            className={`tiptap-button ${editor.isActive('orderedList') ? 'is-active' : ''}`}
          >
            <i className="fas fa-list-ol"></i>
          </button>
        </div>

        {/* 링크 및 미디어 그룹 */}
        <div className="toolbar-group">
          <button
            type="button"
            onClick={() => {
              const url = window.prompt('URL을 입력하세요:');
              if (url) {
                editor.chain().focus().setLink({ href: url, target: '_blank' }).run(); // 새 탭에서 열리도록 target 추가
              }
            }}
            className={`tiptap-button ${editor.isActive('link') ? 'is-active' : ''}`}
          >
            <i className="fas fa-link"></i>
          </button>
          <button
            type="button"
            onClick={() => editor.chain().focus().unsetLink().run()}
            disabled={!editor.isActive('link')}
            className="tiptap-button"
          >
            <i className="fas fa-unlink"></i>
          </button>
          <button
            type="button"
            onClick={onImageButtonClick}
            disabled={isLoadingImage}
            className={`tiptap-button ${isLoadingImage ? 'is-loading' : ''}`}
          >
            {isLoadingImage ? <i className="fas fa-spinner fa-spin"></i> : <i className="fas fa-image"></i>}
          </button>
        </div>

        {/* 텍스트 정렬 그룹 */}
        <div className="toolbar-group">
          <button
            type="button"
            onClick={() => editor.chain().focus().setTextAlign('left').run()}
            className={`tiptap-button ${editor.isActive({ textAlign: 'left' }) ? 'is-active' : ''}`}
          >
            <i className="fas fa-align-left"></i>
          </button>
          <button
            type="button"
            onClick={() => editor.chain().focus().setTextAlign('center').run()}
            className={`tiptap-button ${editor.isActive({ textAlign: 'center' }) ? 'is-active' : ''}`}
          >
            <i className="fas fa-align-center"></i>
          </button>
          <button
            type="button"
            onClick={() => editor.chain().focus().setTextAlign('right').run()}
            className={`tiptap-button ${editor.isActive({ textAlign: 'right' }) ? 'is-active' : ''}`}
          >
            <i className="fas fa-align-right"></i>
          </button>
          <button
            type="button"
            onClick={() => editor.chain().focus().setTextAlign('justify').run()}
            className={`tiptap-button ${editor.isActive({ textAlign: 'justify' }) ? 'is-active' : ''}`}
          >
            <i className="fas fa-align-justify"></i>
          </button>
        </div>

        {/* 색상 및 기타 유틸리티 그룹 */}
        <div className="toolbar-group">
            <input
                type="color"
                onInput={(event) => editor.chain().focus().setColor((event.target as HTMLInputElement).value).run()}
                value={editor.getAttributes('textStyle').color || '#000000'}
                className="tiptap-color-input"
            />
            <button
                type="button"
                onClick={() => editor.chain().focus().unsetColor().run()}
                className="tiptap-button"
            >
                <i className="fas fa-text-width"></i> {/* 색상 지우기 아이콘 */}
            </button>
        </div>

        {/* 실행 취소/다시 실행 및 클리어 서식 그룹 */}
        <div className="toolbar-group">
          <button
            type="button"
            onClick={() => editor.chain().focus().unsetAllMarks().clearNodes().run()}
            className="tiptap-button"
          >
            <i className="fas fa-eraser"></i> {/* 서식 지우기 */}
          </button>
          <button
            type="button"
            onClick={() => editor.chain().focus().undo().run()}
            disabled={!editor.can().undo()}
            className="tiptap-button"
          >
            <i className="fas fa-undo"></i>
          </button>
          <button
            type="button"
            onClick={() => editor.chain().focus().redo().run()}
            disabled={!editor.can().redo()}
            className="tiptap-button"
          >
            <i className="fas fa-redo"></i>
          </button>
        </div>
      </div>
      {/* Tiptap 에디터 본문 */}
      <EditorContent editor={editor} className="tiptap-editor-content" />
    </div>
  );
}

export default TextEditor;
