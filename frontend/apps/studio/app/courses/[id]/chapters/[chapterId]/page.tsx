import Link from "next/link";
import { notFound } from "next/navigation";
import { PageShell } from "@ocs/shared/components/PageShell";
import { ApiError, api } from "@ocs/shared/server";
import type { Chapter } from "@ocs/shared/types";
import { ChapterForm } from "../../../../components/ChapterForm";
import { requireAuthor } from "../../../../components/guard";

export const metadata = { title: "Edit chapter" };

export default async function EditChapterPage({ params }: { params: Promise<{ id: string; chapterId: string }> }) {
  const { id, chapterId } = await params;
  const courseId = Number(id);
  const auth = await requireAuthor(`/studio/courses/${courseId}/chapters/${chapterId}`);
  if ("page" in auth) return auth.page;

  let chapter: Chapter;
  try {
    chapter = await api<Chapter>(`/studio/chapters/${Number(chapterId)}`);
  } catch (e) {
    if (e instanceof ApiError && [400, 403, 404].includes(e.status)) notFound();
    throw e;
  }
  if (chapter.courseId !== courseId) notFound();

  return (
    <PageShell zone="studio">
      <Link href={`/courses/${courseId}`} className="text-sm font-medium text-brand-700 hover:underline">
        ← Back to course
      </Link>
      <h1 className="mt-2 text-2xl font-bold">Edit chapter</h1>
      <div className="card mt-6 p-6">
        <ChapterForm courseId={courseId} chapter={chapter} />
      </div>
    </PageShell>
  );
}
