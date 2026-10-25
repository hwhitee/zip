import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { SafetyEvent } from '../types/wardsentinel';

export type ConnectionStatus = 'CONNECTING' | 'CONNECTED' | 'RECONNECTING' | 'DISCONNECTED';

type EventHandler = (event: SafetyEvent) => void;
type RiskHandler = (riskPayload: any) => void;
type StatusHandler = (status: ConnectionStatus) => void;

class WebSocketService {
  private client: Client | null = null;
  private eventHandlers: Set<EventHandler> = new Set();
  private riskHandlers: Set<RiskHandler> = new Set();
  private statusHandlers: Set<StatusHandler> = new Set();
  private currentStatus: ConnectionStatus = 'DISCONNECTED';
  private processedEventIds: Set<string> = new Set();
  private wardId: string = 'WARD-4B';

  constructor() {
    this.processedEventIds = new Set();
  }

  public connect(wardId = 'WARD-4B') {
    this.wardId = wardId;
    this.updateStatus('CONNECTING');

    const baseUrl = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '');
    const wsUrl = baseUrl ? `${baseUrl}/ws` : '/ws';

    this.client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        this.updateStatus('CONNECTED');
        this.subscribeToTopics();
      },
      onDisconnect: () => {
        this.updateStatus('DISCONNECTED');
      },
      onStompError: (frame: any) => {
        console.warn('STOMP protocol error:', frame?.headers?.['message']);
        this.updateStatus('RECONNECTING');
      },
      onWebSocketClose: () => {
        this.updateStatus('RECONNECTING');
      },
    });

    try {
      this.client.activate();
    } catch (e) {
      console.warn('Failed to activate STOMP client:', e);
      this.updateStatus('DISCONNECTED');
    }
  }

  private subscribeToTopics() {
    if (!this.client || !this.client.connected) return;

    // Ward alerts topic
    this.client.subscribe(`/topic/ward/${this.wardId}/alerts`, (message: IMessage) => {
      try {
        const event: SafetyEvent = JSON.parse(message.body);
        // Duplicate suppression: ignore identical event timestamp echoes
        const eventFingerprint = `${event.eventId}-${event.status}-${event.updatedAt || event.detectedAt}`;
        if (!this.processedEventIds.has(eventFingerprint)) {
          this.processedEventIds.add(eventFingerprint);
          this.eventHandlers.forEach((handler) => handler(event));
        }
      } catch (err) {
        console.error('Failed to parse incoming alert payload:', err);
      }
    });

    // Ward risk scores topic
    this.client.subscribe(`/topic/ward/${this.wardId}/risk`, (message: IMessage) => {
      try {
        const riskData = JSON.parse(message.body);
        this.riskHandlers.forEach((handler) => handler(riskData));
      } catch (err) {
        console.error('Failed to parse incoming risk payload:', err);
      }
    });
  }

  public onEvent(handler: EventHandler): () => void {
    this.eventHandlers.add(handler);
    return () => this.eventHandlers.delete(handler);
  }

  public onRisk(handler: RiskHandler): () => void {
    this.riskHandlers.add(handler);
    return () => this.riskHandlers.delete(handler);
  }

  public onStatus(handler: StatusHandler): () => void {
    this.statusHandlers.add(handler);
    handler(this.currentStatus);
    return () => this.statusHandlers.delete(handler);
  }

  private updateStatus(status: ConnectionStatus) {
    this.currentStatus = status;
    this.statusHandlers.forEach((handler) => handler(status));
  }

  public disconnect() {
    if (this.client) {
      try {
        this.client.deactivate();
      } catch {}
      this.client = null;
    }
    this.updateStatus('DISCONNECTED');
  }
}

export const webSocketService = new WebSocketService();
