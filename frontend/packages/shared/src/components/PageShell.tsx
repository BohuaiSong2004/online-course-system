import type { UserView } from "../types";
import { SiteFooter } from "./SiteFooter";
import { SiteHeader, type Zone } from "./SiteHeader";

/**
 * Page chrome. Pass `user` from SSR on private pages (no header flicker); omit it on public pages
 * so they stay cookie-free and statically cacheable.
 */
export function PageShell({ zone, user, children }: { zone: Zone; user?: UserView | null; children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col">
      <a href="#main" className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 btn-primary">
        Skip to content
      </a>
      <SiteHeader zone={zone} initialUser={user} />
      <main id="main" className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">
        {children}
      </main>
      <SiteFooter />
    </div>
  );
}
