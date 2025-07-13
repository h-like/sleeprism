import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme } from '../contexts/ThemeContext'; // 전역 ThemeContext 훅 임포트
import '../../public/css/ChatPage.css'; // 통합 CSS 파일 임포트

// --- 타입 정의 ---
declare const SockJS: any;
declare const Stomp: any;

interface User { id: number | string; nickname: string; token: string; }
interface ChatMessage { id?: number; chatRoomId: number; senderId: number | string; senderNickname: string; content: string; sentAt: string; messageType?: 'TEXT' | 'IMAGE'; }
interface Room { id: string; name: string; lastMessage?: ChatMessage; otherParticipantProfileImageUrl?: string | null; }

// --- 하위 컴포넌트들 ---

const ChatList: React.FC<{
    rooms: Room[];
    currentRoomId: string | null;
    onSelectRoom: (roomId: string) => void;
}> = ({ rooms, currentRoomId, onSelectRoom }) => (
    <div className="chat-list-sidebar">
        <header className="chat-list-header">
            <h2>Messages</h2>
            {/* 테마 토글 버튼은 이제 전역 Header에서 관리하므로 여기서 제거합니다. */}
        </header>
        <div className="chat-list-items">
            {rooms.map(room => (
                <div key={room.id} className={`chat-list-item ${currentRoomId === room.id ? 'active' : ''}`} onClick={() => onSelectRoom(room.id)}>
                    <div className="chat-list-item-avatar">
                        <img
                            src={room.otherParticipantProfileImageUrl
                                ? `http://localhost:8080${room.otherParticipantProfileImageUrl}`
                                : `https://i.pravatar.cc/48?u=${room.id}` // 임시 기본 이미지
                            }
                            alt="상대방 아바타"
                        />
                    </div>
                    <div className="chat-list-item-info">
                        <div className="name">{room.name}</div>
                        <div className="last-message">
                            {room.lastMessage ? (room.lastMessage.messageType === 'IMAGE' ? '사진을 보냈습니다.' : room.lastMessage.content) : '대화를 시작해보세요.'}
                        </div>
                    </div>
                </div>
            ))}
        </div>
    </div>
);

const ChatWindow: React.FC<{
    roomId: string;
    roomName: string;
    currentUser: User;
}> = ({ roomId, roomName, currentUser }) => {
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const clientRef = useRef<any>(null);
    const messagesEndRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const client = Stomp.over(new SockJS('http://localhost:8080/ws'));
        client.debug = null;
        clientRef.current = client;

        const onConnect = () => {
            client.subscribe(`/topic/chat/room/${roomId}`, (message: any) => {
                setMessages(prev => [...prev, JSON.parse(message.body)]);
            });
            const historySub = client.subscribe(`/user/queue/chat/history/${roomId}`, (message: any) => {
                setMessages(JSON.parse(message.body).reverse());
                historySub.unsubscribe();
            });
            setTimeout(() => {
                if(client.connected) client.send(`/app/chat.history`, {}, JSON.stringify({ chatRoomId: Number(roomId) }));
            }, 100);
        };

        client.connect({ 'Authorization': `Bearer ${currentUser.token}` }, onConnect, () => {});

        return () => { if (clientRef.current?.connected) clientRef.current.disconnect(); };
    }, [roomId, currentUser.token]);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages]);

    const handleSendMessage = (content: string, type: 'TEXT' | 'IMAGE' = 'TEXT') => {
        if (clientRef.current?.connected) {
            clientRef.current.send('/app/chat.sendMessage', {}, JSON.stringify({
                chatRoomId: Number(roomId), content, messageType: type
            }));
        }
    };
    
    const handleImageUpload = async (file: File) => {
        const formData = new FormData();
        formData.append('file', file);
        try {
            const response = await fetch('http://localhost:8080/api/chats/files/upload', {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${currentUser.token}` },
                body: formData,
            });
            if (!response.ok) throw new Error('파일 업로드 실패');
            const result = await response.json();
            handleSendMessage(result.fileUrl, 'IMAGE');
        } catch (error) {
            alert(`파일 업로드 오류: ${error}`);
        }
    };

    return (
        <div className="chat-window-main">
            <header className="chat-header"><h3 className="room-name">{roomName}</h3></header>
            <div className="chat-window">
                {messages.map((msg, index) => {
                    const isMyMessage = msg.senderId === currentUser.id;
                    return (
                        <div key={msg.id || index} className={`message-container ${isMyMessage ? 'my-message' : 'other-message'}`}>
                            <div className="message-item">
                                <div className="message-nickname">{isMyMessage ? '나' : msg.senderNickname}</div>
                                <div className="message-bubble">
                                    {msg.messageType === 'IMAGE' ? <img src={`http://localhost:8080${msg.content}`} alt="첨부 이미지" className="chat-image" /> : msg.content}
                                </div>
                                <div className="message-time">{new Date(msg.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</div>
                            </div>
                        </div>
                    );
                })}
                <div ref={messagesEndRef} />
            </div>
            <MessageInputArea onSendMessage={handleSendMessage} onImageUpload={handleImageUpload} disabled={!clientRef.current?.connected} />
        </div>
    );
};

