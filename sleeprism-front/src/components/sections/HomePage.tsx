import React from 'react';
// import './styles.css';

function HomePage() {
  return (
    <div className="container">
      {/* --- 섹션 1: 메인 & 꿈 입력 --- */}
      <section className="section" style={{ backgroundColor: '#f0f4ff' }}>
        <div className="content-box">
          <h1 className="title">
            간밤에 꾼 꿈,
            <br />
            어떤 의미일까요?
          </h1>
          <p className="description">
            꿈 내용을 입력하면 AI가 핵심 상징을 분석해 드려요.
          </p>
          <div className="dream-input-card">
            <textarea
              className="dream-textarea"
              placeholder="어젯밤 꿈 이야기를 자유롭게 적어주세요. 인상 깊었던 장면이나 감정을 구체적으로 작성하면 더 정확한 분석이 가능합니다."
            />
            <button className="main-button">
              AI로 해몽 결과 보기 →
            </button>
          </div>
        </div>
      </section>

      {/* --- 섹션 2: AI 분석 기능 소개 --- */}
      <section className="section" style={{ backgroundColor: '#ffffff' }}>
        <div className="content-box">
          <div className="feature-icon">🧠</div>
          <h2 className="subtitle">
            AI가 낱낱이 알려주는
            <br />
            꿈의 상징과 심리
          </h2>
          <p className="description">
            수백만 개의 꿈 데이터로 학습한 AI가
            <br />
            당신의 현재 심리상태와 무의식을 해석해 줍니다.
          </p>
        </div>
      </section>

      {/* --- 섹션 3: 수면 관리 기능 소개 --- */}
      <section className="section" style={{ backgroundColor: '#f9fafb' }}>
        <div className="content-box">
          <div className="feature-icon">🛌</div>
          <h2 className="subtitle">
            매일의 기록으로 찾는
            <br />
            최상의 수면 컨디션
          </h2>
          <p className="description">
            수면 시간과 꿈을 꾸준히 기록하고
            <br />
            더 나은 잠을 위한 맞춤 리포트를 받아보세요.
          </p>
        </div>
      </section>

      {/* --- 섹션 4: 최종 행동 유도 (CTA) 및 푸터 --- */}
      <section className="section" style={{ backgroundColor: '#ffffff' }}>
        <div className="content-box">
          <h2 className="subtitle">
            더 나은 내일을 위한
            <br />
            첫걸음을 시작하세요
          </h2>
          <button className="cta-button">
            무료로 시작하기
          </button>
        </div>
        <footer className="footer">
          <p>(주)드림 인사이트 | 대표: 홍길동 | 사업자등록번호: 123-45-67890</p>
          <div className="footer-links">
            <a href="#terms">이용약관</a>
            <span>|</span>
            <a href="#privacy">개인정보처리방침</a>
          </div>
        </footer>
      </section>
    </div>
  );
}

export default HomePage;
