# AI History UI Integration Guide (React + JavaScript)

This guide covers end-to-end frontend integration for:

- Kisan Mitra chat history
- Crop advisory history

Both are intentionally separate APIs and should be shown in separate UI flows.

---

## 1) Backend APIs (Separate by Design)

### Kisan Mitra History API

- **Method:** `GET`
- **Path:** `/api/v1/ai/chat/history`
- **Query Params:**
  - `page` (default `0`)
  - `size` (default `20`, max `100`)
- **Headers:**
  - `X-User-Phone: <logged-in-user-phone>`

#### Sample request

```http
GET /api/v1/ai/chat/history?page=0&size=20
X-User-Phone: 9876543210
```

#### Sample response

```json
{
  "schemaVersion": "1.0.0",
  "history": [
    {
      "conversationId": "9f9958e3-2c92-418f-9a9a-70d05e4256d0",
      "userMessage": "What crops are suitable for this season?",
      "assistantResponse": "Based on your season and region, you can consider...",
      "language": "en",
      "source": "LLM",
      "safetyDecision": "ALLOW",
      "createdAt": "2026-03-30T14:22:11"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 51,
  "totalPages": 3,
  "hasMore": true
}
```

---

### Crop Advisory History API

- **Method:** `GET`
- **Path:** `/api/v1/ai/crop/history`
- **Query Params:**
  - `page` (default `0`)
  - `size` (default `20`, max `100`)
- **Headers:**
  - `X-User-Phone: <logged-in-user-phone>`

#### Sample request

```http
GET /api/v1/ai/crop/history?page=0&size=20
X-User-Phone: 9876543210
```

#### Sample response

```json
{
  "schemaVersion": "1.0.0",
  "history": [
    {
      "district": "Indore",
      "state": "Madhya Pradesh",
      "soilType": "black cotton",
      "season": "kharif",
      "language": "en",
      "responsePayload": "{\"schemaVersion\":\"1.0.0\",\"crops\":[{\"cropName\":\"Soybean\",\"reason\":\"...\",\"confidence\":\"medium\"}],\"safetyDecision\":\"ALLOW\",\"source\":\"LLM\"}",
      "source": "LLM",
      "safetyDecision": "ALLOW",
      "createdAt": "2026-03-30T14:24:19"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 34,
  "totalPages": 2,
  "hasMore": true
}
```

---

## 2) React + JavaScript Integration

### `src/api/aiHistoryApi.js`

```javascript
import axios from "axios";

const api = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL,
  timeout: 15000,
});

export function setAuthHeaders(userPhone, token) {
  api.defaults.headers.common["X-User-Phone"] = userPhone || "";
  if (token) api.defaults.headers.common["Authorization"] = `Bearer ${token}`;
}

export async function fetchKisanMitraHistory({ page = 0, size = 20 }) {
  const res = await api.get("/api/v1/ai/chat/history", {
    params: { page, size },
  });
  return res.data;
}

export async function fetchCropAdvisoryHistory({ page = 0, size = 20 }) {
  const res = await api.get("/api/v1/ai/crop/history", {
    params: { page, size },
  });
  return res.data;
}
```

---

### `src/hooks/usePaginatedHistory.js`

```javascript
import { useCallback, useEffect, useState } from "react";

export default function usePaginatedHistory(fetchFn, initialSize = 20) {
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [size] = useState(initialSize);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [error, setError] = useState("");

  const loadPage = useCallback(
    async (targetPage, reset = false) => {
      try {
        setLoading(true);
        setError("");
        const data = await fetchFn({ page: targetPage, size });
        const nextItems = data?.history || [];
        setItems((prev) => (reset ? nextItems : [...prev, ...nextItems]));
        setPage(data?.page ?? targetPage);
        setHasMore(Boolean(data?.hasMore));
      } catch (e) {
        setError(e?.response?.data?.message || "Failed to load history.");
      } finally {
        setLoading(false);
        setInitialLoading(false);
      }
    },
    [fetchFn, size]
  );

  const refresh = useCallback(() => loadPage(0, true), [loadPage]);

  const loadMore = useCallback(() => {
    if (!loading && hasMore) {
      loadPage(page + 1, false);
    }
  }, [loading, hasMore, loadPage, page]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  return {
    items,
    page,
    hasMore,
    loading,
    initialLoading,
    error,
    refresh,
    loadMore,
  };
}
```

