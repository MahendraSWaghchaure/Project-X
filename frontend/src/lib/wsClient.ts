import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from './authStore';

const WS_BASE_URL = import.meta.env.VITE_WS_BASE_URL ?? '';

/**
 * A native WebSocket can't carry an Authorization header, so the backend (see
 * WebSocketAuthChannelInterceptor) requires the token on the STOMP CONNECT frame itself instead.
 * beforeConnect re-reads the token from the store on every (re)connect attempt, not just once at
 * client creation, so a token refresh mid-session doesn't leave a stale header on reconnects.
 */
export function createStompClient(): Client {
  const client = new Client({
    webSocketFactory: () => new SockJS(`${WS_BASE_URL}/ws`),
    reconnectDelay: 5000,
    beforeConnect: () => {
      client.connectHeaders = { Authorization: `Bearer ${useAuthStore.getState().accessToken ?? ''}` };
    },
  });
  return client;
}
