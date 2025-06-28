import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useTheme } from "../contexts/ThemeContext";
import defaultAvatar from '../../public/assets/default-avatar.png';
import '../../public/css/MyPage.css'

// 백엔드 서버의 기본 URL을 정의합니다.
// 개발 환경에서는 localhost:8080을 사용하고, 배포 시에는 실제 백엔드 도메인으로 변경해야 합니다.
const BACKEND_BASE_URL = "http://localhost:8080";

// JWT 토큰에서 사용자 ID를 디코딩하는 헬퍼 함수
const getUserIdFromToken = (): number | null => {
  const token = localStorage.getItem('jwtToken');
  if (!token) {
    return null;
  }
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(function (c) {
          return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        })
        .join('')
    );
    const decodedToken = JSON.parse(jsonPayload);
    const userIdRaw = decodedToken.userId || decodedToken.id || decodedToken.sub || decodedToken.jti;
    const userId = typeof userIdRaw === 'number' ? userIdRaw : parseInt(userIdRaw as string, 10);
    return isNaN(userId) ? null : userId;
  } catch (e) {
    console.error('JWT 토큰 디코딩 중 오류 발생:', e);
    return null;
  }
};


// 마이페이지 컴포넌트
function MyPage() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('dashboard');
  const { isDarkMode } = useTheme();

  const [currentUserData, setCurrentUserData] = useState({
    profileImage: defaultAvatar,
    name: '로딩 중...',
    email: '',
    age: 0,
    gender: '',
    createdAt: '',
    userId: '',
    lastSleepAnalysis: '',
    averageSleep: '',
    deepSleepQuality: '',
    dreamFrequency: '',
    sleepDisturbances: '',
    dreamJournalEntries: [],
  });

  const [editedNickname, setEditedNickname] = useState('');
  const [editedEmail, setEditedEmail] = useState('');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmNewPassword, setConfirmNewPassword] = useState('');
  const [profileImageFile, setProfileImageFile] = useState<File | null>(null);
  const [removeProfileImage, setRemoveProfileImage] = useState(false);
  const [profileImageUrlPreview, setProfileImageUrlPreview] = useState<string | null>(null);

  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);


  // 사용자 프로필 데이터를 백엔드에서 불러오는 함수
  const fetchUserProfile = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    const token = localStorage.getItem('jwtToken');
    const userId = getUserIdFromToken();

    if (!token || !userId) {
      navigate('/login');
      setIsLoading(false);
      return;
    }

    try {
      const response = await fetch(`${BACKEND_BASE_URL}/api/users/profile`, { // 백엔드 URL 사용
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (response.ok) {
        const data = await response.json();
        // 이미지 URL이 상대 경로로 오는 경우, 백엔드 BASE_URL을 붙여 완전한 URL로 만듭니다.
        const fullProfileImageUrl = data.profileImageUrl
          ? `${BACKEND_BASE_URL}${data.profileImageUrl}` // 상대 경로에 백엔드 URL 추가
          : defaultAvatar;

        setCurrentUserData({
          profileImage: fullProfileImageUrl,
          name: data.nickname || 'Unknown',
          email: data.email || '',
          age: data.age || 0,
          gender: data.gender || '',
          createdAt: data.createdAt || '',
          userId: data.id ? String(data.id) : '',
          lastSleepAnalysis: 'N/A',
          averageSleep: 'N/A',
          deepSleepQuality: 'N/A',
          dreamFrequency: 'N/A',
          sleepDisturbances: 'N/A',
          dreamJournalEntries: [],
        });
        setEditedNickname(data.nickname || '');
        setEditedEmail(data.email || '');
        setProfileImageUrlPreview(fullProfileImageUrl); // 미리보기 이미지 설정도 전체 URL로
      } else {
        const errorData = await response.json();
        setErrorMessage(errorData.message || '프로필 정보를 불러오는데 실패했습니다.');
        console.error('Failed to fetch user profile:', response.status, response.statusText, errorData);
        navigate('/login');
      }
    } catch (error) {
      setErrorMessage('네트워크 오류: 프로필 정보를 불러올 수 없습니다.');
      console.error('Error fetching user profile:', error);
      navigate('/login');
    } finally {
      setIsLoading(false);
    }
  }, [navigate]);

  const handleTabChange = (tab: string) => {
    setActiveTab(tab);
    setErrorMessage(null);
    setSuccessMessage(null);
    if (tab === 'editProfile') {
      fetchUserProfile();
    }
  };

  useEffect(() => {
    fetchUserProfile();
  }, [fetchUserProfile]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setProfileImageFile(file);
      setRemoveProfileImage(false);
      setProfileImageUrlPreview(URL.createObjectURL(file)); // 로컬 파일 미리보기는 createObjectURL 사용
    } else {
      setProfileImageFile(null);
      if (!removeProfileImage) {
        setProfileImageUrlPreview(currentUserData.profileImage); // 파일 선택 해제 시 현재 프로필 이미지로 복원 (전체 URL)
      }
    }
  };

  const handleRemoveImage = () => {
    setProfileImageFile(null);
    setRemoveProfileImage(true);
    setProfileImageUrlPreview(defaultAvatar);
    const fileInput = document.getElementById('profile-image-upload') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  };

  const handleProfileUpdateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    const token = localStorage.getItem('jwtToken');
    if (!token) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      setIsLoading(false);
      return;
    }

    const formData = new FormData();
    const requestDto: { nickname?: string, email?: string, isRemoveProfileImage?: boolean } = {};

    if (editedNickname !== currentUserData.name) {
      requestDto.nickname = editedNickname;
    }
    if (editedEmail !== currentUserData.email) {
      requestDto.email = editedEmail;
    }

    if (currentPassword || newPassword || confirmNewPassword) {
      if (!(currentPassword && newPassword && confirmNewPassword)) {
        setErrorMessage('비밀번호를 변경하려면 현재 비밀번호, 새 비밀번호, 확인 필드를 모두 채워야 합니다.');
        setIsLoading(false);
        return;
      }
      if (newPassword !== confirmNewPassword) {
        setErrorMessage('새 비밀번호와 비밀번호 확인이 일치하지 않습니다.');
        setIsLoading(false);
        return;
      }
      if (newPassword.length < 8) {
        setErrorMessage('새 비밀번호는 최소 8자 이상이어야 합니다.');
        setIsLoading(false);
        return;
      }
      alert('비밀번호 변경은 별도의 기능으로 구현될 예정입니다. 현재 프로필 업데이트에서는 닉네임, 이메일, 프로필 이미지만 변경됩니다.');
    }

    if (removeProfileImage) {
      requestDto.isRemoveProfileImage = true;
    }

    if (Object.keys(requestDto).length === 0 && !profileImageFile && !removeProfileImage) {
        setErrorMessage('변경된 정보가 없습니다.');
        setIsLoading(false);
        return;
    }

    formData.append('request', new Blob([JSON.stringify(requestDto)], { type: 'application/json' }));

    if (profileImageFile) {
      formData.append('profileImageFile', profileImageFile);
    }

    try {
      const response = await fetch(`${BACKEND_BASE_URL}/api/users/profile`, { // 백엔드 URL 사용
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
        body: formData,
      });

      if (response.ok) {
        const updatedData = await response.json();
        setSuccessMessage('프로필이 성공적으로 업데이트되었습니다.');
        // 업데이트된 이미지 URL도 백엔드 URL을 붙여서 사용
        const updatedFullProfileImageUrl = updatedData.profileImageUrl
            ? `${BACKEND_BASE_URL}${updatedData.profileImageUrl}`
            : defaultAvatar;

        setCurrentUserData(prev => ({
          ...prev,
          name: updatedData.nickname || prev.name,
          email: updatedData.email || prev.email,
          profileImage: updatedFullProfileImageUrl,
        }));
        setProfileImageUrlPreview(updatedFullProfileImageUrl);

        setCurrentPassword('');
        setNewPassword('');
        setConfirmNewPassword('');
      } else {
        const errorData = await response.json();
        setErrorMessage(errorData.message || '프로필 업데이트에 실패했습니다.');
        console.error('Profile update failed:', response.status, errorData);
      }
    } catch (error) {
      setErrorMessage('네트워크 오류: 프로필을 업데이트할 수 없습니다.');
      console.error('Error updating profile:', error);
    } finally {
      setIsLoading(false);
      setProfileImageFile(null);
      setRemoveProfileImage(false);
      const fileInput = document.getElementById('profile-image-upload') as HTMLInputElement;
      if (fileInput) {
        fileInput.value = '';
      }
    }
  };

  const handleDeleteAccount = async () => {
    if (!window.confirm('정말로 계정을 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
      return;
    }

    setIsLoading(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    const token = localStorage.getItem('jwtToken');
    if (!token) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      setIsLoading(false);
      return;
    }

    try {
      const response = await fetch(`${BACKEND_BASE_URL}/api/users`, { // 백엔드 URL 사용
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (response.status === 204) {
        localStorage.removeItem('jwtToken');
        alert('계정이 성공적으로 탈퇴되었습니다.');
        navigate('/signup');
      } else {
        const errorData = await response.json();
        setErrorMessage(errorData.message || '계정 탈퇴에 실패했습니다.');
        console.error('Account deletion failed:', response.status, errorData);
      }
    } catch (error) {
      setErrorMessage('네트워크 오류: 계정을 탈퇴할 수 없습니다.');
      console.error('Error deleting account:', error);
    } finally {
      setIsLoading(false);
    }
  };


  const MyPageDashboard = () => (
    <div className={`mypage-dashboard ${isDarkMode ? 'dark-mode' : 'light-mode'}`}>
      <div className="dashboard-header">
        <h1>DreamTrack</h1>
        <div className="user-zone">User Zone</div>
      </div>

      <div className="dashboard-content">
        <div className="profile-section card">
          <img src={currentUserData.profileImage} alt="프로필" className="profile-avatar" />
          <h2>{currentUserData.name}</h2>
          <p>{currentUserData.age} years, {currentUserData.gender}</p>
          <div className="user-info">
            <p><strong>가입일:</strong> {currentUserData.createdAt}</p>
            <p><strong>User ID:</strong> {currentUserData.userId}</p>
          </div>
          <div className="sleep-patterns">
            <h3>수면 패턴</h3>
            <button className="dashboard-button">수면 기록</button>
            <button className="dashboard-button">수면 일지</button>
            <button className="dashboard-button">휴식 음악</button>
          </div>
          <div className="sleep-analysis">
            <h3>최근 수면 분석</h3>
            <p>{currentUserData.lastSleepAnalysis}</p>
            <p>수면 습관이 지난 한 달간 향상되었습니다. 더 나은 통찰력을 위해 추적을 계속하세요.</p>
          </div>
        </div>

        <div className="main-content-grid">
          <div className="upcoming-features card">
            <h3>예정된 기능</h3>
            <ul>
              <li><i className="fas fa-music"></i> 편안한 음악 <span>매일</span></li>
              <li><i className="fas fa-lightbulb"></i> 수면 팁 <span>매주</span></li>
            </ul>
          </div>

          <div className="recent-sleep-logs card">
            <h3>최근 수면 기록</h3>
            <ul>
              <li><i className="fas fa-bed"></i> 수면 트래커 <span>9월 15일</span></li>
            </ul>
            <button className="dashboard-button add-entry-button">수면 기록 추가</button>
          </div>

          <div className="calendar-section card">
            <h3>&lt; 2023년 8월 &gt;</h3>
            <div className="calendar-grid">
              {['M', 'T', 'W', 'T', 'F', 'S', 'S'].map((day, index) => (
                <span key={index} className="calendar-day-header">{day}</span>
              ))}
              {[...Array(35)].map((_, i) => (
                <span key={i} className={`calendar-date ${i % 5 === 0 ? 'highlight' : ''}`}>
                  {i < 2 ? '' : i - 1}
                </span>
              ))}
            </div>
          </div>

          <div className="find-resources card">
            <h3>자료 찾기</h3>
            <div className="resource-buttons">
              <button className="dashboard-button">수면 건강</button>
              <button className="dashboard-button">명상</button>
              <button className="dashboard-button">수면</button>
              <button className="dashboard-button">수면</button>
              <button className="dashboard-button">수면</button>
              <button className="dashboard-button">꿈</button>
            </div>
          </div>

          <div className="your-dream-journal card">
            <h3>나의 꿈 일지</h3>
            <div className="dream-entries">
              {currentUserData.dreamJournalEntries.map(dream => (
                <div key={dream.id} className="dream-item">
                  <p>{dream.title}</p>
                </div>
              ))}
            </div>
          </div>

          <div className="your-sleep-insights card">
            <h3>나의 수면 통찰</h3>
            <ul>
              <li>평균 수면: {currentUserData.averageSleep} <span>지난주</span> <i className="fas fa-chevron-right"></i></li>
              <li>깊은 수면의 질: {currentUserData.deepSleepQuality} <i className="fas fa-chevron-right"></i></li>
              <li>꿈 빈도: {currentUserData.dreamFrequency} <span>지난달</span> <i className="fas fa-chevron-right"></i></li>
              <li>수면 방해: {currentUserData.sleepDisturbances} <span>작년</span> <i className="fas fa-chevron-right"></i></li>
            </ul>
          </div>

          <div className="experts-section card">
            <h3>전문가</h3>
            <ul>
              <li>
                <img src="https://placehold.co/40x40/FF5733/FFFFFF?text=LD" alt="Lung Dreamer" className="expert-avatar" />
                <div className="expert-info">
                  <h4>Lung Dreamer</h4>
                  <p>수면 추적</p>
                </div>
              </li>
              <li>
                <img src="https://placehold.co/40x40/33FF57/FFFFFF?text=EN" alt="Evelyn Night" className="expert-avatar" />
                <div className="expert-info">
                  <h4>Evelyn Night</h4>
                  <p>수면 전문가</p>
                </div>
              </li>
              <li>
                <img src="https://placehold.co/40x40/3357FF/FFFFFF?text=JS" alt="James Slumber" className="expert-avatar" />
                <div className="expert-info">
                  <h4>James Slumber</h4>
                  <p>꿈 분석가</p>
                </div>
              </li>
              <li>
                <img src="https://placehold.co/40x40/FF33E9/FFFFFF?text=CS" alt="Clara Snooze" className="expert-avatar" />
                <div className="expert-info">
                  <h4>Clara Snooze</h4>
                  <p>수면 코치</p>
                </div>
              </li>
              <li>
                <img src="https://placehold.co/40x40/33FFE9/FFFFFF?text=OR" alt="Oliver Rest" className="expert-avatar" />
                <div className="expert-info">
                  <h4>Oliver Rest</h4>
                  <p>휴식 전문가</p>
                </div>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );

  const EditProfilePage = () => (
    <div className={`mypage-subpage ${isDarkMode ? 'dark-mode' : 'light-mode'}`}>
      <h2>개인 정보 수정</h2>
      {isLoading && <p>로딩 중...</p>}
      {errorMessage && <div className="error-message">{errorMessage}</div>}
      {successMessage && <div className="success-message">{successMessage}</div>}
      <form className="profile-edit-form" onSubmit={handleProfileUpdateSubmit}>
        <div className="form-group profile-image-preview-group">
            <label htmlFor="profile-image-upload">프로필 이미지</label>
            <div className="profile-image-wrapper">
                <img src={profileImageUrlPreview || defaultAvatar} alt="프로필 미리보기" className="profile-preview-avatar" />
                <input
                    type="file"
                    id="profile-image-upload"
                    accept="image/*"
                    onChange={handleFileChange}
                    className="file-input"
                />
                <button type="button" className="secondary-button" onClick={handleRemoveImage}>
                    이미지 제거
                </button>
            </div>
        </div>
        <div className="form-group">
          <label htmlFor="nickname">닉네임</label>
          <input
            type="text"
            id="nickname"
            value={editedNickname}
            onChange={(e) => setEditedNickname(e.target.value)}
          />
        </div>
        <div className="form-group">
          <label htmlFor="email">이메일</label>
          <input
            type="email"
            id="email"
            value={editedEmail}
            onChange={(e) => setEditedEmail(e.target.value)}
          />
        </div>
        <div className="form-group">
          <label htmlFor="current-password">현재 비밀번호</label>
          <input
            type="password"
            id="current-password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            placeholder="비밀번호 변경 시에만 입력"
          />
        </div>
        <div className="form-group">
          <label htmlFor="new-password">새 비밀번호</label>
          <input
            type="password"
            id="new-password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            placeholder="비밀번호 변경 시에만 입력"
          />
        </div>
        <div className="form-group">
          <label htmlFor="confirm-password">새 비밀번호 확인</label>
          <input
            type="password"
            id="confirm-password"
            value={confirmNewPassword}
            onChange={(e) => setConfirmNewPassword(e.target.value)}
            placeholder="새 비밀번호 다시 입력"
          />
        </div>
        <button type="submit" className="primary-button" disabled={isLoading}>
          {isLoading ? '저장 중...' : '정보 저장'}
        </button>
        <button type="button" className="danger-button" onClick={handleDeleteAccount} disabled={isLoading}>
          {isLoading ? '탈퇴 중...' : '회원 탈퇴'}
        </button>
      </form>
    </div>
  );

  const LikedPostsPage = () => (
    <div className={`mypage-subpage ${isDarkMode ? 'dark-mode' : 'light-mode'}`}>
      <h2>좋아요한 게시글</h2>
      <p>여기에 좋아요한 게시글 목록이 표시됩니다.</p>
    </div>
  );

  const BookmarkedPostsPage = () => (
    <div className={`mypage-subpage ${isDarkMode ? 'dark-mode' : 'light-mode'}`}>
      <h2>북마크한 게시글</h2>
      <p>여기에 북마크한 게시글 목록이 표시됩니다.</p>
    </div>
  );

  const PurchasedPostsPage = () => (
    <div className={`mypage-subpage ${isDarkMode ? 'dark-mode' : 'light-mode'}`}>
      <h2>내가 산 게시글</h2>
      <p>여기에 구매한 게시글 목록이 표시됩니다.</p>
    </div>
  );

  const MyPostsPage = () => (
    <div className={`mypage-subpage ${isDarkMode ? 'dark-mode' : 'light-mode'}`}>
      <h2>내가 쓴 게시글</h2>
      <p>여기에 내가 작성한 게시글 목록이 표시됩니다.</p>
    </div>
  );

  return (
    <div className={`mypage-container ${isDarkMode ? 'dark' : 'light'}`}>
      <div className="mypage-sidebar">
        <button onClick={() => handleTabChange('dashboard')} className={activeTab === 'dashboard' ? 'active' : ''}>대시보드</button>
        <button onClick={() => handleTabChange('editProfile')} className={activeTab === 'editProfile' ? 'active' : ''}>개인 정보 수정</button>
        <button onClick={() => handleTabChange('likedPosts')} className={activeTab === 'likedPosts' ? 'active' : ''}>좋아요한 게시글</button>
        <button onClick={() => handleTabChange('bookmarkedPosts')} className={activeTab === 'bookmarkedPosts' ? 'active' : ''}>북마크한 게시글</button>
        <button onClick={() => handleTabChange('purchasedPosts')} className={activeTab === 'purchasedPosts' ? 'active' : ''}>내가 산 게시글</button>
        <button onClick={() => handleTabChange('myPosts')} className={activeTab === 'myPosts' ? 'active' : ''}>내가 쓴 게시글</button>
      </div>
      <div className="mypage-content">
        {activeTab === 'dashboard' && <MyPageDashboard />}
        {activeTab === 'editProfile' && <EditProfilePage />}
        {activeTab === 'likedPosts' && <LikedPostsPage />}
        {activeTab === 'bookmarkedPosts' && <BookmarkedPostsPage />}
        {activeTab === 'purchasedPosts' && <PurchasedPostsPage />}
        {activeTab === 'myPosts' && <MyPostsPage />}
      </div>
    </div>
  );
}

export default MyPage;