---

### `src/features/history/KisanMitraHistory.jsx`

```javascript
import React from "react";
import usePaginatedHistory from "../../hooks/usePaginatedHistory";
import { fetchKisanMitraHistory } from "../../api/aiHistoryApi";

function formatDate(dt) {
  return new Date(dt).toLocaleString();
}

export default function KisanMitraHistory() {
  const { items, hasMore, loading, initialLoading, error, loadMore, refresh } =
    usePaginatedHistory(fetchKisanMitraHistory, 20);

  if (initialLoading) return <div className="state">Loading chat history...</div>;
  if (error) return <div className="state error">{error}</div>;

  return (
    <div className="history-page">
      <div className="header">
        <h2>Kisan Mitra History</h2>
        <button onClick={refresh}>Refresh</button>
      </div>

      {!items.length ? (
        <div className="empty">No chat history yet. Ask your first farming question.</div>
      ) : (
        <div className="chat-list">
          {items.map((item, idx) => (
            <div className="chat-card" key={`${item.createdAt}-${idx}`}>
              <div className="meta">
                <span>{formatDate(item.createdAt)}</span>
                <span>{item.language?.toUpperCase()}</span>
              </div>
              <div className="bubble user">{item.userMessage}</div>
              <div className="bubble bot">{item.assistantResponse}</div>
            </div>
          ))}
        </div>
      )}

      {hasMore && (
        <button className="load-more" onClick={loadMore} disabled={loading}>
          {loading ? "Loading..." : "Load More"}
        </button>
      )}
    </div>
  );
}
```

---

### `src/features/history/CropAdvisoryHistory.jsx`

```javascript
import React from "react";
import usePaginatedHistory from "../../hooks/usePaginatedHistory";
import { fetchCropAdvisoryHistory } from "../../api/aiHistoryApi";

function formatDate(dt) {
  return new Date(dt).toLocaleString();
}

function parseResponse(payload) {
  try {
    return JSON.parse(payload);
  } catch {
    return null;
  }
}

export default function CropAdvisoryHistory() {
  const { items, hasMore, loading, initialLoading, error, loadMore, refresh } =
    usePaginatedHistory(fetchCropAdvisoryHistory, 20);

  if (initialLoading) return <div className="state">Loading crop history...</div>;
  if (error) return <div className="state error">{error}</div>;

  return (
    <div className="history-page">
      <div className="header">
        <h2>Crop Advisory History</h2>
        <button onClick={refresh}>Refresh</button>
      </div>

      {!items.length ? (
        <div className="empty">No crop advisory history yet.</div>
      ) : (
        <div className="crop-list">
          {items.map((item, idx) => {
            const parsed = parseResponse(item.responsePayload);
            const crops = parsed?.crops || [];
            return (
              <div className="crop-card" key={`${item.createdAt}-${idx}`}>
                <div className="meta-row">
                  <strong>{item.district}, {item.state}</strong>
                  <span>{formatDate(item.createdAt)}</span>
                </div>

                <div className="sub-row">
                  <span>Season: {item.season || "-"}</span>
                  <span>Soil: {item.soilType || "-"}</span>
                </div>

                {!!crops.length ? (
                  <ul className="crop-tags">
                    {crops.map((c, i) => (
                      <li key={i}>{c.cropName} - {c.reason}</li>
                    ))}
                  </ul>
                ) : (
                  <pre className="raw">{item.responsePayload}</pre>
                )}
              </div>
            );
          })}
        </div>
      )}

      {hasMore && (
        <button className="load-more" onClick={loadMore} disabled={loading}>
          {loading ? "Loading..." : "Load More"}
        </button>
      )}
    </div>
  );
}
```

