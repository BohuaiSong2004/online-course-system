import Link from "next/link";
import { PageShell } from "@ocs/shared/components/PageShell";
import { SignInPrompt } from "@ocs/shared/components/SignInPrompt";
import { getCurrentUser } from "@ocs/shared/server";
import type { UserView } from "@ocs/shared/types";

type Tab = "overview" | "assignments" | "users" | "teams";

/** Resolves a manager/admin, or returns the page to show instead. */
export async function requireManager(next: string, adminOnly = false): Promise<{ user: UserView } | { page: React.ReactElement }> {
  const user = await getCurrentUser();
  if (!user) {
    return {
      page: (
        <PageShell zone="admin" user={null}>
          <SignInPrompt next={next} title="Sign in to Admin" />
        </PageShell>
      ),
    };
  }
  const allowed = adminOnly ? user.role === "ADMIN" : user.role === "MANAGER" || user.role === "ADMIN";
  if (!allowed) {
    return {
      page: (
        <PageShell zone="admin" user={user}>
          <div className="card mx-auto max-w-md p-8 text-center">
            <h1 className="text-xl font-semibold">{adminOnly ? "Admins only" : "Managers and admins only"}</h1>
            <p className="mt-2 text-sm text-ink-soft">You don&apos;t have access to this page.</p>
          </div>
        </PageShell>
      ),
    };
  }
  return { user };
}

export function AdminShell({ user, tab, children }: { user: UserView; tab: Tab; children: React.ReactNode }) {
  const tabs: [Tab, string, string, boolean][] = [
    ["overview", "Overview", "/", true],
    ["assignments", "Assignments", "/assignments", true],
    ["users", "Users", "/users", user.role === "ADMIN"],
    ["teams", "Teams", "/teams", user.role === "ADMIN"],
  ];
  return (
    <PageShell zone="admin" user={user}>
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-3xl font-bold">Admin</h1>
          <p className="mt-1 text-sm text-ink-soft">
            {user.role === "ADMIN" ? "Organisation-wide view" : "Teams you manage"}
          </p>
        </div>
      </div>
      <nav className="mt-6 flex gap-1 overflow-x-auto border-b border-slate-200" aria-label="Admin sections">
        {tabs
          .filter(([, , , show]) => show)
          .map(([id, label, href]) => (
            <Link
              key={id}
              href={href}
              aria-current={tab === id ? "page" : undefined}
              className={`-mb-px whitespace-nowrap border-b-2 px-4 py-2 text-sm font-medium ${
                tab === id ? "border-brand-600 text-brand-700" : "border-transparent text-ink-soft hover:text-ink"
              }`}
            >
              {label}
            </Link>
          ))}
      </nav>
      <div className="mt-6">{children}</div>
    </PageShell>
  );
}
