import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getUserIdFromToken } from './authUtils'; // authUtils에서 getUserIdFromToken 임포트

// 백엔드 WebSocket 엔드포인트 URL
// 백엔드 server.servlet.context-path=/sleeprism 설정이 없다면 http://localhost:8080/ws
// 백엔드 server.servlet.context-path=/sleeprism 설정이 있다면 http://localhost:8080/sleeprism/ws
// 현재 백엔드 로그와 이전 성공 로그를 기반으로, /sleeprism 컨텍스트 경로가 없는 것으로 가정하고 'http://localhost:8080/ws' 사용.
const WEBSOCKET_URL = 'http://localhost:8080/ws'; 

let stompClient: Client | null = null;
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 5;
const RECONNECT_DELAY_MS = 3000;

export const connectWebSocket = (
    onConnectCallback: (frame: any) => void,
    onMessageReceivedCallback: (message: IMessage) => void, // 이 콜백은 필요하지 않을 수 있음 (subscribeToTopic에서 개별 처리)
    onErrorCallback: (error: string) => void,
    userId: number | null // 현재 로그인한 사용자 ID
) => {
    if (stompClient && stompClient.active) {
        console.log('WebSocket is already connected.');
        onConnectCallback('Already connected');
        return;
    }

    // 기존 연결이 있다면 정리 (확실하게 재연결을 위해)
    if (stompClient) {
        stompClient.deactivate();
        stompClient = null;
        console.log('Previous WebSocket client deactivated.');
    }

    const token = localStorage.getItem('jwtToken'); // JWT 토큰 가져오기

    stompClient = new Client({
        webSocketFactory: () => {
            // SockJS가 실제로 연결할 URL을 명확히 로깅합니다.
            console.log("SockJS webSocketFactory will connect to:", WEBSOCKET_URL);
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
