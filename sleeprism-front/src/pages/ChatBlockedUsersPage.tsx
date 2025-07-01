import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getBlockedUsers, unblockUser, type ChatBlockResponse } from '../service/ChatService';
// import { getBlockedUsers, unblockUser, ChatBlockResponse } from '../services/ChatService';
// import { getUserIdFromToken } from '../utils/authUtils'; // 사용자 ID 가져오는 헬퍼 함수

// 사용자 ID를 JWT 토큰에서 디코딩하는 헬퍼 함수 (PostDetailPage와 동일)
// 실제 프로젝트에서는 인증 컨텍스트를 사용하여 전역적으로 관리하는 것이 좋습니다.
interface DecodedToken {
  userId?: number;
  id?: number;
  sub?: string; // subject (주로 사용자 ID)
  // 다른 JWT 페이로드 필드들
}

const getUserIdFromToken = (): number | null => {
  const token = localStorage.getItem('jwtToken');
  if (!token) {
    return null;
  }
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
      return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));

    const decodedToken: DecodedToken = JSON.parse(jsonPayload);
    // 'userId', 'id', 'sub' 중 하나를 사용할 수 있습니다. 백엔드 JWT 구현에 따라 달라집니다.
    const userIdRaw = decodedToken.userId || decodedToken.id || decodedToken.sub;
    const userId = typeof userIdRaw === 'number' ? userIdRaw : parseInt(userIdRaw as string, 10);
    return isNaN(userId) ? null : userId;
  } catch (e) {
    console.error("JWT 토큰 디코딩 중 오류 발생:", e);
    return null;
  }
};


const ChatBlockedUsersPage: React.FC = () => {
  const [blockedUsers, setBlockedUsers] = useState<ChatBlockResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();
  const currentUserId = getUserIdFromToken();

  const fetchBlockedList = async () => {
    if (!currentUserId) {
      setError("로그인이 필요합니다.");
      setLoading(false);
      return;
    }
    try {
      const list = await getBlockedUsers();
      setBlockedUsers(list);
    } catch (err: any) {
      setError(err.message || "차단된 사용자 목록을 불러오는데 실패했습니다.");
      console.error("Failed to fetch blocked users:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBlockedList();
  }, [currentUserId]);

  const handleUnblock = async (blockedUserId: number) => {
    if (!window.confirm(`${blockedUserId}번 사용자의 차단을 해제하시겠습니까?`)) {
      return;
    }
    try {
      await unblockUser(blockedUserId);
      alert('차단이 해제되었습니다.');
      fetchBlockedList(); // 목록 새로고침
    } catch (err: any) {
      alert(`차단 해제 실패: ${err.message}`);
      console.error("Failed to unblock user:", err);
    }
  };

  if (loading) {
    return <div className="flex justify-center items-center h-screen">차단된 사용자 목록 불러오는 중...</div>;
  }

  if (error) {
    return <div className="text-red-500 text-center mt-4">{error}</div>;
  }

  return (
    <div className="container mx-auto p-4 max-w-md bg-white rounded-xl shadow-lg font-inter">
      <h1 className="text-3xl font-bold mb-6 text-center text-gray-800">차단된 사용자 관리</h1>
      {blockedUsers.length === 0 ? (
        <p className="text-center text-gray-600">차단한 사용자가 없습니다.</p>
      ) : (
        <ul className="space-y-3">
          {blockedUsers.map((block) => (
            <li key={block.id} className="flex justify-between items-center bg-gray-50 p-3 rounded-lg shadow-sm border border-gray-100">
              <span className="text-lg text-gray-800">
                {block.blockedNickname} (ID: {block.blockedId})
              </span>
              <button
                onClick={() => handleUnblock(block.blockedId)}
                className="px-4 py-2 bg-red-500 text-white font-semibold rounded-lg shadow-md hover:bg-red-600 transition duration-200"
              >
                차단 해제
              </button>
            </li>
          ))}
        </ul>
      )}
      <button
        onClick={() => navigate('/chat')} // 채팅방 목록으로 돌아가기
        className="w-full mt-6 px-4 py-2 bg-gray-200 text-gray-800 font-semibold rounded-lg shadow-md hover:bg-gray-300 transition duration-300"
      >
        뒤로 가기
      </button>
    </div>
  );
};

export default ChatBlockedUsersPage;
