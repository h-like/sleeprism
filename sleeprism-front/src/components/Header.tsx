import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import defaultAvatar from '../../public/assets/default-avatar.png'; // 예시 경로
import userProfile from '../../public/assets/user-profile.png';   // 예시 경로
import '../../public/css/Header.css'; // CSS 파일을 임포트합니다.
import { useTheme } from '../contexts/ThemeContext';

function Header() {
  const navigate = useNavigate();
  const [isLoggedIn, setIsLoggedIn] = useState<boolean>(false);
  const { isDarkMode, toggleDarkMode } = useTheme();
  
  // 스크롤에 따른 헤더 가시성 상태
  const [showHeader, setShowHeader] = useState<boolean>(true);
  const [lastScrollY, setLastScrollY] = useState<number>(0);

  // 스크롤 이벤트 핸들러
  const handleScroll = () => {
    // 현재 스크롤 위치가 마지막 스크롤 위치보다 크고, 100px 이상 스크롤했을 때 헤더 숨김
    if (window.scrollY > lastScrollY && window.scrollY > 100) {
      setShowHeader(false);
    } 
    // 스크롤을 올리거나, 최상단에 있을 때 헤더 표시
    else {
      setShowHeader(true);
    }
    // 마지막 스크롤 위치 업데이트
    setLastScrollY(window.scrollY);
  };

  useEffect(() => {
    // 로그인 상태 확인 로직
    const checkLoginStatus = () => {
      const token = localStorage.getItem('jwtToken');
      setIsLoggedIn(!!token);
    };
    checkLoginStatus();
    
    // storage 이벤트와 scroll 이벤트 리스너 등록
    window.addEventListener('storage', checkLoginStatus);
    window.addEventListener('scroll', handleScroll);

    // 컴포넌트 언마운트 시 이벤트 리스너 제거
    return () => {
      window.removeEventListener('storage', checkLoginStatus);
      window.removeEventListener('scroll', handleScroll);
    };
  }, [lastScrollY]); // lastScrollY가 변경될 때마다 훅을 재실행하여 스크롤 위치를 추적

  const handleLogout = () => {
    localStorage.removeItem('jwtToken');
    setIsLoggedIn(false);
    alert('로그아웃되었습니다.');
    navigate('/login');
  };

  const goToMyPage = () => {
    navigate('/mypage');
  };
  
  // 로그인 상태와 isDarkMode 상태에 따라 프로필 이미지 경로 결정
  const profileImageSrc = isLoggedIn ? userProfile : defaultAvatar;

  return (
    // showHeader 상태에 따라 'visible' 또는 'hidden' 클래스를 동적으로 추가
    <header className={`header ${isDarkMode ? 'dark' : 'light'} ${showHeader ? 'visible' : 'hidden'}`}>
      <nav className="nav">
        {/* 이미지에 맞게 로고 텍스트를 'MongMong'으로 변경 */}
        <Link to="/" className="logo">🌙 Sleeprsim</Link>
        <div className="search-bar-container">
          <input type="text" placeholder="Search dreams..." className="search-input" />
          <button className="search-icon" aria-label="Search">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="feather feather-search">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
          </button>
        </div>
        <div className="nav-links">
          {/* 이미지에 있는 "Share Dream" 버튼 추가 */}
          <Link to="/posts" className="">
            꿈
          </Link>
          <Link to="/share-dream" className="">
            수면
          </Link>
          <Link to="/register" className="share-dream-btn">
            글쓰기
          </Link>
          
          {/* 다크/라이트 모드 토글 버튼 */}
          <button className="toggle-btn" onClick={toggleDarkMode}>
            {isDarkMode ? '☀️' : '🌙'}
          </button>

          {/* 로그인 상태에 따라 프로필 이미지/아바타 표시 */}
          <img
            src={profileImageSrc}
            alt="프로필"
            className="profile-img"
            onClick={isLoggedIn ? goToMyPage : () => navigate('/login')} // 로그인 시 마이페이지, 아니면 로그인 페이지로 이동
            style={{ cursor: 'pointer' }}
          />
          {/* 이미지에 보이지 않는 기존의 로그인/로그아웃 버튼은 숨기거나 제거했습니다.
              필요하다면 다시 추가할 수 있습니다. */}
        </div>
      </nav>
    </header>
  );
}

export default Header;