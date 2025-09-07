"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { clientApi, errorMessage } from "@ocs/shared/client";
import { Markdown } from "@ocs/shared/components/Markdown";
import type { Chapter } from "@ocs/shared/types";

export function ChapterForm({ courseId, chapter }: { courseId: number; chapter?: Chapter }) {
  const router = useRouter();
  const [content, setContent] = useState(chapter?.content ?? "");
  const [tab, setTab] = useState<"write" | "preview">("write");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    const f = new FormData(e.currentTarget);
    const body = {
      title: f.get("title"),
      videoUrl: f.get("videoUrl"),
      durationMin: Number(f.get("durationMin")),
      preview: f.get("preview") === "on",
      content,
    };
    try {
      if (chapter) {
        await clientApi(`/studio/chapters/${chapter.id}`, { method: "PUT", json: body });
      } else {
        await clientApi(`/studio/courses/${courseId}/chapters`, { method: "POST", json: body });
      }
      router.push(`/courses/${courseId}`);
      router.refresh();
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
    }
  }

  async function onDelete() {
    if (!chapter || !window.confirm(`Delete chapter “${chapter.title}”?`)) return;
    setBusy(true);
    try {
      await clientApi(`/studio/chapters/${chapter.id}`, { method: "DELETE" });
      router.push(`/courses/${courseId}`);
      router.refresh();
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-[1fr_140px]">
        <div>
          <label className="label" htmlFor="title">
            Chapter title
          </label>
          <input id="title" name="title" required maxLength={150} defaultValue={chapter?.title} className="input" />
        </div>
        <div>
          <label className="label" htmlFor="durationMin">
            Minutes
          </label>
          <input id="durationMin" name="durationMin" type="number" min={1} max={600} defaultValue={chapter?.durationMin ?? 10} className="input" />
        </div>
      </div>
      <div>
        <label className="label" htmlFor="videoUrl">
          Video URL <span className="font-normal text-ink-muted">(optional — YouTube or Vimeo links are embedded)</span>
        </label>
        <input
          id="videoUrl"
          name="videoUrl"
          type="url"
          pattern="https://.*"
          placeholder="https://www.youtube.com/watch?v=…"
          defaultValue={chapter?.videoUrl ?? ""}
          className="input"
        />
      </div>
      <label className="flex items-center gap-2 text-sm text-ink-soft">
        <input type="checkbox" name="preview" defaultChecked={chapter?.preview} className="h-4 w-4 rounded border-slate-300" />
        Free preview (visible without enrolling)
      </label>
      <div>
        <div className="mb-2 flex gap-1">
          {(["write", "preview"] as const).map((t) => (
            <button key={t} type="button" onClick={() => setTab(t)} className={tab === t ? "btn-secondary py-1" : "btn-ghost py-1"}>
              {t === "write" ? "Write" : "Preview"}
            </button>
          ))}
          <span className="ml-auto self-center text-xs text-ink-muted">Markdown supported</span>
        </div>
        {tab === "write" ? (
          <textarea
            aria-label="Chapter content"
            rows={18}
            value={content}
            onChange={(e) => setContent(e.target.value)}
            className="input font-mono text-[13px] leading-relaxed"
            placeholder={"## Section heading\n\nWrite the lesson in Markdown…"}
          />
        ) : (
          <div className="min-h-[20rem] rounded-lg border border-slate-200 bg-white p-6">
            {content.trim() ? <Markdown source={content} /> : <p className="text-sm text-ink-muted">Nothing to preview.</p>}
          </div>
        )}
      </div>
      {error && <p className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</p>}
      <div className="flex items-center gap-3">
        <button className="btn-primary" disabled={busy}>
          {busy ? "Saving…" : chapter ? "Save chapter" : "Add chapter"}
        </button>
        {chapter && (
          <a href={`/studio/courses/${courseId}/chapters/${chapter.id}/quiz`} className="btn-secondary">
            {chapter.quizQuestionCount ? `Edit quiz (${chapter.quizQuestionCount})` : "Add a quiz"}
          </a>
        )}
        {chapter && (
          <button type="button" className="btn-danger ml-auto" onClick={onDelete} disabled={busy}>
            Delete chapter
          </button>
        )}
      </div>
    </form>
  );
}
