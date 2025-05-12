import "server-only";
import { cookies } from "next/headers";
import type { ApiErrorBody, UserView } from "./types";

export const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";
export const SITE_URL = process.env.SITE_URL ?? "http://localhost:3000";
const COOKIE = "ocs_token";

export class ApiError extends Error {
  constructor(public status: number, public body: ApiErrorBody | null) {
    super(body?.message ?? `HTTP ${status}`);
  }
}

/**
 * Server-side call to the Spring Boot API. Forwards the visitor's session token (httpOnly cookie)
 * as a Bearer header so SSR pages render personalised state.
 */
export async function api<T>(path: string, init: RequestInit & { revalidate?: number } = {}): Promise<T> {
  const token = (await cookies()).get(COOKIE)?.value;
  const headers = new Headers(init.headers);
  if (token) headers.set("Authorization", `Bearer ${token}`);
  const { revalidate, ...rest } = init;
  const res = await fetch(`${BACKEND_URL}/api${path}`, {
    ...rest,
    headers,
    // Personalised requests are never cached; anonymous public data may be revalidated.
    ...(token || revalidate === undefined ? { cache: "no-store" as const } : { next: { revalidate } }),
  });
  if (!res.ok) {
    let body: ApiErrorBody | null = null;
    try {
      body = await res.json();
    } catch {
      /* non-JSON error */
    }
    throw new ApiError(res.status, body);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

/** Like api(), but returns null on 404 instead of throwing. */
export async function apiOrNull<T>(path: string, init?: RequestInit & { revalidate?: number }): Promise<T | null> {
  try {
    return await api<T>(path, init);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) return null;
    throw e;
  }
}

/** The signed-in user, or null. Never throws (a down API should not break the header). */
export async function getCurrentUser(): Promise<UserView | null> {
  if (!(await cookies()).get(COOKIE)) return null;
  try {
    return await api<UserView>("/auth/me");
  } catch {
    return null;
  }
}
