import { CourseCard } from "@ocs/shared/components/CourseCard";
import { PageShell } from "@ocs/shared/components/PageShell";
import { EmptyState } from "@ocs/shared/components/SignInPrompt";
import { routes } from "@ocs/shared/routes";
import { publicApi } from "@ocs/shared/server";
import type { Course, PageResult } from "@ocs/shared/types";

async function loadFeatured(): Promise<{ courses: Course[]; total: number; categories: string[] }> {
  try {
    const [page, categories] = await Promise.all([
      publicApi<PageResult<Course>>("/courses?size=6", 60),
      publicApi<string[]>("/courses/categories", 300),
    ]);
    return { courses: page.items, total: page.total, categories };
  } catch {
    return { courses: [], total: 0, categories: [] };
  }
}

// Static page regenerated at most once a minute (ISR): no per-request rendering for the busiest URL.
export const revalidate = 60;

export default async function HomePage() {
  const { courses, total, categories } = await loadFeatured();
  return (
    <PageShell zone="shell">
      <section className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-brand-700 via-brand-600 to-brand-500 px-8 py-14 text-white">
        <div className="max-w-2xl">
          <p className="text-sm font-medium uppercase tracking-wider text-brand-100">Corporate learning portal</p>
          <h1 className="mt-3 text-4xl font-bold leading-tight sm:text-5xl">Onboard faster. Learn on your schedule.</h1>
          <p className="mt-4 text-lg text-brand-100">
            Structured courses from your team&apos;s experts — track progress chapter by chapter and pick up right
            where you left off.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <a href={routes.catalog} className="btn bg-white text-brand-700 hover:bg-brand-50">
              Browse {total > 0 ? `${total} courses` : "courses"}
            </a>
            <a href={routes.dashboard} className="btn border border-white/40 text-white hover:bg-white/10">
              My learning
            </a>
          </div>
        </div>
        <div className="pointer-events-none absolute -right-24 -top-24 h-96 w-96 rounded-full bg-white/10 blur-2xl" aria-hidden />
      </section>

      {categories.length > 0 && (
        <section className="mt-10">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-ink-muted">Browse by topic</h2>
          <div className="mt-3 flex flex-wrap gap-2">
            {categories.map((c) => (
              <a key={c} href={`${routes.catalog}?category=${encodeURIComponent(c)}`} className="btn-secondary">
                {c}
              </a>
            ))}
          </div>
        </section>
      )}

      <section className="mt-10 grid gap-4 sm:grid-cols-3" aria-label="Highlights">
        {[
          ["Assigned training", "Managers assign courses with due dates and track completion by team."],
          ["Quizzes & certificates", "Check understanding chapter by chapter; earn a verifiable certificate."],
          ["Learn anywhere", "Pick up where you left off on any device. Progress saves automatically."],
        ].map(([title, text]) => (
          <div key={title} className="card p-5">
            <h2 className="font-semibold">{title}</h2>
            <p className="mt-1 text-sm text-ink-soft">{text}</p>
          </div>
        ))}
      </section>

      <section className="mt-10">
        <div className="flex items-end justify-between">
          <h2 className="text-2xl font-semibold">Newest courses</h2>
          <a href={routes.catalog} className="text-sm font-medium text-brand-700 hover:underline">
            View all →
          </a>
        </div>
        {courses.length === 0 ? (
          <div className="mt-4">
            <EmptyState title="No courses yet">The catalog is empty or the API is not reachable.</EmptyState>
          </div>
        ) : (
          <div className="mt-4 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {courses.map((c) => (
              <CourseCard key={c.id} course={c} />
            ))}
          </div>
        )}
      </section>
    </PageShell>
  );
}
