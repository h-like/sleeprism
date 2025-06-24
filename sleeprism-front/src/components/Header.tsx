// src/components/Header.tsx
import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom'; // useNavigate 임포트 추가

function Header() {
  const navigate = useNavigate(); // useNavigate 훅 사용
  const [isLoggedIn, setIsLoggedIn] = useState<boolean>(false); // 로그인 상태 관리

  // 컴포넌트 마운트 시 또는 로컬 스토리지 변경 시 로그인 상태 확인
  useEffect(() => {
    const checkLoginStatus = () => {
      const token = localStorage.getItem('jwtToken');
      setIsLoggedIn(!!token); // 토큰이 있으면 true, 없으면 false
    };

    checkLoginStatus(); // 초기 로딩 시 상태 확인

    // 로컬 스토리지 변경 이벤트를 감지하여 로그인 상태 업데이트
    // 이 부분은 브라우저 탭 간의 동기화를 위해 필요할 수 있습니다.
    window.addEventListener('storage', checkLoginStatus);

    return () => {
      window.removeEventListener('storage', checkLoginStatus);
    };
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('jwtToken'); // 로컬 스토리지에서 토큰 제거
    setIsLoggedIn(false); // 로그인 상태 업데이트
    alert('로그아웃되었습니다.'); // 사용자에게 알림
    navigate('/login'); // 로그인 페이지로 리다이렉트
  };

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

          {/* 로그인 상태에 따라 '로그인/회원가입' 또는 '로그아웃' 버튼 표시 */}
          {isLoggedIn ? (
            <button
              onClick={handleLogout} // onClick 이벤트 핸들러 추가
              className="text-lg font-medium hover:text-purple-200 transition duration-300 px-3 py-2 rounded-md hover:bg-white hover:bg-opacity-10"
            >
              로그아웃
            </button>
          ) : (
            <>
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
            </>
          )}
        </div>
      </nav>
    </header>
  );
}

export default Header;