// 백엔드 기본 URL 및 컨텍스트 경로
const BACKEND_BASE_URL = 'http://localhost:8080';
const API_BASE_URL = `${BACKEND_BASE_URL}/api/users`;

export interface UserProfile {
  id: number;
  email: string;
  username: string;
  nickname: string;
  profileImageUrl: string | null;
  role: string; // UserRole enum (e.g., "USER", "ADMIN")
  status: string; // UserStatus enum (e.g., "ACTIVE", "INACTIVE")
  socialProvider: string | null;
  socialId: string | null;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

// JWT 토큰 가져오는 헬퍼 함수 (PostDetailPage와 동일)
const getAuthHeaders = () => {
  const token = localStorage.getItem('jwtToken');
  return token ? { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' } : { 'Content-Type': 'application/json' };
};

/**
 * 모든 활성 사용자 목록을 조회합니다. (그룹 채팅방 초대용)
 * @returns UserProfile[]
 */
export const fetchAllUsers = async (): Promise<UserProfile[]> => {
  const response = await fetch(`${API_BASE_URL}/all`, { // 모든 사용자 조회 API (가정)
    method: 'GET',
    headers: getAuthHeaders(),
  });
  if (!response.ok) {
    const errorData = await response.json();
    throw new Error(errorData.message || '사용자 목록을 불러오는데 실패했습니다.');
  }
  return response.json();
};

// 필요한 경우 다른 사용자 관련 API 함수 추가 (예: getUserProfile, updateUserProfile 등)
