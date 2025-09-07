import type { Metadata } from "next";
import { api } from "@ocs/shared/server";
import type { AdminUser, PageResult, Team } from "@ocs/shared/types";
import { AdminShell, requireManager } from "../components/AdminShell";
import { NewTeamForm, TeamRow } from "./TeamForms";

export const metadata: Metadata = { title: "Teams", robots: { index: false } };

export default async function TeamsPage() {
  const auth = await requireManager("/admin/teams", true);
  if ("page" in auth) return auth.page;
  const [teams, managers, admins] = await Promise.all([
    api<Team[]>("/admin/teams"),
    api<PageResult<AdminUser>>("/admin/users?role=MANAGER&size=100"),
    api<PageResult<AdminUser>>("/admin/users?role=ADMIN&size=100"),
  ]);
  const candidates = [...managers.items, ...admins.items].filter((u) => u.active);

  return (
    <AdminShell user={auth.user} tab="teams">
      <section className="card p-6" aria-labelledby="new-team">
        <h2 id="new-team" className="font-semibold">
          New team
        </h2>
        <p className="mt-1 text-sm text-ink-soft">Team managers can assign courses to, and see reports for, their team.</p>
        <NewTeamForm managers={candidates} />
      </section>
      <div className="card mt-6 overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-slate-200 text-xs uppercase tracking-wider text-ink-muted">
            <tr>
              <th scope="col" className="px-4 py-3">Team</th>
              <th scope="col" className="px-4 py-3">Manager</th>
              <th scope="col" className="px-4 py-3">Members</th>
              <th scope="col" className="px-4 py-3">
                <span className="sr-only">Actions</span>
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {teams.map((t) => (
              <TeamRow key={t.id} team={t} managers={candidates} />
            ))}
          </tbody>
        </table>
      </div>
    </AdminShell>
  );
}
