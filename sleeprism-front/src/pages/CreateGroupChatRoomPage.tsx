import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchAllUsers, type UserProfile } from '../service/UserService';
import { createGroupChatRoom } from '../service/ChatService';
// import { createGroupChatRoom } from '../services/ChatService';
// import { getUserIdFromToken } from '../utils/authUtils'; // 사용자 ID 가져오는 헬퍼 함수
// import { fetchAllUsers, UserProfile } from '../services/UserService'; // UserService에서 fetchAllUsers 임포트

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


// UserService에서 사용할 UserProfile 타입 정의 (UserService가 없다고 가정)
// 실제로는 src/services/UserService.ts 파일을 만들고 fetchAllUsers 함수와 함께 정의해야 합니다.
// 임시로 여기에 정의합니다.
// interface UserProfile {
//   id: number;
//   nickname: string;
//   email: string;
// }

// 임시 fetchAllUsers 함수 (실제로는 UserService에 구현)
// const fetchAllUsers = async (): Promise<UserProfile[]> => {
//   const token = localStorage.getItem('jwtToken');
//   const response = await fetch('http://localhost:8080/api/users', { // 모든 사용자 조회 API (가정)
//     headers: {
//       'Authorization': `Bearer ${token}`,
//       'Content-Type': 'application/json',
//     },
//   });
//   if (!response.ok) {
//     const errorData = await response.json();
//     throw new Error(errorData.message || '사용자 목록을 불러오는데 실패했습니다.');
//   }
//   const users: UserProfile[] = await response.json();
//   // 현재 로그인된 사용자를 목록에서 제외
//   const currentUserId = getUserIdFromToken();
//   return users.filter(user => user.id !== currentUserId);
// };


const CreateGroupChatRoomPage: React.FC = () => {
  const [roomName, setRoomName] = useState<string>('');
  const [selectedParticipants, setSelectedParticipants] = useState<number[]>([]);
  const [allUsers, setAllUsers] = useState<UserProfile[]>([]);
  const [loadingUsers, setLoadingUsers] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();
  const currentUserId = getUserIdFromToken();

  useEffect(() => {
    const loadUsers = async () => {
      if (!currentUserId) {
        setError("로그인이 필요합니다.");
        setLoadingUsers(false);
        return;
      }
      try {
        const users = await fetchAllUsers(); // 모든 사용자 목록 가져오기
        // 본인 제외하고 목록 설정
        setAllUsers(users.filter(user => user.id !== currentUserId));
      } catch (err: any) {
        setError(err.message || "사용자 목록을 불러오는데 실패했습니다.");
        console.error("Failed to fetch all users:", err);
      } finally {
        setLoadingUsers(false);
      }
    };
    loadUsers();
  }, [currentUserId]);

  const handleParticipantChange = (userId: number, isChecked: boolean) => {
    if (isChecked) {
      setSelectedParticipants((prev) => [...prev, userId]);
    } else {
      setSelectedParticipants((prev) => prev.filter((id) => id !== userId));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!roomName.trim()) {
      alert('채팅방 이름을 입력해주세요.');
      return;
    }
    if (selectedParticipants.length === 0) {
      alert('초대할 참가자를 한 명 이상 선택해주세요.');
      return;
    }

    try {
      // 그룹 채팅방 생성 요청
      const newRoom = await createGroupChatRoom(roomName, selectedParticipants);
      alert(`그룹 채팅방 '${newRoom.name}'이(가) 성공적으로 생성되었습니다!`);
      navigate(`/chat/${newRoom.id}`); // 생성된 채팅방으로 이동
    } catch (err: any) {
      setError(err.message || "그룹 채팅방 생성에 실패했습니다.");
      console.error("Failed to create group chat room:", err);
    }
  };

  if (loadingUsers) {
    return <div className="flex justify-center items-center h-screen">사용자 목록 불러오는 중...</div>;
  }

  if (error) {
    return <div className="text-red-500 text-center mt-4">{error}</div>;
  }

  return (
    <div className="container mx-auto p-4 max-w-lg bg-white rounded-xl shadow-lg font-inter">
      <h1 className="text-3xl font-bold mb-6 text-center text-gray-800">그룹 채팅방 만들기</h1>
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label htmlFor="roomName" className="block text-gray-700 text-sm font-bold mb-2">
            채팅방 이름:
          </label>
          <input
            type="text"
            id="roomName"
            value={roomName}
            onChange={(e) => setRoomName(e.target.value)}
            className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            placeholder="채팅방 이름을 입력하세요"
            required
          />
        </div>

        <div>
          <h3 className="text-gray-700 text-sm font-bold mb-2">초대할 사용자 선택:</h3>
          {allUsers.length === 0 ? (
            <p className="text-gray-600">초대할 수 있는 다른 사용자가 없습니다.</p>
          ) : (
            <div className="border border-gray-300 rounded-lg p-3 max-h-60 overflow-y-auto">
              {allUsers.map((user) => (
                <div key={user.id} className="flex items-center mb-2">
                  <input
                    type="checkbox"
                    id={`user-${user.id}`}
                    value={user.id}
                    checked={selectedParticipants.includes(user.id)}
                    onChange={(e) => handleParticipantChange(user.id, e.target.checked)}
                    className="mr-2 h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <label htmlFor={`user-${user.id}`} className="text-gray-700">
                    {user.nickname} (ID: {user.id})
                  </label>
                </div>
              ))}
            </div>
          )}
        </div>

        <button
          type="submit"
          className="w-full px-4 py-2 bg-gradient-to-r from-green-500 to-teal-600 text-white font-semibold rounded-lg shadow-md hover:from-green-600 hover:to-teal-700 transition duration-300 transform hover:scale-105"
        >
          채팅방 생성
        </button>
        <button
          type="button"
          onClick={() => navigate('/chat')}
          className="w-full mt-3 px-4 py-2 bg-gray-200 text-gray-800 font-semibold rounded-lg shadow-md hover:bg-gray-300 transition duration-300"
        >
          취소
        </button>
      </form>
    </div>
  );
};

export default CreateGroupChatRoomPage;
