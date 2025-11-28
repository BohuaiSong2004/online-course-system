"use client";
import { useHydrated } from "@ocs/shared/hooks";
import { createContext, useContext, useEffect, useState } from "react";
import { ProgressBar } from "@ocs/shared/components/ProgressBar";
import { clientApi, errorMessage } from "@ocs/shared/client";
import { formatMinutes, levelLabel } from "@ocs/shared/format";
import { routes } from "@ocs/shared/routes";
import type { Chapter, Course, CourseDetail, ViewerState } from "@ocs/shared/types";

/**
 * Personal layer on top of the statically generated course page: the server HTML is identical for
 * every visitor, and this provider fetches the caller's own state (enrolled, progress, edit rights).
 */
const ViewerContext = createContext<{ viewer: ViewerState | null; loading: boolean }>({ viewer: null, loading: true });

export function ViewerProvider({ slug, children }: { slug: string; children: React.ReactNode }) {
  const [state, setState] = useState<{ viewer: ViewerState | null; loading: boolean }>({ viewer: null, loading: true });
  useEffect(() => {
    let cancelled = false;
    clientApi<CourseDetail>(`/courses/${encodeURIComponent(slug)}`)
      .then((d) => !cancelled && setState({ viewer: d.viewer, loading: false }))
      .catch(() => !cancelled && setState({ viewer: null, loading: false }));
    return () => {
      cancelled = true;
    };
  }, [slug]);
  return <ViewerContext.Provider value={state}>{children}</ViewerContext.Provider>;
}

export function ChapterOutline({ slug, chapters }: { slug: string; chapters: Chapter[] }) {
  const { viewer } = useContext(ViewerContext);
  const done = new Set(viewer?.completedChapterIds ?? []);
  const enrolled = !!viewer?.enrolled || !!viewer?.canEdit;
  return (
    <ol className="card mt-3 divide-y divide-slate-100">
      {chapters.map((ch, i) => {
        const unlocked = enrolled || ch.preview;
        const completed = done.has(ch.id);
        return (
          <li key={ch.id} className="flex items-center gap-4 px-5 py-4">
            <span
              className={`grid h-8 w-8 shrink-0 place-items-center rounded-full text-sm font-semibold ${
                completed ? "bg-emerald-100 text-emerald-800" : "bg-slate-100 text-ink-soft"
              }`}
              aria-label={completed ? "Completed" : `Chapter ${i + 1}`}
            >
              {completed ? "✓" : i + 1}
            </span>
            <div className="min-w-0 flex-1">
              {unlocked ? (
                <a href={routes.chapter(slug, ch.id)} className="font-medium text-ink hover:text-brand-700">
                  {ch.title}
                </a>
              ) : (
                <span className="font-medium text-ink-soft">{ch.title}</span>
              )}
              <div className="text-xs text-ink-muted">
                {ch.durationMin} min
                {ch.videoUrl ? " · video" : ""}
                {ch.quizQuestionCount ? ` · quiz (${ch.quizQuestionCount} questions)` : ""}
              </div>
            </div>
            {ch.preview && !enrolled ? (
              <span className="text-xs font-medium text-brand-700">Free preview</span>
            ) : !unlocked ? (
              <span className="text-xs text-ink-muted" role="img" aria-label="Locked">
                🔒
              </span>
            ) : null}
          </li>
        );
      })}
    </ol>
  );
}

export function EnrollAside({ course, chapters }: { course: Course; chapters: Chapter[] }) {
  const hydrated = useHydrated();
  const { viewer, loading } = useContext(ViewerContext);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const done = new Set(viewer?.completedChapterIds ?? []);
  const next = chapters.find((c) => !done.has(c.id)) ?? chapters[0];
  const start = next ? routes.chapter(course.slug, next.id) : routes.dashboard;

  let action: React.ReactNode;
  if (loading) {
    action = <span className="block h-10 w-full animate-pulse rounded-lg bg-slate-100" aria-hidden />;
  } else if (!viewer?.signedIn) {
    action = (
      <>
        <a href={routes.login(routes.course(course.slug))} className="btn-primary w-full">
          Sign in to enroll
        </a>
        <p className="mt-2 text-center text-xs text-ink-muted">Free for all employees</p>
      </>
    );
  } else if (viewer.enrolled) {
    action = (
      <>
        <a href={start} className="btn-primary w-full">
          {viewer.enrollmentStatus === "COMPLETED" ? "Review course" : "Continue learning"}
        </a>
        {viewer.certificateCode && (
          <a href={routes.certificate(viewer.certificateCode)} className="btn-secondary mt-2 w-full">
            🎓 View certificate
          </a>
        )}
      </>
    );
  } else {
    action = (
      <>
        <button
          className="btn-primary w-full"
          disabled={!hydrated || busy}
          onClick={async () => {
            setBusy(true);
            setError(null);
            try {
              await clientApi(`/courses/${course.id}/enroll`, { method: "POST" });
              window.location.href = start;
            } catch (e) {
              setError(errorMessage(e));
              setBusy(false);
            }
          }}
        >
          {busy ? "Enrolling…" : "Enroll for free"}
        </button>
        {error && (
          <p role="alert" className="mt-2 text-sm text-rose-700">
            {error}
          </p>
        )}
      </>
    );
  }

  return (
    <div className="card p-6">
      {viewer?.enrolled && (
        <div className="mb-4">
          <ProgressBar done={done.size} total={chapters.length} />
        </div>
      )}
      {action}
      {viewer?.canEdit && (
        <a href={routes.studioCourse(course.id)} className="btn-secondary mt-3 w-full">
          Edit in Studio
        </a>
      )}
      <ul className="mt-6 space-y-2 text-sm text-ink-soft">
        <li>📚 {course.chapterCount} chapters</li>
        <li>⏱ {formatMinutes(course.totalMinutes)} total</li>
        <li>🎯 {levelLabel(course.level)} level</li>
        <li>🎓 Certificate on completion</li>
      </ul>
    </div>
  );
}
