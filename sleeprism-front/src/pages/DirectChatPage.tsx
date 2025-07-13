import React, { useState, useEffect, useRef } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import { useTheme } from '../contexts/ThemeContext'; // 전역 ThemeContext 훅 임포트
import '../../public/css/DirectChatPage.css';

// --- 타입 정의 ---
declare const SockJS: any;
declare const Stomp: any;

interface User { id: number | string; nickname: string; token: string; }
interface ChatMessage { id?: number; chatRoomId: number; senderId: number | string; senderNickname: string; content: string; sentAt: string; messageType?: 'TEXT' | 'IMAGE'; }

// --- 하위 컴포넌트들 (기존과 동일) ---

const ChatWindow: React.FC<{ messages: ChatMessage[]; currentUser: User | null }> = ({ messages, currentUser }) => {
    const messagesEndRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages]);

    if (!currentUser) return null;

    const renderMessageContent = (msg: ChatMessage) => {
        if (msg.messageType === 'IMAGE') {
            return <img src={`http://localhost:8080${msg.content}`} alt="첨부 이미지" className="chat-image" />;
        }
        return msg.content;
    };

    return (
        <div className="chat-window">
            {messages.map((msg, index) => {
                const key = msg.id || `${msg.senderId}-${msg.sentAt}-${index}`;
                const isMyMessage = msg.senderId === currentUser.id;
                return (
                    <div key={key} className={`message-container ${isMyMessage ? 'my-message' : 'other-message'}`}>
                        <div className="message-item">
                            <div className="message-nickname">
                                {isMyMessage ? '나' : msg.senderNickname}
                            </div>
                            <div className="message-bubble">
                                {renderMessageContent(msg)}
                            </div>
                            <div className="message-time">
                                {new Date(msg.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                            </div>
                        </div>
                    </div>
                );
            })}
            <div ref={messagesEndRef} />
        </div>
    );
};

const MessageInput: React.FC<{
    onSendMessage: (content: string, type: 'TEXT' | 'IMAGE') => void;
    disabled: boolean;
    currentUser: User | null;
}> = ({ onSendMessage, disabled, currentUser }) => {
    const [message, setMessage] = useState('');
    const fileInputRef = useRef<HTMLInputElement>(null);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (message.trim()) {
            onSendMessage(message, 'TEXT');
            setMessage('');
        }
    };

    const handleFileIconClick = () => {
        fileInputRef.current?.click();
    };

    const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file || !currentUser) return;

        const formData = new FormData();
        formData.append('file', file);

        try {
            const response = await fetch('http://localhost:8080/api/chats/files/upload', {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${currentUser.token}` },
                body: formData,
            });
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: '서버 응답 파싱 실패' }));
                throw new Error(errorData.message || `파일 업로드 실패`);
            }
            const result = await response.json();
            onSendMessage(result.fileUrl, 'IMAGE');
        } catch (error: any) {
            alert(`파일 업로드 오류: ${error.message}`);
        } finally {
            if(fileInputRef.current) fileInputRef.current.value = "";
        }
    };

    return (
        <div className="message-input-area">
            <form onSubmit={handleSubmit} className="message-input-form">
                <input type="file" ref={fileInputRef} onChange={handleFileChange} style={{ display: 'none' }} accept="image/*" />
                <button type="button" className="chat-action-button" onClick={handleFileIconClick} disabled={disabled}>
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l8.57-8.57A4 4 0 1 1 18 8.84l-8.59 8.59a2 2 0 0 1-2.83-2.83l8.49-8.48"/></svg>
                </button>
                <input
                    type="text"
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    className="message-input-field"
                    placeholder="메시지를 입력하세요..."
                    disabled={disabled}
                    autoComplete="off"
                />
                <button type="submit" className="send-button" disabled={disabled}>
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/></svg>
                </button>
            </form>
        </div>
    );
};


// --- 1:1 채팅 페이지 메인 컴포넌트 ---
export default function DirectChatPage() {
    const { chatRoomId } = useParams<{ chatRoomId: string }>();
    const location = useLocation();
    const navigate = useNavigate();

    // ▼▼▼ 로컬 테마 상태 제거, 전역 컨텍스트 사용 ▼▼▼
    const { isDarkMode } = useTheme();

    const [currentUser, setCurrentUser] = useState<User | null>(null);
    const [connectionStatus, setConnectionStatus] = useState<string>('연결 중...');
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    
    useEffect(() => {
        if (!chatRoomId) {
            alert('잘못된 접근입니다. 채팅방 정보가 없습니다.');
            navigate(-1);
        }
    }, [chatRoomId, navigate]);

    const roomName = location.state?.roomName || '채팅';
    const clientRef = useRef<any>(null);

    useEffect(() => {
        const token = localStorage.getItem('jwtToken');
        if (token) {
            try {
                const payload = JSON.parse(atob(token.split('.')[1]));
                if (!payload.userId || !payload.nickname) throw new Error("토큰에 필수 사용자 정보가 없습니다.");
                setCurrentUser({ id: payload.userId, nickname: payload.nickname, token: token });
            } catch (error) { navigate('/login'); }
        } else { navigate('/login'); }
    }, [navigate]);

    useEffect(() => {
        if (!currentUser || !chatRoomId) return;

        const client = Stomp.over(new SockJS('http://localhost:8080/ws'));
        client.debug = null;
        clientRef.current = client;

        const onConnect = () => {
            setConnectionStatus('연결됨');
            client.subscribe(`/topic/chat/room/${chatRoomId}`, (message: any) => {
                setMessages(prev => [...prev, JSON.parse(message.body)]);
            });
            const historySub = client.subscribe(`/user/queue/chat/history/${chatRoomId}`, (message: any) => {
                setMessages(JSON.parse(message.body).reverse());
                historySub.unsubscribe();
            });
            setTimeout(() => {
                if (client.connected) client.send(`/app/chat.history`, {}, JSON.stringify({ chatRoomId: Number(chatRoomId) }));
            }, 100);
        };

        const onError = (error: any) => {
            console.error('STOMP 연결 오류:', error);
            setConnectionStatus('연결 오류');
        };

        client.connect({ 'Authorization': `Bearer ${currentUser.token}` }, onConnect, onError);

        return () => { if (clientRef.current?.connected) clientRef.current.disconnect(); };
    }, [currentUser, chatRoomId]);


    const handleSendMessage = (content: string, type: 'TEXT' | 'IMAGE' = 'TEXT') => {
        if (clientRef.current?.connected && chatRoomId) {
            clientRef.current.send('/app/chat.sendMessage', {}, JSON.stringify({
                chatRoomId: Number(chatRoomId), content, messageType: type
            }));
        }
    };

    if (!currentUser) {
        return <div className="flex items-center justify-center h-screen">사용자 정보를 확인 중입니다...</div>;
    }

    return (
        // ▼▼▼ isDarkMode 상태에 따라 클래스 동적 적용 ▼▼▼
        <div className={`chat-page-container ${isDarkMode ? 'dark' : 'light'}`}>
            <header className="chat-header">
                <button onClick={() => navigate(-1)} className="back-button">
                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M15 18l-6-6 6-6"/></svg>
                </button>
                <span className="room-name">{roomName}</span>
                {/* 헤더에 있던 로컬 테마 토글 버튼은 제거 (전역 헤더에서 관리) */}
            </header>

            <ChatWindow messages={messages} currentUser={currentUser} />
            <MessageInput 
                onSendMessage={handleSendMessage} 
                disabled={connectionStatus !== '연결됨'}
                currentUser={currentUser}
            />
        </div>
    );
}
