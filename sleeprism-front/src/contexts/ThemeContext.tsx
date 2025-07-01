import React, { createContext, useState, useContext, useEffect, type ReactNode } from 'react';

// 1. Context의 타입을 정의합니다.
// isDarkMode (boolean): 현재 다크 모드인지 여부
// toggleDarkMode (function): 다크 모드를 토글하는 함수
interface ThemeContextType {
  isDarkMode: boolean;
  toggleDarkMode: () => void;
}

// 2. Context를 생성하고 초기값을 undefined로 설정합니다.
// createContext에 타입을 지정하여 TypeScript의 타입 추론을 돕습니다.
const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

// 3. Provider 컴포넌트를 만듭니다.
// 이 컴포넌트는 앱의 최상위 컴포넌트(예: App.tsx)에서 자식들을 감싸야 합니다.
export const ThemeProvider = ({ children }: { children: ReactNode }) => {
  // 4. 로컬 스토리지에서 테마 설정을 불러와 초기 상태로 사용합니다.
  // 사용자가 마지막으로 설정한 테마를 기억하여 새로고침해도 유지되도록 합니다.
  const [isDarkMode, setIsDarkMode] = useState<boolean>(() => {
    try {
      const savedTheme = localStorage.getItem('theme');
      return savedTheme === 'dark';
    } catch (error) {
      // localStorage에 접근할 수 없는 환경(SSR 등)을 위한 예외 처리
      console.error("Failed to access localStorage:", error);
      return false; // 기본값으로 false (라이트 모드)
    }
  });

  // 5. isDarkMode 상태가 변경될 때마다 로컬 스토리지와 body 클래스를 업데이트합니다.
  useEffect(() => {
    try {
      if (isDarkMode) {
        document.body.classList.add('dark-mode');
        document.body.classList.remove('light-mode');
        localStorage.setItem('theme', 'dark');
      } else {
        document.body.classList.add('light-mode');
        document.body.classList.remove('dark-mode');
        localStorage.setItem('theme', 'light');
      }
    } catch (error) {
      console.error("Failed to update theme classes or localStorage:", error);
    }
  }, [isDarkMode]);

  // 6. 다크 모드 상태를 토글하는 함수
  const toggleDarkMode = () => {
    setIsDarkMode(prevMode => !prevMode);
  };

  // 7. Context Provider에 전달할 값을 정의합니다.
  const value = { isDarkMode, toggleDarkMode };

  return (
    <ThemeContext.Provider value={value}>
      {children}
    </ThemeContext.Provider>
  );
};

// 8. custom hook을 만들어 편리하게 Context 값을 사용합니다.
export const useTheme = () => {
  const context = useContext(ThemeContext);
  // Context가 Provider 내에서 사용되지 않았을 경우 에러를 발생시켜 디버깅을 돕습니다.
  if (context === undefined) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};