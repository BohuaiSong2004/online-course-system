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
export async function clientApi<T>(path: string, init: RequestInit & { json?: unknown } = {}): Promise<T> {
  const { json, ...rest } = init;
  const headers = new Headers(rest.headers);
  if (json !== undefined) headers.set("Content-Type", "application/json");
  const res = await fetch(`/backend${path}`, {
    ...rest,
    headers,
    credentials: "same-origin",
    body: json !== undefined ? JSON.stringify(json) : rest.body,
  });
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
