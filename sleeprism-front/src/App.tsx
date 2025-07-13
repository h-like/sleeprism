// src/App.js 또는 src/index.js (메인 엔트리 파일)
import React, { useState } from 'react'; // useState import
import { BrowserRouter as Router, Routes, Route, BrowserRouter } from 'react-router-dom';
import PostListPage from './pages/PostListPage';
import PostDetailPage from './pages/PostDetailPage';
import RegisterPage from './pages/RegisterPage';
import PostCreatePage from './pages/PostCreatePage';
import LoginPage from './pages/LoginPage';
import NotFoundPage from './pages/NotFoundPage';

import { ThemeProvider } from './contexts/ThemeContext';
import Header from './components/Header';
import MyPage from './components/MyPage';
import SoundMixer from './components/SoundMixer';
import PostEditPage from './pages/PostEditPage';
import DirectChatPage from './pages/DirectChatPage';
import ChatPage from './pages/ChatPage';
import LandingPage from './pages/LandingPage';




function App() {
  // 다크 모드 상태 관리
  const [isDarkMode, setIsDarkMode] = useState(false);

  // 다크 모드 토글 함수
  const toggleDarkMode = () => {
    setIsDarkMode(prevMode => !prevMode);
  };

  return (
     <Router>
            <ThemeProvider>
                <Header /> {/* 모든 페이지에 공통으로 보이는 헤더 */}
                <Routes>
                    {/* 랜딩 페이지 라우트 */}

                    {/* <Route path="/" element={<HomePage />} /> */}
                    <Route path="/" element={<LandingPage />} />

                    {/* 기존의 다른 페이지 라우트들 */}
                    <Route path="/posts" element={<PostListPage />} />
                    <Route path="/posts/:postId" element={<PostDetailPage />} />
                    <Route path="/posts/:postId/edit" element={<PostEditPage />} />
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/register" element={<RegisterPage />} />
                    <Route path="/posts/new" element={<PostCreatePage />} />
                    <Route path="/chat/:chatRoomId" element={<DirectChatPage />} />
                    <Route path="/chat" element={<ChatPage />} />
                    {/* <Route path="/myPage" element={<MyPage />} /> */}
                    <Route path="/myPage/*" element={<MyPage />} />
                    <Route path="/sound-mixer" element={<SoundMixer />} />


                    {/* Not Found 페이지는 항상 맨 마지막에 위치해야 합니다. */}
                    <Route path="*" element={<NotFoundPage />} />
                </Routes>
            </ThemeProvider>
        </Router>
  );
}

export default App;
