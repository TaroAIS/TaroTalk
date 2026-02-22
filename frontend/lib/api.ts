const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";
const API_TIMEOUT_MS = Number(process.env.NEXT_PUBLIC_API_TIMEOUT_MS ?? "8000");
const AUTH_TOKEN_KEY = process.env.NEXT_PUBLIC_AUTH_TOKEN_KEY ?? "tarotalk_token";

function buildUrl(path: string) {
  if (!API_BASE) {
    return path;
  }
  const base = API_BASE.endsWith("/") ? API_BASE.slice(0, -1) : API_BASE;
  if (base.endsWith("/api") && path.startsWith("/api/")) {
    return `${base}${path.slice(4)}`;
  }
  if (path.startsWith("/")) {
    return `${base}${path}`;
  }
  return `${base}/${path}`;
}

function buildTraceId() {
  return `web-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

function readAuthToken(): string | null {
  if (typeof window === "undefined" || !window.localStorage) {
    return null;
  }
  const token = window.localStorage.getItem(AUTH_TOKEN_KEY);
  if (!token) {
    return null;
  }
  const normalized = token.trim();
  return normalized.length > 0 ? normalized : null;
}

function withTraceHeaders(headers?: HeadersInit): HeadersInit {
  const next = new Headers(headers);
  if (!next.has("X-Trace-Id")) {
    next.set("X-Trace-Id", buildTraceId());
  }
  if (!next.has("Authorization")) {
    const token = readAuthToken();
    if (token) {
      next.set("Authorization", `Bearer ${token}`);
    }
  }
  return next;
}

async function fetchWithTimeout(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), API_TIMEOUT_MS);
  try {
    return await fetch(input, { ...init, signal: controller.signal });
  } finally {
    clearTimeout(timeoutId);
  }
}

export async function apiGet<T>(path: string): Promise<T> {
  const res = await fetchWithTimeout(buildUrl(path), {
    headers: withTraceHeaders()
  });
  if (!res.ok) {
    throw new Error(`Request failed: ${res.status}`);
  }
  return res.json();
}

export async function apiPost<T>(path: string, body: unknown): Promise<T> {
  const res = await fetchWithTimeout(buildUrl(path), {
    method: "POST",
    headers: withTraceHeaders({ "Content-Type": "application/json" }),
    body: JSON.stringify(body)
  });
  if (!res.ok) {
    throw new Error(`Request failed: ${res.status}`);
  }
  return res.json();
}

export async function apiPut<T>(path: string, body: unknown): Promise<T> {
  const res = await fetchWithTimeout(buildUrl(path), {
    method: "PUT",
    headers: withTraceHeaders({ "Content-Type": "application/json" }),
    body: JSON.stringify(body)
  });
  if (!res.ok) {
    throw new Error(`Request failed: ${res.status}`);
  }
  return res.json();
}
