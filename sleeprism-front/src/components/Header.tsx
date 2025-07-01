import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import defaultAvatar from '../../public/assets/default-avatar.png';
import logo from '../../public/assets/logo.png';
import '../../public/css/Header.css';
import { useTheme } from '../contexts/ThemeContext';

// 백엔드 서버의 기본 URL을 정의합니다.
// 개발 환경에서는 localhost:8080을 사용하고, 배포 시에는 실제 백엔드 도메인으로 변경해야 합니다.
const BACKEND_BASE_URL = "http://localhost:8080";

// JWT 토큰에서 사용자 ID를 디코딩하는 헬퍼 함수
interface DecodedToken {
  userId?: number;
  id?: number;
  sub?: string;
  jti?: string;
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
    // 토큰 payload 구조에 따라 userId, id, sub, jti 등을 확인하여 사용자 ID를 추출합니다.
    const userIdRaw = decodedToken.userId || decodedToken.id || decodedToken.sub || decodedToken.jti;
    const userId = typeof userIdRaw === 'number' ? userIdRaw : parseInt(userIdRaw as string, 10);
    return isNaN(userId) ? null : userId;
  } catch (e) {
    console.error('JWT 토큰 디코딩 중 오류 발생:', e);
    return null;
  }
};

function Header() {
  const navigate = useNavigate();
  // isLoggedIn 상태를 useEffect 내부에서 토큰 존재 여부에 따라 설정하도록 초기값을 false로 설정
  const [isLoggedIn, setIsLoggedIn] = useState<boolean>(false); 
  const [userProfileImage, setUserProfileImage] = useState<string | null>(null);
  const { isDarkMode, toggleDarkMode } = useTheme();

  const [showHeader, setShowHeader] = useState<boolean>(true);
  const [lastScrollY, setLastScrollY] = useState<number>(0);
  const [isMenuOpen, setIsMenuOpen] = useState<boolean>(false);
  const [isProfileDropdownOpen, setIsProfileDropdownOpen] = useState<boolean>(false);

  // 사용자 프로필 이미지를 가져와 상태를 업데이트하는 함수
  // 이 함수는 로그인 상태가 변경되거나, 컴포넌트가 마운트될 때만 호출되도록 의존성을 비워둡니다.
  const updateUserProfileImage = useCallback(async () => {
    const token = localStorage.getItem('jwtToken');
    const loggedInStatus = !!token; // 현재 시점의 로그인 상태를 가져옵니다.
    setIsLoggedIn(loggedInStatus); // isLoggedIn 상태를 업데이트합니다.

    if (!loggedInStatus) {
      setUserProfileImage(defaultAvatar); // 로그인 상태가 아니면 기본 아바타로 설정
      return;
    }

    const userId = getUserIdFromToken();
    if (!userId) {
      setUserProfileImage(defaultAvatar); // 토큰이 없거나 유효하지 않으면 기본 아바타로 설정
      return;
    }

    try {
      const response = await fetch(`${BACKEND_BASE_URL}/api/users/profile`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (response.ok) {
        const data = await response.json();
        const imageUrl = data.profileImageUrl
          ? `${BACKEND_BASE_URL}${data.profileImageUrl}`
          : defaultAvatar;
        setUserProfileImage(imageUrl);
      } else {
        console.error('Failed to fetch user profile image:', response.status, response.statusText);
        setUserProfileImage(defaultAvatar); // API 호출 실패 시 기본 아바타 사용
      }
    } catch (error) {
      console.error('Error fetching user profile:', error);
      setUserProfileImage(defaultAvatar); // 네트워크 오류 발생 시 기본 아바타 사용
    }
  }, []); // 의존성 배열을 비워 함수는 컴포넌트 마운트 시 한 번만 생성되도록 함

  // 1. 로그인 상태 변화 및 프로필 이미지 로딩을 위한 useEffect
  // 이 useEffect는 컴포넌트 마운트 시, 그리고 storage 이벤트나 커스텀 로그인/로그아웃 이벤트 발생 시에만
  // updateUserProfileImage를 호출하여 프로필 정보를 업데이트합니다.
  useEffect(() => {
    // 컴포넌트 마운트 시 로그인 상태 확인 및 프로필 이미지 초기 로드
    updateUserProfileImage();

    // `storage` 이벤트 리스너 추가: 다른 탭에서 로그인/로그아웃 시 상태 동기화
    const handleStorageChange = () => {
      updateUserProfileImage(); // 스토리지 변경 시 프로필 이미지 다시 가져오기
    };
    window.addEventListener('storage', handleStorageChange);

    // 사용자 정의 이벤트 리스너: 로그인/로그아웃 페이지에서 이벤트를 dispatch할 수 있음
    const handleLoginEvent = () => {
      updateUserProfileImage(); // 로그인/로그아웃 성공 시 프로필 이미지 다시 가져오기
    };
    window.addEventListener('loginSuccess', handleLoginEvent);
    window.addEventListener('logoutSuccess', handleLoginEvent);

    // 컴포넌트 언마운트 시 리스너 제거
    return () => {
      window.removeEventListener('storage', handleStorageChange);
      window.removeEventListener('loginSuccess', handleLoginEvent);
      window.removeEventListener('logoutSuccess', handleLoginEvent);
    };
  }, [updateUserProfileImage]); // updateUserProfileImage가 변경될 때만 이 useEffect를 재실행 (useCallback 덕분에 거의 변경되지 않음)

  // 2. 스크롤 이벤트 핸들러 및 헤더 표시/숨김을 위한 useEffect
  // 이 useEffect는 스크롤 관련 로직만 담당하며, 프로필 이미지 로딩과는 독립적입니다.
  const handleScroll = useCallback(() => {
    // window.scrollY를 직접 사용하여 lastScrollY 의존성 제거
    const currentScrollY = window.scrollY;
    if (currentScrollY > lastScrollY && currentScrollY > 100) {
      setShowHeader(false);
      // 메뉴가 열려있을 경우 닫기
      setIsMenuOpen(false);
      setIsProfileDropdownOpen(false);
    } else {
      setShowHeader(true);
    }
    setLastScrollY(currentScrollY); // lastScrollY 상태 업데이트
  }, [lastScrollY]); // lastScrollY가 변경될 때만 handleScroll 함수를 재생성

  useEffect(() => {
    window.addEventListener('scroll', handleScroll);
    return () => {
      window.removeEventListener('scroll', handleScroll);
    };
  }, [handleScroll]); // handleScroll이 변경될 때만 이 useEffect를 재실행 (useCallback 덕분에 거의 변경되지 않음)

  // 드롭다운 메뉴 외부를 클릭했을 때 메뉴 닫기
  useEffect(() => {
    const handleDocumentClick = (event: MouseEvent) => {
      // 프로필 드롭다운이 열려있고, 클릭된 요소가 드롭다운 영역 밖에 있을 경우
      const dropdown = document.querySelector('.profile-dropdown');
      if (isProfileDropdownOpen && dropdown && !dropdown.contains(event.target as Node)) {
        setIsProfileDropdownOpen(false);
      }
    };

    document.addEventListener('click', handleDocumentClick);

    return () => {
      document.removeEventListener('click', handleDocumentClick);
    };
  }, [isProfileDropdownOpen]); // isProfileDropdownOpen 상태가 변경될 때만 이 useEffect를 재실행

  const handleLogout = () => {
    localStorage.removeItem('jwtToken');
    // `storage` 이벤트는 현재 탭에서는 발생하지 않으므로, 상태를 직접 업데이트하고 이벤트를 디스패치합니다.
    setIsLoggedIn(false);
    setUserProfileImage(defaultAvatar); // 로그아웃 시 기본 아바타로 즉시 변경
    setIsProfileDropdownOpen(false); // 드롭다운 닫기
    alert('로그아웃되었습니다.');
    navigate('/login');
    // 로그아웃 성공 이벤트를 dispatch하여 다른 컴포넌트(Header)에서 즉시 감지하도록 합니다.
    window.dispatchEvent(new Event('logoutSuccess'));
  };

  const goToMyPage = () => {
    setIsProfileDropdownOpen(false); // 드롭다운 닫기
    navigate('/mypage');
  };

  const toggleMenu = () => {
    setIsMenuOpen(!isMenuOpen);
  };

  const handleLinkClick = () => {
    setIsMenuOpen(false);
  };

  // 프로필 이미지 클릭 시 드롭다운 메뉴 토글
  const toggleProfileDropdown = (event: React.MouseEvent) => {
    event.stopPropagation(); // 이벤트 버블링 방지
    setIsProfileDropdownOpen(prevState => !prevState);
  };

  return (
    <header className={`header ${isDarkMode ? 'dark' : 'light'} ${showHeader ? 'visible' : 'hidden'} ${isMenuOpen ? 'menu-open' : ''}`}>
      <nav className="nav">
        <Link to="/" className="logo" onClick={handleLinkClick}>
          <img
            src={logo}
            alt="Sleeprism Logo"
            style={{
              height: '40px',
              width: 'auto',
              objectFit: 'contain',
              cursor: 'pointer',
            }}
          />
          <span className="logo-text">Sleeprism</span>
        </Link>

        <div className="search-bar-container desktop-only">
          <input type="text" placeholder="Search dreams..." className="search-input" />
          <button className="search-icon" aria-label="Search">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="feather feather-search">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
          </button>
        </div>
        
        <button className="menu-toggle" onClick={toggleMenu} aria-label="Toggle menu">
            <span className="menu-line"></span>
            <span className="menu-line"></span>
            <span className="menu-line"></span>
        </button>
        
        <div className={`nav-links-wrapper ${isMenuOpen ? 'active' : ''}`}>
          <div className="nav-links">
              {/* "전체" 게시글을 보여주는 링크 (필요하다면 추가) */}
              <Link to="/posts?category=ALL" className="nav-link" onClick={handleLinkClick}>
                게시판
              </Link>
              <Link to="/sound-mixer" className="nav-link" onClick={handleLinkClick}>
                음악
              </Link>
            {isLoggedIn ? (
              <>
                <Link to="/posts/new" className="cta-button" onClick={handleLinkClick}>
                  글쓰기
                </Link>
                {/* 드롭다운 메뉴를 클릭으로 토글하도록 수정 */}
                <div className={`profile-dropdown ${isProfileDropdownOpen ? 'active' : ''}`}>
                  <img
                    src={userProfileImage || defaultAvatar} // userProfileImage가 null이면 defaultAvatar 사용
                    alt="프로필"
                    className="profile-img"
                    onClick={toggleProfileDropdown} // 클릭 이벤트 핸들러 추가
                    style={{ cursor: 'pointer' }}
                  />
                  {/* 드롭다운 메뉴의 가시성을 상태에 따라 제어 */}
                  <div className="dropdown-content">
                    <button onClick={goToMyPage} className="dropdown-item">마이페이지</button>
                    <button onClick={handleLogout} className="dropdown-item logout-item">로그아웃</button>
                  </div>
                </div>
              </>
            ) : (
              <button onClick={() => { navigate('/login'); handleLinkClick(); }} className="cta-button">
                로그인
              </button>
            )}

            {/* 다크/라이트 모드 토글 버튼 - Codepen 디자인 적용 */}
            <input 
              id="theme-toggle" 
              className="toggle" 
              type="checkbox" 
              checked={isDarkMode} 
              onChange={toggleDarkMode} 
              aria-label="Toggle dark mode"
            />
          </div>
        </div>
      </nav>
    </header>
  );
}

export default Header;
