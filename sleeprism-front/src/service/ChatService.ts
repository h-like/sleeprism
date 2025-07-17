    import { Client, StompHeaders, type IMessage } from '@stomp/stompjs';
    import SockJS from 'sockjs-client'; // SockJS 임포트

    // 백엔드 WebSocket 엔드포인트 URL (SockJS 사용)
    const WEBSOCKET_URL = 'http://localhost:8080/ws'; // <-- /ws 로 변경

    let stompClient: Client | null = null;
    let reconnectAttempts = 0;
    const MAX_RECONNECT_ATTEMPTS = 5;
    const RECONNECT_DELAY_MS = 3000;

    export const connectWebSocket = (
        onConnectCallback: (frame: any) => void,
        onMessageReceivedCallback: (message: IMessage) => void,
        onErrorCallback: (error: string) => void,
        userId: number | null
    ): Promise<void> => {
        return new Promise((resolve, reject) => {
            if (stompClient && stompClient.active) {
                console.log('STOMP Debug: WebSocket is already connected.');
                onConnectCallback('Already connected');
                resolve();
                return;
            }

            const token = localStorage.getItem('jwtToken');
            const connectHeaders: StompHeaders = {};

            if (token) {
                connectHeaders['Authorization'] = `Bearer ${token}`;
                console.log("STOMP Debug: Preparing CONNECT frame with Authorization header.");
            } else {
                console.warn("STOMP Debug: No JWT token found in localStorage. Connection will likely fail authentication.");
                onErrorCallback('오류: JWT 토큰을 찾을 수 없습니다. 연결 인증에 실패할 수 있습니다.');
                reject(new Error('JWT 토큰을 찾을 수 없습니다.'));
                return;
            }

            stompClient = new Client({
                // SockJS를 사용하여 WebSocket 연결을 설정합니다.
                webSocketFactory: () => {
                    console.log("SockJS will attempt to connect to:", WEBSOCKET_URL);
                    return new SockJS(WEBSOCKET_URL); // <-- SockJS 사용
                },
                debug: (str) => {
                    console.log('STOMP Debug: ', str);
                },
                reconnectDelay: RECONNECT_DELAY_MS,
                heartbeatIncoming: 10000,
                heartbeatOutgoing: 10000,
                connectHeaders: connectHeaders,

                onConnect: (frame) => {
                    console.log('STOMP Connect: Successfully connected!', frame);
                    reconnectAttempts = 0;

                    if (userId) {
                        const subscribeHeaders: StompHeaders = {};
                        if (token) {
                            subscribeHeaders['Authorization'] = `Bearer ${token}`;
                        }

                        stompClient?.subscribe(`/user/${userId}/queue/errors`, (message) => {
                            console.error("STOMP Personal Error Queue Message:", message.body);
                            onErrorCallback(message.body);
                        }, subscribeHeaders);
                        console.log(`STOMP Debug: Subscribed to personal error queue: /user/${userId}/queue/errors`);

                        stompClient?.subscribe(`/user/${userId}/queue/notifications`, (message: IMessage) => {
                            console.log("STOMP New notification received:", message.body);
                        }, subscribeHeaders);
                    }

                    onConnectCallback(frame);
                    resolve();
                },
                onStompError: (frame) => {
                    console.error('STOMP Error: Broker reported error: ' + frame.headers['message']);
                    console.error('STOMP Error: Additional details: ' + frame.body);
                    onErrorCallback(`STOMP 오류: ${frame.headers['message'] || frame.body || '알 수 없는 STOMP 오류'}`);
                    reject(new Error(`STOMP Error: ${frame.headers['message']}`));
                },
                onDisconnect: (frame) => {
                    console.log('STOMP Debug: Disconnected: ' + frame);
                    onErrorCallback('WebSocket 연결이 끊어졌습니다.');
                    if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                        reconnectAttempts++;
                        console.warn(`STOMP Debug: Reconnecting... attempt ${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS}`);
                        setTimeout(() => {
                            console.log('STOMP Debug: Attempting to reconnect WebSocket...');
                            stompClient = null;
                            connectWebSocket(onConnectCallback, onMessageReceivedCallback, onErrorCallback, userId).then(resolve).catch(reject);
                        }, RECONNECT_DELAY_MS);
                    } else {
                        console.error('STOMP Error: Max reconnect attempts reached. WebSocket connection failed.');
                        onErrorCallback('최대 재연결 시도 횟수를 초과했습니다. 페이지를 새로고침해주세요.');
                        reject(new Error('최대 재연결 시도 횟수 초과'));
                    }
                },
            });

            stompClient.activate();
            console.log('STOMP Debug: stompClient.activate() called.');
        });
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
            // alert('채팅 서버와의 연결이 끊어졌습니다. 페이지를 새로고침하거나 다시 시도해주세요.'); // alert 제거
            console.error('STOMP Error: Please refresh the page or try again.');
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
    const CHATS_API_BASE_URL = `${BACKEND_BASE_URL}/api/chats`;
    const CHAT_BLOCKS_API_BASE_URL = `${BACKEND_BASE_URL}/api/chats/blocks`;

    interface ChatRoomCreateRequest {
        type: 'SINGLE' | 'GROUP';
        name?: string;
        participantUserIds: number[];
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

    const getAuthHeaders = () => {
        const token = localStorage.getItem('jwtToken');
        const headers: Record<string, string> = { 'Content-Type': 'application/json' };
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
            console.log("getAuthHeaders: JWT Token found and added to Authorization header.");
        } else {
            console.warn("getAuthHeaders: No JWT Token found in localStorage.");
        }
        return headers;
    };

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

    export type { ChatRoomResponse, ChatMessageResponse, ChatParticipantResponse, ChatBlockResponse };
    