import Link from "next/link";
import { PageShell } from "@ocs/shared/components/PageShell";
import { ChapterForm } from "../../../../components/ChapterForm";
import { requireAuthor } from "../../../../components/guard";

export const metadata = { title: "New chapter" };

export default async function NewChapterPage({ params }: { params: Promise<{ id: string }> }) {
  const courseId = Number((await params).id);
  const auth = await requireAuthor(`/studio/courses/${courseId}/chapters/new`);
  if ("page" in auth) return auth.page;
  return (
    <PageShell zone="studio">
      <Link href={`/courses/${courseId}`} className="text-sm font-medium text-brand-700 hover:underline">
        ← Back to course
      </Link>
      <h1 className="mt-2 text-2xl font-bold">New chapter</h1>
      <div className="card mt-6 p-6">
        <ChapterForm courseId={courseId} />
      </div>
    </PageShell>
  );
}
