import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import '../../public/css/PostListPage.css';
import ScrollToTopButton from '../components/ScrollToTopButton';

interface Post {
  id: number;
  title: string;
  content: string;
  category: string;
  viewCount: number;
  isDeleted: boolean;
  authorNickname: string;
  createdAt: string;
  updatedAt: string;
  likeCount: number;
  commentCount: number;
}

// 카테고리 정의
const POST_CATEGORIES = [
  { name: 'DREAM_DIARY', label: '꿈 다이어리' },
  { name: 'SLEEP_INFO', label: '수면 정보' },
  { name: 'FREE_TALK', label: '자유 게시판' },
];

// 인기글 기간 정의
const POPULAR_PERIODS = [
  { name: 'all_time', label: '전체 기간' },
  { name: 'today', label: '오늘' },
  { name: 'week', label: '이번 주' },
  { name: 'month', label: '이번 달' },
];

const extractAndConvertFirstImageUrl = (htmlContent: string): string | null => {
  if (!htmlContent) return null;

  try {
    const parser = new DOMParser();
    const doc = parser.parseFromString(htmlContent, 'text/html');
    const imgElement = doc.querySelector('img');

    if (imgElement) {
      let src = imgElement.getAttribute('src');
      if (src) {
        const BACKEND_BASE_URL = 'http://localhost:8080';
        // FIX: src가 이미 backendBaseUrl로 시작하면 그대로 반환합니다.
        // HtmlSanitizer 수정으로 DB에 이미 절대 경로가 저장되기 때문입니다.
        if (src.startsWith(BACKEND_BASE_URL)) {
          return src;
        } 
        // 그 외의 경우 (상대 경로인 경우)에만 backendBaseUrl을 붙입니다.
        // (이 로직은 현재 상황에서는 거의 실행되지 않을 것입니다.)
        else if (src.startsWith('/')) { 
          return `${BACKEND_BASE_URL}${src}`;
        }
      }
    }
    return null;
  } catch (error) {
    console.error('HTML 콘텐츠 파싱 오류:', error);
    return null;
  }
};

const getPlainTextSummary = (htmlContent: string, maxLength: number = 150): string => {
  if (!htmlContent) return '';
  const parser = new DOMParser();
  const doc = parser.parseFromString(htmlContent, 'text/html');
  const textContent = doc.body.textContent || '';
  if (textContent.length > maxLength) {
    const trimmedText = textContent.substring(0, maxLength);
    return (
      trimmedText.substring(0, Math.min(trimmedText.length, trimmedText.lastIndexOf(' '))) + '...'
    );
  }
  return textContent;
};

