import type { Metadata } from "next";
import { api } from "@ocs/shared/server";
import type { AdminUser, PageResult, Team } from "@ocs/shared/types";
import { ROLES } from "@ocs/shared/types";
import { roleLabel } from "@ocs/shared/format";
import { AdminShell, requireManager } from "../components/AdminShell";
import { UserRow } from "./UserRow";

export const metadata: Metadata = { title: "Users", robots: { index: false } };

type Search = Promise<Record<string, string | string[] | undefined>>;
const one = (v: string | string[] | undefined) => (Array.isArray(v) ? v[0] : v) ?? "";

export default async function UsersPage({ searchParams }: { searchParams: Search }) {
  const auth = await requireManager("/admin/users", true);
  if ("page" in auth) return auth.page;
  const sp = await searchParams;
  const q = one(sp.q);
  const role = one(sp.role);
  const page = Math.max(1, Number(one(sp.page)) || 1);
  const qs = new URLSearchParams({ page: String(page), size: "25" });
  if (q) qs.set("q", q);
  if (role) qs.set("role", role);
  const [users, teams] = await Promise.all([api<PageResult<AdminUser>>(`/admin/users?${qs}`), api<Team[]>("/admin/teams")]);
  const link = (p: number) => {
    const n = new URLSearchParams(qs);
    n.set("page", String(p));
    return `/admin/users?${n}`;
  };

  return (
    <AdminShell user={auth.user} tab="users">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <h2 className="text-lg font-semibold">{users.total} users</h2>
        <form method="get" className="flex flex-wrap gap-2" aria-label="Search users">
          <input name="q" defaultValue={q} placeholder="Name or email" className="input w-56" aria-label="Name or email" />
          <select name="role" defaultValue={role} className="input w-auto" aria-label="Role">
            <option value="">All roles</option>
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {roleLabel(r)}
              </option>
            ))}
          </select>
          <button className="btn-secondary">Search</button>
        </form>
      </div>
      <p className="mt-2 text-sm text-ink-soft">
        Changing someone&apos;s role or deactivating them signs them out within 15 minutes on every device.
      </p>
      <div className="card mt-4 overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-slate-200 text-xs uppercase tracking-wider text-ink-muted">
            <tr>
              <th scope="col" className="px-4 py-3">User</th>
              <th scope="col" className="px-4 py-3">Role</th>
              <th scope="col" className="px-4 py-3">Team</th>
              <th scope="col" className="px-4 py-3">Active</th>
              <th scope="col" className="px-4 py-3">
                <span className="sr-only">Save</span>
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {users.items.map((u) => (
              <UserRow key={u.id} user={u} teams={teams} isSelf={u.id === auth.user.id} />
            ))}
          </tbody>
        </table>
      </div>
      {users.totalPages > 1 && (
        <nav className="mt-6 flex items-center justify-center gap-2" aria-label="Pagination">
          {page > 1 && (
            <a className="btn-secondary" href={link(page - 1)}>
              ← Previous
            </a>
          )}
          <span className="px-3 text-sm text-ink-muted">
            Page {page} of {users.totalPages}
          </span>
          {page < users.totalPages && (
            <a className="btn-secondary" href={link(page + 1)}>
              Next →
            </a>
          )}
        </nav>
      )}
    </AdminShell>
  );
}
