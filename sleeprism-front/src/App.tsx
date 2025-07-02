// src/App.js 또는 src/index.js (메인 엔트리 파일)
import React, { useState } from 'react'; // useState import
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import PostListPage from './pages/PostListPage';
import PostDetailPage from './pages/PostDetailPage';
import RegisterPage from './pages/RegisterPage';
import PostCreatePage from './pages/PostCreatePage';
import LoginPage from './pages/LoginPage';
import LandingPage from './pages/LandingPage';
import NotFoundPage from './pages/NotFoundPage';

import ChatRoomList from './components/ChatRoomList';
import ChatWindow from './components/ChatWindow';
import CreateGroupChatRoomPage from './pages/CreateGroupChatRoomPage';
import ChatBlockedUsersPage from './pages/ChatBlockedUsersPage';
import { ThemeProvider } from './contexts/ThemeContext';
import Header from './components/Header';
import MyPage from './components/MyPage';
import SoundMixer from './components/SoundMixer';
import PostEditPage from './pages/PostEditPage';



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
         <Header />
      <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/posts" element={<PostListPage />} />
      <Route path="/posts/:postId" element={<PostDetailPage />} />
      <Route path="/posts/:postId/edit" element={<PostEditPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/posts/new" element={<PostCreatePage />} />
      <Route path="/chat" element={<ChatRoomList />} />
      <Route path="/chat/:chatRoomId" element={<ChatWindow />} />
      <Route path="/chat/create-group" element={<CreateGroupChatRoomPage />} />
      <Route path="/chat/blocked-users" element={<ChatBlockedUsersPage />} />
      <Route path="/myPage" element={<MyPage />} />
      <Route path="*" element={<NotFoundPage />} />
       <Route path="/sound-mixer" element={<SoundMixer />} />

      </Routes>
      </ThemeProvider>
    </Router>
  );
}

export default App;