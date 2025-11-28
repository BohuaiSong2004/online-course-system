"use client";
import { useHydrated } from "@ocs/shared/hooks";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { clientApi, errorMessage } from "@ocs/shared/client";
import { levelLabel } from "@ocs/shared/format";
import { LEVELS, type Course } from "@ocs/shared/types";

export function CourseForm({ course }: { course?: Course }) {
  const hydrated = useHydrated();
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setSaved(false);
    const body = Object.fromEntries(new FormData(e.currentTarget));
    try {
      if (course) {
        await clientApi<Course>(`/studio/courses/${course.id}`, { method: "PUT", json: body });
        setSaved(true);
        router.refresh();
      } else {
        const created = await clientApi<Course>("/studio/courses", { method: "POST", json: body });
        router.push(`/courses/${created.id}`);
      }
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form method="post" onSubmit={onSubmit} className="space-y-4">
      <div>
        <label className="label" htmlFor="title">
          Title
        </label>
        <input id="title" name="title" required maxLength={150} defaultValue={course?.title} className="input" />
      </div>
      <div>
        <label className="label" htmlFor="summary">
          Summary <span className="font-normal text-ink-muted">(shown on cards and in search results)</span>
        </label>
        <textarea id="summary" name="summary" rows={2} maxLength={500} defaultValue={course?.summary} className="input" />
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <label className="label" htmlFor="category">
            Category
          </label>
          <input id="category" name="category" required maxLength={50} defaultValue={course?.category} className="input" placeholder="Engineering" />
        </div>
        <div>
          <label className="label" htmlFor="level">
            Level
          </label>
          <select id="level" name="level" defaultValue={course?.level ?? "BEGINNER"} className="input">
            {LEVELS.map((l) => (
              <option key={l} value={l}>
                {levelLabel(l)}
              </option>
            ))}
          </select>
        </div>
      </div>
      <div>
        <label className="label" htmlFor="description">
          Description <span className="font-normal text-ink-muted">(Markdown)</span>
        </label>
        <textarea id="description" name="description" rows={6} defaultValue={course?.description ?? ""} className="input font-mono" />
      </div>
      {error && <p className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</p>}
      <div className="flex items-center gap-3">
        <button className="btn-primary" disabled={!hydrated || busy}>
          {busy ? "Saving…" : course ? "Save changes" : "Create course"}
        </button>
        {saved && <span className="text-sm text-emerald-700">Saved ✓</span>}
      </div>
    </form>
  );
}
