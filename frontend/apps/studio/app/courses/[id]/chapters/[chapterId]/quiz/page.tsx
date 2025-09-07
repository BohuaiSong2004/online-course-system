import Link from "next/link";
import { notFound } from "next/navigation";
import { PageShell } from "@ocs/shared/components/PageShell";
import { ApiError, api } from "@ocs/shared/server";
import type { Chapter, QuizQuestion } from "@ocs/shared/types";
import { QuizEditor } from "../../../../../components/QuizEditor";
import { requireAuthor } from "../../../../../components/guard";

export const metadata = { title: "Edit quiz" };

export default async function QuizPage({ params }: { params: Promise<{ id: string; chapterId: string }> }) {
  const { id, chapterId } = await params;
  const courseId = Number(id);
  const auth = await requireAuthor(`/studio/courses/${courseId}/chapters/${chapterId}/quiz`);
  if ("page" in auth) return auth.page;

  let chapter: Chapter;
  let quiz: { passPercent: number; questions: QuizQuestion[] };
  try {
    [chapter, quiz] = await Promise.all([
      api<Chapter>(`/studio/chapters/${Number(chapterId)}`),
      api<{ passPercent: number; questions: QuizQuestion[] }>(`/studio/chapters/${Number(chapterId)}/quiz`),
    ]);
  } catch (e) {
    if (e instanceof ApiError && [400, 403, 404].includes(e.status)) notFound();
    throw e;
  }
  if (chapter.courseId !== courseId) notFound();

  return (
    <PageShell zone="studio" user={auth.user}>
      <Link href={`/courses/${courseId}/chapters/${chapter.id}`} className="text-sm font-medium text-brand-700 hover:underline">
        ← Back to chapter
      </Link>
      <h1 className="mt-2 text-2xl font-bold">Quiz · {chapter.title}</h1>
      <p className="mt-1 text-sm text-ink-soft">
        Learners complete this chapter by passing the quiz. Each question has exactly one correct answer.
      </p>
      <div className="card mt-6 p-6">
        <QuizEditor courseId={courseId} chapterId={chapter.id} initial={quiz} />
      </div>
    </PageShell>
  );
}
