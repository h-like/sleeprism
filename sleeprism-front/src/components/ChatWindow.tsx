import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import type { IMessage } from '@stomp/stompjs';
import { connectWebSocket, disconnectWebSocket, sendMessage, subscribeToTopic } from '../utils/ChatWebSocket';
import { blockUser, getBlockedUsers, getChatHistory, unblockUser, type ChatBlockResponse, type ChatMessageResponse } from '../service/ChatService';
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


const ChatWindow: React.FC = () => {
  const { chatRoomId } = useParams<{ chatRoomId: string }>();
  const navigate = useNavigate();
  const currentUserId = getUserIdFromToken();

  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [newMessage, setNewMessage] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [blockedUsers, setBlockedUsers] = useState<ChatBlockResponse[]>([]);

  const messagesEndRef = useRef<HTMLDivElement>(null); // 메시지 스크롤 하단 고정
  const hasFetchedInitialMessages = useRef(false);

  // 현재 채팅방의 정보 (메시지 전송 시 사용)
  // 실제로는 ChatRoomService.getChatRoomDetails(chatRoomId)를 통해 가져와야 하지만,
  // 여기서는 편의상 메시지 전송 로직에 집중합니다.

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const handleMessageReceived = useCallback((message: IMessage) => {
    const receivedMessage: ChatMessageResponse = JSON.parse(message.body);
    console.log('Received chat message:', receivedMessage);
    setMessages((prevMessages) => [...prevMessages, receivedMessage]);
    // 메시지 수신 후 스크롤 하단으로 이동
    setTimeout(scrollToBottom, 100); // DOM 업데이트 후 스크롤을 위해 약간의 딜레이
  }, []);

  const handleWebSocketError = useCallback((err: string) => {
    setError(err);
    console.error("WebSocket Error in ChatWindow:", err);
  }, []);

  useEffect(() => {
    if (!currentUserId) {
      setError("로그인이 필요합니다.");
      setLoading(false);
      return;
    }

    // 채팅 메시지 내역 불러오기
    const fetchChatMessages = async () => {
      try {
        const history = await getChatHistory(Number(chatRoomId));
        setMessages(history.reverse()); // 최신 메시지가 아래로 오도록 뒤집기
        hasFetchedInitialMessages.current = true;
      } catch (err: any) {
        setError(err.message || "메시지 내역을 불러오는데 실패했습니다.");
        console.error("Failed to fetch chat history:", err);
      } finally {
        setLoading(false);
      }
    };

    // 차단된 사용자 목록 불러오기 (메시지 전송 전에 확인할 수 있도록)
    const fetchBlockedUsers = async () => {
      try {
        const blockedList = await getBlockedUsers();
        setBlockedUsers(blockedList);
      } catch (err: any) {
        console.error("Failed to fetch blocked users:", err);
        // 에러를 사용자에게 표시할 필요는 없을 수 있음
      }
    };

    fetchChatMessages();
    fetchBlockedUsers();

    // WebSocket 연결 및 구독
    // connectWebSocket 함수가 이미 내부적으로 재연결 로직을 가지고 있으므로,
    // 여기서 직접 재연결 시도를 관리할 필요는 없습니다.
    connectWebSocket(
      (frame) => {
        console.log('WebSocket Connected (ChatWindow):', frame);
        // 채팅방 토픽 구독
        const unsubscribe = subscribeToTopic(`/topic/chat/room/${chatRoomId}`, handleMessageReceived);
        // 클린업 함수에서 구독 해제
        return () => {
          unsubscribe();
        };
      },
      handleMessageReceived, // 이 콜백은 개별 토픽 구독에서 처리됨
      handleWebSocketError,
      currentUserId
    );

    // 컴포넌트 언마운트 시 WebSocket 연결 해제
    return () => {
      disconnectWebSocket();
    };
  }, [chatRoomId, currentUserId, handleMessageReceived, handleWebSocketError]);

  // 메시지가 업데이트될 때마다 스크롤 하단으로 이동
  useEffect(() => {
    if (hasFetchedInitialMessages.current) {
      scrollToBottom();
    }
  }, [messages]);

  const handleSendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (newMessage.trim() === '') return;

    // 차단 여부 확인 (클라이언트 측에서 기본적인 확인)
    // 백엔드에서 다시 검증되지만, 사용자 경험을 위해 미리 확인
    // 현재 채팅방 참가자 중 내가 차단한 사람이 있는지 또는 나를 차단한 사람이 있는지 확인
    // 이 로직은 1:1 채팅에만 의미가 있으므로, 그룹 채팅에서는 메시지 차단 로직을 백엔드에서만 처리하는 것이 간단합니다.
    // 여기서는 간단히 구현을 위해 차단된 사람이 있다면 메시지 전송 불가로 처리 (실제로는 1:1만)
    // if (blockedUsers.some(block => messages.some(msg => msg.senderId === block.blockedId))) {
    //   setError("차단된 사용자에게 메시지를 보낼 수 없습니다.");
    //   return;
    // }

    const messagePayload = {
      chatRoomId: Number(chatRoomId),
      content: newMessage,
    };
    sendMessage('/chat/sendMessage', messagePayload);
    setNewMessage('');
  };

  const handleBlockUser = async (targetUserId: number) => {
    if (!window.confirm(`${targetUserId}번 사용자를 차단하시겠습니까? 차단하면 해당 사용자로부터 메시지를 받을 수 없습니다.`)) {
      return;
    }
    try {
      await blockUser(targetUserId);
      const updatedBlockedUsers = await getBlockedUsers(); // 차단 목록 새로고침
      setBlockedUsers(updatedBlockedUsers);
      alert(`${targetUserId}번 사용자를 차단했습니다.`);
    } catch (err: any) {
      alert(`차단 실패: ${err.message}`);
    }
  };

  const handleUnblockUser = async (targetUserId: number) => {
    if (!window.confirm(`${targetUserId}번 사용자의 차단을 해제하시겠습니까?`)) {
      return;
    }
    try {
      await unblockUser(targetUserId);
      const updatedBlockedUsers = await getBlockedUsers(); // 차단 목록 새로고침
      setBlockedUsers(updatedBlockedUsers);
      alert(`${targetUserId}번 사용자의 차단을 해제했습니다.`);
    } catch (err: any) {
      alert(`차단 해제 실패: ${err.message}`);
    }
  };

  // 특정 사용자가 차단되었는지 확인하는 헬퍼
  const isUserBlocked = (userId: number): boolean => {
    return blockedUsers.some(block => block.blockedId === userId);
  };


  if (loading) {
    return <div className="flex justify-center items-center h-screen">메시지 불러오는 중...</div>;
  }

  if (error) {
    return <div className="text-red-500 text-center mt-4">{error}</div>;
  }

  return (
    <div className="flex flex-col h-screen bg-gray-50 font-inter">
      <header className="flex-shrink-0 bg-indigo-600 text-white p-4 shadow-md flex justify-between items-center">
        <button onClick={() => navigate(-1)} className="text-white hover:text-gray-200">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-arrow-left"><path d="m12 19-7-7 7-7"/><path d="M19 12H5"/></svg>
        </button>
        <h1 className="text-xl font-semibold">채팅방 (ID: {chatRoomId})</h1>
        {/* TODO: 1:1 채팅방의 경우 상대방 닉네임 표시, 그룹 채팅방의 경우 채팅방 이름 표시 */}
        <div className="w-6"></div> {/* Align items */}
      </header>

      <div className="flex-grow overflow-y-auto p-4 space-y-4">
        {messages.map((msg, index) => (
          <div
            key={msg.id || index} // 새로운 메시지는 id가 없을 수 있으므로 index 사용 (임시)
            className={`flex ${msg.senderId === currentUserId ? 'justify-end' : 'justify-start'}`}
          >
            <div className={`flex flex-col max-w-xs sm:max-w-md ${
              msg.senderId === currentUserId
                ? 'bg-blue-500 text-white rounded-bl-xl rounded-tl-xl rounded-tr-xl'
                : 'bg-gray-200 text-gray-800 rounded-br-xl rounded-tr-xl rounded-tl-xl'
            } p-3 shadow-sm`}>
              <span className="font-semibold text-sm mb-1">
                {msg.senderId === currentUserId ? '나' : msg.senderNickname}
                {msg.senderId !== currentUserId && ( // 상대방 메시지에만 차단/해제 버튼 표시
                  isUserBlocked(msg.senderId) ? (
                    <button
                      onClick={() => handleUnblockUser(msg.senderId)}
                      className="ml-2 text-xs text-red-200 hover:text-red-100 underline"
                    >
                      (차단 해제)
                    </button>
                  ) : (
                    <button
                      onClick={() => handleBlockUser(msg.senderId)}
                      className="ml-2 text-xs text-blue-200 hover:text-blue-100 underline"
                    >
                      (차단)
                    </button>
                  )
                )}
              </span>
              <p className="text-base">{msg.content}</p>
              <span className="text-xs mt-1 self-end opacity-80">
                {new Date(msg.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
              </span>
            </div>
          </div>
        ))}
        <div ref={messagesEndRef} />
      </div>

      <form onSubmit={handleSendMessage} className="flex-shrink-0 p-4 bg-white border-t border-gray-200 shadow-md">
        <div className="flex rounded-lg border border-gray-300 overflow-hidden">
          <input
            type="text"
            value={newMessage}
            onChange={(e) => setNewMessage(e.target.value)}
            placeholder="메시지를 입력하세요..."
            className="flex-grow p-3 focus:outline-none focus:ring-2 focus:ring-blue-400"
            disabled={!currentUserId} // 로그인되지 않으면 입력 비활성화
          />
          <button
            type="submit"
            className="bg-blue-500 text-white px-6 py-3 font-semibold hover:bg-blue-600 transition duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
            disabled={!currentUserId || newMessage.trim() === ''}
          >
            전송
          </button>
        </div>
        {!currentUserId && <p className="text-red-500 text-sm mt-2 text-center">로그인 후 채팅을 이용할 수 있습니다.</p>}
      </form>
    </div>
  );
};

export default ChatWindow;
