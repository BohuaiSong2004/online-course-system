import { NextResponse, type NextRequest } from "next/server";

/**
 * Silent session refresh at the edge of the app.
 *
 * Every page request passes through the shell (it proxies the other zones), so this one middleware
 * keeps sessions alive for all micro-frontends: when the 15-minute access token is missing or about to
 * expire but a refresh token exists, it rotates the pair with the API, forwards the new access token to
 * the zone rendering this request, and sets both httpOnly cookies on the response.
 */
const ACCESS = "ocs_token";
const REFRESH = "ocs_refresh";
const BACKEND = process.env.BACKEND_URL ?? "http://localhost:8080";
const SECURE = process.env.OCS_COOKIE_SECURE === "true";

// Same zone map as the rewrites in next.config.mjs. When this middleware refreshes the session it must
// proxy zone paths itself: cookies set by middleware are dropped when next.config rewrites proxy the
// request to another origin, but kept on a middleware rewrite.
const ZONES: [prefix: string, origin: string][] = [
  ["/courses", process.env.CATALOG_URL ?? "http://localhost:3001"],
  ["/learn", process.env.LEARN_URL ?? "http://localhost:3002"],
  ["/studio", process.env.STUDIO_URL ?? "http://localhost:3003"],
  ["/admin", process.env.ADMIN_URL ?? "http://localhost:3004"],
];

function zoneOrigin(pathname: string): string | null {
  const z = ZONES.find(([prefix]) => pathname === prefix || pathname.startsWith(prefix + "/"));
  return z ? z[1] : null;
}

function expiresWithin(token: string, ms: number): boolean {
  try {
    const payload = JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
    return payload.exp * 1000 - Date.now() < ms;
  } catch {
    return true;
  }
}

export async function middleware(req: NextRequest) {
  const access = req.cookies.get(ACCESS)?.value;
  const refresh = req.cookies.get(REFRESH)?.value;
  if (!refresh || (access && !expiresWithin(access, 60_000))) return NextResponse.next();

  let res: Response;
  try {
    res = await fetch(`${BACKEND}/api/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken: refresh }),
      cache: "no-store",
    });
  } catch {
    return NextResponse.next(); // API unreachable: render as-is, try again on the next request
  }
  if (!res.ok) {
    const out = NextResponse.next();
    if (res.status === 401) {
      out.cookies.delete(ACCESS);
      out.cookies.delete(REFRESH);
    }
    return out;
  }

  const data: { token: string; expiresIn: number; refreshToken: string; refreshExpiresIn: number } = await res.json();
  req.cookies.set(ACCESS, data.token);
  req.cookies.set(REFRESH, data.refreshToken);
  const headers = new Headers(req.headers);
  headers.set("cookie", req.cookies.toString());

  const zone = zoneOrigin(req.nextUrl.pathname);
  const out = zone
    ? NextResponse.rewrite(new URL(req.nextUrl.pathname + req.nextUrl.search, zone), { request: { headers } })
    : NextResponse.next({ request: { headers } });
  const base = { httpOnly: true, sameSite: "lax" as const, secure: SECURE, path: "/" };
  out.cookies.set(ACCESS, data.token, { ...base, maxAge: data.expiresIn });
  out.cookies.set(REFRESH, data.refreshToken, { ...base, maxAge: data.refreshExpiresIn });
  return out;
}

export const config = {
  // Pages only: static assets and the /backend API proxy don't need a fresh token.
  matcher: ["/((?!_next/|backend/|favicon.ico|robots.txt|sitemap.xml).*)"],
};
