// src/pages/PostListPage.tsx
import React, { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import '../../public/css/PostListPage.css'

interface Post {
  id: number
  title: string
  content: string
  category: string
  viewCount: number
  isDeleted: boolean
  authorNickname: string
  createdAt: string
  updatedAt: string
  likeCount: number
  commentCount: number
}

const extractAndConvertFirstImageUrl = (htmlContent: string): string | null => {
  if (!htmlContent) return null

  try {
    const parser = new DOMParser()
    const doc = parser.parseFromString(htmlContent, 'text/html')
    const imgElement = doc.querySelector('img')

    if (imgElement) {
      let src = imgElement.getAttribute('src')
      if (src) {
        const BACKEND_BASE_URL = 'http://localhost:8080'
        const FILE_API_PATH_PREFIX = '/api/posts/files/'
        if (src.startsWith(BACKEND_BASE_URL)) {
          return src
        } else if (src.startsWith(FILE_API_PATH_PREFIX)) {
          return `${BACKEND_BASE_URL}${src}`
        } else if (src.startsWith('/')) {
          return `${BACKEND_BASE_URL}${src}`
        }
      }
    }
    return null
  } catch (error) {
    console.error('Error parsing HTML content:', error)
    return null
  }
}

const getPlainTextSummary = (htmlContent: string, maxLength: number = 150): string => {
  if (!htmlContent) return ''
  const parser = new DOMParser()
  const doc = parser.parseFromString(htmlContent, 'text/html')
  const textContent = doc.body.textContent || ''
  if (textContent.length > maxLength) {
    const trimmedText = textContent.substring(0, maxLength)
    return (
      trimmedText.substring(0, Math.min(trimmedText.length, trimmedText.lastIndexOf(' '))) + '...'
    )
  }
  return textContent
}

function PostListPage() {
  const [posts, setPosts] = useState<Post[]>([])
  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)
  const [activeTab, setActiveTab] = useState<'latest' | 'popular'>('latest')
  const navigate = useNavigate()

  const latestTabRef = useRef<HTMLButtonElement>(null)
  const popularTabRef = useRef<HTMLButtonElement>(null)
  const tabIndicatorRef = useRef<HTMLDivElement>(null)

  const updateTabIndicator = (tab: 'latest' | 'popular') => {
    // 탭을 감싸는 부모 요소의 위치를 기준으로 계산
    const parent = latestTabRef.current?.parentElement
    const activeRef = tab === 'latest' ? latestTabRef.current : popularTabRef.current
    const indicator = tabIndicatorRef.current

    if (activeRef && indicator && parent) {
      indicator.style.width = `${activeRef.offsetWidth}px`
      // 부모 요소를 기준으로 상대적인 위치 계산
      indicator.style.transform = `translateX(${activeRef.offsetLeft - parent.offsetLeft}px)`
    }
  }

  useEffect(() => {
    const fetchPosts = async () => {
      try {
        const response = await fetch('http://localhost:8080/api/posts')

        if (!response.ok) {
          const errorText = await response.text()
          console.error(`HTTP error! Status: ${response.status}, Response: ${errorText}`)
          throw new Error(`게시글을 불러오는데 실패했습니다: ${response.status} ${response.statusText}`)
        }

        const data: Post[] = await response.json()
        setPosts(data)
      } catch (e: any) {
        console.error('게시글 목록을 가져오는 중 오류 발생:', e)
        setError(e.message || '알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.')
      } finally {
        setLoading(false)
      }
    }

    fetchPosts()
  }, [])

  useEffect(() => {
    updateTabIndicator(activeTab)
    window.addEventListener('resize', () => updateTabIndicator(activeTab))
    return () => window.removeEventListener('resize', () => updateTabIndicator(activeTab))
  }, [activeTab])

  const handleCreateNewPost = () => {
    navigate('/posts/new')
  }

  const handleTabClick = (tab: 'latest' | 'popular') => {
    setActiveTab(tab)
    console.log(`${tab} 탭이 선택되었습니다.`)
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-100 p-4 font-inter">
        <p className="text-xl text-gray-700">게시글을 불러오는 중...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-red-100 p-4 rounded-lg shadow-md font-inter">
        <p className="text-xl text-red-700">{error}</p>
      </div>
    )
  }

  return (
    <div className="main-container">
      <div className="content-wrapper">
        {/* 콘텐츠와 사이드바를 위한 Flex 컨테이너 */}
        <div className="flex flex-col lg:flex-row lg:space-x-8">
          {/* 메인 콘텐츠 영역 (게시글 목록) */}
          <main className="lg:w-2/3 w-full">
            {/* 탭 네비게이션: 첫 번째 줄에 위치 */}
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

            {/* 글쓰기 버튼: 왼쪽에 배치 */}
                <button
                    onClick={handleCreateNewPost}
                    className="create-post-button mr-4" // 오른쪽 마진 추가
                    // style={{display:'flex', justifyContent: 'flex-end', padding: '16px'}}
                >
                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-pencil-line"><path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/><path d="m19 14 3 3"/><path d="m17 12 3 3"/></svg>
                    <span>글쓰기</span>
                </button>
             

            {/* 게시글 목록 */}
            {posts.length === 0 ? (
              <p className="text-center text-gray-600 text-lg py-10">아직 작성된 게시글이 없습니다.</p>
            ) : (
              <ul className="post-list">
                {posts.map((post) => {
                  const thumbnailUrl = extractAndConvertFirstImageUrl(post.content)
                  const summaryText = getPlainTextSummary(post.content)

                  return (
                    <li
                      key={post.id}
                      className="post-item"
                      onClick={() => navigate(`/posts/${post.id}`)}
                    >
                      {/* 썸네일 영역: 이미지가 있을 때만 표시 */}
                      <div className={`post-thumbnail ${!thumbnailUrl ? 'no-image' : ''}`}>
                        {thumbnailUrl && <img src={thumbnailUrl} alt="게시글 썸네일" />}
                      </div>
                      <div className="post-info">
                        <h2 className="post-title">{post.title}</h2>
                        <p className="post-summary">{summaryText || '내용이 없습니다.'}</p>
                        <div className="post-meta">
                          <span className="post-meta-item">
                            <svg
                              xmlns="http://www.w3.org/2000/svg"
                              width="16"
                              height="16"
                              viewBox="0 0 24 24"
                              fill="none"
                              stroke="currentColor"
                              strokeWidth="2"
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              className="lucide lucide-user"
                            >
                              <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
                              <circle cx="12" cy="7" r="4" />
                            </svg>
                            <span>{post.authorNickname}</span>
                          </span>
                          <span className="post-meta-item">
                            <svg
                              xmlns="http://www.w3.org/2000/svg"
                              width="16"
                              height="16"
                              viewBox="0 0 24 24"
                              fill="none"
                              stroke="currentColor"
                              strokeWidth="2"
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              className="lucide lucide-calendar"
                            >
                              <path d="M8 2v4" />
                              <path d="M16 2v4" />
                              <rect width="18" height="18" x="3" y="4" rx="2" />
                              <path d="M3 10h18" />
                            </svg>
                            <span>{new Date(post.createdAt).toLocaleDateString('ko-KR')}</span>
                          </span>
                          <span className="post-meta-item">
                            <svg
                              xmlns="http://www.w3.org/2000/svg"
                              width="16"
                              height="16"
                              viewBox="0 0 24 24"
                              fill="none"
                              stroke="currentColor"
                              strokeWidth="2"
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              className="lucide lucide-message-circle"
                            >
                              <path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z" />
                            </svg>
                            <span>{post.commentCount}</span>
                          </span>
                          <span className="post-meta-item">
                            <svg
                              xmlns="http://www.w3.org/2000/svg"
                              width="16"
                              height="16"
                              viewBox="0 0 24 24"
                              fill="none"
                              stroke="currentColor"
                              strokeWidth="2"
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              className="lucide lucide-thumbs-up"
                            >
                              <path d="M7 10v12h11c1.1 0 2-1 2-2V8.5a2 2 0 0 0-1-1.74l-4.24-2.12a2 2 0 0 0-1.72 0L8 5.76A2 2 0 0 0 7 7.5V10Z" />
                              <path d="M2 17h3a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2H2v12Z" />
                            </svg>
                            <span>{post.likeCount}</span>
                          </span>
                        </div>
                      </div>
                    </li>
                  )
                })}
              </ul>
            )}
          </main>

          {/* 사이드바 영역 */}
          <aside className="lg:w-1/3 w-full mt-8 lg:mt-0">
            <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
              <h3 className="text-xl font-bold text-gray-800 mb-4">바로 그 데이블입니다!</h3>
              <p className="text-gray-600 mb-4">데이블에 참여해보세요!</p>
              <button className="w-full py-3 bg-blue-500 text-white font-semibold rounded-md hover:bg-blue-600 transition duration-300">
                로그인하기
              </button>
            </div>
            <div className="mt-6 bg-white p-6 rounded-lg shadow-md border border-gray-200">
              <h3 className="text-xl font-bold text-gray-800 mb-4">테이블의 핵심 기능을 확인해 보세요</h3>
              <ul className="space-y-3 text-gray-700">
                <li className="flex items-center space-x-2">
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="20"
                    height="20"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    className="lucide lucide-check-circle text-green-500"
                  >
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                    <path d="m9 11 3 3L22 4" />
                  </svg>
                  <span>테이블 참여방법</span>
                </li>
                <li className="flex items-center space-x-2">
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="20"
                    height="20"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    className="lucide lucide-check-circle text-green-500"
                  >
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                    <path d="m9 11 3 3L22 4" />
                  </svg>
                  <span>테이블 프로필 만들고 관리하기</span>
                </li>
              </ul>
            </div>
          </aside>
        </div>
      </div>
    </div>
  )
}

export default PostListPage