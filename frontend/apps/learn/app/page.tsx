import type { Metadata } from "next";
import Link from "next/link";
import { DueBadge } from "@ocs/shared/components/Badge";
import { PageShell } from "@ocs/shared/components/PageShell";
import { ProgressBar } from "@ocs/shared/components/ProgressBar";
import { EmptyState, SignInPrompt } from "@ocs/shared/components/SignInPrompt";
import { formatDate } from "@ocs/shared/format";
import { routes } from "@ocs/shared/routes";
import { api, getCurrentUser } from "@ocs/shared/server";
import type { Activity, Certificate, EnrollmentView } from "@ocs/shared/types";

export const metadata: Metadata = { title: "My learning", robots: { index: false } };

const ACTION_LABEL: Record<Activity["action"], string> = {
  ENROLL: "Enrolled in",
  UNENROLL: "Left",
  VIEW_CHAPTER: "Viewed",
  COMPLETE_CHAPTER: "Completed",
  COMPLETE_COURSE: "Finished course",
  ASSIGNED: "Was assigned",
  QUIZ_PASSED: "Passed the quiz in",
  QUIZ_FAILED: "Attempted the quiz in",
};

export default async function Dashboard() {
  const user = await getCurrentUser();
  if (!user) {
    return (
      <PageShell zone="learn" user={null}>
        <SignInPrompt next={routes.dashboard} title="Sign in to see your courses" />
      </PageShell>
    );
  }
  const [enrollments, activity, certificates] = await Promise.all([
    api<EnrollmentView[]>("/me/enrollments"),
    api<Activity[]>("/me/activity?limit=12"),
    api<Certificate[]>("/me/certificates"),
  ]);
  const active = enrollments.filter((e) => e.status !== "COMPLETED");
  const completed = enrollments.filter((e) => e.status === "COMPLETED");
  const assigned = active.filter((e) => e.dueDate);
  const overdue = assigned.filter((e) => e.dueDate! < new Date().toISOString().slice(0, 10));

  return (
    <PageShell zone="learn" user={user}>
      <h1 className="text-3xl font-bold">Hi, {user.displayName.split(" ")[0]} 👋</h1>
      <p className="mt-1 text-ink-soft">
        {active.length} in progress · {completed.length} completed · {certificates.length} certificate
        {certificates.length === 1 ? "" : "s"}
      </p>

      {overdue.length > 0 && (
        <div role="alert" className="mt-6 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-900">
          <strong>{overdue.length} assigned course{overdue.length === 1 ? " is" : "s are"} overdue.</strong> Please complete{" "}
          {overdue.length === 1 ? "it" : "them"} as soon as possible.
        </div>
      )}

      <div className="mt-8 grid gap-8 lg:grid-cols-3">
        <div className="min-w-0 space-y-8 lg:col-span-2">
          <section aria-labelledby="in-progress">
            <h2 id="in-progress" className="text-lg font-semibold">
              In progress
            </h2>
            {active.length === 0 ? (
              <div className="mt-3">
                <EmptyState title="Nothing in progress">
                  <a href={routes.catalog} className="font-medium text-brand-700 hover:underline">
                    Browse the catalog
                  </a>{" "}
                  to enroll in your first course.
                </EmptyState>
              </div>
            ) : (
              <ul className="mt-3 space-y-3">
                {active.map((e) => (
                  <li key={e.courseId} className="card flex flex-col gap-4 p-5 sm:flex-row sm:items-center">
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <a href={routes.course(e.slug)} className="font-semibold hover:text-brand-700">
                          {e.title}
                        </a>
                        {e.dueDate && <DueBadge dueDate={e.dueDate} />}
                      </div>
                      <p className="text-xs text-ink-muted">
                        {e.category} · {e.instructorName}
                        {e.assignedByName ? ` · assigned by ${e.assignedByName}` : ` · enrolled ${formatDate(e.enrolledAt)}`}
                      </p>
                      <div className="mt-3 max-w-sm">
                        <ProgressBar done={e.completedChapters} total={e.totalChapters} />
                      </div>
                    </div>
                    {e.nextChapterId && (
                      <Link href={`/${e.slug}/${e.nextChapterId}`} className="btn-primary">
                        {e.completedChapters === 0 ? "Start" : "Continue"}
                      </Link>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </section>

          {completed.length > 0 && (
            <section aria-labelledby="completed">
              <h2 id="completed" className="text-lg font-semibold">
                Completed
              </h2>
              <ul className="mt-3 grid gap-3 sm:grid-cols-2">
                {completed.map((e) => {
                  const cert = certificates.find((c) => c.courseId === e.courseId);
                  return (
                    <li key={e.courseId} className="card p-4">
                      <a href={routes.course(e.slug)} className="font-medium hover:text-brand-700">
                        {e.title}
                      </a>
                      <p className="mt-1 text-xs text-emerald-800">✓ Completed {formatDate(e.completedAt)}</p>
                      {cert && (
                        <a href={routes.certificate(cert.code)} className="mt-2 inline-block text-sm font-medium text-brand-700 hover:underline">
                          🎓 Certificate
                        </a>
                      )}
                    </li>
                  );
                })}
              </ul>
            </section>
          )}
        </div>

        <aside aria-labelledby="activity">
          <h2 id="activity" className="text-lg font-semibold">
            Recent activity
          </h2>
          <div className="card mt-3 p-5">
            {activity.length === 0 ? (
              <p className="text-sm text-ink-muted">No activity yet.</p>
            ) : (
              <ol className="space-y-4">
                {activity.map((a) => (
                  <li key={a.id} className="text-sm">
                    <span className="text-ink-soft">{ACTION_LABEL[a.action] ?? a.action}</span>{" "}
                    <span className="font-medium">{a.chapterTitle ?? a.courseTitle ?? "a course"}</span>
                    <div className="text-xs text-ink-muted">{new Date(a.createdAt).toLocaleString("en-US")}</div>
                  </li>
                ))}
              </ol>
            )}
          </div>
        </aside>
      </div>
    </PageShell>
  );
}
