"use client";
import { useEffect, useState } from "react";
import { clientApi } from "../client";
import { roleLabel } from "../format";
import { routes } from "../routes";
import type { UserView } from "../types";
import { LogoutButton } from "./LogoutButton";

export type Zone = "shell" | "catalog" | "learn" | "studio" | "admin";

function NavLink({ href, active, children }: { href: string; active: boolean; children: React.ReactNode }) {
  return (
    <a
      href={href}
      aria-current={active ? "page" : undefined}
      className={`rounded-md px-3 py-2 text-sm font-medium transition ${
        active ? "bg-brand-50 text-brand-700" : "text-ink-soft hover:bg-slate-100 hover:text-ink"
      }`}
    >
      {children}
    </a>
  );
}

/**
 * Shared top bar for every zone. It resolves the signed-in user in the browser (unless the page
 * already knows it from SSR), which keeps public pages free of cookies so they can be served
 * statically. Cross-zone links are plain anchors (full page loads between independent apps).
 */
export function SiteHeader({ zone, initialUser }: { zone: Zone; initialUser?: UserView | null }) {
  const [user, setUser] = useState<UserView | null | undefined>(initialUser);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (initialUser !== undefined) return;
    let cancelled = false;
    // /auth/session answers 200 for anonymous visitors too, so public pages log no console errors.
    clientApi<{ user: UserView | null }>("/auth/session")
      .then((s) => !cancelled && setUser(s.user))
      .catch(() => !cancelled && setUser(null));
    return () => {
      cancelled = true;
    };
  }, [initialUser]);

  const canAuthor = user?.role === "INSTRUCTOR" || user?.role === "ADMIN";
  const canManage = user?.role === "MANAGER" || user?.role === "ADMIN";
  const links = (
    <>
      <NavLink href={routes.catalog} active={zone === "catalog"}>
        Courses
      </NavLink>
      {user && (
        <NavLink href={routes.dashboard} active={zone === "learn"}>
          My learning
        </NavLink>
      )}
      {canAuthor && (
        <NavLink href={routes.studio} active={zone === "studio"}>
          Studio
        </NavLink>
      )}
      {canManage && (
        <NavLink href={routes.admin} active={zone === "admin"}>
          Admin
        </NavLink>
      )}
    </>
  );

  return (
    <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/90 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-6xl items-center gap-4 px-4">
        <a href={routes.home} className="flex items-center gap-2 font-semibold text-ink">
          <span className="grid h-8 w-8 place-items-center rounded-lg bg-brand-600 text-sm font-bold text-white" aria-hidden>
            OC
          </span>
          <span>OpenCourse</span>
        </a>
        <nav className="hidden flex-1 items-center gap-1 md:flex" aria-label="Main">
          {links}
        </nav>
        <div className="ml-auto flex items-center gap-2 md:ml-0">
          {user === undefined ? (
            <span className="h-8 w-24 animate-pulse rounded-lg bg-slate-100" aria-hidden />
          ) : user ? (
            <>
              <a href={routes.account} className="hidden rounded-lg px-2 py-1 text-right hover:bg-slate-100 sm:block">
                <span className="block text-sm font-medium text-ink">{user.displayName}</span>
                <span className="block text-xs text-ink-muted">{roleLabel(user.role)}</span>
              </a>
              <LogoutButton />
            </>
          ) : (
            <>
              <a href={routes.login()} className="btn-ghost">
                Sign in
              </a>
              <a href={routes.register()} className="btn-primary hidden sm:inline-flex">
                Get started
              </a>
            </>
          )}
          <button
            type="button"
            className="btn-ghost px-2 md:hidden"
            aria-expanded={open}
            aria-controls="mobile-nav"
            aria-label="Menu"
            onClick={() => setOpen((o) => !o)}
          >
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden>
              <path d="M3 5h14M3 10h14M3 15h14" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
            </svg>
          </button>
        </div>
      </div>
      {open && (
        <nav id="mobile-nav" className="flex flex-col gap-1 border-t border-slate-200 px-4 py-2 md:hidden" aria-label="Main">
          {links}
          {user && (
            <NavLink href={routes.account} active={false}>
              Account
            </NavLink>
          )}
        </nav>
      )}
    </header>
  );
}
