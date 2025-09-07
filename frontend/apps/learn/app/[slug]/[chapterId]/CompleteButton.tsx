"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { clientApi, errorMessage } from "@ocs/shared/client";
import { routes } from "@ocs/shared/routes";
import type { ProgressResponse } from "@ocs/shared/types";

export function CompleteButton({ chapterId, slug, completed }: { chapterId: number; slug: string; completed: boolean }) {
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [certificate, setCertificate] = useState<string | null | undefined>(undefined);

  if (certificate !== undefined) {
    return <CourseCompleteBanner certificate={certificate} />;
  }
  if (completed) {
    return <span className="rounded-lg bg-emerald-50 px-4 py-2 text-sm font-medium text-emerald-800">✓ Completed</span>;
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
              setCertificate(res.certificateCode);
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
      {error && (
        <p role="alert" className="mt-2 text-sm text-rose-700">
          {error}
        </p>
      )}
    </div>
  );
}

export function CourseCompleteBanner({ certificate }: { certificate: string | null }) {
  return (
    <div role="status" className="rounded-lg bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-900">
      🎉 Course complete!{" "}
      {certificate && (
        <a href={routes.certificate(certificate)} className="underline">
          View your certificate
        </a>
      )}{" "}
      ·{" "}
      <a href="/learn" className="underline">
        Back to my learning
      </a>
    </div>
  );
}
