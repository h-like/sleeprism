import React, { useState, useEffect, useRef } from 'react';
import { useTheme } from '../contexts/ThemeContext';

// --- 타입 정의 (TypeScript) ---
// SockJS와 Stomp 라이브러리가 전역 스코프에 로드되므로, TypeScript가 이를 인식하도록 선언합니다.
declare const SockJS: any;
declare const Stomp: any;

interface User {
  id: number | string;
  nickname: string;
  token: string;
}

interface ChatMessage {
  id?: number;
  chatRoomId: number;
  senderId: number | string;
  senderNickname: string;
  content: string;
  sentAt: string;
}

interface Room {
  id: string;
  name: string;
}

interface ConnectionStatus {
  text: string;
  color: string;
}

// --- 스타일 객체 (Tailwind CSS 클래스) ---
const styles = {
    container: "container mx-auto max-w-4xl h-screen p-4 flex flex-col font-sans",
    chatAppSection: "h-full bg-white rounded-lg shadow-lg flex",
    roomListContainer: "w-1/3 border-r border-gray-200 flex flex-col",
    chatWindowContainer: "w-2/3 flex flex-col h-full",
    messageBubble: (isMyMessage: boolean) => `px-4 py-2 rounded-lg inline-block ${isMyMessage ? 'bg-blue-500 text-white rounded-br-none' : 'bg-gray-200 text-gray-800 rounded-bl-none'}`,
    messageContainer: (isMyMessage: boolean) => `flex mb-4 max-w-lg ${isMyMessage ? 'justify-end ml-auto' : 'justify-start'}`,
};


// --- 하위 컴포넌트들 ---

const RoomList: React.FC<{
  rooms: Room[];
  currentRoomId: string | null;
  onSelectRoom: (roomId: string) => void;
  connectionStatus: ConnectionStatus;
}> = ({ rooms, currentRoomId, onSelectRoom, connectionStatus }) => (
    <div className={styles.roomListContainer}>
        <div className="p-4 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-800">채팅방 목록</h2>
        </div>
        <ul className="overflow-y-auto flex-grow">
            {rooms.length > 0 ? (
                rooms.map(room => (
                    <li key={room.id} onClick={() => onSelectRoom(room.id)} className={`p-4 cursor-pointer hover:bg-gray-100 border-b border-gray-200 ${currentRoomId === room.id ? 'bg-blue-100' : ''}`}>
                        <p className="font-semibold text-gray-700">{room.name}</p>
                    </li>
                ))
            ) : (
                <p className="p-4 text-center text-gray-500">참여중인 채팅방이 없습니다.</p>
            )}
        </ul>
        <div className={`p-4 text-sm text-center border-t border-gray-200 ${connectionStatus.color}`}>
            {connectionStatus.text}
        </div>
    </div>
);

const ChatWindow: React.FC<{ messages: ChatMessage[]; currentUser: User }> = ({ messages, currentUser }) => {
    const messagesEndRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages]);

    return (
        <div className="flex-grow p-4 overflow-y-auto bg-gray-50">
            {messages.length === 0 ? (
                <p className="text-center text-gray-500">메시지가 없습니다.</p>
            ) : (
                messages.map((msg, index) => {
                    const key = msg.id || `${msg.senderId}-${msg.sentAt}-${index}`;
                    const isMyMessage = msg.senderId === currentUser.id;
                    return (
                        <div key={key} className={styles.messageContainer(isMyMessage)}>
                            <div>
                                <div className={`text-xs text-gray-500 mb-1 ${isMyMessage ? 'text-right' : ''}`}>
                                    {isMyMessage ? '나' : msg.senderNickname}
                                </div>
                                <div className={styles.messageBubble(isMyMessage)}>
                                    {msg.content}
                                </div>
                                <div className={`text-xs text-gray-400 mt-1 ${isMyMessage ? 'text-right' : ''}`}>
                                    {new Date(msg.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                </div>
                            </div>
                        </div>
                    );
                })
            )}
            <div ref={messagesEndRef} />
        </div>
    );
};

const MessageInput: React.FC<{ onSendMessage: (content: string) => void; disabled: boolean }> = ({ onSendMessage, disabled }) => {
    const [message, setMessage] = useState('');

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (message.trim()) {
            onSendMessage(message);
            setMessage('');
        }
    };

    return (
        <div className="p-4 bg-white border-t border-gray-200">
            <form onSubmit={handleSubmit} className="flex items-center">
                <input
                    type="text"
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    className="flex-grow shadow-sm border rounded-l-md w-full py-2 px-4 text-gray-700"
                    placeholder="메시지를 입력하세요..."
                    disabled={disabled}
                    autoComplete="off"
                />
                <button type="submit" className="bg-blue-500 hover:bg-blue-600 text-white font-bold py-2 px-4 rounded-r-md disabled:bg-gray-400" disabled={disabled}>
                    전송
                </button>
            </form>
        </div>
    );
};


