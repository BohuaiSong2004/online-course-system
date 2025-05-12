import Link from "next/link";
import { PageShell } from "@ocs/shared/components/PageShell";
import { CourseForm } from "../../components/CourseForm";
import { requireAuthor } from "../../components/guard";

export const metadata = { title: "New course" };

export default async function NewCoursePage() {
  const auth = await requireAuthor("/studio/courses/new");
  if ("page" in auth) return auth.page;
  return (
    <PageShell zone="studio">
      <Link href="/" className="text-sm font-medium text-brand-700 hover:underline">
        ← Studio
      </Link>
      <h1 className="mt-2 text-2xl font-bold">New course</h1>
      <p className="mt-1 text-sm text-ink-soft">Courses start as drafts. Add chapters, then publish.</p>
      <div className="card mt-6 max-w-3xl p-6">
        <CourseForm />
      </div>
    </PageShell>
  );
}