---

## 3) Professional UI Ideas

### Keep both histories separate in UX

- **Tab A:** `Kisan Mitra`
- **Tab B:** `Crop Advisory`

Each tab should maintain independent:

- list state
- filters
- pagination
- loading and errors

---

### Kisan Mitra history (Chat-first design)

- Show **message pair cards** (user question + assistant answer).
- Group by date sections: `Today`, `Yesterday`, `Older`.
- Add preview truncation and open full message in a drawer/modal.
- Show badges: `language`, `source`.

---

### Crop advisory history (Advisory-card design)

- Show each request as an advisory card:
  - district/state
  - season/soil chips
  - top crop recommendations
  - timestamp
- If `responsePayload` parsing fails, show raw JSON fallback.
- Add a CTA button: `Re-use this advisory` (prefill new advisory request).

---

### Pro UX improvements

- Add search:
  - Kisan Mitra: search by `userMessage` and `assistantResponse`
  - Crop advisory: search by `district`, `state`, `season`
- Add filters:
  - language
  - date range
  - source (`LLM`, `CACHE`, `FALLBACK`)
- Support infinite scroll with `hasMore`.
- Keep scroll position when user navigates back.
- Add skeleton loaders + empty states + retry state.
- Make time readable:
  - relative in list (`5 min ago`)
  - full timestamp in details

---

## 4) Suggested Folder Structure

```text
src/
  api/
    aiHistoryApi.js
  hooks/
    usePaginatedHistory.js
  features/
    history/
      KisanMitraHistory.jsx
      CropAdvisoryHistory.jsx
      HistoryTabs.jsx
      history.css
```

---

## 5) Integration Checklist

- Ensure `X-User-Phone` header is always attached after login.
- Set `REACT_APP_API_BASE_URL` correctly.
- Call separate endpoints in separate screens/tabs.
- Handle pagination with `page`, `size`, `hasMore`.
- Add robust empty/error/loading states.
- Verify history isolation across 2 different user accounts.
- Confirm backend Flyway migration has run in target DB.

---

## 6) Quick Notes

- History APIs are intentionally separate and production-friendly.
- Keep domain separation in UI for clearer user mental model.
- Do not merge both histories in one list unless product explicitly needs unified timeline.

---

## 7) Theme-First + Tailwind Guidance (Important)

Your frontend should **follow existing app theme first**.  
Use Tailwind only as a utility layer, not as a new visual language.

### Theme rules for UI team

- Reuse existing color tokens (primary, surface, text, muted, border, success, warning).
- Reuse existing radius, spacing, shadows, typography scale from current theme.
- Match existing button style hierarchy (primary, secondary, ghost).
- Keep icon style and card density consistent with existing screens.
- Avoid introducing new random colors or gradients for history screens.

---

### Tailwind setup approach

If Tailwind is already present, map design tokens in `tailwind.config.js`:

```javascript
// tailwind.config.js
module.exports = {
  content: ["./src/**/*.{js,jsx,ts,tsx}"],
  theme: {
    extend: {
      colors: {
        brand: "var(--color-brand)",
        surface: "var(--color-surface)",
        surfaceAlt: "var(--color-surface-alt)",
        text: "var(--color-text)",
        muted: "var(--color-muted)",
        border: "var(--color-border)",
        success: "var(--color-success)",
        warning: "var(--color-warning)",
      },
      borderRadius: {
        xl2: "var(--radius-xl)",
      },
      boxShadow: {
        card: "var(--shadow-card)",
      },
    },
  },
  plugins: [],
};
```

> If your app already uses another token naming style, keep that style.  
> The point is: **Tailwind classes should consume your theme tokens**.

