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
    const decodedToken = JSON.parse(jsonPayload);
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
  // JWT 토큰의 존재 여부로 로그인 상태를 관리합니다.
  const [isLoggedIn, setIsLoggedIn] = useState<boolean>(!!localStorage.getItem('jwtToken'));
  const [userProfileImage, setUserProfileImage] = useState<string | null>(null);
  const { isDarkMode, toggleDarkMode } = useTheme();

  const [showHeader, setShowHeader] = useState<boolean>(true);
  const [lastScrollY, setLastScrollY] = useState<number>(0);
  const [isMenuOpen, setIsMenuOpen] = useState<boolean>(false);
  // 프로필 드롭다운 메뉴 상태 추가
  const [isProfileDropdownOpen, setIsProfileDropdownOpen] = useState<boolean>(false);

  // 사용자 프로필 이미지를 가져오는 함수 (useCallback으로 최적화)
  // `loggedIn` 매개변수를 추가하여 로그인 상태에 따라 이미지를 가져올지 결정
  const fetchUserProfileImage = useCallback(async (loggedIn: boolean) => {
    if (!loggedIn) {
      setUserProfileImage(defaultAvatar); // 로그인 상태가 아니면 기본 아바타로 설정
      return;
    }

    const token = localStorage.getItem('jwtToken');
    const userId = getUserIdFromToken();

    if (!token || !userId) {
      setUserProfileImage(defaultAvatar); // 토큰이 없거나 유효하지 않으면 기본 아바타로 설정
      return;
    }

    try {
      const response = await fetch(`${BACKEND_BASE_URL}/api/users/profile`, { // 백엔드 URL 사용
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (response.ok) {
        const data = await response.json();
        // 백엔드에서 반환된 profileImageUrl이 있다면 백엔드 URL을 붙여서 사용
        // profileImageUrl이 null이거나 비어있으면 defaultAvatar 사용
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
  }, []);

  // 스크롤 이벤트 핸들러
  const handleScroll = useCallback(() => {
    if (window.scrollY > lastScrollY && window.scrollY > 100) {
      setShowHeader(false);
      // 메뉴가 열려있을 경우 닫기
      setIsMenuOpen(false);
      setIsProfileDropdownOpen(false);
    } else {
      setShowHeader(true);
    }
    setLastScrollY(window.scrollY);
  }, [lastScrollY]);

  // 로그인 상태와 프로필 이미지 업데이트를 위한 useEffect
  useEffect(() => {
    // 토큰의 존재 여부를 기준으로 로그인 상태를 확인
    const token = localStorage.getItem('jwtToken');
    const loggedIn = !!token;
    setIsLoggedIn(loggedIn); // 로그인 상태 업데이트

    // 로그인 상태에 따라 프로필 이미지를 가져옵니다.
    fetchUserProfileImage(loggedIn);

    // `storage` 이벤트 리스너 추가: 다른 탭에서 로그인/로그아웃 시 상태 동기화
    // 이 리스너는 실시간 업데이트에 도움이 됩니다.
    const handleStorageChange = () => {
      const newToken = localStorage.getItem('jwtToken');
      const newLoggedInState = !!newToken;
      setIsLoggedIn(newLoggedInState); // 변경된 로그인 상태를 즉시 반영
      fetchUserProfileImage(newLoggedInState); // 변경된 상태로 프로필 이미지 다시 가져오기
    };
    window.addEventListener('storage', handleStorageChange);

    // 로그인 및 로그아웃 시점의 이벤트를 감지하여 즉시 헤더 업데이트
    // 이 부분을 추가하여 로그인/로그아웃 시 즉각적인 반응을 유도합니다.
    const handleLoginEvent = () => {
        const newToken = localStorage.getItem('jwtToken');
        const newLoggedInState = !!newToken;
        setIsLoggedIn(newLoggedInState);
        fetchUserProfileImage(newLoggedInState);
    };

    // 사용자 정의 이벤트 리스너 (예: 로그인/로그아웃 페이지에서 이벤트를 dispatch할 수 있음)
    window.addEventListener('loginSuccess', handleLoginEvent);
    window.addEventListener('logoutSuccess', handleLoginEvent);


    // 스크롤 이벤트 리스너 추가
    window.addEventListener('scroll', handleScroll);

    // 컴포넌트 언마운트 시 리스너 제거
    return () => {
      window.removeEventListener('storage', handleStorageChange);
      window.removeEventListener('loginSuccess', handleLoginEvent); // 리스너 제거
      window.removeEventListener('logoutSuccess', handleLoginEvent); // 리스너 제거
      window.removeEventListener('scroll', handleScroll);
    };
  }, [fetchUserProfileImage, handleScroll]); // 의존성 배열에 함수 추가

  const handleLogout = () => {
    localStorage.removeItem('jwtToken');
    // `storage` 이벤트가 현재 탭에서는 발생하지 않으므로, 상태를 직접 업데이트합니다.
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
  }, [isProfileDropdownOpen]);


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
            <Link to="/posts" className="nav-link" onClick={handleLinkClick}>
              꿈
            </Link>
            <Link to="/sleep-data" className="nav-link" onClick={handleLinkClick}>
              수면
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
