import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getUserChatRooms, type ChatRoomResponse } from '../service/ChatService';
import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
// import { getUserChatRooms, ChatRoomResponse } from '../services/ChatService'; // ChatRoomResponse import
// import { getUserIdFromToken } from '../utils/authUtils'; // 사용자 ID 가져오는 헬퍼 함수
// import SockJS from 'sockjs-client'; // SockJS import
// import { Client, IMessage } from '@stomp/stompjs'; // STOMP 타입 import

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


const ChatRoomList: React.FC = () => {
  const [chatRooms, setChatRooms] = useState<ChatRoomResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();
  const currentUserId = getUserIdFromToken();

  // WebSocket 연결 상태 관리
  const [stompClient, setStompClient] = useState<Client | null>(null);

  useEffect(() => {
    // 채팅방 목록 불러오기
    const fetchChatRooms = async () => {
      if (!currentUserId) {
        setError("로그인이 필요합니다.");
        setLoading(false);
        return;
      }
      try {
        const rooms = await getUserChatRooms();
        setChatRooms(rooms);
      } catch (err: any) {
        setError(err.message || "채팅방 목록을 불러오는데 실패했습니다.");
        console.error("Failed to fetch chat rooms:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchChatRooms();

    // WebSocket 연결 설정 (NotificationService와 동일하게)
    // 여기서는 알림용으로만 사용하거나, 특정 채팅방에 대한 알림을 받을 수 있습니다.
    // 모든 채팅 메시지를 여기서 구독하기보다는, ChatWindow에서 개별 채팅방을 구독하는 것이 효율적입니다.
    if (currentUserId && !stompClient) {
      const client = new Client({
        webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
        debug: (str) => {
          // console.log('STOMP Debug (ChatRoomList): ', str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
      });

      client.onConnect = (frame) => {
        console.log('Connected to WebSocket (ChatRoomList):', frame);
        setStompClient(client);

        // 사용자 개인 큐 구독
      client.subscribe(`/user/${currentUserId}/queue/errors`, (message: IMessage) => {
        console.error("Error from Backend:", message.body);
      });
      client.subscribe(`/user/${currentUserId}/queue/notifications`, (message: IMessage) => {
        console.log("New notification received:", message.body);
        fetchChatRooms(); // 알림 오면 목록 새로고침
      });
    };

        client.onStompError = (frame) => {
      console.error('STOMP Error (ChatRoomList):', frame);
      setError(`WebSocket 오류: ${frame.headers['message'] || frame.body}`);
    };

    client.activate();
  }

    return () => {
      if (stompClient && stompClient.active) {
        stompClient.deactivate();
        console.log('WebSocket disconnected (ChatRoomList).');
      }
    };
  }, [currentUserId]); // currentUserId가 변경될 때마다 다시 연결 시도

  const handleChatRoomClick = (roomId: number) => {
    navigate(`/chat/${roomId}`); // 채팅방 상세 페이지로 이동
  };

  if (loading) {
    return <div className="flex justify-center items-center h-screen">채팅방 목록 불러오는 중...</div>;
  }

  if (error) {
    return <div className="text-red-500 text-center mt-4">{error}</div>;
  }

  return (
    <div className="container mx-auto p-4 max-w-md bg-white rounded-xl shadow-lg font-inter">
      <h1 className="text-3xl font-bold mb-6 text-center text-gray-800">내 채팅방 목록</h1>
      {chatRooms.length === 0 ? (
        <p className="text-center text-gray-600">아직 참여하고 있는 채팅방이 없습니다.</p>
      ) : (
        <ul className="space-y-4">
          {chatRooms.map((room) => (
            <li
              key={room.id}
              className="bg-gray-50 p-4 rounded-lg shadow-sm border border-gray-100 cursor-pointer hover:bg-gray-100 transition duration-200"
              onClick={() => handleChatRoomClick(room.id)}
            >
              <div className="flex justify-between items-center mb-2">
                <span className={`px-2 py-1 text-xs font-semibold rounded-full ${
                  room.type === 'SINGLE' ? 'bg-blue-100 text-blue-800' : 'bg-green-100 text-green-800'
                }`}>
                  {room.type === 'SINGLE' ? '1:1 채팅' : '그룹 채팅'}
                </span>
                <span className="text-sm text-gray-500">
                  {new Date(room.updatedAt).toLocaleDateString()}
                </span>
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
      <button
        onClick={() => navigate('/chat/create-group')} // 그룹 채팅방 생성 페이지로 이동
        className="w-full mt-6 px-4 py-2 bg-gradient-to-r from-purple-500 to-indigo-600 text-white font-semibold rounded-lg shadow-md hover:from-purple-600 hover:to-indigo-700 transition duration-300 transform hover:scale-105"
      >
        그룹 채팅방 만들기
      </button>

      {/* 차단된 사용자 목록 보기 버튼 (옵션) */}
      <button
        onClick={() => navigate('/chat/blocked-users')}
        className="w-full mt-3 px-4 py-2 bg-gray-200 text-gray-800 font-semibold rounded-lg shadow-md hover:bg-gray-300 transition duration-300"
      >
        차단된 사용자 관리
      </button>
    </div>
  );
};

export default ChatRoomList;
