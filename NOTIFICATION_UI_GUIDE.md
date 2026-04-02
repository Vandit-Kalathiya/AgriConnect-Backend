# AgriConnect — Notification System: Complete UI Integration Guide

> **For:** Frontend / UI Team  
> **Backend:** Fully implemented and running  
> **Notification-Service Port:** `2530`  
> **API Gateway Port (REST):** `8080`  
> **Ws-Gateway Port (WebSocket):** `8081`  
> **Last Updated:** April 2026

---

## Table of Contents

1. [System Architecture](#1-system-architecture)
2. [How Notifications Flow](#2-how-notifications-flow)
3. [Environment Setup](#3-environment-setup)
4. [Install Dependencies](#4-install-dependencies)
5. [TypeScript Types](#5-typescript-types)
6. [API Service Layer](#6-api-service-layer)
7. [WebSocket Service (Real-Time)](#7-websocket-service-real-time)
8. [State Management (Zustand)](#8-state-management-zustand)
9. [UI Components](#9-ui-components)
   - [Notification Bell](#91-notification-bell)
   - [Notification Drawer](#92-notification-drawer)
   - [Notification Item](#93-notification-item)
   - [Toast for Real-Time Alerts](#94-toast-for-real-time-alerts)
10. [Wiring It All Together](#10-wiring-it-all-together)
11. [REST API Reference](#11-rest-api-reference)
12. [WebSocket Reference](#12-websocket-reference)
13. [Event Type → Label & Icon Map](#13-event-type--label--icon-map)
14. [Error Handling](#14-error-handling)
15. [Performance Checklist](#15-performance-checklist)
16. [End-to-End Testing](#16-end-to-end-testing)

---

## 1. System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         BROWSER / UI APP                            │
│                                                                     │
│   ┌──────────────────┐        ┌──────────────────────────────────┐  │
│   │  REST API calls  │        │   WebSocket (STOMP over ws://)   │  │
│   │  (read history,  │        │   ws://localhost:8081            │  │
│   │  mark read, etc.)│        │   /notifications/ws              │  │
│   └────────┬─────────┘        └───────────────┬──────────────────┘  │
└────────────┼─────────────────────────────────-┼────────────────────┘
             │ HTTP (JWT Bearer header / cookie) │ Native WS + JWT
             ▼                                  ▼
┌─────────────────────────┐       ┌──────────────────────────────┐
│   API Gateway port 8080 │       │   Ws-Gateway port 8081       │
│                         │       │   (Spring Cloud Gateway       │
│  JWT auth filter on all │       │    Reactive — native WS       │
│  HTTP requests          │       │    proxy, no SockJS needed)   │
│  Route: /notifications/ │       │  Route: /notifications/ws/**  │
│  ** → Notification-Svc  │       │  → lb:ws://Notification-Svc   │
└────────────┬────────────┘       └──────────────┬───────────────┘
             │ HTTP                               │ WebSocket upgrade
             ▼                                   ▼
                  ┌─────────────────────────────────┐
                  │       Notification-Service       │
                  │       port 2530                  │
                  │                                  │
                  │  ┌─────────────────────────┐    │
                  │  │ Kafka Consumer          │    │
                  │  │ (agriconnect.notif.*)   │    │
                  │  └──────────┬──────────────┘    │
                  │             │                    │
                  │  ┌──────────▼──────────────┐    │
                  │  │ NotificationEventRouter │    │
                  │  └──────────┬──────────────┘    │
                  │             │                    │
                  │  ┌──────────▼──────────────┐    │
                  │  │   InAppDispatcher       │    │
                  │  │  1. Save to PostgreSQL  │    │
                  │  │  2. Push via WebSocket  │◄───┼── UI receives here
                  │  └─────────────────────────┘    │
                  └─────────────────────────────────┘
```

**Two gateway entry points:**
- **REST API** → `http://localhost:8080/notifications/api/...` — JWT Bearer header required
- **WebSocket** → `ws://localhost:8081/notifications/ws` — JWT in STOMP connect headers or cookie

> **Why two gateways?**  
> Spring Cloud Gateway MVC (port 8080) handles standard HTTP traffic well but does not reliably proxy native WebSocket connections. `Ws-Gateway` (port 8081) is a dedicated Spring Cloud Gateway **Reactive** instance that properly handles the WebSocket upgrade protocol. The UI uses `http://localhost:8080` for all REST calls and `ws://localhost:8081` for the WebSocket connection.

---

## 2. How Notifications Flow

1. User performs an action (creates a listing, signs a contract, etc.)
2. Producer service (Market-Access-App, Contract-Farming-App, etc.) publishes a `NotificationEvent` to Kafka
3. `Notification-Service` consumes the event and dispatches it to all requested channels
4. For `IN_APP` channel:
   - Saves to `notifications` table in PostgreSQL
   - Immediately pushes the notification over WebSocket to `/topic/notifications/{userId}`
5. The UI receives it in real-time via WebSocket subscription → shows toast + increments badge
6. If the user was offline, they get it from REST API on next page load

---

## 3. Environment Setup

Create a `.env` file in your frontend project root:

```env
# REST API — all HTTP calls go through Api-Gateway
VITE_API_BASE_URL=http://localhost:8080

# WebSocket — goes through dedicated Ws-Gateway (native ws://, NOT http://)
VITE_WS_URL=ws://localhost:8081/notifications/ws
```

> **Production:** Change both to your production domains, e.g.:
> ```env
> VITE_API_BASE_URL=https://api.agriconnect.in
> VITE_WS_URL=wss://ws.agriconnect.in/notifications/ws
> ```

---

## 4. Install Dependencies

```bash
npm install @stomp/stompjs axios date-fns zustand
```

| Package | Purpose |
|---------|---------|
| `@stomp/stompjs` | STOMP protocol client — works over native `ws://` (no SockJS needed) |
| `axios` | HTTP client for REST API calls |
| `date-fns` | Human-readable timestamps (`2 minutes ago`) |
| `zustand` | Lightweight state management |

> **No `sockjs-client` needed.** The `Ws-Gateway` uses native WebSocket (not SockJS), so you connect with a plain `ws://` or `wss://` URL using `brokerURL`.

---

## 5. TypeScript Types

Create `src/types/notification.ts`:

```typescript
export type NotificationChannel = 'IN_APP' | 'EMAIL' | 'SMS' | 'PUSH';

export type NotificationStatus = 'PENDING' | 'SENT' | 'FAILED' | 'SKIPPED_DUPLICATE';

export interface Notification {
  id: string;               // UUID
  eventId: string;          // Kafka event ID (unique)
  userId: string;           // Owner user ID
  eventType: string;        // e.g. "ORDER_PLACED", "CONTRACT_SIGNED"
  sourceService: string;    // e.g. "market-access", "contract-farming"
  templateId: string;       // e.g. "market.test", "order.placed"
  channel: NotificationChannel;
  status: NotificationStatus;
  read: boolean;            // false = unread (shows badge)
  errorMessage: string | null;
  retryCount: number;
  createdAt: string;        // ISO 8601 UTC — use date-fns to format
  updatedAt: string;
}

export interface NotificationPage {
  content: Notification[];
  totalElements: number;
  totalPages: number;
  number: number;           // current page (0-indexed)
  size: number;
  last: boolean;
}

export interface UnreadCountResponse {
  count: number;
}

export interface NotificationStats {
  total: number;
  totalSent: number;
  totalFailed: number;
  totalPending: number;
  totalSkipped: number;
  totalDeliveryLogs: number;
}
```

---

## 6. API Service Layer

Create `src/services/notificationApi.ts`:

```typescript
import axios from 'axios';
import type {
  Notification,
  NotificationPage,
  UnreadCountResponse,
  NotificationStats,
} from '../types/notification';

// Use gateway URL for production (requires JWT cookie/header)
// Use direct URL for local dev without auth
const BASE = import.meta.env.VITE_API_BASE_URL + '/notifications/api/notifications';

// Attach JWT from wherever you store it (cookie is automatic, header needs manual attach)
const api = axios.create({ baseURL: BASE, withCredentials: true });

// ── Fetch paginated notifications ──────────────────────────────────────────
export async function fetchNotifications(
  userId: string,
  channel = 'IN_APP',
  page = 0,
  size = 20
): Promise<NotificationPage> {
  const { data } = await api.get<NotificationPage>('', {
    params: { userId, channel, page, size },
  });
  return data;
}

// ── Unread badge count ─────────────────────────────────────────────────────
export async function fetchUnreadCount(userId: string): Promise<number> {
  const { data } = await api.get<UnreadCountResponse>('/unread-count', {
    params: { userId },
  });
  return data.count;
}

// ── Mark single notification as read ──────────────────────────────────────
export async function markAsRead(notificationId: string): Promise<void> {
  await api.patch(`/${notificationId}/read`);
}

// ── Mark all as read ───────────────────────────────────────────────────────
export async function markAllAsRead(userId: string): Promise<number> {
  const { data } = await api.patch<{ updated: number }>('/read-all', null, {
    params: { userId },
  });
  return data.updated;
}

// ── Delete a notification ──────────────────────────────────────────────────
export async function deleteNotification(
  notificationId: string,
  userId: string
): Promise<void> {
  await api.delete(`/${notificationId}`, { params: { userId } });
}

// ── Admin stats ────────────────────────────────────────────────────────────
export async function fetchStats(): Promise<NotificationStats> {
  const { data } = await api.get<NotificationStats>('/stats');
  return data;
}
```

---

## 7. WebSocket Service (Real-Time)

Create `src/services/notificationSocket.ts`:

```typescript
import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';

// Native ws:// — goes through the dedicated Ws-Gateway (port 8081)
// Set VITE_WS_URL=ws://localhost:8081/notifications/ws in your .env
const WS_URL = import.meta.env.VITE_WS_URL; // ws://localhost:8081/notifications/ws

type NotificationHandler = (notification: any) => void;

class NotificationSocketService {
  private client: Client | null = null;
  private subscription: StompSubscription | null = null;
  private currentUserId: string | null = null;
  private handlers: Set<NotificationHandler> = new Set();
  private reconnectAttempts = 0;
  private readonly MAX_RECONNECT_DELAY = 30_000; // 30s cap

  /**
   * Connect and subscribe.
   * @param userId   - logged-in user's UUID
   * @param jwtToken - JWT string (from your auth store). Sent in STOMP CONNECT headers.
   *                   If you use HttpOnly cookies the browser sends them automatically — pass
   *                   empty string in that case.
   */
  connect(userId: string, jwtToken = ''): void {
    if (this.client?.active && this.currentUserId === userId) return;

    this.currentUserId = userId;
    this.disconnect(); // clean up previous session

    this.client = new Client({
      // Native WebSocket URL — no SockJS, no http://, must be ws:// or wss://
      brokerURL: WS_URL,
      reconnectDelay: this.getReconnectDelay(),

      // Pass JWT in STOMP CONNECT frame headers
      connectHeaders: jwtToken ? { Authorization: `Bearer ${jwtToken}` } : {},

      onConnect: () => {
        this.reconnectAttempts = 0;
        console.log(`[WS] Connected via Ws-Gateway — subscribing for user ${userId}`);
        this.subscription = this.client!.subscribe(
          `/topic/notifications/${userId}`,
          (message: IMessage) => {
            try {
              const notification = JSON.parse(message.body);
              this.handlers.forEach(handler => handler(notification));
            } catch {
              console.warn('[WS] Failed to parse notification message');
            }
          }
        );
      },

      onDisconnect: () => {
        console.log('[WS] Disconnected');
      },

      onStompError: (frame) => {
        console.error('[WS] STOMP error:', frame.headers['message']);
        this.reconnectAttempts++;
      },

      onWebSocketClose: () => {
        this.reconnectAttempts++;
      },
    });

    this.client.activate();
  }

  disconnect(): void {
    this.subscription?.unsubscribe();
    this.subscription = null;
    if (this.client?.active) {
      this.client.deactivate();
    }
    this.client = null;
    this.currentUserId = null;
  }

  // Register a handler that will be called for every incoming notification
  onNotification(handler: NotificationHandler): () => void {
    this.handlers.add(handler);
    // Returns an unsubscribe function
    return () => this.handlers.delete(handler);
  }

  get isConnected(): boolean {
    return this.client?.active ?? false;
  }

  private getReconnectDelay(): number {
    // Exponential backoff: 5s, 10s, 20s, 30s (capped)
    return Math.min(5000 * Math.pow(2, this.reconnectAttempts), this.MAX_RECONNECT_DELAY);
  }
}

// Singleton — one WebSocket per browser session
export const notificationSocket = new NotificationSocketService();
```

---

## 8. State Management (Zustand)

Create `src/stores/notificationStore.ts`:

```typescript
import { create } from 'zustand';
import type { Notification, NotificationPage } from '../types/notification';
import {
  fetchNotifications,
  fetchUnreadCount,
  markAsRead,
  markAllAsRead,
  deleteNotification,
} from '../services/notificationApi';
import { notificationSocket } from '../services/notificationSocket';

interface NotificationStore {
  // State
  notifications: Notification[];
  unreadCount: number;
  totalPages: number;
  currentPage: number;
  isLoading: boolean;
  isDrawerOpen: boolean;
  error: string | null;

  // Actions
  initForUser: (userId: string, jwtToken?: string) => void;
  loadNotifications: (userId: string, page?: number) => Promise<void>;
  loadMore: (userId: string) => Promise<void>;
  refreshUnreadCount: (userId: string) => Promise<void>;
  markRead: (notificationId: string) => Promise<void>;
  markAllRead: (userId: string) => Promise<void>;
  remove: (notificationId: string, userId: string) => Promise<void>;
  addIncoming: (notification: Notification) => void;
  openDrawer: () => void;
  closeDrawer: () => void;
  disconnect: () => void;
}

export const useNotificationStore = create<NotificationStore>((set, get) => ({
  notifications: [],
  unreadCount: 0,
  totalPages: 1,
  currentPage: 0,
  isLoading: false,
  isDrawerOpen: false,
  error: null,

  // Called once after login with the authenticated userId
  initForUser: (userId: string, jwtToken = '') => {
    // Load initial data
    get().loadNotifications(userId);
    get().refreshUnreadCount(userId);

    // Connect WebSocket through gateway and handle incoming real-time notifications
    notificationSocket.connect(userId, jwtToken);
    notificationSocket.onNotification((incoming: Notification) => {
      get().addIncoming(incoming);
    });
  },

  loadNotifications: async (userId, page = 0) => {
    set({ isLoading: true, error: null });
    try {
      const data: NotificationPage = await fetchNotifications(userId, 'IN_APP', page, 20);
      set({
        notifications: page === 0 ? data.content : [...get().notifications, ...data.content],
        totalPages: data.totalPages,
        currentPage: data.number,
        isLoading: false,
      });
    } catch (err: any) {
      set({ error: err.message, isLoading: false });
    }
  },

  loadMore: async (userId) => {
    const { currentPage, totalPages } = get();
    if (currentPage + 1 < totalPages) {
      await get().loadNotifications(userId, currentPage + 1);
    }
  },

  refreshUnreadCount: async (userId) => {
    try {
      const count = await fetchUnreadCount(userId);
      set({ unreadCount: count });
    } catch {
      // silently fail — badge count is non-critical
    }
  },

  markRead: async (notificationId) => {
    await markAsRead(notificationId);
    set(state => ({
      notifications: state.notifications.map(n =>
        n.id === notificationId ? { ...n, read: true } : n
      ),
      unreadCount: Math.max(0, state.unreadCount - 1),
    }));
  },

  markAllRead: async (userId) => {
    await markAllAsRead(userId);
    set(state => ({
      notifications: state.notifications.map(n => ({ ...n, read: true })),
      unreadCount: 0,
    }));
  },

  remove: async (notificationId, userId) => {
    await deleteNotification(notificationId, userId);
    set(state => {
      const removed = state.notifications.find(n => n.id === notificationId);
      return {
        notifications: state.notifications.filter(n => n.id !== notificationId),
        unreadCount: removed && !removed.read
          ? Math.max(0, state.unreadCount - 1)
          : state.unreadCount,
      };
    });
  },

  // Called by WebSocket handler when a new notification arrives in real-time
  addIncoming: (notification) => {
    set(state => ({
      // Prevent duplicates (in case of reconnect replaying)
      notifications: state.notifications.some(n => n.id === notification.id)
        ? state.notifications
        : [notification, ...state.notifications],
      unreadCount: state.unreadCount + 1,
    }));
  },

  openDrawer: () => set({ isDrawerOpen: true }),
  closeDrawer: () => set({ isDrawerOpen: false }),
  disconnect: () => notificationSocket.disconnect(),
}));
```

---

## 9. UI Components

### 9.1 Notification Bell

Create `src/components/notifications/NotificationBell.tsx`:

```tsx
import React, { useEffect } from 'react';
import { useNotificationStore } from '../../stores/notificationStore';

interface Props {
  userId: string;
  jwtToken?: string; // pass your JWT string if not using HttpOnly cookie
}

export function NotificationBell({ userId, jwtToken = '' }: Props) {
  const { unreadCount, isDrawerOpen, openDrawer, closeDrawer, initForUser } =
    useNotificationStore();

  useEffect(() => {
    initForUser(userId, jwtToken);
    // Cleanup WebSocket on unmount (e.g. logout)
    return () => useNotificationStore.getState().disconnect();
  }, [userId]);

  return (
    <button
      onClick={() => (isDrawerOpen ? closeDrawer() : openDrawer())}
      aria-label={`Notifications — ${unreadCount} unread`}
      style={{ position: 'relative', background: 'none', border: 'none', cursor: 'pointer', fontSize: 24 }}
    >
      🔔
      {unreadCount > 0 && (
        <span style={{
          position: 'absolute',
          top: -6, right: -6,
          background: '#e53e3e',
          color: '#fff',
          borderRadius: '50%',
          minWidth: 18, height: 18,
          fontSize: 11, fontWeight: 700,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          padding: '0 4px',
        }}>
          {unreadCount > 99 ? '99+' : unreadCount}
        </span>
      )}
    </button>
  );
}
```

---

### 9.2 Notification Drawer

Create `src/components/notifications/NotificationDrawer.tsx`:

```tsx
import React, { useEffect } from 'react';
import { useNotificationStore } from '../../stores/notificationStore';
import { NotificationItem } from './NotificationItem';

interface Props {
  userId: string;
}

export function NotificationDrawer({ userId }: Props) {
  const {
    notifications,
    isLoading,
    isDrawerOpen,
    totalPages,
    currentPage,
    closeDrawer,
    markAllRead,
    loadMore,
  } = useNotificationStore();

  if (!isDrawerOpen) return null;

  const hasMore = currentPage + 1 < totalPages;

  return (
    <>
      {/* Backdrop */}
      <div
        onClick={closeDrawer}
        style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.3)', zIndex: 999
        }}
      />

      {/* Drawer panel */}
      <div style={{
        position: 'fixed', top: 0, right: 0, bottom: 0,
        width: 380, background: '#fff', boxShadow: '-4px 0 24px rgba(0,0,0,0.15)',
        zIndex: 1000, display: 'flex', flexDirection: 'column',
      }}>

        {/* Header */}
        <div style={{
          padding: '16px 20px', borderBottom: '1px solid #e2e8f0',
          display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        }}>
          <h2 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>🔔 Notifications</h2>
          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <button
              onClick={() => markAllRead(userId)}
              style={{ fontSize: 13, color: '#3182ce', background: 'none', border: 'none', cursor: 'pointer' }}
            >
              Mark all read
            </button>
            <button
              onClick={closeDrawer}
              style={{ fontSize: 20, background: 'none', border: 'none', cursor: 'pointer', lineHeight: 1 }}
            >
              ✕
            </button>
          </div>
        </div>

        {/* List */}
        <div style={{ flex: 1, overflowY: 'auto', padding: '8px 0' }}>
          {isLoading && notifications.length === 0 && (
            <div style={{ padding: 40, textAlign: 'center', color: '#718096' }}>
              Loading...
            </div>
          )}

          {!isLoading && notifications.length === 0 && (
            <div style={{ padding: 40, textAlign: 'center' }}>
              <div style={{ fontSize: 40, marginBottom: 12 }}>🎉</div>
              <div style={{ color: '#718096', fontSize: 14 }}>You're all caught up!</div>
            </div>
          )}

          {notifications.map(n => (
            <NotificationItem key={n.id} notification={n} userId={userId} />
          ))}

          {hasMore && (
            <button
              onClick={() => loadMore(userId)}
              disabled={isLoading}
              style={{
                width: '100%', padding: '14px', border: 'none',
                borderTop: '1px solid #e2e8f0', background: '#f7fafc',
                cursor: 'pointer', color: '#3182ce', fontSize: 14,
              }}
            >
              {isLoading ? 'Loading...' : 'Load more'}
            </button>
          )}
        </div>
      </div>
    </>
  );
}
```

---

### 9.3 Notification Item

Create `src/components/notifications/NotificationItem.tsx`:

```tsx
import React from 'react';
import { formatDistanceToNow } from 'date-fns';
import type { Notification } from '../../types/notification';
import { useNotificationStore } from '../../stores/notificationStore';

// Map eventType to human-readable label and icon
const EVENT_META: Record<string, { icon: string; label: string }> = {
  USER_REGISTERED:       { icon: '👤', label: 'Welcome to AgriConnect!' },
  ORDER_PLACED:          { icon: '🛒', label: 'Order placed' },
  LISTING_CREATED:       { icon: '📋', label: 'New listing published' },
  LISTING_UPDATED:       { icon: '✏️', label: 'Listing updated' },
  CONTRACT_SIGNED:       { icon: '📝', label: 'Contract signed' },
  CONTRACT_CREATED:      { icon: '📄', label: 'New contract created' },
  AGREEMENT_SIGNED:      { icon: '✅', label: 'Agreement signed' },
  AGREEMENT_GENERATED:   { icon: '📜', label: 'Agreement ready' },
  STORAGE_BOOKED:        { icon: '🏪', label: 'Storage booking confirmed' },
  PAYMENT_RECEIVED:      { icon: '💰', label: 'Payment received' },
  MARKET_TEST_EVENT:     { icon: '🔔', label: 'Test notification' },
};

const DEFAULT_META = { icon: '🔔', label: 'New notification' };

interface Props {
  notification: Notification;
  userId: string;
}

export function NotificationItem({ notification, userId }: Props) {
  const { markRead, remove } = useNotificationStore();
  const meta = EVENT_META[notification.eventType] ?? DEFAULT_META;

  const handleClick = () => {
    if (!notification.read) markRead(notification.id);
  };

  return (
    <div
      onClick={handleClick}
      style={{
        display: 'flex', alignItems: 'flex-start', gap: 12,
        padding: '14px 20px',
        borderBottom: '1px solid #f0f4f8',
        background: notification.read ? '#fff' : '#ebf8ff',
        cursor: 'pointer',
        transition: 'background 0.2s',
      }}
    >
      {/* Icon */}
      <div style={{ fontSize: 22, lineHeight: 1, flexShrink: 0, marginTop: 2 }}>
        {meta.icon}
      </div>

      {/* Content */}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontWeight: notification.read ? 400 : 700,
          fontSize: 14, color: '#1a202c',
          whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
        }}>
          {meta.label}
        </div>
        <div style={{ fontSize: 12, color: '#718096', marginTop: 2 }}>
          {notification.sourceService} · {notification.templateId}
        </div>
        <div style={{ fontSize: 11, color: '#a0aec0', marginTop: 4 }}>
          {formatDistanceToNow(new Date(notification.createdAt), { addSuffix: true })}
        </div>
      </div>

      {/* Status badges */}
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4 }}>
        {!notification.read && (
          <div style={{
            width: 8, height: 8, borderRadius: '50%',
            background: '#3182ce', flexShrink: 0, marginTop: 4,
          }} />
        )}
        <button
          onClick={(e) => { e.stopPropagation(); remove(notification.id, userId); }}
          style={{
            background: 'none', border: 'none', cursor: 'pointer',
            color: '#cbd5e0', fontSize: 16, lineHeight: 1, padding: 0,
          }}
          aria-label="Delete notification"
        >
          ×
        </button>
      </div>
    </div>
  );
}
```

---

### 9.4 Toast for Real-Time Alerts

Create `src/components/notifications/NotificationToast.tsx`:

```tsx
import React, { useEffect, useRef, useState } from 'react';
import type { Notification } from '../../types/notification';
import { notificationSocket } from '../../services/notificationSocket';

const EVENT_META: Record<string, { icon: string; label: string }> = {
  ORDER_PLACED:        { icon: '🛒', label: 'Order placed' },
  CONTRACT_SIGNED:     { icon: '📝', label: 'Contract signed' },
  AGREEMENT_SIGNED:    { icon: '✅', label: 'Agreement signed' },
  PAYMENT_RECEIVED:    { icon: '💰', label: 'Payment received' },
  LISTING_CREATED:     { icon: '📋', label: 'New listing' },
};

interface Toast {
  id: string;
  notification: Notification;
}

interface Props {
  userId: string;
}

export function NotificationToast({ userId }: Props) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const timers = useRef<Map<string, ReturnType<typeof setTimeout>>>(new Map());

  useEffect(() => {
    const unsubscribe = notificationSocket.onNotification((notification: Notification) => {
      const toast: Toast = { id: crypto.randomUUID(), notification };
      setToasts(prev => [...prev, toast]);

      const timer = setTimeout(() => {
        setToasts(prev => prev.filter(t => t.id !== toast.id));
        timers.current.delete(toast.id);
      }, 5000);

      timers.current.set(toast.id, timer);
    });

    return () => {
      unsubscribe();
      timers.current.forEach(clearTimeout);
    };
  }, [userId]);

  const dismiss = (id: string) => {
    setToasts(prev => prev.filter(t => t.id !== id));
    const timer = timers.current.get(id);
    if (timer) { clearTimeout(timer); timers.current.delete(id); }
  };

  if (toasts.length === 0) return null;

  return (
    <div style={{
      position: 'fixed', bottom: 24, right: 24,
      display: 'flex', flexDirection: 'column', gap: 10,
      zIndex: 2000, maxWidth: 360,
    }}>
      {toasts.map(({ id, notification }) => {
        const meta = EVENT_META[notification.eventType] ?? { icon: '🔔', label: 'New notification' };
        return (
          <div
            key={id}
            style={{
              background: '#fff', borderRadius: 12,
              boxShadow: '0 8px 32px rgba(0,0,0,0.15)',
              padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 12,
              borderLeft: '4px solid #3182ce',
              animation: 'slideIn 0.3s ease',
            }}
          >
            <span style={{ fontSize: 22 }}>{meta.icon}</span>
            <div style={{ flex: 1 }}>
              <div style={{ fontWeight: 700, fontSize: 14, color: '#1a202c' }}>{meta.label}</div>
              <div style={{ fontSize: 12, color: '#718096' }}>{notification.sourceService}</div>
            </div>
            <button
              onClick={() => dismiss(id)}
              style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#a0aec0', fontSize: 18 }}
            >
              ×
            </button>
          </div>
        );
      })}
    </div>
  );
}
```

Add this CSS once in your global stylesheet:

```css
@keyframes slideIn {
  from { transform: translateX(120%); opacity: 0; }
  to   { transform: translateX(0);    opacity: 1; }
}
```

---

## 10. Wiring It All Together

In your main layout component (e.g. `src/layouts/AppLayout.tsx`):

```tsx
import React from 'react';
import { NotificationBell }   from '../components/notifications/NotificationBell';
import { NotificationDrawer } from '../components/notifications/NotificationDrawer';
import { NotificationToast }  from '../components/notifications/NotificationToast';

// Get userId and jwtToken from your auth context / store
function AppLayout({ children, userId, jwtToken }: { children: React.ReactNode; userId: string; jwtToken?: string }) {
  return (
    <div>
      {/* Header */}
      <header style={{
        display: 'flex', justifyContent: 'flex-end',
        alignItems: 'center', padding: '12px 24px',
        borderBottom: '1px solid #e2e8f0', background: '#fff',
      }}>
        <NotificationBell userId={userId} jwtToken={jwtToken} />
      </header>

      {/* Page content */}
      <main>{children}</main>

      {/* Notification drawer (slides in from right) */}
      <NotificationDrawer userId={userId} />

      {/* Real-time toast popups */}
      <NotificationToast userId={userId} />
    </div>
  );
}

export default AppLayout;
```

---

## 11. REST API Reference

> **Base URL (only use this — via Gateway):** `http://localhost:8080/notifications/api/notifications`  
> **Auth:** JWT cookie (`jwt_token`) set automatically by browser, or `Authorization: Bearer <token>` header

| Method | Path | Params | Description |
|--------|------|--------|-------------|
| `GET` | `/` | `userId`, `channel?`, `page?`, `size?` | Paginated notification list |
| `GET` | `/unread-count` | `userId` | Badge count (unread only) |
| `PATCH` | `/{id}/read` | — | Mark one as read |
| `PATCH` | `/read-all` | `userId` | Mark all as read |
| `DELETE` | `/{id}` | `userId` | Delete one notification |
| `GET` | `/stats` | — | Admin counts by status |

### GET `/` — Response shape

```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "eventId": "evt-abc123",
      "userId": "user-456",
      "eventType": "ORDER_PLACED",
      "sourceService": "market-access",
      "templateId": "order.placed",
      "channel": "IN_APP",
      "status": "SENT",
      "read": false,
      "errorMessage": null,
      "retryCount": 0,
      "createdAt": "2026-04-01T12:00:00Z",
      "updatedAt": "2026-04-01T12:00:01Z"
    }
  ],
  "totalElements": 45,
  "totalPages": 3,
  "number": 0,
  "size": 20,
  "last": false
}
```

---

## 12. WebSocket Reference

> **Endpoint:** `ws://localhost:8081/notifications/ws` (via Ws-Gateway → Notification-Service)  
> **Protocol:** STOMP over native WebSocket (`ws://` — no SockJS)  
> **Subscribe destination:** `/topic/notifications/{userId}`  
> **Auth:** JWT passed in STOMP `connectHeaders` as `Authorization: Bearer <token>`, or via HttpOnly cookie sent automatically by browser.

> **Why Ws-Gateway (port 8081) and not Api-Gateway (port 8080)?**  
> The Api-Gateway runs Spring Cloud Gateway MVC which does not reliably proxy native WebSocket upgrades. The dedicated `Ws-Gateway` runs Spring Cloud Gateway Reactive and handles `ws://` connections natively and correctly.

### Message payload (pushed on every new IN_APP notification):

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "eventId": "evt-abc123",
  "userId": "user-456",
  "eventType": "ORDER_PLACED",
  "sourceService": "market-access",
  "templateId": "order.placed",
  "channel": "IN_APP",
  "status": "SENT",
  "read": false,
  "errorMessage": null,
  "retryCount": 0,
  "createdAt": "2026-04-01T12:00:00.000Z",
  "updatedAt": "2026-04-01T12:00:00.000Z"
}
```

### Connection lifecycle:

| Event | What to do |
|-------|-----------|
| `onConnect` | Subscribe to `/topic/notifications/{userId}`, show online indicator |
| Incoming message | Add to notification list, increment badge, show toast |
| `onDisconnect` | Show subtle "reconnecting..." indicator |
| Auto-reconnect | Handled automatically by `@stomp/stompjs` with exponential backoff |
| User logout | Call `notificationSocket.disconnect()` |

---

## 13. Event Type → Label & Icon Map

Use this map to show human-friendly text in the UI instead of raw event type strings:

| eventType | Icon | Label | Source Service |
|-----------|------|-------|----------------|
| `USER_REGISTERED` | 👤 | Welcome to AgriConnect! | api-gateway |
| `ORDER_PLACED` | 🛒 | Your order has been placed | market-access |
| `LISTING_CREATED` | 📋 | New listing published | market-access |
| `LISTING_UPDATED` | ✏️ | Listing updated | market-access |
| `CONTRACT_SIGNED` | 📝 | Contract signed successfully | contract-farming |
| `CONTRACT_CREATED` | 📄 | New contract created | contract-farming |
| `AGREEMENT_SIGNED` | ✅ | Agreement signed | generate-agreement |
| `AGREEMENT_GENERATED` | 📜 | Agreement document ready | generate-agreement |
| `STORAGE_BOOKED` | 🏪 | Storage booking confirmed | market-access |
| `PAYMENT_RECEIVED` | 💰 | Payment received | api-gateway |
| `MARKET_TEST_EVENT` | 🔔 | Test notification | market-access |

> Add new entries as the backend introduces more event types.

---

## 14. Error Handling

### Axios interceptor (add once in your app setup):

```typescript
// src/services/notificationApi.ts — add at the bottom
api.interceptors.response.use(
  res => res,
  error => {
    const status = error.response?.status;
    if (status === 401) {
      window.location.href = '/login'; // redirect on expired JWT
    } else if (status === 403) {
      console.error('[Notifications] Access denied');
    } else if (status >= 500) {
      console.error('[Notifications] Server error — will retry on next load');
    }
    return Promise.reject(error);
  }
);
```

### Status codes handled:

| HTTP Status | UI Behaviour |
|-------------|-------------|
| `200` / `204` | Update state silently |
| `400` | Show inline validation error |
| `401` | Redirect to `/login` |
| `403` | Show "Access denied" snackbar |
| `404` | Remove item from local list (already deleted) |
| `500` | Show "Something went wrong, try again" |
| Network error | Show offline banner, retry on reconnect |

---

## 15. Performance Checklist

- **Don't poll** — WebSocket delivers notifications instantly; polling is not needed
- **Lazy load** drawer — don't render `NotificationDrawer` content until it's opened
- **Pagination** — load 20 items per page, load more on scroll/click
- **Deduplicate** — `addIncoming` in the store already prevents duplicate notifications
- **Disconnect on logout** — call `notificationSocket.disconnect()` when user logs out
- **Single WebSocket** — `notificationSocket` is a singleton; never create multiple instances
- **Auto-reconnect** — already built into the service with exponential backoff (5s → 10s → 20s → 30s cap)
- **Read state** — mark as read optimistically in local state before the API call returns

---

## 16. End-to-End Testing

### Step 1: Start all services

```powershell
# Start Kafka, Schema Registry, Kafka UI
cd "D:\VK18\My Projects\AgriConnect\Backend"
docker compose up -d kafka schema-registry kafka-ui

# Start Notification-Service (in IDE or terminal)
# Start Market-Access-App (in IDE or terminal)
```

### Step 2: Verify backend is up

```powershell
# Backend team verifies services are healthy (not UI calls)
curl.exe http://localhost:2530/actuator/health
curl.exe http://localhost:8080/actuator/health
curl.exe http://localhost:8081/actuator/health  # Ws-Gateway

# Verify notification endpoint is reachable through gateway (what UI calls)
curl.exe "http://localhost:8080/notifications/api/notifications/unread-count?userId=test"
```

### Step 3: Test WebSocket connection (browser console)

Open your app, open DevTools → Network tab → filter `ws` — you should see a connection to `ws://localhost:8081/notifications/ws`.

In the console:

```javascript
// Should log "New notification: {...}" within seconds of triggering Step 4
```

### Step 4: Publish a test notification

```powershell
$uri = "http://localhost:2527/notifications/test/publish?userId=YOUR_USER_ID&recipientEmail=test@example.com"
$body = @{ listingId="L-1001"; price="2500"; crop="wheat" } | ConvertTo-Json -Compress
Invoke-RestMethod -Method POST -Uri $uri -ContentType "application/json" -Body $body
```

Replace `YOUR_USER_ID` with the actual UUID of the logged-in user in your UI.

### Step 5: Verify in UI

- [ ] Toast pops up in bottom-right within 1 second
- [ ] Bell badge increments by 1
- [ ] Notification appears at the top of the drawer (unread, blue background)
- [ ] Click notification → turns white (marked as read), badge decrements
- [ ] Click "Mark all read" → all turn white, badge = 0
- [ ] Click delete (×) → notification disappears
- [ ] Refresh page → notifications still there (persisted in DB)
- [ ] Disconnect internet → reconnecting indicator shown
- [ ] Reconnect internet → WebSocket reconnects automatically, badge is correct

---

*Guide maintained by the AgriConnect Backend Team — April 2026*
