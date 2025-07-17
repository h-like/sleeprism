import React, { useState, useEffect, useRef } from 'react';
import '../../public/css/LandingPage.css';
import { useTheme } from '../contexts/ThemeContext';
import ScrollToTopButton from '../components/ScrollToTopButton';

// --- 타입 정의 (제공해주신 PostListPageTSX.txt 기반) --- //
interface Post {
  id: number;
  title: string;
  content: string;
  authorNickname: string;
  createdAt: string;
  likeCount: number;
  commentCount: number;
}

// --- 헬퍼 함수 (제공해주신 PostListPageTSX.txt 기반) --- //
const getPlainTextSummary = (htmlContent: string, maxLength: number = 100): string => {
  if (!htmlContent) return '내용이 없습니다.';
  const parser = new DOMParser();
  const doc = parser.parseFromString(htmlContent, 'text/html');
  const textContent = doc.body.textContent || '';
  if (textContent.length > maxLength) {
    return textContent.substring(0, maxLength) + '...';
  }
  return textContent;
};


// --- 개별 컴포넌트 --- //

const useFadeInOnScroll = () => {
    const ref = useRef<HTMLElement>(null);
    useEffect(() => {
        const observer = new IntersectionObserver(
            ([entry]) => { if (entry.isIntersecting) { entry.target.classList.add('is-visible'); } },
            { threshold: 0.1 }
        );
        if (ref.current) observer.observe(ref.current);
        return () => { if (ref.current) observer.unobserve(ref.current); };
    }, []);
    return ref;
};

const HeroSection = ({ onScrollToPopular }: { onScrollToPopular: () => void }) => (
    <section id="hero" className="hero-section">
        <div className="video-background">
            <video className="video-element" autoPlay loop muted playsInline poster="images/surreal-bed.png">
                {/* <source src="video/night_sky.mp4" type="video/mp4" /> */}
                <source src="video/surreal-dream.mp4" type="video/mp4" />
            </video>
            <div className="video-overlay"></div>
        </div>
        <div className="hero-content">
            <h1 className="hero-title">
                <span className="block">당신의 꿈,</span>
                <span className="block mt-2">그 이상의 의미.</span>
            </h1>
            <p className="hero-subtitle">Dreamscape가 AI와 타로의 힘으로 당신의 꿈 속 깊은 곳을 탐험합니다.</p>
            <button onClick={onScrollToPopular} className="hero-button">인기 꿈 해석 보기</button>
        </div>
    </section>
);

const FeaturesSection = () => {
    const sectionRef = useFadeInOnScroll();
    const item1Ref = useFadeInOnScroll();
    const item2Ref = useFadeInOnScroll();
    const item3Ref = useFadeInOnScroll();
    return (
        <section id="features" className="features-section" ref={sectionRef}>
            <div className="container">
                <div className="section-header">
                    <h2 className="section-title">밤의 언어를<br />해독하는 방법</h2>
                    <p className="section-description">복잡한 꿈의 상징부터 마음을 편안하게 해주는 ASMR까지, Dreamscape는 당신의 밤을 위한 완벽한 동반자입니다.</p>
                </div>
                <div className="features-grid">
                    <div className="feature-item" ref={item1Ref}>
                        <img src="https://placehold.co/800x600/1a1a1c/a1a1a6?text=꿈+기록+화면" alt="꿈 기록 화면 예시" className="feature-image" />
                        <h3 className="feature-title">1. 꿈을 기록하세요</h3>
                        <p className="feature-description">어젯밤 꾼 꿈을 자유롭게 적어보세요. 사소한 디테일이 해석의 중요한 열쇠가 될 수 있습니다.</p>
                    </div>
                    <div className="feature-item" ref={item2Ref}>
                        <img src="https://placehold.co/800x600/1a1a1c/a1a1a6?text=AI+해석과+타로카드" alt="AI와 타로카드 해석 예시" className="feature-image" />
                        <h3 className="feature-title">2. AI와 타로의 만남</h3>
                        <p className="feature-description">강력한 AI가 꿈의 상징을 분석하고, 당신이 직접 선택한 타로카드가 직관적인 통찰을 더합니다.</p>
                    </div>
                    <div className="feature-item" ref={item3Ref}>
                        <img src="https://placehold.co/800x600/1a1a1c/a1a1a6?text=ASMR+사운드+믹서" alt="ASMR 사운드 믹서 예시" className="feature-image" />
                        <h3 className="feature-title">3. 나만의 ASMR 제작</h3>
                        <p className="feature-description">빗소리, 모닥불 소리, 우주 소리 등 다양한 사운드를 믹스하여 꿈꾸기 좋은 환경을 만들어보세요.</p>
                    </div>
                </div>
            </div>
        </section>
    );
};

