"use client";
import { useState } from "react";
import { clientApi, errorMessage } from "@ocs/shared/client";
import { routes } from "@ocs/shared/routes";

interface Props {
  courseId: number;
  slug: string;
  signedIn: boolean;
  enrolled: boolean;
  completed: boolean;
  nextChapterId: number | null;
  published: boolean;
}

export function EnrollPanel({ courseId, slug, signedIn, enrolled, completed, nextChapterId, published }: Props) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const start = nextChapterId ? routes.chapter(slug, nextChapterId) : routes.dashboard;

  if (!published) {
    return <p className="text-sm text-ink-soft">This course is a draft and not open for enrollment yet.</p>;
  }
  if (!signedIn) {
    return (
      <>
        <a href={routes.login(routes.course(slug))} className="btn-primary w-full">
          Sign in to enroll
        </a>
        <p className="mt-2 text-center text-xs text-ink-muted">Free for all employees</p>
      </>
    );
  }
  if (enrolled) {
    return (
      <a href={start} className="btn-primary w-full">
        {completed ? "Review course" : "Continue learning"}
      </a>
    );
  }
  return (
    <>
      <button
        className="btn-primary w-full"
        disabled={busy}
        onClick={async () => {
          setBusy(true);
          setError(null);
          try {
            await clientApi(`/courses/${courseId}/enroll`, { method: "POST" });
            window.location.href = start;
          } catch (e) {
            setError(errorMessage(e));
            setBusy(false);
          }
        }}
      >
        {busy ? "Enrolling…" : "Enroll for free"}
      </button>
      {error && <p className="mt-2 text-sm text-rose-700">{error}</p>}
    </>
  );
}
