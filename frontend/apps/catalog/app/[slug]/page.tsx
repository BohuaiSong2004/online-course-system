import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { accentFor } from "@ocs/shared/components/CourseCard";
import { LevelBadge } from "@ocs/shared/components/Badge";
import { Markdown } from "@ocs/shared/components/Markdown";
import { PageShell } from "@ocs/shared/components/PageShell";
import { formatDate, formatMinutes, levelLabel } from "@ocs/shared/format";
import { routes } from "@ocs/shared/routes";
import { SITE_URL, publicApi, publicApiOrNull } from "@ocs/shared/server";
import type { CourseDetail } from "@ocs/shared/types";
import { ChapterOutline, EnrollAside, ViewerProvider } from "./CourseViewer";

type Params = Promise<{ slug: string }>;

// Course pages are static HTML regenerated at most once a minute (ISR). Everything personal —
// enrollment, progress, edit rights — is loaded in the browser, so the cached page is the same for all.
export const revalidate = 60;

export async function generateStaticParams() {
  try {
    const courses = await publicApi<{ slug: string }[]>("/courses/sitemap", 300);
    return courses.map((c) => ({ slug: c.slug }));
  } catch {
    return []; // API not reachable at build time: pages are generated on first request instead
  }
}

async function load(slug: string) {
  return publicApiOrNull<CourseDetail>(`/courses/${encodeURIComponent(slug)}`, 60);
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
  };
}

export default async function CoursePage({ params }: { params: Params }) {
  const detail = await load((await params).slug);
  if (!detail) notFound();
  const { course, chapters, enrollmentCount } = detail;

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
      <nav className="text-sm text-ink-muted" aria-label="Breadcrumb">
        <a href={routes.catalog} className="hover:text-ink">
          Courses
        </a>{" "}
        /{" "}
        <a href={`${routes.catalog}?category=${encodeURIComponent(course.category)}`} className="hover:text-ink">
          {course.category}
        </a>
      </nav>

      <ViewerProvider slug={course.slug}>
        <div className="mt-4 grid gap-8 lg:grid-cols-3">
          <div className="min-w-0 lg:col-span-2">
            <div className={`rounded-2xl bg-gradient-to-br p-6 text-white sm:p-8 ${accentFor(course.category)}`}>
              <LevelBadge level={course.level} />
              <h1 className="mt-3 text-3xl font-bold sm:text-4xl">{course.title}</h1>
              <p className="mt-3 text-lg text-white/90">{course.summary}</p>
              <p className="mt-4 text-sm text-white/90">
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
              <ChapterOutline slug={course.slug} chapters={chapters} />
            </section>
          </div>

          <aside className="lg:sticky lg:top-24 lg:self-start">
            <EnrollAside course={course} chapters={chapters} />
          </aside>
        </div>
      </ViewerProvider>
    </PageShell>
  );
}
