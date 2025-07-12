'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import { useNotifications } from '@/contexts/notification-context';
import { logger } from '@/lib/logger';

export interface WebSocketMessage {
  type: string;
  payload: any;
  timestamp: string;
}

interface UseWebSocketOptions {
  url: string;
  enabled?: boolean;
  reconnectDelay?: number;
  maxReconnectAttempts?: number;
  onMessage?: (message: WebSocketMessage) => void;
  onOpen?: () => void;
  onClose?: () => void;
  onError?: (error: Event) => void;
}

export function useWebSocket({
  url,
  enabled = true,
  reconnectDelay = 3000,
  maxReconnectAttempts = 5,
  onMessage,
  onOpen,
  onClose,
  onError
}: UseWebSocketOptions) {
  const ws = useRef<WebSocket | null>(null);
  const reconnectCount = useRef(0);
  const reconnectTimeout = useRef<NodeJS.Timeout | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [isReconnecting, setIsReconnecting] = useState(false);
  const { addNotification } = useNotifications();

  const connect = useCallback(() => {
    if (!enabled || ws.current?.readyState === WebSocket.OPEN) return;

    try {
      ws.current = new WebSocket(url);

      ws.current.onopen = () => {
        logger.info('WebSocket connected', { url });
        setIsConnected(true);
        setIsReconnecting(false);
        reconnectCount.current = 0;
        onOpen?.();
        
        addNotification({
          type: 'success',
          title: 'Connected',
          message: 'Real-time updates are now active'
        });
      };

      ws.current.onmessage = (event) => {
        try {
          const message: WebSocketMessage = JSON.parse(event.data);
          onMessage?.(message);
          
          // Handle specific message types
          handleWebSocketMessage(message);
        } catch (error) {
          logger.error('Failed to parse WebSocket message', error as Error, { 
            eventData: event.data 
          });
        }
      };

      ws.current.onclose = () => {
        logger.info('WebSocket disconnected', { 
          reconnectCount: reconnectCount.current,
          maxReconnectAttempts 
        });
        setIsConnected(false);
        onClose?.();
        
        // Attempt reconnection
        if (enabled && reconnectCount.current < maxReconnectAttempts) {
          setIsReconnecting(true);
          reconnectTimeout.current = setTimeout(() => {
            reconnectCount.current++;
            connect();
          }, reconnectDelay);
        } else if (reconnectCount.current >= maxReconnectAttempts) {
          addNotification({
            type: 'error',
            title: 'Connection Lost',
            message: 'Unable to establish real-time connection. Please refresh the page.'
          });
        }
      };

      ws.current.onerror = (error) => {
        logger.error('WebSocket error', error as Error);
        onError?.(error);
      };
    } catch (error) {
      logger.error('Failed to create WebSocket', error as Error, { url });
    }
  }, [url, enabled, reconnectDelay, maxReconnectAttempts, onMessage, onOpen, onClose, onError, addNotification]);

  const disconnect = useCallback(() => {
    if (reconnectTimeout.current) {
      clearTimeout(reconnectTimeout.current);
      reconnectTimeout.current = null;
    }
    
    if (ws.current) {
      ws.current.close();
      ws.current = null;
    }
    
    setIsConnected(false);
    setIsReconnecting(false);
  }, []);

  const sendMessage = useCallback((message: any) => {
    if (ws.current?.readyState === WebSocket.OPEN) {
      ws.current.send(JSON.stringify(message));
      return true;
    }
    return false;
  }, []);

  const handleWebSocketMessage = useCallback((message: WebSocketMessage) => {
    switch (message.type) {
      case 'debate_started':
        addNotification({
          type: 'debate',
          title: 'Debate Started',
          message: `"${message.payload.topic}" has begun`,
          action: {
            label: 'View Debate',
            onClick: () => {
              // Navigate to debate
              window.location.href = `/debate/${message.payload.debateId}`;
            }
          },
          metadata: { debateId: message.payload.debateId }
        });
        break;
        
      case 'turn_added':
        addNotification({
          type: 'info',
          title: 'New Turn',
          message: `${message.payload.participantName} has responded`,
          metadata: { debateId: message.payload.debateId }
        });
        break;
        
      case 'debate_completed':
        addNotification({
          type: 'success',
          title: 'Debate Completed',
          message: `"${message.payload.topic}" has finished`,
          action: {
            label: 'View Summary',
            onClick: () => {
              window.location.href = `/debate/${message.payload.debateId}/summary`;
            }
          },
          metadata: { debateId: message.payload.debateId }
        });
        break;
        
      case 'participant_joined':
        addNotification({
          type: 'info',
          title: 'Participant Joined',
          message: `${message.payload.participantName} joined the debate`
        });
        break;
        
      case 'error':
        addNotification({
          type: 'error',
          title: 'Error',
          message: message.payload.message
        });
        break;
    }
  }, [addNotification]);

  useEffect(() => {
    connect();
    return () => {
      disconnect();
    };
  }, [connect, disconnect]);

  return {
    isConnected,
    isReconnecting,
    sendMessage,
    disconnect,
    reconnect: connect
  };
}

// Global WebSocket hook for debate updates
export function useDebateWebSocket(debateId?: string) {
  const wsUrl = process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:5013/ws';
  const url = debateId ? `${wsUrl}?debate_id=${debateId}` : wsUrl;
  
  return useWebSocket({
    url,
    enabled: true,
    onMessage: (message) => {
      logger.debug('Debate WebSocket message received', { 
        type: message.type, 
        debateId 
      });
    }
  });
}