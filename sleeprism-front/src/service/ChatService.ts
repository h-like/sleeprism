import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
// import { getUserIdFromToken } from './authUtils'; // authUtils에서 getUserIdFromToken 임포트

// 백엔드 WebSocket 엔드포인트 URL
// 이 URL이 브라우저 개발자 도구의 Network 탭에서 Request URL과 일치해야 합니다.
const WEBSOCKET_URL = 'http://localhost:8080/ws'; 

let stompClient: Client | null = null;
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 5;
const RECONNECT_DELAY_MS = 3000;

export const connectWebSocket = (
    onConnectCallback: (frame: any) => void,
    onMessageReceivedCallback: (message: IMessage) => void,
    onErrorCallback: (error: string) => void,
    userId: number | null // 현재 로그인한 사용자 ID
) => {
    if (stompClient && stompClient.active) {
        console.log('WebSocket is already connected.');
        onConnectCallback('Already connected');
        return;
    }

    const token = localStorage.getItem('jwtToken'); // JWT 토큰 가져오기

    stompClient = new Client({
        webSocketFactory: () => {
            // SockJS 연결 시도 시점에 실제 URL을 로깅하여 확인
            console.log("SockJS will attempt to connect to:", WEBSOCKET_URL);
            return new SockJS(WEBSOCKET_URL);
        },
        debug: (str) => {
            console.log('STOMP Debug: ', str);
        },
        reconnectDelay: RECONNECT_DELAY_MS,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        // WebSocket 연결 시 STOMP CONNECT 프레임에 Authorization 헤더 추가
        connectHeaders: token ? { 'Authorization': `Bearer ${token}` } : {},
    });

    stompClient.onConnect = (frame) => {
        console.log('Connected to WebSocket STOMP:', frame);
        reconnectAttempts = 0;

        if (userId) {
            // 개인 큐 구독 (에러 메시지, 개인 알림 등)
            stompClient?.subscribe(`/user/${userId}/queue/errors`, (message) => {
                console.error("Personal Error Queue Message:", message.body);
                onErrorCallback(message.body);
            });
            console.log(`Subscribed to personal error queue: /user/${userId}/queue/errors`);

            // (선택 사항) 새로운 채팅방이 생성되거나, 내가 초대되었을 때 알림을 받을 수 있는 전역 큐 구독
            stompClient?.subscribe(`/user/${userId}/queue/notifications`, (message: IMessage) => {
                console.log("New notification received:", message.body);
                // 여기에 알림 처리 로직 또는 채팅방 목록 새로고침 로직 추가
            });
        }

        onConnectCallback(frame);
    };

    stompClient.onStompError = (frame) => {
        console.error('STOMP Error:', frame);
        onErrorCallback(`STOMP 오류: ${frame.headers['message'] || frame.body}`);
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            console.log(`Reconnecting... attempt ${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS}`);
        } else {
            console.error('Max reconnect attempts reached. Please refresh the page.');
            onErrorCallback('최대 재연결 시도 횟수를 초과했습니다. 페이지를 새로고침해주세요.');
        }
    };

    stompClient.onDisconnect = () => {
        console.log('Disconnected from WebSocket STOMP.');
    };

    stompClient.activate();
};

export const disconnectWebSocket = () => {
    if (stompClient && stompClient.active) {
        stompClient.deactivate();
        stompClient = null;
        console.log('WebSocket disconnected.');
    }
};

export const sendMessage = (destination: string, body: any) => {
    if (stompClient && stompClient.active) {
        stompClient.publish({
            destination: destination,
            body: JSON.stringify(body),
            headers: { 'content-type': 'application/json' },
        });
    } else {
        console.warn('WebSocket is not connected. Message not sent:', body);
        // 연결이 끊어졌을 때 사용자에게 알림을 주는 것이 좋습니다.
        alert('채팅 서버와의 연결이 끊어졌습니다. 페이지를 새로고침하거나 다시 시도해주세요.');
    }
};

export const subscribeToTopic = (destination: string, callback: (message: IMessage) => void) => {
    if (stompClient && stompClient.active) {
        console.log(`Subscribing to topic: ${destination}`);
        const subscription = stompClient.subscribe(destination, callback);
        return () => {
            console.log(`Unsubscribing from topic: ${destination}`);
            subscription.unsubscribe();
        };
    } else {
        console.warn('WebSocket is not connected. Cannot subscribe to topic.');
        return () => {};
    }
};

export const isWebSocketConnected = (): boolean => {
    return stompClient !== null && stompClient.active;
};

// 백엔드 기본 URL 및 컨텍스트 경로
const BACKEND_BASE_URL = 'http://localhost:8080';
const CHATS_API_BASE_URL = `${BACKEND_BASE_URL}/api/chats`; // 채팅 API 엔드포인트
const CHAT_BLOCKS_API_BASE_URL = `${BACKEND_BASE_URL}/api/chats/blocks`; // 채팅 차단 API 엔드포인트

