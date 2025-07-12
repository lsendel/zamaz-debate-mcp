'use client';

import React, { createContext, useContext, useEffect } from 'react';
import { useDebateWebSocket } from '@/hooks/use-websocket';
import { useOrganization } from '@/hooks/use-organization';

interface WebSocketContextType {
  isConnected: boolean;
  isReconnecting: boolean;
  sendMessage: (message: any) => boolean;
}

const WebSocketContext = createContext<WebSocketContextType | undefined>(undefined);

export function WebSocketProvider({ children }: { children: React.ReactNode }) {
  const { currentOrganization } = useOrganization();
  const { isConnected, isReconnecting, sendMessage } = useDebateWebSocket();

  // Subscribe to organization updates when connected
  useEffect(() => {
    if (isConnected && currentOrganization) {
      sendMessage({
        type: 'subscribe',
        payload: {
          organizationId: currentOrganization.id,
          events: ['debate_updates', 'organization_updates']
        }
      });
    }
  }, [isConnected, currentOrganization, sendMessage]);

  return (
    <WebSocketContext.Provider value={{ isConnected, isReconnecting, sendMessage }}>
      {children}
    </WebSocketContext.Provider>
  );
}

export function useWebSocketContext() {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error('useWebSocketContext must be used within WebSocketProvider');
  }
  return context;
}