---

### Professional Tailwind UI pattern for both histories

Use these base wrappers consistently:

```html
<div className="min-h-screen bg-surface text-text">
  <div className="mx-auto w-full max-w-5xl px-4 py-4 md:px-6 md:py-6">
    <!-- content -->
  </div>
</div>
```

#### Section header pattern

```html
<div className="mb-4 flex items-center justify-between">
  <h2 className="text-lg font-semibold md:text-xl">Kisan Mitra History</h2>
  <button className="rounded-lg border border-border bg-surfaceAlt px-3 py-1.5 text-sm hover:bg-surface">
    Refresh
  </button>
</div>
```

#### Card pattern (shared)

```html
<div className="rounded-xl2 border border-border bg-surfaceAlt p-4 shadow-card">
  <!-- card body -->
</div>
```

#### Metadata chips

```html
<span className="rounded-full border border-border px-2 py-0.5 text-xs text-muted">EN</span>
<span className="rounded-full bg-brand/10 px-2 py-0.5 text-xs text-brand">LLM</span>
```

---

### Tailwind classes suggestion by screen

#### Kisan Mitra history

- List container: `space-y-3`
- User bubble: `ml-auto max-w-[85%] rounded-2xl bg-brand px-3 py-2 text-white`
- Bot bubble: `mr-auto max-w-[85%] rounded-2xl border border-border bg-surface px-3 py-2`
- Time row: `mb-2 flex items-center justify-between text-xs text-muted`

#### Crop advisory history

- Grid list: `grid grid-cols-1 gap-3 md:grid-cols-2`
- Card title row: `mb-2 flex items-start justify-between gap-3`
- Soil/season chips row: `mb-3 flex flex-wrap gap-2`
- Recommendation list: `space-y-1 text-sm`

---

### Accessibility + polish checklist

- Keep minimum touch target `44px` for buttons.
- Maintain contrast ratio with your existing theme tokens.
- Use focus styles: `focus:outline-none focus:ring-2 focus:ring-brand/40`.
- Add skeleton loaders using current theme shades (not bright gray defaults).
- For empty states, use existing illustration/icon style if app has one.

---

### Final instruction to UI team

- Keep **two separate tabs/screens**.
- Keep existing app visual language.
- Use Tailwind utilities only to implement faster, while honoring theme tokens.
- Do not ship custom one-off styles that conflict with existing design system.

---

## 8) Separate Frontend Pages and Route Paths (React Router)

Yes, use **two separate page routes** for clean UX and deep-link support.

### Recommended frontend URLs

- `"/history/kisan-mitra"` -> Kisan Mitra chat history page
- `"/history/crop-advisory"` -> Crop advisory history page

Optional default redirect:

- `"/history"` -> redirect to `"/history/kisan-mitra"`

---

### Example `react-router-dom` route setup

```javascript
// src/routes/AppRoutes.jsx
import React from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import KisanMitraHistory from "../features/history/KisanMitraHistory";
import CropAdvisoryHistory from "../features/history/CropAdvisoryHistory";
import HistoryTabsLayout from "../features/history/HistoryTabsLayout";

export default function AppRoutes() {
  return (
    <Routes>
      {/* other app routes */}

      <Route path="/history" element={<HistoryTabsLayout />}>
        <Route index element={<Navigate to="/history/kisan-mitra" replace />} />
        <Route path="kisan-mitra" element={<KisanMitraHistory />} />
        <Route path="crop-advisory" element={<CropAdvisoryHistory />} />
      </Route>
    </Routes>
  );
}
```

---

### History tabs layout (shared shell + tab links)