interface ChatRoomCreateRequest {
    type: 'SINGLE' | 'GROUP';
    name?: string; // 그룹 채팅방용
    participantUserIds: number[]; // 1:1 채팅은 상대방 ID 1개, 그룹 채팅은 여러 개
}

interface ChatRoomResponse {
    id: number;
    type: 'SINGLE' | 'GROUP';
    name: string;
    creatorId?: number;
    creatorNickname?: string;
    createdAt: string;
    updatedAt: string;
    participants: ChatParticipantResponse[];
    lastMessage?: ChatMessageResponse;
}

interface ChatParticipantResponse {
    id: number;
    userId: number;
    userNickname: string;
    chatRoomId: number;
    joinedAt: string;
    isLeft: boolean;
}

interface ChatMessageResponse {
    id: number;
    chatRoomId: number;
    senderId: number;
    senderNickname: string;
    content: string;
    sentAt: string;
    isRead: boolean;
}

interface ChatBlockRequest {
    blockedUserId: number;
}

interface ChatBlockResponse {
    id: number;
    blockerId: number;
    blockerNickname: string;
    blockedId: number;
    blockedNickname: string;
    createdAt: string;
}

// JWT 토큰 가져오는 헬퍼 함수 (기존 PostDetailPage와 동일)
const getAuthHeaders = () => {
    const token = localStorage.getItem('jwtToken');
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
};

/**
 * 1대1 채팅방을 생성하거나 기존 채팅방을 가져옵니다.
 * @param otherUserId 상대방 사용자 ID
 * @returns ChatRoomResponse
 */
export const createOrGetSingleChatRoom = async (otherUserId: number): Promise<ChatRoomResponse> => {
    const response = await fetch(`${CHATS_API_BASE_URL}/single`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ type: 'SINGLE', participantUserIds: [otherUserId] }),
    });
    if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || '1대1 채팅방 생성/조회에 실패했습니다.');
    }
    return response.json();
};

/**
 * 그룹 채팅방을 생성합니다.
 * @param name 채팅방 이름
 * @param participantUserIds 초대할 사용자 ID 목록
 * @returns ChatRoomResponse
 */
export const createGroupChatRoom = async (name: string, participantUserIds: number[]): Promise<ChatRoomResponse> => {
    const response = await fetch(`${CHATS_API_BASE_URL}/group`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ type: 'GROUP', name, participantUserIds }),
    });
    if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || '그룹 채팅방 생성에 실패했습니다.');
    }
    return response.json();
};

/**
 * 현재 사용자의 모든 채팅방 목록을 조회합니다.
 * @returns ChatRoomResponse[]
 */
export const getUserChatRooms = async (): Promise<ChatRoomResponse[]> => {
    const response = await fetch(CHATS_API_BASE_URL, {
        method: 'GET',
        headers: getAuthHeaders(),
    });
    if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || '채팅방 목록을 불러오는데 실패했습니다.');
    }
    return response.json();
};

/**
 * 특정 채팅방의 메시지 내역을 조회합니다.
 * @param chatRoomId 채팅방 ID
 * @param page 페이지 번호 (0부터 시작)
 * @param size 페이지당 메시지 수
 * @returns ChatMessageResponse[]
 */
export const getChatHistory = async (chatRoomId: number, page: number = 0, size: number = 50): Promise<ChatMessageResponse[]> => {
    const response = await fetch(`${CHATS_API_BASE_URL}/${chatRoomId}/messages?page=${page}&size=${size}`, {
        method: 'GET',
        headers: getAuthHeaders(),
    });
    if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || '채팅 메시지 내역을 불러오는데 실패했습니다.');
    }
    return response.json();
};

/**
 * 사용자를 차단합니다.
 * @param blockedUserId 차단할 사용자 ID
 * @returns ChatBlockResponse
 */
export const blockUser = async (blockedUserId: number): Promise<ChatBlockResponse> => {
    const response = await fetch(CHAT_BLOCKS_API_BASE_URL, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ blockedUserId }),
    });
    if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || '사용자 차단에 실패했습니다.');
    }
    return response.json();
};

/**
 * 사용자 차단을 해제합니다.
 * @param blockedUserId 차단 해제할 사용자 ID
 */
export const unblockUser = async (blockedUserId: number): Promise<void> => {
    const response = await fetch(`${CHAT_BLOCKS_API_BASE_URL}/${blockedUserId}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
    });
    if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || '사용자 차단 해제에 실패했습니다.');
    }
};

/**
 * 현재 사용자가 차단한 사용자 목록을 조회합니다.
 * @returns ChatBlockResponse[]
 */
export const getBlockedUsers = async (): Promise<ChatBlockResponse[]> => {
    const response = await fetch(CHAT_BLOCKS_API_BASE_URL, {
        method: 'GET',
        headers: getAuthHeaders(),
    });
    if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || '차단된 사용자 목록을 불러오는데 실패했습니다.');
    }
    return response.json();
};

// ChatWindow 및 ChatRoomList에서 사용할 수 있도록 타입 내보내기
export type { ChatRoomResponse, ChatMessageResponse, ChatParticipantResponse, ChatBlockResponse };
