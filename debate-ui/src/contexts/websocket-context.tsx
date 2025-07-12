'use client';

import React, { createContext, useContext, useEffect } from 'react';
import { useDebateWebSocket } from '@/hooks/use-websocket';
import { useOrganization } from '@/hooks/use-organization';

interface WebSocketContextType {
  isConnected: boolean;
  isReconnecting: boolean;
  sendMessage: (_message: any) => boolean;
}

const WebSocketContext = createContext<WebSocketContextType | undefined>(undefined);

export function WebSocketProvider({ children }: { children: React.ReactNode }) {
  const { currentOrg } = useOrganization();
  const { isConnected, isReconnecting, sendMessage } = useDebateWebSocket();

  // Subscribe to organization updates when connected
  useEffect(() => {
    if (isConnected && currentOrg) {
      sendMessage({
        type: 'subscribe',
        payload: {
          organizationId: currentOrg.id,
          events: ['debate_updates', 'organization_updates']
        }
      });
    }
  }, [isConnected, currentOrg, sendMessage]);

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