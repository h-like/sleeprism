import React, { createContext, useContext, useState, useRef, type ReactNode, useCallback, useEffect } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WEBSOCKET_URL = 'http://localhost:8080/ws';

// 1. 웹소켓 컨텍스트 타입 정의
interface WebSocketContextType {
  stompClient: Client | null;
  isConnected: boolean;
  connect: (token: string) => void;
  disconnect: () => void;
}

// 2. 컨텍스트 생성
const WebSocketContext = createContext<WebSocketContextType | null>(null);

// 3. 다른 컴포넌트에서 컨텍스트를 쉽게 사용하기 위한 커스텀 훅
export const useWebSocket = () => {
  const context = useContext(WebSocketContext);
  if (!context) {
    // 이 에러는 Provider로 감싸지지 않았을 때 발생합니다.
    throw new Error('useWebSocket must be used within a WebSocketProvider');
  }
  return context;
};

// 4. 앱 전체를 감싸는 Provider 컴포넌트
export const WebSocketProvider = ({ children }: { children: ReactNode }) => {
  const [isConnected, setIsConnected] = useState(false);
  const [stompClient, setStompClient] = useState<Client | null>(null);
  const clientRef = useRef<Client | null>(null);

  const disconnect = useCallback(() => {
    if (clientRef.current?.active) {
      console.log("WebSocketProvider: Disconnecting WebSocket.");
      clientRef.current.deactivate();
      clientRef.current = null;
      setIsConnected(false);
      setStompClient(null);
    }
  }, []);

  const connect = useCallback((token: string) => {
    if (clientRef.current?.active) {
      console.log("WebSocketProvider: Already connected.");
      return;
    }
    
    console.log("WebSocketProvider: Attempting to connect...");
    
    const client = new Client({
      webSocketFactory: () => new SockJS(WEBSOCKET_URL),
      connectHeaders: { Authorization: `Bearer ${token}` },
      debug: (str) => console.log(`STOMP DEBUG: ${str}`),
      reconnectDelay: 5000,
    });

    client.onConnect = (frame) => {
      console.log('<<<<< WebSocketProvider: Connected! >>>>>', frame);
      clientRef.current = client;
      setStompClient(client);
      setIsConnected(true);
    };

    client.onStompError = (frame) => {
      console.error('STOMP Error:', frame.headers['message'], frame.body);
      setIsConnected(false);
    };

    client.onWebSocketClose = (event) => {
        console.warn('WebSocket connection closed.', event);
        setIsConnected(false);
    };
    
    client.onWebSocketError = (error) => {
        console.error('WebSocket Error', error);
    };

    client.activate();
  }, []);

  useEffect(() => {
    // 앱이 종료될 때 연결을 확실히 해제하기 위한 cleanup
    return () => {
      if (clientRef.current?.active) {
        clientRef.current.deactivate();
      }
    };
  }, []);

  const value = { stompClient, isConnected, connect, disconnect };

  return (
    <WebSocketContext.Provider value={value}>
      {children}
    </WebSocketContext.Provider>
  );
};
