import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTheme } from '../contexts/ThemeContext';
import defaultAvatar from '../../public/assets/default-avatar.png';
import logo from '../../public/assets/logo.png';
import '../../public/css/Header.css';

const BACKEND_BASE_URL = "http://localhost:8080";

function Header() {
    const navigate = useNavigate();
    const { isDarkMode, toggleDarkMode } = useTheme();
    const [isLoggedIn, setIsLoggedIn] = useState<boolean>(false);
    const [userProfileImage, setUserProfileImage] = useState<string>(defaultAvatar);
    const [isProfileDropdownOpen, setIsProfileDropdownOpen] = useState<boolean>(false);
    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState<boolean>(false);

    const updateUserProfile = useCallback(async () => {
        const token = localStorage.getItem('jwtToken');
        if (!token) {
            setIsLoggedIn(false);
            setUserProfileImage(defaultAvatar);
            return;
        }

        setIsLoggedIn(true);
        try {
            const response = await fetch(`${BACKEND_BASE_URL}/api/users/profile`, {
                headers: { 'Authorization': `Bearer ${token}` },
            });
            if (response.ok) {
                const data = await response.json();
                setUserProfileImage(data.profileImageUrl ? `${BACKEND_BASE_URL}${data.profileImageUrl}` : defaultAvatar);
            } else {
                setUserProfileImage(defaultAvatar);
            }
        } catch (error) {
            setUserProfileImage(defaultAvatar);
        }
    }, []);

    useEffect(() => {
        updateUserProfile();
        window.addEventListener('storage', updateUserProfile);
        window.addEventListener('loginSuccess', updateUserProfile);
        window.addEventListener('logoutSuccess', updateUserProfile);

        return () => {
            window.removeEventListener('storage', updateUserProfile);
            window.removeEventListener('loginSuccess', updateUserProfile);
            window.removeEventListener('logoutSuccess', updateUserProfile);
        };
    }, [updateUserProfile]);

    const handleLogout = () => {
        localStorage.removeItem('jwtToken');
        window.dispatchEvent(new Event('logoutSuccess'));
        navigate('/login');
    };

    const closeAllMenus = () => {
        setIsProfileDropdownOpen(false);
        setIsMobileMenuOpen(false);
    };

    return (
        <header className={`header ${isMobileMenuOpen ? 'menu-open' : ''}`}>
            <nav className="nav">
                <Link to="/" className="logo" onClick={closeAllMenus}>
                    <img src={logo} alt="Sleeprism Logo" />
                    <span className="logo-text">Sleeprism</span>
                </Link>

                {/* 데스크탑 네비게이션 */}
                <div className="nav-links-wrapper">
                    <div className="nav-links">
                        <Link to="/posts?category=ALL" className="nav-link">게시판</Link>
                        <Link to="/sound-mixer" className="nav-link">음악</Link>
                        {isLoggedIn ? (
                            <>
                                <Link to="/posts/new" className="cta-button">글쓰기</Link>
                                <div className={`profile-dropdown ${isProfileDropdownOpen ? 'active' : ''}`}>
                                    <img
                                        src={userProfileImage}
                                        alt="프로필"
                                        className="profile-img"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            setIsProfileDropdownOpen(prev => !prev);
                                        }}
                                    />
                                    <div className="dropdown-content">
                                        <button onClick={() => { navigate('/mypage'); closeAllMenus(); }} className="dropdown-item">마이페이지</button>
                                        <button onClick={handleLogout} className="dropdown-item logout-item">로그아웃</button>
                                    </div>
                                </div>
                            </>
                        ) : (
                            <button onClick={() => navigate('/login')} className="cta-button">로그인</button>
                        )}
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

                {/* 모바일 메뉴 토글 */}
                <button className="menu-toggle" onClick={() => setIsMobileMenuOpen(prev => !prev)} aria-label="Toggle menu">
                    <span className="menu-line"></span>
                    <span className="menu-line"></span>
                    <span className="menu-line"></span>
                </button>
            </nav>
        </header>
    );
}

export default Header;