```javascript
// src/features/history/HistoryTabsLayout.jsx
import React from "react";
import { NavLink, Outlet } from "react-router-dom";

const tabClass = ({ isActive }) =>
  [
    "rounded-lg px-3 py-2 text-sm font-medium transition",
    isActive
      ? "bg-brand text-white"
      : "border border-border bg-surfaceAlt text-text hover:bg-surface",
  ].join(" ");

export default function HistoryTabsLayout() {
  return (
    <div className="min-h-screen bg-surface text-text">
      <div className="mx-auto w-full max-w-5xl px-4 py-4 md:px-6 md:py-6">
        <div className="mb-4 flex flex-wrap gap-2">
          <NavLink to="/history/kisan-mitra" className={tabClass}>
            Kisan Mitra
          </NavLink>
          <NavLink to="/history/crop-advisory" className={tabClass}>
            Crop Advisory
          </NavLink>
        </div>
        <Outlet />
      </div>
    </div>
  );
}
```

---

### Query parameter strategy

Use URL query params so refresh/share links preserve state:

- `"/history/kisan-mitra?page=0&size=20"`
- `"/history/crop-advisory?page=1&size=20"`

Recommended behavior:

- Read `page` and `size` from URL on mount.
- Update URL when user clicks Load More / changes page size.
- Keep independent query state for each page route.

---

### Route-level data ownership

Keep state local per route:

- `KisanMitraHistory` owns only chat history state.
- `CropAdvisoryHistory` owns only crop advisory history state.
- No shared mixed list state between both routes.

This avoids bugs where one tab’s pagination or filters affect the other.

---

### Production navigation behavior

- Switching tabs should preserve each tab’s last loaded state (optional enhancement using route cache/store).
- Browser back/forward should restore page and scroll position.
- Deep links should open correct tab directly.

---

### Path + API mapping (clear contract)

- Frontend page path: `"/history/kisan-mitra"`
  - Backend API: `GET /api/v1/ai/chat/history`
- Frontend page path: `"/history/crop-advisory"`
  - Backend API: `GET /api/v1/ai/crop/history`

---

## 9) Mobile-First UX Add-on (Recommended for Farmer Usage)

Most users will access history on phones.  
Design both history pages mobile-first, then enhance for larger screens.

### Mobile UX goals

- Fast scanning in bright/outdoor conditions
- One-hand usage with large tap targets
- Minimal cognitive load (clear hierarchy, less clutter)
- Stable behavior with weak or intermittent network

---

### Layout blueprint (mobile first)

- Top: sticky segmented tabs (`Kisan Mitra`, `Crop Advisory`)
- Below tabs: compact search + filter trigger
- Main area: card list with progressive loading
- Bottom: sticky action row for `Load More` / quick filters (optional)

Use this wrapper:

```html
<div className="min-h-screen bg-surface text-text">
  <div className="mx-auto w-full max-w-5xl px-3 py-3 sm:px-4 sm:py-4">
    <!-- sticky tabs + content -->
  </div>
</div>
```

---

### Sticky tab bar (Tailwind + theme-safe)

```html
<div className="sticky top-0 z-20 -mx-3 mb-3 border-b border-border bg-surface/95 px-3 py-2 backdrop-blur sm:mx-0 sm:px-0">
  <div className="flex gap-2 overflow-x-auto">
    <button className="whitespace-nowrap rounded-lg bg-brand px-3 py-2 text-sm font-medium text-white">
      Kisan Mitra
    </button>
    <button className="whitespace-nowrap rounded-lg border border-border bg-surfaceAlt px-3 py-2 text-sm font-medium text-text">
      Crop Advisory
    </button>
  </div>
</div>
```

Why:

- Keeps context visible while scrolling long history.
- Horizontal overflow handles small phones without wrapping.

---

### Bottom-sheet filters (instead of desktop sidebar)

Use a modal/bottom sheet for filters:

- Kisan Mitra filters:
  - language
  - date range
  - source (`LLM`, `CACHE`, `FALLBACK`)
- Crop advisory filters:
  - state
  - district
  - season
  - language

Bottom-sheet behavior:

- Open from floating filter button or top-right filter icon.
- Snap to ~70-85% height.
- Sticky footer with `Reset` and `Apply`.
- Preserve last used filters per tab.

---

### Card design rules on small screens

- Keep cards compact but readable.
- Prefer 14-16px body text minimum.
- Keep metadata on one line where possible.
- Truncate long response preview, with `View details`.

#### Kisan Mitra card

- top row: timestamp + language/source chips
- user bubble
- assistant bubble
- optional: `Continue this chat` CTA

#### Crop advisory card

- title: `District, State`
- chips: season, soil, language
- content: 1-2 top crop recommendations
- CTA: `Reuse Advisory`

---

### Touch and accessibility standards

- Minimum tap area: `44px` height/width
- Button padding: `px-3 py-2` minimum
- Visible focus style:
  - `focus:outline-none focus:ring-2 focus:ring-brand/40`
- Ensure contrast for outdoor readability
- Avoid tiny gray text for critical details

---

### Performance guidelines (important)

- Page size:
  - mobile default `size=10` or `size=15`
  - desktop default `size=20`
- Use `hasMore` from backend for pagination flow.
- Lazy-render heavy payloads (`responsePayload`) only when expanded.
- If list exceeds ~80 rendered cards, use virtualization (e.g. `react-window`).
- Debounce search input (`250-350ms`).

---

### Network resilience UX

- Show skeleton cards for initial load.
- Show inline retry block on failures:
  - message + `Retry` button
- Preserve already loaded history when next-page fetch fails.
- Show non-blocking toast for pagination errors.

---

### Navigation behavior on mobile

- Keep tab state and scroll position on back navigation.
- Keep separate state caches for both routes:
  - `/history/kisan-mitra`
  - `/history/crop-advisory`
- Deep links should open the exact tab + query params.

Examples:

- `/history/kisan-mitra?page=1&size=10`
- `/history/crop-advisory?page=0&size=10&season=kharif`

---

### Professional polish checklist

- Skeleton loader matches current theme shades
- Empty states have clear CTA
- Sticky tabs do not hide content behind notch/safe area
- Chips and badges use your existing token palette
- Time displayed as relative in list + absolute in details
- Bottom sheet closes with swipe + backdrop tap

---

### Keep this principle

Use Tailwind to accelerate development, but keep decisions anchored to your current theme and design language.  
The user should feel this is part of the same app, not a newly styled module.

---

## 10) Delete APIs (UI Integration)

### Delete Kisan Mitra chat history

- **Method:** `DELETE`
- **Path:** `/api/v1/ai/chat/history`
- **Headers:** `X-User-Phone`
- **Optional query param:** `conversationId`

#### Use cases

- Delete all Kisan Mitra chats for current user:
  - `DELETE /api/v1/ai/chat/history`
- Delete one specific chat thread only:
  - `DELETE /api/v1/ai/chat/history?conversationId=<conversationId>`

#### Sample response

```json
{
  "schemaVersion": "1.0.0",
  "scope": "all-chat-history",
  "conversationId": "",
  "deletedCount": 12
}
```

---

### Delete all AI histories (chat + crop advisory)

- **Method:** `DELETE`
- **Path:** `/api/v1/ai/history/all`
- **Headers:** `X-User-Phone`

#### Sample response

```json
{
  "schemaVersion": "1.0.0",
  "deletedKisanMitra": 12,
  "deletedCropAdvisory": 8,
  "totalDeleted": 20
}
```

---

### UI recommendations for delete flows

- Use confirm modal for destructive actions.
- Provide two actions in Kisan Mitra:
  - `Delete this chat`
  - `Delete all chats`
- In settings/privacy screen provide:
  - `Delete all AI history`
- On success:
  - remove deleted items from local list immediately
  - show toast confirmation

---

### Delete single chat thread (session-level)

- **Method:** `DELETE`
- **Path:** `/api/v1/ai/chat/conversations/{conversationId}`
- **Headers:** `X-User-Phone`