// --- 메인 채팅 앱 컴포넌트 ---
export default function ChatApp() {
    const [currentUser, setCurrentUser] = useState<User | null>(null);
    const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>({ text: '연결 상태: 대기중', color: 'text-gray-500' });
    
    const [rooms, setRooms] = useState<Room[]>([]);
    const [currentRoomId, setCurrentRoomId] = useState<string | null>(null);
    const [messages, setMessages] = useState<ChatMessage[]>([]);

    const clientRef = useRef<any>(null);
    const subscriptions = useRef<Record<string, any>>({});

      const { isDarkMode } = useTheme();

    // 1. 컴포넌트 마운트 시 사용자 정보 설정
    useEffect(() => {
        const token = localStorage.getItem('jwtToken');
        if (token) {
            try {
                const payload = JSON.parse(atob(token.split('.')[1]));
                if (!payload.userId || !payload.nickname) throw new Error("토큰에 필수 사용자 정보가 없습니다.");
                setCurrentUser({ id: payload.userId, nickname: payload.nickname, token: token });
            } catch (error) {
                console.error("유효하지 않은 토큰입니다:", error);
            }
        }
    }, []);

    // 2. 로그인 후 사용자의 채팅방 목록을 가져오는 로직
    useEffect(() => {
        if (!currentUser?.token) return;

        const fetchUserChatRooms = async () => {
            try {
                const response = await fetch('http://localhost:8080/api/chats/rooms', {
                    headers: { 'Authorization': `Bearer ${currentUser.token}` }
                });
                if (!response.ok) throw new Error('채팅방 목록을 불러오는데 실패했습니다.');
                const data = await response.json();
                const fetchedRooms: Room[] = data.map((room: any) => ({
                    id: room.id.toString(),
                    name: room.name,
                }));
                setRooms(fetchedRooms);
            } catch (error) {
                console.error(error);
            }
        };
        fetchUserChatRooms();
    }, [currentUser]);

    // 3. 웹소켓 연결 및 해제 로직
    useEffect(() => {
        if (!currentUser) return;

        const connect = () => {
            const socket = new SockJS('http://localhost:8080/ws');
            const client = Stomp.over(socket);
            client.debug = (str: string) => console.log(new Date(), str);
            clientRef.current = client;

            client.connect(
                { 'Authorization': `Bearer ${currentUser.token}` },
                () => { setConnectionStatus({ text: '연결 상태: 연결됨', color: 'text-green-600' }); },
                (error: any) => {
                    console.error('STOMP 연결 오류:', error);
                    setConnectionStatus({ text: '연결 상태: 오류', color: 'text-red-600' });
                }
            );
        };
        connect();
        return () => {
            if (clientRef.current?.connected) {
                clientRef.current.disconnect();
            }
        };
    }, [currentUser]);

    // 4. 채팅방 구독 관리 로직 (수정됨)
    useEffect(() => {
        // 연결 상태가 '연결됨'이 아니거나, 방이 선택되지 않았으면 아무것도 하지 않습니다.
        if (connectionStatus.text !== '연결 상태: 연결됨' || !currentRoomId) {
            return;
        }

        const client = clientRef.current;
        if (!client) return;
        
        // 이전 구독이 있다면 모두 해제
        Object.values(subscriptions.current).forEach(sub => sub.unsubscribe());
        subscriptions.current = {};
        setMessages([]);

        // 실시간 메시지 수신용 구독
        const messageSub = client.subscribe(`/topic/chat/room/${currentRoomId}`, (message: any) => {
            const msgData: ChatMessage = JSON.parse(message.body);
            setMessages(prev => [...prev, msgData]);
        });
        subscriptions.current['message'] = messageSub;

        // ▼▼▼ 서버가 첫 구독을 처리할 시간을 주기 위해 약간의 지연을 추가합니다. ▼▼▼
        const timerId = setTimeout(() => {
            if (!client.connected) return; // 지연 후에도 연결이 살아있는지 확인

            // 과거 내역 수신용 구독
            const historySub = client.subscribe(`/user/queue/chat/history/${currentRoomId}`, (message: any) => {
                const history: ChatMessage[] = JSON.parse(message.body);
                setMessages(history.reverse());
                historySub.unsubscribe();
            });

            // 서버에 과거 내역 요청
            client.send(`/app/chat.history`, {}, JSON.stringify({ chatRoomId: Number(currentRoomId) }));
        }, 50); // 50밀리초 지연 (보통 이 정도로 충분합니다)

        // 이 useEffect의 정리(cleanup) 함수
        return () => {
            clearTimeout(timerId); // 컴포넌트가 사라지면 타이머도 제거
            Object.values(subscriptions.current).forEach(sub => sub.unsubscribe());
        };

    }, [currentRoomId, connectionStatus.text]); // 방 ID 또는 '연결 상태'가 변경될 때만 실행


    const handleSendMessage = (content: string) => {
        if (clientRef.current?.connected && currentRoomId) {
            clientRef.current.send('/app/chat.sendMessage', {}, JSON.stringify({
                chatRoomId: Number(currentRoomId),
                content: content,
            }));
        }
    };

    if (!currentUser) {
        return <div className="flex items-center justify-center h-screen">로그인 정보를 확인 중입니다...</div>;
    }

    return (
        <div className={styles.container}>
            <div className={styles.chatAppSection}>
                <RoomList 
                    rooms={rooms}
                    currentRoomId={currentRoomId}
                    onSelectRoom={setCurrentRoomId}
                    connectionStatus={connectionStatus}
                />
                <div className={styles.chatWindowContainer}>
                    <div className="p-4 border-b border-gray-200">
                        <h2 className="text-lg font-semibold text-gray-800">
                            {currentRoomId ? `${rooms.find(r => r.id === currentRoomId)?.name || ''}` : '채팅방을 선택하세요'}
                        </h2>
                    </div>
                    {currentRoomId ? (
                        <>
                            <ChatWindow messages={messages} currentUser={currentUser} />
                            <MessageInput onSendMessage={handleSendMessage} disabled={!clientRef.current?.connected} />
                        </>
                    ) : (
                        <div className="flex-grow flex items-center justify-center text-gray-500">
                            <p>왼쪽에서 채팅방을 선택하여 대화를 시작하세요.</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
