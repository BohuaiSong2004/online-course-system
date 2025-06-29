"use client";
import type { ApiErrorBody } from "./types";

export class ClientApiError extends Error {
  constructor(public status: number, public body: ApiErrorBody | null) {
    super(body?.message ?? `Request failed (${status})`);
  }
}

/**
 * Browser-side call. Goes through the shell's same-origin `/backend/*` rewrite to the API, so the
 * httpOnly session cookie is sent automatically and no token is ever exposed to JavaScript.
 */
let refreshing: Promise<boolean> | null = null;

/** One shared refresh at a time, so parallel 401s don't each rotate the refresh token. */
function refreshSession(): Promise<boolean> {
  refreshing ??= fetch("/backend/auth/refresh", { method: "POST", credentials: "same-origin" })
    .then((r) => r.ok)
    .catch(() => false)
    .finally(() => setTimeout(() => (refreshing = null), 0));
  return refreshing;
}

export async function clientApi<T>(path: string, init: RequestInit & { json?: unknown } = {}): Promise<T> {
  const { json, ...rest } = init;
  const headers = new Headers(rest.headers);
  if (json !== undefined) headers.set("Content-Type", "application/json");
  const send = () =>
    fetch(`/backend${path}`, {
      ...rest,
      headers,
      credentials: "same-origin",
      body: json !== undefined ? JSON.stringify(json) : rest.body,
    });
  let res = await send();
  // Access token expired while the page was open: refresh once and retry.
  if (res.status === 401 && !path.startsWith("/auth/") && (await refreshSession())) {
    res = await send();
  }
  if (!res.ok) {
    let body: ApiErrorBody | null = null;
    try {
      body = await res.json();
    } catch {
      /* ignore */
    }
    throw new ClientApiError(res.status, body);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export function errorMessage(e: unknown): string {
  if (e instanceof ClientApiError) {
    if (e.body?.fields) {
      return Object.entries(e.body.fields)
        .map(([k, v]) => `${k}: ${v}`)
        .join(" · ");
    }
    return e.body?.message ?? e.message;
  }
  return "Network error, please try again.";
}
