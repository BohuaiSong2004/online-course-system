"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { clientApi, errorMessage } from "@ocs/shared/client";
import type { Chapter, Course } from "@ocs/shared/types";

export function PublishControls({ course }: { course: Course }) {
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const published = course.status === "PUBLISHED";

  async function run(action: () => Promise<unknown>, after?: () => void) {
    setBusy(true);
    setError(null);
    try {
      await action();
      after ? after() : router.refresh();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-3">
      <button
        className={published ? "btn-secondary w-full" : "btn-primary w-full"}
        disabled={busy}
        onClick={() => run(() => clientApi(`/studio/courses/${course.id}/${published ? "unpublish" : "publish"}`, { method: "POST" }))}
      >
        {published ? "Unpublish" : "Publish course"}
      </button>
      <button
        className="btn-danger w-full"
        disabled={busy}
        onClick={() => {
          if (!window.confirm(`Delete “${course.title}”? This removes all chapters and enrollments.`)) return;
          run(() => clientApi(`/studio/courses/${course.id}`, { method: "DELETE" }), () => router.push("/"));
        }}
      >
        Delete course
      </button>
      {error && <p className="text-sm text-rose-700">{error}</p>}
    </div>
  );
}

export function ChapterList({ courseId, chapters }: { courseId: number; chapters: Chapter[] }) {
  const router = useRouter();
  const [order, setOrder] = useState(chapters);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function move(index: number, delta: number) {
    const target = index + delta;
    if (target < 0 || target >= order.length) return;
    const next = [...order];
    [next[index], next[target]] = [next[target], next[index]];
    setOrder(next);
    setBusy(true);
    setError(null);
    try {
      await clientApi(`/studio/courses/${courseId}/chapters/order`, { method: "PUT", json: { chapterIds: next.map((c) => c.id) } });
      router.refresh();
    } catch (e) {
      setError(errorMessage(e));
      setOrder(order);
    } finally {
      setBusy(false);
    }
  }

  if (order.length === 0) {
    return <p className="px-5 py-8 text-center text-sm text-ink-muted">No chapters yet. Add one to publish this course.</p>;
  }
  return (
    <>
      <ol className="divide-y divide-slate-100">
        {order.map((c, i) => (
          <li key={c.id} className="flex items-center gap-3 px-5 py-3">
            <span className="w-6 text-sm text-ink-muted">{i + 1}</span>
            <a href={`/studio/courses/${courseId}/chapters/${c.id}`} className="flex-1 font-medium hover:text-brand-700">
              {c.title}
              {c.preview && <span className="ml-2 text-xs font-normal text-brand-700">preview</span>}
            </a>
            <span className="text-xs text-ink-muted">{c.durationMin} min</span>
            <div className="flex gap-1">
              <button className="btn-ghost px-2 py-1" disabled={busy || i === 0} onClick={() => move(i, -1)} aria-label="Move up">
                ↑
              </button>
              <button className="btn-ghost px-2 py-1" disabled={busy || i === order.length - 1} onClick={() => move(i, 1)} aria-label="Move down">
                ↓
              </button>
            </div>
          </li>
        ))}
      </ol>
      {error && <p className="px-5 pb-3 text-sm text-rose-700">{error}</p>}
    </>
  );
}