Use this for "Delete chat" in conversation menu.

Sample response:

```json
{
  "schemaVersion": "1.0.0",
  "conversationId": "9f9958e3-2c92-418f-9a9a-70d05e4256d0",
  "deletedConversation": 1,
  "deletedKisanMitraHistory": 5
}
```

---

## 11) Professional Chat Sessions (ChatGPT-style behavior)

To support professional chat history UX, use conversation-based APIs:

- list all user conversations (chat list page)
- fetch messages of a selected conversation (chat detail page)
- continue chat by reusing `conversationId`

### New chat session APIs

#### A) List user chat sessions

- **Method:** `GET`
- **Path:** `/api/v1/ai/chat/conversations?page=0&size=20`
- **Headers:** `X-User-Phone`

Returns conversation summaries with:

- `conversationId`
- `title`
- `lastMessagePreview`
- `createdAt`
- `updatedAt`
- pagination fields (`page`, `size`, `totalElements`, `totalPages`, `hasMore`)

#### B) Get messages of one session

- **Method:** `GET`
- **Path:** `/api/v1/ai/chat/conversations/{conversationId}/messages?page=0&size=50`
- **Headers:** `X-User-Phone`

Returns ordered chat messages:

- `sequenceNo`
- `role` (`user` / `assistant`)
- `content`
- `source`
- `safetyDecision`
- `createdAt`

#### C) Continue chat in selected session

- Existing API: `POST /api/v1/ai/chat/respond`
- Pass same `conversationId` in request body.
- Backend appends new user + assistant messages to that thread.

---

### Chat UI architecture (recommended)

#### Left panel / mobile top list: Conversation list

- Call `/chat/conversations`
- Show:
  - title from first/last user query
  - last message preview
  - updated time
- CTA: `New Chat`

#### Right panel / detail screen: Messages in selected conversation

- Call `/chat/conversations/{conversationId}/messages`
- Render bubbles by role
- Sending message:
  - call `/chat/respond` with selected `conversationId`
  - refresh current conversation messages
  - refresh conversation list preview/time

---

### Creating a "New Chat"

- Frontend can create a new chat simply by calling `/chat/respond` **without** `conversationId`.
- Backend generates a new `conversationId`.
- UI stores that id as current active thread.

---

### Important implementation note

Send only the latest user message in `messages` when continuing a chat.  
Do not send full prior history every time. Backend already loads recent context from DB.

### Performance note

Conversation list and message list APIs use optimized DB queries/projections to avoid N+1 reads:

- `/chat/conversations` uses a single native query for summary + preview.
- `/chat/conversations/{conversationId}/messages` uses scoped native query with pagination.
- all delete endpoints are user-scoped and index-friendly.

---

## 12) Rename Chat API (ChatGPT-style thread naming)

You can allow users to rename a conversation from UI (three-dot menu).

### Endpoint

- **Method:** `PATCH`
- **Path:** `/api/v1/ai/chat/conversations/{conversationId}/title`
- **Headers:** `X-User-Phone`
- **Body:**

```json
{
  "title": "Tomato disease management plan"
}
```

### Response

```json
{
  "schemaVersion": "1.0.0",
  "conversationId": "9f9958e3-2c92-418f-9a9a-70d05e4256d0",
  "title": "Tomato disease management plan",
  "updatedAt": "2026-03-30T18:12:30"
}
```

### UI behavior recommendation

- Show rename action in conversation list item menu.
- Open small modal with title input (max 140 chars).
- On success, update local conversation list instantly.
- If title is empty/invalid, keep API validation message.

---

## 13) DB Cleanup and Consolidation Plan (Safe)

Yes, unnecessary rows/tables can be removed, but do it in phases.

### Current recommendation

