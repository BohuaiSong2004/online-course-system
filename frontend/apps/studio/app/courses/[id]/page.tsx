import Link from "next/link";
import { notFound } from "next/navigation";
import { StatusBadge } from "@ocs/shared/components/Badge";
import { PageShell } from "@ocs/shared/components/PageShell";
import { formatMinutes } from "@ocs/shared/format";
import { routes } from "@ocs/shared/routes";
import { ApiError, api } from "@ocs/shared/server";
import type { CourseDetail } from "@ocs/shared/types";
import { ChapterList, PublishControls } from "../../components/CourseControls";
import { CourseForm } from "../../components/CourseForm";
import { requireAuthor } from "../../components/guard";

export const metadata = { title: "Edit course" };

export default async function EditCoursePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const auth = await requireAuthor(`/studio/courses/${id}`);
  if ("page" in auth) return auth.page;

  let detail: CourseDetail;
  try {
    detail = await api<CourseDetail>(`/studio/courses/${Number(id)}`);
  } catch (e) {
    if (e instanceof ApiError && (e.status === 404 || e.status === 403 || e.status === 400)) notFound();
    throw e;
  }
  const { course, chapters, enrollmentCount } = detail;

  return (
    <PageShell zone="studio" user={auth.user}>
      <Link href="/" className="text-sm font-medium text-brand-700 hover:underline">
        ← Studio
      </Link>
      <div className="mt-2 flex flex-wrap items-center gap-3">
        <h1 className="text-2xl font-bold">{course.title}</h1>
        <StatusBadge status={course.status} />
      </div>
      <p className="mt-1 text-sm text-ink-soft">
        {chapters.length} chapters · {formatMinutes(course.totalMinutes)} · {enrollmentCount} enrolled
        {course.status === "PUBLISHED" && (
          <>
            {" "}
            ·{" "}
            <a href={routes.course(course.slug)} className="text-brand-700 hover:underline">
              View public page ↗
            </a>
          </>
        )}
      </p>

      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <section className="card">
            <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
              <h2 className="font-semibold">Chapters</h2>
              <Link href={`/courses/${course.id}/chapters/new`} className="btn-primary py-1.5">
                + Add chapter
              </Link>
            </div>
            <ChapterList key={chapters.map((c) => c.id).join(",")} courseId={course.id} chapters={chapters} />
          </section>
          <section className="card p-6">
            <h2 className="mb-4 font-semibold">Course details</h2>
            <CourseForm course={course} />
          </section>
        </div>
        <aside className="card h-fit p-6">
          <h2 className="font-semibold">Visibility</h2>
          <p className="mb-4 mt-1 text-sm text-ink-soft">
            {course.status === "PUBLISHED"
              ? "Live in the catalog. Unpublishing hides it from new learners."
              : "Draft — only you and admins can see it."}
          </p>
          <PublishControls course={course} />
        </aside>
      </div>
    </PageShell>
  );
}