const MessageInputArea: React.FC<{
    onSendMessage: (content: string, type: 'TEXT' | 'IMAGE') => void;
    onImageUpload: (file: File) => void;
    disabled: boolean;
}> = ({ onSendMessage, onImageUpload, disabled }) => {
    const [message, setMessage] = useState('');
    const fileInputRef = useRef<HTMLInputElement>(null);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (message.trim()) {
            onSendMessage(message, 'TEXT');
            setMessage('');
        }
    };

    return (
        <div className="message-input-area">
            <form onSubmit={handleSubmit} className="message-input-form">
                <input type="file" ref={fileInputRef} onChange={(e) => e.target.files && onImageUpload(e.target.files[0])} style={{ display: 'none' }} accept="image/*" />
                <button type="button" className="chat-action-button" onClick={() => fileInputRef.current?.click()} disabled={disabled}>
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l8.57-8.57A4 4 0 1 1 18 8.84l-8.59 8.59a2 2 0 0 1-2.83-2.83l8.49-8.48"/></svg>
                </button>
                <input type="text" value={message} onChange={(e) => setMessage(e.target.value)} className="message-input-field" placeholder="메시지를 입력하세요..." disabled={disabled} />
                <button type="submit" className="send-button" disabled={disabled}>
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/></svg>
                </button>
            </form>
        </div>
    );
};


// --- 메인 페이지 컴포넌트 ---
export default function ChatPage() {
    const navigate = useNavigate();
    const { isDarkMode } = useTheme(); // 전역 테마 컨텍스트 사용
    const [currentUser, setCurrentUser] = useState<User | null>(null);
    const [rooms, setRooms] = useState<Room[]>([]);
    const [currentRoomId, setCurrentRoomId] = useState<string | null>(null);

    // 로컬 테마 관리 로직 제거
    // useEffect(() => { ... });
    // const toggleTheme = () => { ... };

    useEffect(() => {
        const token = localStorage.getItem('jwtToken');
        if (token) {
            try {
                const payload = JSON.parse(atob(token.split('.')[1]));
                setCurrentUser({ id: payload.userId, nickname: payload.nickname, token: token });
            } catch (error) { navigate('/login'); }
        } else { navigate('/login'); }
    }, [navigate]);

    useEffect(() => {
        if (!currentUser?.token) return;
        const fetchRooms = async () => {
            try {
                const response = await fetch('http://localhost:8080/api/chats/rooms', {
                    headers: { 'Authorization': `Bearer ${currentUser.token}` }
                });
                const data = await response.json();
                const processedRooms: Room[] = data.map((roomData: any) => {
                    const otherParticipant = roomData.participants?.find(
                        (p: any) => p.userId !== currentUser.id
                    );
                    return {
                        id: roomData.id.toString(),
                        name: roomData.name,
                        lastMessage: roomData.lastMessage,
                        otherParticipantProfileImageUrl: otherParticipant ? otherParticipant.profileImageUrl : null
                    };
                });
                setRooms(processedRooms);
            } catch (error) { console.error("채팅방 목록 로딩 실패:", error); }
        };
        fetchRooms();
    }, [currentUser]);

    const currentRoom = rooms.find(r => r.id === currentRoomId);

    return (
        // isDarkMode 상태에 따라 클래스 동적 적용
        <div className={`chat-layout-container ${isDarkMode ? 'dark' : 'light'}`}>
            {/* ChatList에서 로컬 테마 관련 props 제거 */}
            <ChatList rooms={rooms} currentRoomId={currentRoomId} onSelectRoom={setCurrentRoomId} />
            {currentRoom && currentUser ? (
                <ChatWindow roomId={currentRoom.id} roomName={currentRoom.name} currentUser={currentUser} />
            ) : (
                <div className="chat-window-placeholder">
                    <div>
                        <p>채팅방을 선택해주세요.</p>
                        <p>왼쪽 목록에서 대화를 시작할 채팅방을 선택하세요.</p>
                    </div>
                </div>
            )}
        </div>
    );
}