- Keep using `ai_conversations` + `ai_messages` as primary source for chat threads.
- Keep `ai_crop_advisory_history` for crop advisory-specific history.
- Treat `ai_kisan_mitra_history` as transitional/duplicate data for now.

---

### Phase 1 (Immediate, safe): Row cleanup only

You can safely delete old/unused rows without schema risk.

#### A) Delete old Kisan Mitra duplicate history rows (example: older than 90 days)

```sql
DELETE FROM ai_kisan_mitra_history
WHERE created_at < NOW() - INTERVAL '90 days';
```

#### B) Delete old crop advisory history rows (example: older than 180 days)

```sql
DELETE FROM ai_crop_advisory_history
WHERE created_at < NOW() - INTERVAL '180 days';
```

#### C) Delete old conversations with no chat messages (orphan conversations)

```sql
DELETE FROM ai_conversations c
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_messages m
  WHERE m.conversation_ref_id = c.id
    AND m.endpoint_type = 'CHAT'
);
```

> Run in maintenance window and backup first in production.

---

### Phase 2 (Recommended): API migration

Before removing any table:

- Move UI fully to:
  - `GET /api/v1/ai/chat/conversations`
  - `GET /api/v1/ai/chat/conversations/{conversationId}/messages`
- Stop using old `/chat/history` list in UI.
- Validate chat list, message list, rename, delete, continue-chat flows.

---

### Phase 3 (After verification): remove duplicate table

If `ai_kisan_mitra_history` is no longer used:

1. Disable writes to it in backend.
2. Monitor for at least one release cycle.
3. Drop table in a dedicated migration:

```sql
DROP TABLE IF EXISTS ai_kisan_mitra_history;
```

Only do this after confirming:

- no endpoint reads it
- no reporting job depends on it
- no analytics/dashboard query depends on it

---

### Performance + data hygiene best practices

- Keep retention policy (e.g., 90/180/365 days based on business needs).
- Archive before delete if compliance requires.
- Batch large deletes (avoid long table locks).
- Ensure cleanup jobs run off-peak.
- Monitor row counts and query latency monthly.

---

### Minimal-risk production rollout checklist

- DB backup/snapshot created
- Read paths confirmed on `ai_conversations` + `ai_messages`
- UI switched to conversation APIs
- Cleanup SQL tested on staging with realistic data volume
- Observability check (API p95, DB CPU, lock wait)
- Execute cleanup in controlled window

---

## 14) Automated Retention Cleanup (Now in Backend)

Backend now runs scheduled cleanup for old AI data using configured retention.

### What gets auto-cleaned

- old rows from `ai_messages`
- old rows from `ai_kisan_mitra_history`
- old rows from `ai_crop_advisory_history`
- old orphan rows from `ai_conversations` (no CHAT messages, older than retention)

### Config used

From `application.yml` / env:

- `ai.persistence.retention-days`
- `ai.persistence.cleanup-enabled`
- `ai.persistence.cleanup-batch-size`
- `ai.persistence.cleanup-max-batches`
- `ai.persistence.cleanup-cron`

### How cleanup is executed

- batched delete loops (to avoid long locks)
- each table cleaned in capped batches per run
- logs include breakdown by table for observability

---

### UI implications after automated cleanup

Frontend should gracefully handle cleanup effects:

- conversation list can become empty after retention cutoff
- opening stale/deleted thread may return no messages
- refresh should re-fetch and fallback to "new chat" state

#### Recommended UI handling

- If selected `conversationId` not found/empty:
  - show toast: `This chat was archived/cleaned up`
  - auto-route to `/history/kisan-mitra` or create new chat
- If conversation list is empty:
  - show first-use empty state CTA (`Start new conversation`)
- Run silent refresh on screen focus/reopen

---

### Suggested retention defaults

- `ai_messages`: 90 days (or business requirement)
- `ai_crop_advisory_history`: 180 days if advisory recall is important
- `ai_kisan_mitra_history`: 30-90 days while transitional; can be removed later