function PostListPage() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  
  const [activeTab, setActiveTab] = useState<'latest' | 'popular'>(() => {
    const queryParams = new URLSearchParams(window.location.search);
    return (queryParams.get('tab') === 'popular' ? 'popular' : 'latest');
  });

  const [popularPeriod, setPopularPeriod] = useState<string>(() => {
    const queryParams = new URLSearchParams(window.location.search);
    const periodParam = queryParams.get('period');
    if (periodParam && POPULAR_PERIODS.some(p => p.name === periodParam)) {
      return periodParam;
    }
    return 'all_time';
  });

  const [selectedCategories, setSelectedCategories] = useState<Set<string>>(() => {
    const queryParams = new URLSearchParams(window.location.search);
    const categoryParam = queryParams.get('category');
    if (categoryParam) {
      return new Set(categoryParam.split(',').filter(c => POST_CATEGORIES.some(pc => pc.name === c)));
    }
    return new Set();
  });

  const navigate = useNavigate();
  const location = useLocation();

  const latestTabRef = useRef<HTMLButtonElement>(null);
  const popularTabRef = useRef<HTMLButtonElement>(null);
  const tabIndicatorRef = useRef<HTMLDivElement>(null);

  const updateTabIndicator = useCallback((tab: 'latest' | 'popular') => {
    const parent = latestTabRef.current?.parentElement;
    const activeRef = tab === 'latest' ? latestTabRef.current : popularTabRef.current;
    const indicator = tabIndicatorRef.current;

    if (activeRef && indicator && parent) {
      indicator.style.width = `${activeRef.offsetWidth}px`;
      indicator.style.transform = `translateX(${activeRef.offsetLeft - parent.offsetLeft}px)`;
    }
  }, []);

  const fetchPosts = useCallback(async (currentTab: 'latest' | 'popular', categories: Set<string>, period: string) => {
    setLoading(true);
    setError(null);
    try {
      let url = 'http://localhost:8080/api/posts';
      const queryParams = new URLSearchParams();

      if (currentTab === 'latest') {
        const categoryArray = Array.from(categories);
        if (categoryArray.length > 0) {
          queryParams.append('category', categoryArray.join(','));
        }
      } else { // 'popular' 탭
        url = 'http://localhost:8080/api/posts/popular';
        if (period !== 'all_time') {
          queryParams.append('period', period);
        }
      }

      const queryString = queryParams.toString();
      if (queryString) {
        url = `${url}?${queryString}`;
      }

      console.log('Fetching posts from URL:', url);

      const response = await fetch(url);

      if (!response.ok) {
        const errorText = await response.text();
        console.error(`HTTP 오류! 상태: ${response.status}, 응답: ${errorText}`);
        throw new Error(`게시글을 불러오는데 실패했습니다: ${response.status} ${response.statusText}`);
      }

      const data: Post[] = await response.json();
      setPosts(data);
    } catch (e: any) {
      console.error('게시글 목록을 가져오는 중 오류 발생:', e);
      setError(e.message || '알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const queryParams = new URLSearchParams(location.search);
    const categoryParam = queryParams.get('category');
    const periodParam = queryParams.get('period');
    const tabParam = queryParams.get('tab') === 'popular' ? 'popular' : 'latest';

    if (tabParam !== activeTab) {
        setActiveTab(tabParam);
    }
    
    const newCategories = new Set(categoryParam ? categoryParam.split(',').filter(c => POST_CATEGORIES.some(pc => pc.name === c)) : []);
    if (JSON.stringify(Array.from(newCategories).sort()) !== JSON.stringify(Array.from(selectedCategories).sort())) {
        setSelectedCategories(newCategories);
    }

    const newPeriod = (periodParam && POPULAR_PERIODS.some(p => p.name === periodParam)) ? periodParam : 'all_time';
    if (newPeriod !== popularPeriod) {
        setPopularPeriod(newPeriod);
    }

  }, [location.search]);

  useEffect(() => {
    fetchPosts(activeTab, selectedCategories, popularPeriod);
  }, [activeTab, selectedCategories, popularPeriod, fetchPosts]);

  useEffect(() => {
    updateTabIndicator(activeTab);
    window.addEventListener('resize', () => updateTabIndicator(activeTab));
    return () => window.removeEventListener('resize', () => updateTabIndicator(activeTab));
  }, [activeTab, updateTabIndicator]);

  const handleCreateNewPost = () => {
    navigate('/posts/new');
  };

  const handleTabClick = (tab: 'latest' | 'popular') => {
    if (tab === activeTab) {
        return;
    }
    setActiveTab(tab);

    const newQueryParams = new URLSearchParams();
    if (tab === 'popular') {
      newQueryParams.append('tab', 'popular');
      if (popularPeriod !== 'all_time') {
          newQueryParams.append('period', popularPeriod);
      }
    } else {
      if (selectedCategories.size > 0) {
        newQueryParams.append('category', Array.from(selectedCategories).join(','));
      }
      newQueryParams.append('tab', 'latest'); // 최신글 탭일 때 tab=latest 명시
    }
    
    navigate(`/posts?${newQueryParams.toString()}`);
    console.log(`${tab} 탭이 선택되었습니다.`);
  };

  const handleCategoryButtonClick = (categoryName: string) => {
    if (activeTab === 'popular') {
        return;
    }

    setSelectedCategories(prevSelectedCategories => {
      const newSelected = new Set(prevSelectedCategories);

      if (categoryName === 'ALL') {
        if (newSelected.size === 0) {
            return newSelected;
        }
        navigate('/posts?tab=latest'); // 'ALL' 선택 시 tab=latest 명시
        return new Set();
      }

      if (newSelected.has(categoryName)) {
        newSelected.delete(categoryName);
      } else {
        newSelected.add(categoryName);
      }

      const newQueryParams = new URLSearchParams();
      newQueryParams.append('tab', 'latest'); // 카테고리 선택은 항상 최신글 탭과 연결

      if (newSelected.size === 0) {
        navigate(`/posts?tab=latest`);
      } else {
        newQueryParams.append('category', Array.from(newSelected).join(','));
        navigate(`/posts?${newQueryParams.toString()}`); 
      }
      
      return newSelected;
    });
  };

  const handlePeriodButtonClick = (periodName: string) => {
    setPopularPeriod(periodName);
    const newQueryParams = new URLSearchParams();
    newQueryParams.append('tab', 'popular');
    if (periodName !== 'all_time') {
        newQueryParams.append('period', periodName);
    }
    navigate(`/posts?${newQueryParams.toString()}`);
  };


  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-100 p-4 font-inter">
        <p className="text-xl text-gray-700">게시글을 불러오는 중...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-red-100 p-4 rounded-lg shadow-md font-inter">
        <p className="text-xl text-red-700">{error}</p>
      </div>
    );
  }

  if (posts.length === 0 && !loading) { // 게시글이 없을 때 메시지 표시
    return (
      <div className="main-container">
        <div className="content-wrapper">
          <div className="flex flex-col lg:flex-row lg:space-x-8">
            <main className="lg:w-2/3 w-full">
              {/* 탭 네비게이션 및 카테고리/기간 버튼은 그대로 유지 */}
              <div className="tab-navigation mb-6">
                <button
                  ref={latestTabRef}
                  onClick={() => handleTabClick('latest')}
                  className={`tab-button ${activeTab === 'latest' ? 'active' : ''}`}
                >
                  최신글
                </button>
                <button
                  ref={popularTabRef}
                  onClick={() => handleTabClick('popular')}
                  className={`tab-button ${activeTab === 'popular' ? 'active' : ''}`}
                >
                  인기글
                </button>
              </div>

              {activeTab === 'latest' && (
                  <div className="category-buttons-container mb-6 p-4 bg-gray-50 rounded-lg shadow-inner">
                    <button
                      onClick={() => handleCategoryButtonClick('ALL')}
                      className={`px-5 py-2 rounded-full font-semibold transition-colors duration-200 shadow-md
                        ${selectedCategories.size === 0 ? 'all-active-btn' : ''}
                      `}
                    >
                      전체
                    </button>
                    {POST_CATEGORIES.map((category) => (
                      <button
                        key={category.name}
                        onClick={() => handleCategoryButtonClick(category.name)}
                        className={`px-5 py-2 rounded-full font-semibold transition-colors duration-200 shadow-md
                          ${selectedCategories.has(category.name) ? 'active-category' : ''}
                        `}
                      >
                        {category.label}
                      </button>
                    ))}
                  </div>
              )}

              {activeTab === 'popular' && (
                <div className="period-buttons-container flex flex-wrap justify-center gap-3 mb-6 p-4 bg-blue-50 rounded-lg shadow-inner">
                  {POPULAR_PERIODS.map((period) => (
                    <button
                      key={period.name}
                      onClick={() => handlePeriodButtonClick(period.name)}
                      className={`px-5 py-2 rounded-full font-semibold transition-colors duration-200 shadow-md
                        ${popularPeriod === period.name ? 'popular-period-active-btn' : ''}
                      `}
                    >
                      {period.label}
                    </button>
                  ))}
                </div>
              )}

              <div className="fixed bottom-8 right-8 z-50 floating-create-button-container">
                <button
                  onClick={handleCreateNewPost}
                  className="create-post-button"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-pencil-line"><path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/><path d="m19 14 3 3"/><path d="m17 12 3 3"/></svg>
                  <span>글쓰기</span>
                </button>
              </div>
              <p className="text-center text-gray-600 text-lg py-10 no-post">아직 작성된 게시글이 없습니다.</p>
            </main>
          </div>
        </div>
      </div>
    );
  }


  return (
    <div className="main-container">
      <ScrollToTopButton />
      <div className="content-wrapper">
        <div className="flex flex-col lg:flex-row lg:space-x-8">
          <main className="lg:w-2/3 w-full">
            {/* 탭 네비게이션: 최신글 / 인기글 */}
            <div className="tab-navigation mb-6">
              <button
                ref={latestTabRef}
                onClick={() => handleTabClick('latest')}
                className={`tab-button ${activeTab === 'latest' ? 'active' : ''}`}
              >
                최신글
              </button>
              <button
                ref={popularTabRef}
                onClick={() => handleTabClick('popular')}
                className={`tab-button ${activeTab === 'popular' ? 'active' : ''}`}
              >
                인기글
              </button>
              {/* 인디케이터 */}
              {/* <div ref={tabIndicatorRef} className="tab-indicator"></div> */}
            </div>

            {/* 카테고리 버튼 (최신글 탭일 때만 표시 및 활성화) */}
            {activeTab === 'latest' && (
                <div className="category-buttons-container mb-6 p-4 bg-gray-50 rounded-lg shadow-inner">
                  <button
                    onClick={() => handleCategoryButtonClick('ALL')}
                    className={`px-5 py-2 rounded-full font-semibold transition-colors duration-200 shadow-md
                      ${selectedCategories.size === 0 ? 'all-active-btn' : ''}
                    `}
                  >
                    전체
                  </button>
                  {POST_CATEGORIES.map((category) => (
                    <button
                      key={category.name}
                      onClick={() => handleCategoryButtonClick(category.name)}
                      className={`px-5 py-2 rounded-full font-semibold transition-colors duration-200 shadow-md
                        ${selectedCategories.has(category.name) ? 'active-category' : ''}
                      `}
                    >
                      {category.label}
                    </button>
                  ))}
                </div>
            )}

            {/* 인기글 기간 선택 버튼 (인기글 탭일 때만 표시) */}
            {activeTab === 'popular' && (
              <div className="period-buttons-container flex flex-wrap justify-center gap-3 mb-6 p-4 bg-blue-50 rounded-lg shadow-inner">
                {POPULAR_PERIODS.map((period) => (
                  <button
                    key={period.name}
                    onClick={() => handlePeriodButtonClick(period.name)}
                    className={`px-5 py-2 rounded-full font-semibold transition-colors duration-200 shadow-md
                      ${popularPeriod === period.name ? 'popular-period-active-btn' : ''}
                    `}
                  >
                    {period.label}
                  </button>
                ))}
              </div>
            )}

            {/* 글쓰기 버튼 */}
            <div className="fixed bottom-8 right-8 z-50 floating-create-button-container">
              <button
                onClick={handleCreateNewPost}
                className="create-post-button"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-pencil-line"><path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/><path d="m19 14 3 3"/><path d="m17 12 3 3"/></svg>
                <span>글쓰기</span>
              </button>
            </div>

            {/* 게시글 목록 */}
            <ul className="post-list">
              {posts.map((post) => {
                const thumbnailUrl = extractAndConvertFirstImageUrl(post.content);
                const summaryText = getPlainTextSummary(post.content);

                return (
                  <li
                    key={post.id}
                    className="post-item"
                    // 상세 페이지로 이동할 때 현재 PostListPage의 상태를 state로 전달
                    onClick={() => navigate(`/posts/${post.id}`, { 
                        state: { 
                            tab: activeTab, 
                            category: Array.from(selectedCategories), 
                            period: popularPeriod 
                        } 
                    })}
                  >
                    {/* FIX: 썸네일이 없을 경우 기본 이미지 표시 */}
                    <div className={`post-thumbnail ${!thumbnailUrl ? 'no-image' : ''}`}>
                      {thumbnailUrl ? (
                        <img src={thumbnailUrl} alt="게시글 썸네일" />
                      ) : (
                        <img src="https://placehold.co/150x100/E0E0E0/888888?text=No+Image" alt="No Image Placeholder" />
                      )}
                    </div>
                    <div className="post-info">
                      <h2 className="post-title">{post.title}</h2>
                      <p className="post-summary">{summaryText || '내용이 없습니다.'}</p>
                      <div className="post-meta">
                        <span className="post-meta-item">
                          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-user"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" /></svg>
                          <span>{post.authorNickname}</span>
                        </span>
                        <span className="post-meta-item">
                          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-calendar"><path d="M8 2v4" /><path d="M16 2v4" /><rect width="18" height="18" x="3" y="4" rx="2" /><path d="M3 10h18" /></svg>
                          <span>{new Date(post.createdAt).toLocaleDateString('ko-KR')}</span>
                        </span>
                        <span className="post-meta-item">
                          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-message-circle"><path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z" /></svg>
                          <span>{post.commentCount}</span>
                        </span>
                        <span className="post-meta-item">
                          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-heart-icon lucide-heart"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
                          <span>{post.likeCount}</span>
                        </span>
                      </div>
                    </div>
                  </li>
                );
              })}
            </ul>
          </main>

          {/* 사이드바 영역 */}
          
        </div>
      </div>
    </div>
  );
}

export default PostListPage;
