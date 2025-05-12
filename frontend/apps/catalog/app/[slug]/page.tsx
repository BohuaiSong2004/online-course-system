import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { accentFor } from "@ocs/shared/components/CourseCard";
import { LevelBadge, StatusBadge } from "@ocs/shared/components/Badge";
import { Markdown } from "@ocs/shared/components/Markdown";
import { PageShell } from "@ocs/shared/components/PageShell";
import { ProgressBar } from "@ocs/shared/components/ProgressBar";
import { formatDate, formatMinutes, levelLabel } from "@ocs/shared/format";
import { routes } from "@ocs/shared/routes";
import { SITE_URL, apiOrNull } from "@ocs/shared/server";
import type { CourseDetail } from "@ocs/shared/types";
import { EnrollPanel } from "./EnrollPanel";

type Params = Promise<{ slug: string }>;

async function load(slug: string) {
  return apiOrNull<CourseDetail>(`/courses/${encodeURIComponent(slug)}`, { revalidate: 60 });
}

export async function generateMetadata({ params }: { params: Params }): Promise<Metadata> {
  const detail = await load((await params).slug);
  if (!detail) return { title: "Course not found" };
  const { course } = detail;
  return {
    title: course.title,
    description: course.summary,
    alternates: { canonical: `/courses/${course.slug}` },
    openGraph: { title: course.title, description: course.summary, type: "website", url: `/courses/${course.slug}` },
    robots: course.status === "PUBLISHED" ? undefined : { index: false },
  };
}

export default async function CoursePage({ params }: { params: Params }) {
  const detail = await load((await params).slug);
  if (!detail) notFound();
  const { course, chapters, viewer, enrollmentCount } = detail;
  const done = new Set(viewer.completedChapterIds);
  const nextChapter = chapters.find((c) => !done.has(c.id)) ?? chapters[0];

  // Structured data so search engines can show this as a Course rich result.
  const jsonLd = {
    "@context": "https://schema.org",
    "@type": "Course",
    name: course.title,
    description: course.summary,
    url: `${SITE_URL}/courses/${course.slug}`,
    provider: { "@type": "Organization", name: "OpenCourse" },
    educationalLevel: levelLabel(course.level),
    timeRequired: `PT${course.totalMinutes}M`,
    hasCourseInstance: { "@type": "CourseInstance", courseMode: "online", instructor: { "@type": "Person", name: course.instructorName } },
  };

  return (
    <PageShell zone="catalog">
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd).replace(/</g, "\\u003c") }} />
      <nav className="text-sm text-ink-muted">
        <a href={routes.catalog} className="hover:text-ink">
          Courses
        </a>{" "}
        /{" "}
        <a href={`${routes.catalog}?category=${encodeURIComponent(course.category)}`} className="hover:text-ink">
          {course.category}
        </a>
      </nav>

      <div className="mt-4 grid gap-8 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <div className={`rounded-2xl bg-gradient-to-br p-8 text-white ${accentFor(course.category)}`}>
            <div className="flex flex-wrap items-center gap-2">
              <LevelBadge level={course.level} />
              {course.status !== "PUBLISHED" && <StatusBadge status={course.status} />}
            </div>
            <h1 className="mt-3 text-3xl font-bold sm:text-4xl">{course.title}</h1>
            <p className="mt-3 text-lg text-white/90">{course.summary}</p>
            <p className="mt-4 text-sm text-white/80">
              By {course.instructorName} · {course.chapterCount} chapters · {formatMinutes(course.totalMinutes)} ·{" "}
              {enrollmentCount} enrolled
              {course.publishedAt && <> · Published {formatDate(course.publishedAt)}</>}
            </p>
          </div>

          {course.description && (
            <section className="mt-8">
              <h2 className="text-xl font-semibold">About this course</h2>
              <div className="mt-3">
                <Markdown source={course.description} />
              </div>
            </section>
          )}

          <section className="mt-8">
            <h2 className="text-xl font-semibold">Course content</h2>
            <ol className="card mt-3 divide-y divide-slate-100">
              {chapters.map((ch, i) => {
                const unlocked = viewer.enrolled || viewer.canEdit || ch.preview;
                const completed = done.has(ch.id);
                return (
                  <li key={ch.id} className="flex items-center gap-4 px-5 py-4">
                    <span
                      className={`grid h-8 w-8 shrink-0 place-items-center rounded-full text-sm font-semibold ${
                        completed ? "bg-emerald-100 text-emerald-700" : "bg-slate-100 text-ink-soft"
                      }`}
                    >
                      {completed ? "✓" : i + 1}
                    </span>
                    <div className="min-w-0 flex-1">
                      {unlocked ? (
                        <a href={routes.chapter(course.slug, ch.id)} className="font-medium text-ink hover:text-brand-700">
                          {ch.title}
                        </a>
                      ) : (
                        <span className="font-medium text-ink-soft">{ch.title}</span>
                      )}
                      <div className="text-xs text-ink-muted">{ch.durationMin} min</div>
                    </div>
                    {ch.preview && !viewer.enrolled ? (
                      <span className="text-xs font-medium text-brand-700">Free preview</span>
                    ) : !unlocked ? (
                      <span className="text-xs text-ink-muted" aria-label="Locked">
                        🔒
                      </span>
                    ) : null}
                  </li>
                );
              })}
            </ol>
          </section>
        </div>

        <aside className="lg:sticky lg:top-24 lg:self-start">
          <div className="card p-6">
            {viewer.enrolled && (
              <div className="mb-4">
                <ProgressBar done={viewer.completedChapterIds.length} total={chapters.length} />
              </div>
            )}
            <EnrollPanel
              courseId={course.id}
              slug={course.slug}
              signedIn={viewer.signedIn}
              enrolled={viewer.enrolled}
              completed={viewer.enrollmentStatus === "COMPLETED"}
              nextChapterId={nextChapter?.id ?? null}
              published={course.status === "PUBLISHED"}
            />
            {viewer.canEdit && (
              <a href={routes.studioCourse(course.id)} className="btn-secondary mt-3 w-full">
                Edit in Studio
              </a>
            )}
            <ul className="mt-6 space-y-2 text-sm text-ink-soft">
              <li>📚 {course.chapterCount} chapters</li>
              <li>⏱ {formatMinutes(course.totalMinutes)} total</li>
              <li>🎯 {levelLabel(course.level)} level</li>
              <li>✅ Progress saved automatically</li>
            </ul>
          </div>
        </aside>
      </div>
    </PageShell>
  );
}
