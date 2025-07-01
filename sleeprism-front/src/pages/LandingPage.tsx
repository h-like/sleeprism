import React from "react";
import { useTheme } from "../contexts/ThemeContext";


const features = [
  {
    title: "꿈 이야기 커뮤니티",
    description: "다양한 사람들과 꿈에 대해 자유롭게 이야기하고, 해몽을 나눌 수 있어요.",
    icon: "💭",
  },
  {
    title: "수면 정보 공유",
    description: "수면 습관, 꿀팁, 고민 등 수면에 관한 모든 이야기를 나눠보세요.",
    icon: "🛌",
  },
  {
    title: "백색소음 생성기",
    description: "편안한 수면을 위한 백색소음을 직접 만들어 들을 수 있어요.",
    icon: "🎵",
  },
];

const LandingPage = () => {
  const { isDarkMode } = useTheme(); // useTheme 훅 사용

  return (
    <div className={`landing ${isDarkMode ? 'dark' : 'light'}`}>
      <div className="main-container">
      <section className="hero">
        <h1>꿈과 수면, 모두를 위한 커뮤니티</h1>
        <p>꿈을 나누고, 수면을 개선하며, 백색소음으로 더 깊은 휴식을 경험하세요.</p>
        <button className="start-btn">지금 시작하기</button>
      </section>
      <section className="features">
        {features.map((f, i) => (
          <div className="feature-card" key={i}>
            <div className="icon">{f.icon}</div>
            <h3>{f.title}</h3>
            <p>{f.description}</p>
          </div>
        ))}
      </section>
      <footer className="footer">
        © 2025 SleepRism. All rights reserved.
      </footer>
    </div>
    </div>
  );
};

export default LandingPage;