const PopularPostsSection = ({ sectionRef }: { sectionRef: React.RefObject<HTMLElement> }) => {
    const [posts, setPosts] = useState<Post[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    useEffect(() => {
        const fetchPopularPosts = async () => {
            try {
                const url = 'http://localhost:8080/api/posts/popular?period=all_time';
                const response = await fetch(url);
                if (!response.ok) throw new Error('인기 게시글을 불러오는 데 실패했습니다.');
                const data: Post[] = await response.json();
                setPosts(data.slice(0, 3));
            } catch (e: any) { setError(e.message); } 
            finally { setLoading(false); }
        };
        fetchPopularPosts();
    }, []);
    return (
        <div id="popular-dreams" className="popular-posts-container" ref={sectionRef}>
            <div className="section-header">
                <h2 className="section-title-small">최근 주목받은 꿈들</h2>
                <p className="section-description">다른 사람들은 어떤 꿈을 꾸고 어떤 해석을 받았을까요?</p>
            </div>
            {loading && <p className="loading-text">인기 게시글을 불러오는 중...</p>}
            {error && <p className="error-text">{error}</p>}
            {!loading && !error && (
                <div className="popular-posts-grid">
                    {posts.map((post) => (
                        <div key={post.id} className="post-card">
                            <h4 className="post-card-title">{post.title}</h4>
                            <p className="post-card-author">by {post.authorNickname}</p>
                            <p className="post-card-summary">{getPlainTextSummary(post.content)}</p>
                            <div className="post-card-meta">
                                <span className="post-card-tag blue">#길몽</span>
                                <span className="post-card-tag purple">#성공</span>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

const MarketplaceSection = () => {
    const titleRef = useFadeInOnScroll();
    const item1Ref = useFadeInOnScroll();
    const item2Ref = useFadeInOnScroll();
    return (
        <section id="marketplace" className="marketplace-section">
            <div className="container">
                <div className="section-header" ref={titleRef}>
                    <h2 className="section-title">꿈의 거래소</h2>
                    <p className="section-description">상대방의 꿈을 사서 새로운 운명을 만들어보세요! <br /> 길몽의 좋은 기운을 얻거나, 다른 이의 악몽을 해결해주며 특별한 경험을 할 수 있습니다.</p>
                </div>
                <div className="marketplace-item" ref={item1Ref}>
                    <div className="marketplace-text">
                        <h3 className="marketplace-title">길몽을 구매하여<br />행운을 내 것으로</h3>
                        <p className="marketplace-description">성공, 재물, 합격의 기운이 담긴 강력한 길몽을 구매하고 그 에너지를 직접 느껴보세요. 중요한 일을 앞두고 있다면 최고의 선택이 될 것입니다.</p>
                        <a href="#" className="marketplace-link">길몽 마켓 둘러보기 &rarr;</a>
                    </div>
                    <div className="marketplace-image-container">
                        <img src="https://placehold.co/1200x900/000000/a1a1a6?text=길몽+컬렉션" alt="길몽 컬렉션 이미지" className="marketplace-image" />
                    </div>
                </div>
                <div className="marketplace-item reverse" ref={item2Ref}>
                    <div className="marketplace-text">
                        <h3 className="marketplace-title">창의적 영감을 주는<br />예지몽과 아이디어</h3>
                        <p className="marketplace-description">예술가, 작가, 개발자를 위한 특별한 꿈 마켓. 다른 사람의 꿈에서 새로운 아이디어를 발견하고 당신의 창작 활동에 날개를 다세요.</p>
                        <a href="#" className="marketplace-link">영감 마켓 둘러보기 &rarr;</a>
                    </div>
                    <div className="marketplace-image-container">
                        <img src="https://placehold.co/1200x900/000000/a1a1a6?text=영감을+주는+꿈" alt="영감을 주는 꿈 이미지" className="marketplace-image" />
                    </div>
                </div>
            </div>
        </section>
    );
};

const ThemeToggleButton = () => {
    const { isDarkMode, toggleDarkMode } = useTheme();
    return (
        <button onClick={toggleDarkMode} className="theme-toggle-button" aria-label="Toggle theme">
            {isDarkMode ? (
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>
            ) : (
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path></svg>
            )}
        </button>
    );
};


/**
 * 메인 랜딩 페이지 컴포넌트
 */
const LandingPage = () => {
    const popularPostsRef = useRef<HTMLElement>(null);
    const handleScrollToPopular = () => {
        popularPostsRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    };

    return (
        <div className="landing-page">
            <HeroSection onScrollToPopular={handleScrollToPopular} />
            <FeaturesSection />
            <div className="features-section" style={{ paddingTop: 0, paddingBottom: 0 }}>
                <div className="container">
                    <PopularPostsSection sectionRef={popularPostsRef} />
                </div>
            </div>
            <MarketplaceSection />
            {/* <ThemeToggleButton /> */}
            <ScrollToTopButton /> {/* ScrollToTopButton 렌더링 */}
        </div>
    );
};

export default LandingPage;
