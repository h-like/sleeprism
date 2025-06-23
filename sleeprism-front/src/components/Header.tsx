// src/components/Header.tsx
import React from 'react';
import { Link } from 'react-router-dom';

function Header() {
  return (
    <header className="bg-gradient-to-r from-purple-600 to-indigo-700 text-white shadow-lg fixed top-0 left-0 w-full z-50">
      <nav className="container mx-auto px-4 py-4 flex justify-between items-center">
        {/* 로고 또는 서비스 이름 */}
        <Link to="/" className="text-3xl font-extrabold tracking-tight hover:text-purple-200 transition duration-300">
          SleepRism
        </Link>

        {/* 내비게이션 링크들 */}
        <div className="flex space-x-6">
          <Link 
            to="/posts" 
            className="text-lg font-medium hover:text-purple-200 transition duration-300 px-3 py-2 rounded-md hover:bg-white hover:bg-opacity-10"
          >
            게시글 목록
          </Link>
          <Link 
            to="/posts/new" 
            className="text-lg font-medium hover:text-purple-200 transition duration-300 px-3 py-2 rounded-md hover:bg-white hover:bg-opacity-10"
          >
            새 게시글 작성
          </Link>
          <Link 
            to="/login" 
            className="text-lg font-medium hover:text-purple-200 transition duration-300 px-3 py-2 rounded-md hover:bg-white hover:bg-opacity-10"
          >
            로그인
          </Link>
          <Link 
            to="/register" 
            className="text-lg font-medium hover:text-purple-200 transition duration-300 px-3 py-2 rounded-md hover:bg-white hover:bg-opacity-10"
          >
            회원가입
          </Link>
        </div>
      </nav>
    </header>
  );
}

export default Header;
