"use client";
import { useEffect, useState } from "react";
import { CourseCard } from "@ocs/shared/components/CourseCard";
import { EmptyState } from "@ocs/shared/components/SignInPrompt";
import type { Course, PageResult } from "@ocs/shared/types";

/**
 * Server-rendered course grid. If the page was generated while the API was unreachable (for example
 * during `docker build`), the browser fetches the list instead of showing an empty catalog until the
 * next ISR regeneration.
 */
export function FeaturedCourses({ initial, loadInBrowser }: { initial: Course[]; loadInBrowser: boolean }) {
  const [courses, setCourses] = useState(initial);
  const [loading, setLoading] = useState(loadInBrowser);

  useEffect(() => {
    if (!loadInBrowser) return;
    fetch("/backend/courses?size=6")
      .then((r) => (r.ok ? r.json() : Promise.reject(r.status)))
      .then((p: PageResult<Course>) => setCourses(p.items))
      .catch(() => setCourses([]))
      .finally(() => setLoading(false));
  }, [loadInBrowser]);

  if (loading) {
    return (
      <div className="mt-4 grid gap-5 sm:grid-cols-2 lg:grid-cols-3" aria-busy="true">
        {[0, 1, 2].map((i) => (
          <div key={i} className="h-56 animate-pulse rounded-xl bg-slate-100" />
        ))}
      </div>
    );
  }
  if (courses.length === 0) {
    return (
      <div className="mt-4">
        <EmptyState title="No courses yet">The catalog is empty or the API is not reachable.</EmptyState>
      </div>
    );
  }
  return (
    <div className="mt-4 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      {courses.map((c) => (
        <CourseCard key={c.id} course={c} />
      ))}
    </div>
  );
}
