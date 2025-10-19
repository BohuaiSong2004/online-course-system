import type { Metadata } from "next";
import { CourseCard } from "@ocs/shared/components/CourseCard";
import { PageShell } from "@ocs/shared/components/PageShell";
import { EmptyState } from "@ocs/shared/components/SignInPrompt";
import { levelLabel } from "@ocs/shared/format";
import { routes } from "@ocs/shared/routes";
import { publicApi } from "@ocs/shared/server";
import { LEVELS, type Course, type PageResult } from "@ocs/shared/types";

type Search = Promise<Record<string, string | string[] | undefined>>;
const one = (v: string | string[] | undefined) => (Array.isArray(v) ? v[0] : v) ?? "";

export async function generateMetadata({ searchParams }: { searchParams: Search }): Promise<Metadata> {
  const sp = await searchParams;
  const category = one(sp.category);
  const title = category ? `${category} courses` : "All courses";
  return {
    title,
    description: `Browse ${category ? category.toLowerCase() + " " : ""}courses on OpenCourse — self-paced lessons with progress tracking.`,
    alternates: { canonical: category ? `/courses?category=${encodeURIComponent(category)}` : "/courses" },
  };
}

function buildQuery(params: Record<string, string>) {
  const q = new URLSearchParams(Object.entries(params).filter(([, v]) => v));
  const s = q.toString();
  return s ? `${routes.catalog}?${s}` : routes.catalog;
}

export default async function CatalogPage({ searchParams }: { searchParams: Search }) {
  const sp = await searchParams;
  const q = one(sp.q);
  const category = one(sp.category);
  const level = one(sp.level);
  const page = Math.max(1, Number(one(sp.page)) || 1);

  const query = new URLSearchParams({ page: String(page), size: "9" });
  if (q) query.set("q", q);
  if (category) query.set("category", category);
  if (level) query.set("level", level);

  const [result, categories] = await Promise.all([
    publicApi<PageResult<Course>>(`/courses?${query}`, 60),
    publicApi<string[]>("/courses/categories", 300),
  ]);
  const base = { q, category, level };

  return (
    <PageShell zone="catalog">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">{category ? `${category} courses` : "All courses"}</h1>
          <p className="mt-1 text-ink-soft">
            {result.total} course{result.total === 1 ? "" : "s"}
            {q && (
              <>
                {" "}
                matching “<span className="font-medium text-ink">{q}</span>”
              </>
            )}
          </p>
        </div>
        <form action={routes.catalog} method="get" className="flex w-full gap-2 sm:w-96">
          {category && <input type="hidden" name="category" value={category} />}
          {level && <input type="hidden" name="level" value={level} />}
          <input name="q" defaultValue={q} placeholder="Search courses…" className="input" aria-label="Search courses" />
          <button className="btn-primary" type="submit">
            Search
          </button>
        </form>
      </div>

      <div className="mt-6 flex flex-wrap items-center gap-2">
        <a href={buildQuery({ ...base, category: "" })} className={!category ? "btn-primary" : "btn-secondary"}>
          All topics
        </a>
        {categories.map((c) => (
          <a key={c} href={buildQuery({ ...base, category: c })} className={c === category ? "btn-primary" : "btn-secondary"}>
            {c}
          </a>
        ))}
        <span className="mx-2 hidden h-6 w-px bg-slate-300 sm:block" />
        {LEVELS.map((l) => (
          <a
            key={l}
            href={buildQuery({ ...base, level: l === level ? "" : l })}
            className={`rounded-full px-3 py-1 text-xs font-medium ring-1 ring-inset ${
              l === level ? "bg-ink text-white ring-ink" : "bg-white text-ink-soft ring-slate-300 hover:bg-slate-50"
            }`}
          >
            {levelLabel(l)}
          </a>
        ))}
      </div>

      <div className="mt-8">
        <h2 className="sr-only">Results</h2>
        {result.items.length === 0 ? (
          <EmptyState title="No courses found">
            Try a different search or{" "}
            <a href={routes.catalog} className="font-medium text-brand-700 hover:underline">
              clear the filters
            </a>
            .
          </EmptyState>
        ) : (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {result.items.map((c) => (
              <CourseCard key={c.id} course={c} />
            ))}
          </div>
        )}
      </div>

      {result.totalPages > 1 && (
        <nav className="mt-10 flex items-center justify-center gap-2" aria-label="Pagination">
          {page > 1 && (
            <a className="btn-secondary" href={buildQuery({ ...base, page: String(page - 1) })} rel="prev">
              ← Previous
            </a>
          )}
          <span className="px-3 text-sm text-ink-muted">
            Page {page} of {result.totalPages}
          </span>
          {page < result.totalPages && (
            <a className="btn-secondary" href={buildQuery({ ...base, page: String(page + 1) })} rel="next">
              Next →
            </a>
          )}
        </nav>
      )}
    </PageShell>
  );
}
