import type { Metadata } from "next";
import Link from "next/link";
import { StatusBadge } from "@ocs/shared/components/Badge";
import { PageShell } from "@ocs/shared/components/PageShell";
import { EmptyState } from "@ocs/shared/components/SignInPrompt";
import { formatDate, formatMinutes } from "@ocs/shared/format";
import { routes } from "@ocs/shared/routes";
import { api } from "@ocs/shared/server";
import type { Course } from "@ocs/shared/types";
import { requireAuthor } from "./components/guard";

export const metadata: Metadata = { title: "Studio", robots: { index: false } };

export default async function StudioHome() {
  const auth = await requireAuthor(routes.studio);
  if ("page" in auth) return auth.page;
  const courses = await api<Course[]>("/studio/courses");

  return (
    <PageShell zone="studio" user={auth.user}>
      <div className="flex items-end justify-between">
        <div>
          <h1 className="text-3xl font-bold">Studio</h1>
          <p className="mt-1 text-ink-soft">
            {auth.user.role === "ADMIN" ? "All courses on the platform" : "Courses you teach"}
          </p>
        </div>
        <Link href="/courses/new" className="btn-primary">
          + New course
        </Link>
      </div>

      <div className="mt-8">
        {courses.length === 0 ? (
          <EmptyState title="No courses yet">Create your first course to get started.</EmptyState>
        ) : (
          <div className="card overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-200 text-xs uppercase tracking-wider text-ink-muted">
                <tr>
                  <th className="px-5 py-3">Course</th>
                  <th className="px-5 py-3">Status</th>
                  <th className="px-5 py-3">Chapters</th>
                  <th className="px-5 py-3">Length</th>
                  <th className="px-5 py-3">Updated</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {courses.map((c) => (
                  <tr key={c.id} className="hover:bg-slate-50">
                    <td className="px-5 py-3">
                      <Link href={`/courses/${c.id}`} className="font-medium text-ink hover:text-brand-700">
                        {c.title}
                      </Link>
                      <div className="text-xs text-ink-muted">
                        {c.category}
                        {auth.user.role === "ADMIN" && <> · {c.instructorName}</>}
                      </div>
                    </td>
                    <td className="px-5 py-3">
                      <StatusBadge status={c.status} />
                    </td>
                    <td className="px-5 py-3">{c.chapterCount}</td>
                    <td className="px-5 py-3">{formatMinutes(c.totalMinutes)}</td>
                    <td className="px-5 py-3 text-ink-muted">{formatDate(c.updatedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </PageShell>
  );
}
