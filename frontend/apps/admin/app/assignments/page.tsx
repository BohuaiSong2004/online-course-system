import type { Metadata } from "next";
import { AssignmentBadge } from "@ocs/shared/components/Badge";
import { ProgressBar } from "@ocs/shared/components/ProgressBar";
import { EmptyState } from "@ocs/shared/components/SignInPrompt";
import { formatDay } from "@ocs/shared/format";
import { api } from "@ocs/shared/server";
import type { AssignmentRow, Course, PageResult, Team } from "@ocs/shared/types";
import { AdminShell, requireManager } from "../components/AdminShell";
import { AssignForm, RemoveAssignment } from "./AssignmentForms";

export const metadata: Metadata = { title: "Assignments", robots: { index: false } };

type Search = Promise<Record<string, string | string[] | undefined>>;
const one = (v: string | string[] | undefined) => (Array.isArray(v) ? v[0] : v) ?? "";
const STATUSES = [
  ["", "All statuses"],
  ["OVERDUE", "Overdue"],
  ["NOT_STARTED", "Not started"],
  ["IN_PROGRESS", "In progress"],
  ["COMPLETED", "Completed"],
];

export default async function AssignmentsPage({ searchParams }: { searchParams: Search }) {
  const auth = await requireManager("/admin/assignments");
  if ("page" in auth) return auth.page;
  const sp = await searchParams;
  const filters = { courseId: one(sp.courseId), teamId: one(sp.teamId), status: one(sp.status) };
  const qs = new URLSearchParams(Object.entries(filters).filter(([, v]) => v));
  const [rows, teams, catalog] = await Promise.all([
    api<AssignmentRow[]>(`/admin/assignments?${qs}`),
    api<Team[]>("/admin/teams"),
    api<PageResult<Course>>("/courses?size=50"),
  ]);
  const courses = catalog.items;

  return (
    <AdminShell user={auth.user} tab="assignments">
      <section className="card p-6" aria-labelledby="assign-h">
        <h2 id="assign-h" className="font-semibold">
          Assign a course
        </h2>
        <p className="mt-1 text-sm text-ink-soft">
          Assigning enrolls the learners and shows the due date on their dashboard. Re-assigning updates the due date.
        </p>
        <AssignForm courses={courses} teams={teams} />
      </section>

      <section className="mt-8" aria-labelledby="list-h">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <h2 id="list-h" className="text-lg font-semibold">
            {rows.length} assignment{rows.length === 1 ? "" : "s"}
          </h2>
          <form method="get" className="flex flex-wrap gap-2" aria-label="Filter assignments">
            <select name="courseId" defaultValue={filters.courseId} className="input w-auto" aria-label="Course">
              <option value="">All courses</option>
              {courses.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.title}
                </option>
              ))}
            </select>
            <select name="teamId" defaultValue={filters.teamId} className="input w-auto" aria-label="Team">
              <option value="">All teams</option>
              {teams.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </select>
            <select name="status" defaultValue={filters.status} className="input w-auto" aria-label="Status">
              {STATUSES.map(([v, l]) => (
                <option key={v} value={v}>
                  {l}
                </option>
              ))}
            </select>
            <button className="btn-secondary">Filter</button>
          </form>
        </div>
        {rows.length === 0 ? (
          <div className="mt-3">
            <EmptyState title="No assignments match" />
          </div>
        ) : (
          <div className="card mt-3 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-200 text-xs uppercase tracking-wider text-ink-muted">
                <tr>
                  <th scope="col" className="px-4 py-3">Learner</th>
                  <th scope="col" className="px-4 py-3">Course</th>
                  <th scope="col" className="px-4 py-3">Progress</th>
                  <th scope="col" className="px-4 py-3">Due</th>
                  <th scope="col" className="px-4 py-3">Status</th>
                  <th scope="col" className="px-4 py-3">
                    <span className="sr-only">Actions</span>
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {rows.map((a) => (
                  <tr key={a.id}>
                    <td className="px-4 py-3">
                      <div className="font-medium">{a.userName}</div>
                      <div className="text-xs text-ink-muted">{a.teamName ?? "No team"}</div>
                    </td>
                    <td className="px-4 py-3">{a.courseTitle}</td>
                    <td className="min-w-40 px-4 py-3">
                      <ProgressBar done={a.completedChapters} total={a.totalChapters} label={`Progress of ${a.userName}`} />
                    </td>
                    <td className="whitespace-nowrap px-4 py-3">{a.dueDate ? formatDay(a.dueDate) : "—"}</td>
                    <td className="px-4 py-3">
                      <AssignmentBadge status={a.status} />
                    </td>
                    <td className="px-4 py-3 text-right">
                      <RemoveAssignment id={a.id} label={`${a.courseTitle} for ${a.userName}`} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </AdminShell>
  );
}
