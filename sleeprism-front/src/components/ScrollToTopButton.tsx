import React, { useState, useEffect } from 'react';
import '../../public/css/LandingPage.css'; // 기존 CSS 파일을 재사용합니다.

const ScrollToTopButton = () => {
  const [isVisible, setIsVisible] = useState(false);

  // 페이지가 300px 이상 스크롤되면 버튼을 표시하는 함수
  const toggleVisibility = () => {
    if (window.scrollY > 300) {
      setIsVisible(true);
    } else {
      setIsVisible(false);
    }
  };

  // 페이지 최상단으로 부드럽게 스크롤하는 함수
  const scrollToTop = () => {
    window.scrollTo({
      top: 0,
      behavior: 'smooth',
    });
  };

  // 컴포넌트가 마운트될 때 scroll 이벤트 리스너를 추가하고,
  // 언마운트될 때 제거합니다.
  useEffect(() => {
    window.addEventListener('scroll', toggleVisibility);
    return () => {
      window.removeEventListener('scroll', toggleVisibility);
    };
  }, []);

  return (
    <>
      {isVisible && (
        <button
          onClick={scrollToTop}
          className="scroll-to-top-button"
          aria-label="Go to top"
        >
          {/* 위쪽 화살표 아이콘 */}
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 19V5M5 12l7-7 7 7"/>
          </svg>
        </button>
      )}
    </>
  );
};

export default ScrollToTopButton;
