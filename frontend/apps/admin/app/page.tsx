import type { Metadata } from "next";
import Link from "next/link";
import { AssignmentBadge } from "@ocs/shared/components/Badge";
import { EmptyState } from "@ocs/shared/components/SignInPrompt";
import { formatDay } from "@ocs/shared/format";
import { api } from "@ocs/shared/server";
import type { AssignmentRow, ReportOverview } from "@ocs/shared/types";
import { AdminShell, requireManager } from "./components/AdminShell";

export const metadata: Metadata = { title: "Admin overview", robots: { index: false } };

function Stat({ label, value, tone = "" }: { label: string; value: string | number; tone?: string }) {
  return (
    <div className="card p-5">
      <div className="text-sm text-ink-soft">{label}</div>
      <div className={`mt-1 text-3xl font-bold ${tone}`}>{value}</div>
    </div>
  );
}

function Bar({ pct }: { pct: number }) {
  return (
    <div className="flex items-center gap-2">
      <div className="h-2 w-24 overflow-hidden rounded-full bg-slate-100" aria-hidden>
        <div className="h-full rounded-full bg-brand-600" style={{ width: `${Math.min(100, pct)}%` }} />
      </div>
      <span className="tabular-nums">{pct}%</span>
    </div>
  );
}

export default async function Overview() {
  const auth = await requireManager("/admin");
  if ("page" in auth) return auth.page;
  const [report, overdue] = await Promise.all([
    api<ReportOverview>("/admin/reports/overview"),
    api<AssignmentRow[]>("/admin/assignments?status=OVERDUE"),
  ]);

  return (
    <AdminShell user={auth.user} tab="overview">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-semibold">Training at a glance</h2>
        <div className="flex gap-2">
          <a href="/backend/admin/reports/assignments.csv" className="btn-secondary">
            ⬇ Export CSV
          </a>
          <Link href="/assignments" className="btn-primary">
            Assign a course
          </Link>
        </div>
      </div>

      <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="Active learners" value={report.learners} />
        <Stat label="Assignments" value={report.assignments} />
        <Stat label="Completion rate" value={`${report.completionRate}%`} tone="text-brand-700" />
        <Stat label="Overdue" value={report.overdue} tone={report.overdue > 0 ? "text-rose-700" : ""} />
      </div>

      <div className="mt-8 grid gap-8 lg:grid-cols-2">
        <section aria-labelledby="by-course">
          <h2 id="by-course" className="text-lg font-semibold">
            By course
          </h2>
          <div className="card mt-3 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-200 text-xs uppercase tracking-wider text-ink-muted">
                <tr>
                  <th scope="col" className="px-4 py-3">Course</th>
                  <th scope="col" className="px-4 py-3">Enrolled</th>
                  <th scope="col" className="px-4 py-3">Completion</th>
                  <th scope="col" className="px-4 py-3">Overdue</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {report.courses.map((c) => (
                  <tr key={c.courseId}>
                    <td className="px-4 py-3 font-medium">{c.title}</td>
                    <td className="px-4 py-3 tabular-nums">{c.enrolled}</td>
                    <td className="px-4 py-3">
                      <Bar pct={c.completionRate} />
                    </td>
                    <td className={`px-4 py-3 tabular-nums ${c.overdue ? "font-semibold text-rose-700" : ""}`}>{c.overdue}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <section aria-labelledby="by-team">
          <h2 id="by-team" className="text-lg font-semibold">
            By team
          </h2>
          {report.teams.length === 0 ? (
            <div className="mt-3">
              <EmptyState title="No teams">Ask an admin to assign you as a team manager.</EmptyState>
            </div>
          ) : (
            <div className="card mt-3 overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-slate-200 text-xs uppercase tracking-wider text-ink-muted">
                  <tr>
                    <th scope="col" className="px-4 py-3">Team</th>
                    <th scope="col" className="px-4 py-3">Members</th>
                    <th scope="col" className="px-4 py-3">Assignments done</th>
                    <th scope="col" className="px-4 py-3">Overdue</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {report.teams.map((t) => (
                    <tr key={t.teamId}>
                      <td className="px-4 py-3">
                        <div className="font-medium">{t.name}</div>
                        <div className="text-xs text-ink-muted">{t.managerName ?? "No manager"}</div>
                      </td>
                      <td className="px-4 py-3 tabular-nums">{t.members}</td>
                      <td className="px-4 py-3">
                        <Bar pct={t.completionRate} />
                        <div className="text-xs text-ink-muted">
                          {t.completed}/{t.assigned}
                        </div>
                      </td>
                      <td className={`px-4 py-3 tabular-nums ${t.overdue ? "font-semibold text-rose-700" : ""}`}>{t.overdue}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>

      <section className="mt-8" aria-labelledby="overdue-h">
        <h2 id="overdue-h" className="text-lg font-semibold">
          Overdue assignments
        </h2>
        {overdue.length === 0 ? (
          <div className="mt-3">
            <EmptyState title="Nothing overdue 🎉" />
          </div>
        ) : (
          <ul className="card mt-3 divide-y divide-slate-100">
            {overdue.slice(0, 10).map((a) => (
              <li key={a.id} className="flex flex-wrap items-center justify-between gap-2 px-4 py-3 text-sm">
                <div>
                  <span className="font-medium">{a.userName}</span> <span className="text-ink-muted">· {a.teamName ?? "No team"}</span>
                  <div className="text-ink-soft">{a.courseTitle}</div>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-ink-muted">due {formatDay(a.dueDate)}</span>
                  <AssignmentBadge status={a.status} />
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </AdminShell>
  );
}
