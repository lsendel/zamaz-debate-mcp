import { useEffect, useRef } from 'react';
import { useWorkflowStore } from '../store/workflowStore';

interface TelemetryMessage {
  type: 'telemetry' | 'status' | 'alert';
  source: string;
  data: any;
  timestamp: number;
}

export class TelemetryWebSocketService {
  private ws: WebSocket | null = null;
  private reconnectInterval: NodeJS.Timeout | null = null;
  private reconnectAttempts = 0;
  private readonly maxReconnectAttempts = 5;
  private readonly baseReconnectDelay = 1000;
  private readonly subscribers: Map<string, Set<(data: any) => void>> = new Map();

  constructor(private readonly wsUrl: string) {}

  connect() {
    if (this.ws?.readyState === WebSocket.OPEN) {
      return;
    }

    try {
      this.ws = new WebSocket(this.wsUrl);
      
      this.ws.onopen = () => {
        console.log('Telemetry WebSocket connected');
        this.reconnectAttempts = 0;
        if (this.reconnectInterval) {
          clearInterval(this.reconnectInterval);
          this.reconnectInterval = null;
        }
        
        // Send authentication or subscription messages if needed
        this.sendMessage({
          type: 'subscribe',
          topics: ['telemetry.*', 'workflow.status.*']
        });
      };

      this.ws.onmessage = (event) => {
        try {
          const message: TelemetryMessage = JSON.parse(event.data);
          this.handleMessage(message);
        } catch (error) {
          console.error('Failed to parse telemetry message:', error);
        }
      };

      this.ws.onerror = (error) => {
        console.error('Telemetry WebSocket error:', error);
      };

      this.ws.onclose = () => {
        console.log('Telemetry WebSocket disconnected');
        this.attemptReconnect();
      };
    } catch (error) {
      console.error('Failed to create WebSocket connection:', error);
      this.attemptReconnect();
    }
  }

  private attemptReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached');
      return;
    }

    const delay = this.baseReconnectDelay * Math.pow(2, this.reconnectAttempts);
    console.log(`Attempting to reconnect in ${delay}ms...`);

    setTimeout(() => {
      this.reconnectAttempts++;
      this.connect();
    }, delay);
  }

  private handleMessage(message: TelemetryMessage) {
    const { addTelemetryData } = useWorkflowStore.getState();
    
    switch (message.type) {
      case 'telemetry':
        addTelemetryData(message.source, {
          timestamp: message.timestamp,
          value: message.data.value,
          deviceId: message.data.deviceId,
          metadata: message.data.metadata
        });
        break;
        
      case 'alert':
        console.warn('Telemetry alert:', message);
        break;
        
      case 'status':
        console.log('Telemetry status update:', message);
        break;
    }

    // Notify subscribers
    const subscribers = this.subscribers.get(message.source);
    if (subscribers) {
      subscribers.forEach(callback => callback(message.data));
    }
  }

  subscribe(source: string, callback: (data: any) => void) {
    if (!this.subscribers.has(source)) {
      this.subscribers.set(source, new Set());
    }
    this.subscribers.get(source)!.add(callback);

    // Return unsubscribe function
    return () => {
      const subscribers = this.subscribers.get(source);
      if (subscribers) {
        subscribers.delete(callback);
        if (subscribers.size === 0) {
          this.subscribers.delete(source);
        }
      }
    };
  }

  sendMessage(message: any) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      console.warn('WebSocket is not connected, queuing message');
      // Could implement a message queue here
    }
  }

  disconnect() {
    if (this.reconnectInterval) {
      clearInterval(this.reconnectInterval);
      this.reconnectInterval = null;
    }
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }
}

// React hook for using the WebSocket service
export const useTelemetryWebSocket = (wsUrl: string, enabled = true) => {
  const serviceRef = useRef<TelemetryWebSocketService | null>(null);

  useEffect(() => {
    if (!enabled) return;

    serviceRef.current = new TelemetryWebSocketService(wsUrl);
    serviceRef.current.connect();

    return () => {
      serviceRef.current?.disconnect();
    };
  }, [wsUrl, enabled]);

  return serviceRef.current;
};

// Default WebSocket URL (should be configured via environment variable)
const DEFAULT_WS_URL = process.env.REACT_APP_TELEMETRY_WS_URL || 'ws://localhost:5004/telemetry/ws';

// Singleton instance for the application
let telemetryService: TelemetryWebSocketService | null = null;

export const getTelemetryService = () => {
  if (!telemetryService) {
    telemetryService = new TelemetryWebSocketService(DEFAULT_WS_URL);
  }
  return telemetryService;
};