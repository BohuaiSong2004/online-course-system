import { getCurrentUser } from "../server";
import { routes } from "../routes";
import { LogoutButton } from "./LogoutButton";

export type Zone = "shell" | "catalog" | "learn" | "studio";

function NavLink({ href, active, children }: { href: string; active: boolean; children: React.ReactNode }) {
  return (
    <a
      href={href}
      className={`rounded-md px-3 py-2 text-sm font-medium transition ${
        active ? "bg-brand-50 text-brand-700" : "text-ink-soft hover:bg-slate-100 hover:text-ink"
      }`}
    >
      {children}
    </a>
  );
}

/** Shared top bar rendered on the server by every zone. Cross-zone links are plain anchors. */
export async function SiteHeader({ zone }: { zone: Zone }) {
  const user = await getCurrentUser();
  const canAuthor = user?.role === "INSTRUCTOR" || user?.role === "ADMIN";
  return (
    <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/90 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-6xl items-center gap-6 px-4">
        <a href={routes.home} className="flex items-center gap-2 font-semibold text-ink">
          <span className="grid h-8 w-8 place-items-center rounded-lg bg-brand-600 text-sm font-bold text-white">
            OC
          </span>
          <span className="hidden sm:inline">OpenCourse</span>
        </a>
        <nav className="flex flex-1 items-center gap-1">
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
        </nav>
        {user ? (
          <div className="flex items-center gap-3">
            <div className="hidden text-right sm:block">
              <div className="text-sm font-medium text-ink">{user.displayName}</div>
              <div className="text-xs text-ink-muted">{user.role.toLowerCase()}</div>
            </div>
            <LogoutButton />
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <a href={routes.login()} className="btn-ghost">
              Sign in
            </a>
            <a href={routes.register()} className="btn-primary">
              Get started
            </a>
          </div>
        )}
      </div>
    </header>
  );
}
