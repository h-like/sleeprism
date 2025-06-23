// src/App.js 또는 src/index.js (메인 엔트리 파일)
import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import PostListPage from './pages/PostListPage';
import PostDetailPage from './pages/PostDetailPage';
// import { LoginPage } from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import PostCreatePage from './pages/PostCreatePage';
import LoginPage from './pages/LoginPage';
import LandingPage from './pages/LandingPage';
import NotFoundPage from './pages/NotFoundPage';
import Header from './components/Header';

// 컴포넌트 임포트 (아직 없으면 빈 파일로라도 만들어두세요)
// import PostListPage from './pages/PostListPage'; // 게시글 목록
// import PostDetailPage from './pages/PostDetailPage'; // 게시글 상세
// import LoginPage from './pages/LoginPage'; // 로그인 페이지 (나중에)
// import RegisterPage from './pages/RegisterPage'; // 회원가입 페이지 (나중에)
// import PostCreatePage from './pages/PostCreatePage'; // 게시글 작성 (나중에)
// import NotFoundPage from './pages/NotFoundPage'; // 404 페이지 (선택 사항)

function App() {
  return (
    <Router>
      {/* 내비게이션 바 등 공통 컴포넌트는 이곳에 */}
      <Header />

      <Routes>
        {/* 1. 게시글 목록 (메인 페이지) */}
        <Route path="/" element={<LandingPage />} />
        <Route path="/posts" element={<PostListPage />} /> {/* /posts 경로도 목록으로 */}

        {/* 2. 게시글 상세 */}
        <Route path="/posts/:postId" element={<PostDetailPage />} />

        {/* 3. 로그인/회원가입 (나중에 구현) */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {/* 4. 게시글 작성 (나중에 구현) */}
        <Route path="/posts/new" element={<PostCreatePage />} />
        {/* <Route path="/posts/:postId/edit" element={<PostEditPage />} /> */}

        {/* 5. 404 Not Found 페이지 (선택 사항) */}
        <Route path="*" element={<NotFoundPage />} />
      </Routes>

      {/* 풋터 등 공통 컴포넌트는 이곳에 */}
      {/* <Footer /> */}
    </Router>
  );
}

export default App
