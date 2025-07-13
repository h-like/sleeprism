// UserContext.tsx
import { createContext, useContext, useEffect, useState } from "react";

// --- 데이터 타입 정의 ---
interface UserProfile {
  profileImageUrl: string;
  nickname: string;
  email: string;
  age: number;
  gender: string;
  createdAt: string;
  userId: string;
  postCount: number;
  likedPostCount: number;
  bookmarkedPostCount: number;
}

// --- Mock 데이터 및 API 함수 ---
const MOCK_USER_PROFILE: UserProfile = {
  profileImageUrl: 'https://placehold.co/128x128/8B5CF6/FFFFFF?text=ME',
  nickname: '드림캐처',
  email: 'dream@example.com',
  age: 28,
  gender: '비공개',
  createdAt: '2023-05-10',
  userId: 'user12345',
  postCount: 12,
  likedPostCount: 42,
  bookmarkedPostCount: 8,
};

const fetchUserProfile = async (): Promise<UserProfile> => {
  console.log("Fetching user profile...");
  return new Promise(resolve => setTimeout(() => resolve(MOCK_USER_PROFILE), 1000));
};

export const updateUserProfile = async (formData: FormData): Promise<Partial<UserProfile>> => {
    console.log("Updating user profile with:", formData);
    const updatedData: Partial<UserProfile> = {};
    const requestData = JSON.parse(formData.get('request') as string);
    
    if (requestData.nickname) {
        updatedData.nickname = requestData.nickname;
    }
    if (formData.has('profileImageFile')) {
        const file = formData.get('profileImageFile') as File;
        updatedData.profileImageUrl = URL.createObjectURL(file);
    } else if (requestData.isRemoveProfileImage) {
        updatedData.profileImageUrl = 'https://placehold.co/128x128/E5E7EB/4B5563?text=:)';
    }

    return new Promise(resolve => setTimeout(() => resolve(updatedData), 1500));
};

// --- 사용자 데이터 컨텍스트 (Context API) ---
interface UserContextType {
  user: UserProfile | null;
  isLoading: boolean;
  error: string | null;
  refetch: () => void;
  updateUser: (updatedData: Partial<UserProfile>) => void;
}
const UserContext = createContext<UserContextType | null>(null);

export const useUser = () => {
  const context = useContext(UserContext);
  if (!context) throw new Error('useUser must be used within a UserProvider');
  return context;
};

export const UserProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadUser = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const userData = await fetchUserProfile();
      setUser(userData);
    } catch (err) {
      setError("프로필 정보를 불러오는 데 실패했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => { loadUser(); }, []);
  
  const updateUser = (updatedData: Partial<UserProfile>) => {
    setUser(prevUser => prevUser ? { ...prevUser, ...updatedData } : null);
  };

  const value = { user, isLoading, error, refetch: loadUser, updateUser };
  return <UserContext.Provider value={value}>{children}</UserContext.Provider>;
};