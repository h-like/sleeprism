import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getUserChatRooms, type ChatRoomResponse } from '../service/ChatService';
import { useWebSocket } from '../contexts/WebSocketProvider'; // 1. 전역 컨텍스트 훅만 임포트
import type { IMessage } from '@stomp/stompjs';

// JWT 토큰에서 사용자 ID를 가져오는 헬퍼 함수 (기존과 동일)
interface DecodedToken {
  userId?: number;
  id?: number;
  sub?: string;
}
const getUserIdFromToken = (): number | null => {
  const token = localStorage.getItem('jwtToken');
  if (!token) return null;
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(atob(base64).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join(''));
    const decoded: DecodedToken = JSON.parse(jsonPayload);
    const userId = parseInt(String(decoded.userId || decoded.id || decoded.sub), 10);
    return isNaN(userId) ? null : userId;
  } catch (e) {
    console.error("Error decoding JWT token:", e);
    return null;
  }
};


const ChatRoomList: React.FC = () => {
  const [chatRooms, setChatRooms] = useState<ChatRoomResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();
  const currentUserId = getUserIdFromToken();

  // 2. 전역 웹소켓 클라이언트와 연결 상태를 가져옴
  const { stompClient, isConnected } = useWebSocket();

  // 채팅방 목록을 새로고침하는 함수
  const fetchChatRooms = useCallback(async () => {
    if (!currentUserId) return;
    try {
      const rooms = await getUserChatRooms();
      setChatRooms(rooms);
    } catch (err: any) {
      setError(err.message || "채팅방 목록을 불러오는데 실패했습니다.");
    } finally {
      setLoading(false);
    }
  }, [currentUserId]);

  // 3. useEffect 로직을 전역 연결 상태(isConnected)에 따라 동작하도록 재구성
  useEffect(() => {
    // 초기 데이터 로드
    fetchChatRooms();

    if (!isConnected || !stompClient || !currentUserId) {
      console.log("ChatRoomList: Waiting for global WebSocket connection...");
      return;
    }
    
    console.log('ChatRoomList: Global WebSocket is connected. Subscribing to notifications...');

    // 알림 큐 구독
    const subscription = stompClient.subscribe(`/user/${currentUserId}/queue/notifications`, (message: IMessage) => {
      console.log("New notification received in ChatRoomList:", message.body);
      // 알림을 받으면 채팅방 목록을 새로고침
      fetchChatRooms();
    });

    // 컴포넌트가 사라질 때 구독만 해제
    return () => {
      console.log('ChatRoomList: Unsubscribing from notifications...');
      subscription.unsubscribe();
    };
  }, [isConnected, stompClient, currentUserId, fetchChatRooms]); // 의존성 배열 정리

  const handleChatRoomClick = (roomId: number) => {
    navigate(`/chat/${roomId}`);
  };

  if (loading) return <div className="flex justify-center items-center h-screen">채팅방 목록 불러오는 중...</div>;
  if (error) return <div className="text-red-500 text-center mt-4">{error}</div>;

  return (
    <div className="container mx-auto p-4 max-w-md bg-white rounded-xl shadow-lg font-inter">
      {/* ... JSX 렌더링 부분은 기존과 동일 ... */}
      <h1 className="text-3xl font-bold mb-6 text-center text-gray-800">내 채팅방 목록</h1>
      {chatRooms.length === 0 ? (
        <p className="text-center text-gray-600">아직 참여하고 있는 채팅방이 없습니다.</p>
      ) : (
        <ul className="space-y-4">
          {chatRooms.map((room) => (
            <li key={room.id} className="bg-gray-50 p-4 rounded-lg shadow-sm border border-gray-100 cursor-pointer hover:bg-gray-100 transition duration-200" onClick={() => handleChatRoomClick(room.id)}>
              <div className="flex justify-between items-center mb-2">
                <span className={`px-2 py-1 text-xs font-semibold rounded-full ${room.type === 'SINGLE' ? 'bg-blue-100 text-blue-800' : 'bg-green-100 text-green-800'}`}>
                  {room.type === 'SINGLE' ? '1:1 채팅' : '그룹 채팅'}
                </span>
                <span className="text-sm text-gray-500">{new Date(room.updatedAt).toLocaleDateString()}</span>
              </div>
              <h2 className="text-lg font-semibold text-gray-800">{room.name || '이름 없는 채팅방'}</h2>
              <p className="text-gray-600 text-sm mt-1 truncate">
                {room.lastMessage ? `${room.lastMessage.senderNickname}: ${room.lastMessage.content}` : '메시지 없음'}
              </p>
              <div className="text-right text-xs text-gray-400 mt-2">
                참여자: {room.participants.length}명
              </div>
            </li>
          ))}
        </ul>
      )}
      <button onClick={() => navigate('/chat/create-group')} className="w-full mt-6 px-4 py-2 bg-gradient-to-r from-purple-500 to-indigo-600 text-white font-semibold rounded-lg shadow-md hover:from-purple-600 hover:to-indigo-700 transition duration-300 transform hover:scale-105">
        그룹 채팅방 만들기
      </button>
      <button onClick={() => navigate('/chat/blocked-users')} className="w-full mt-3 px-4 py-2 bg-gray-200 text-gray-800 font-semibold rounded-lg shadow-md hover:bg-gray-300 transition duration-300">
        차단된 사용자 관리
      </button>
    </div>
  );
};

export default ChatRoomList;