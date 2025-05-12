"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { clientApi, errorMessage } from "@ocs/shared/client";
import type { ProgressResponse } from "@ocs/shared/types";

export function CompleteButton({ chapterId, slug, completed }: { chapterId: number; slug: string; completed: boolean }) {
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [courseDone, setCourseDone] = useState(false);

  if (courseDone) {
    return (
      <div className="rounded-lg bg-emerald-50 px-4 py-2 text-sm font-medium text-emerald-700">
        🎉 Course complete!{" "}
        <a href="/learn" className="underline">
          Back to my learning
        </a>
      </div>
    );
  }
  if (completed) {
    return <span className="rounded-lg bg-emerald-50 px-4 py-2 text-sm font-medium text-emerald-700">✓ Completed</span>;
  }
  return (
    <div className="text-center">
      <button
        className="btn-primary"
        disabled={busy}
        onClick={async () => {
          setBusy(true);
          setError(null);
          try {
            const res = await clientApi<ProgressResponse>(`/chapters/${chapterId}/complete`, { method: "POST" });
            if (res.courseCompleted) {
              setCourseDone(true);
              router.refresh();
            } else if (res.nextChapterId) {
              router.push(`/${slug}/${res.nextChapterId}`);
              router.refresh();
            } else {
              router.refresh();
            }
          } catch (e) {
            setError(errorMessage(e));
          } finally {
            setBusy(false);
          }
        }}
      >
        {busy ? "Saving…" : "Mark complete & continue"}
      </button>
      {error && <p className="mt-2 text-sm text-rose-700">{error}</p>}
    </div>
  );
}
