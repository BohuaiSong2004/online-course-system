import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Markdown } from "@ocs/shared/components/Markdown";
import { PageShell } from "@ocs/shared/components/PageShell";
import { ProgressBar } from "@ocs/shared/components/ProgressBar";
import { SignInPrompt } from "@ocs/shared/components/SignInPrompt";
import { routes } from "@ocs/shared/routes";
import { ApiError, api, getCurrentUser } from "@ocs/shared/server";
import { Video } from "@ocs/shared/components/Video";
import type { ChapterView, CourseDetail } from "@ocs/shared/types";
import { CompleteButton } from "./CompleteButton";
import { Quiz } from "./Quiz";

type Params = Promise<{ slug: string; chapterId: string }>;

export const metadata: Metadata = { robots: { index: false } };

export default async function ChapterPage({ params }: { params: Params }) {
  const { slug, chapterId } = await params;
  const id = Number(chapterId);
  if (!Number.isInteger(id) || id <= 0) notFound();

  let view: ChapterView;
  let detail: CourseDetail;
  const user = await getCurrentUser();
  try {
    [view, detail] = await Promise.all([
      api<ChapterView>(`/chapters/${id}`),
      api<CourseDetail>(`/courses/${encodeURIComponent(slug)}`),
    ]);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    if (e instanceof ApiError && e.status === 401) {
      return (
        <PageShell zone="learn" user={user}>
          <SignInPrompt next={routes.chapter(slug, id)} title="Sign in to read this chapter" />
        </PageShell>
      );
    }
    if (e instanceof ApiError && e.status === 403) {
      return (
        <PageShell zone="learn" user={user}>
          <div className="card mx-auto max-w-md p-8 text-center">
            <h1 className="text-xl font-semibold">This chapter is locked</h1>
            <p className="mt-2 text-sm text-ink-soft">Enroll in the course to unlock every chapter.</p>
            <a href={routes.course(slug)} className="btn-primary mt-6">
              View course
            </a>
          </div>
        </PageShell>
      );
    }
    throw e;
  }
  if (view.courseSlug !== slug) notFound();

  const { chapter } = view;
  const done = new Set(detail.viewer.completedChapterIds);
  const unlocked = (c: { preview: boolean }) => detail.viewer.enrolled || detail.viewer.canEdit || c.preview;

  return (
    <PageShell zone="learn" user={user}>
      <div className="grid gap-8 lg:grid-cols-4">
        <aside className="min-w-0 lg:col-span-1">
          <a href={routes.course(slug)} className="text-sm font-medium text-brand-700 hover:underline">
            ← {view.courseTitle}
          </a>
          {detail.viewer.enrolled && (
            <div className="mt-4">
              <ProgressBar done={done.size} total={detail.chapters.length} />
            </div>
          )}
          <ol className="mt-4 space-y-1">
            {detail.chapters.map((c, i) => {
              const current = c.id === chapter.id;
              const label = (
                <>
                  <span className={`w-5 text-xs ${done.has(c.id) ? "text-emerald-600" : "text-ink-muted"}`}>
                    {done.has(c.id) ? "✓" : i + 1}
                  </span>
                  <span className="flex-1">{c.title}</span>
                </>
              );
              const cls = `flex gap-2 rounded-lg px-3 py-2 text-sm ${
                current ? "bg-brand-50 font-medium text-brand-700" : "text-ink-soft hover:bg-slate-100"
              }`;
              return (
                <li key={c.id}>
                  {unlocked(c) ? (
                    <Link href={`/${slug}/${c.id}`} className={cls} aria-current={current ? "page" : undefined}>
                      {label}
                    </Link>
                  ) : (
                    <span className={`${cls} cursor-not-allowed opacity-60`}>{label} 🔒</span>
                  )}
                </li>
              );
            })}
          </ol>
        </aside>

        <article className="min-w-0 lg:col-span-3">
          <div className="card p-8">
            <p className="text-sm text-ink-muted">
              Chapter {chapter.position} · {chapter.durationMin} min
              {chapter.preview && !view.enrolled && " · Free preview"}
            </p>
            <h1 className="mt-1 text-3xl font-bold">{chapter.title}</h1>
            {chapter.videoUrl && (
              <div className="mt-6">
                <Video url={chapter.videoUrl} title={chapter.title} />
              </div>
            )}
            <div className="mt-6">
              <Markdown source={chapter.content ?? ""} />
            </div>
          </div>

          {view.quiz.length > 0 && (
            <section className="card mt-6 p-6 sm:p-8" aria-labelledby="quiz-heading">
              <Quiz
                chapterId={chapter.id}
                slug={slug}
                questions={view.quiz}
                passPercent={chapter.quizPassPercent ?? 70}
                enrolled={view.enrolled}
                bestAttempt={view.bestAttempt}
              />
            </section>
          )}

          <div className="mt-6 flex flex-wrap items-center justify-between gap-3">
            {view.prevChapterId ? (
              <Link href={`/${slug}/${view.prevChapterId}`} className="btn-secondary">
                ← Previous
              </Link>
            ) : (
              <span />
            )}
            {view.quiz.length > 0 ? (
              view.completed ? (
                <span className="rounded-lg bg-emerald-50 px-4 py-2 text-sm font-medium text-emerald-800">✓ Quiz passed</span>
              ) : (
                <span className="text-sm text-ink-muted">Pass the quiz to complete this chapter</span>
              )
            ) : view.enrolled ? (
              <CompleteButton chapterId={chapter.id} slug={slug} completed={view.completed} />
            ) : (
              <a href={routes.course(slug)} className="btn-primary">
                Enroll to track progress
              </a>
            )}
            {view.nextChapterId && (
              <Link href={`/${slug}/${view.nextChapterId}`} className="btn-secondary">
                Next →
              </Link>
            )}
          </div>
        </article>
      </div>
    </PageShell>
  );
}
