"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { clientApi, errorMessage } from "@ocs/shared/client";
import type { QuizQuestion } from "@ocs/shared/types";

interface DraftOption {
  key: string;
  text: string;
  correct: boolean;
}
interface DraftQuestion {
  key: string;
  prompt: string;
  explanation: string;
  options: DraftOption[];
}

let seq = 0;
const key = () => `k${++seq}`;
const blankQuestion = (): DraftQuestion => ({
  key: key(),
  prompt: "",
  explanation: "",
  options: [
    { key: key(), text: "", correct: true },
    { key: key(), text: "", correct: false },
  ],
});

export function QuizEditor({
  courseId,
  chapterId,
  initial,
}: {
  courseId: number;
  chapterId: number;
  initial: { passPercent: number; questions: QuizQuestion[] };
}) {
  const router = useRouter();
  const [passPercent, setPassPercent] = useState(initial.passPercent);
  const [questions, setQuestions] = useState<DraftQuestion[]>(
    initial.questions.map((q) => ({
      key: key(),
      prompt: q.prompt,
      explanation: q.explanation ?? "",
      options: q.options.map((o) => ({ key: key(), text: o.text, correct: !!o.correct })),
    })),
  );
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  const update = (qi: number, fn: (q: DraftQuestion) => DraftQuestion) =>
    setQuestions((qs) => qs.map((q, i) => (i === qi ? fn(q) : q)));

  async function save() {
    setBusy(true);
    setError(null);
    setSaved(false);
    try {
      await clientApi(`/studio/chapters/${chapterId}/quiz`, {
        method: "PUT",
        json: {
          passPercent,
          questions: questions.map((q) => ({
            prompt: q.prompt,
            explanation: q.explanation || null,
            options: q.options.map((o) => ({ text: o.text, correct: o.correct })),
          })),
        },
      });
      setSaved(true);
      router.refresh();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-6">
      <div className="max-w-xs">
        <label htmlFor="pass" className="label">
          Pass mark (%)
        </label>
        <input
          id="pass"
          type="number"
          min={1}
          max={100}
          value={passPercent}
          onChange={(e) => setPassPercent(Number(e.target.value))}
          className="input"
        />
      </div>

      {questions.length === 0 && <p className="text-sm text-ink-muted">No questions yet — this chapter has no quiz.</p>}

      <ol className="space-y-6">
        {questions.map((q, qi) => (
          <li key={q.key} className="rounded-xl border border-slate-200 p-4">
            <div className="flex items-start justify-between gap-3">
              <label className="label" htmlFor={`p-${q.key}`}>
                Question {qi + 1}
              </label>
              <button
                type="button"
                className="text-sm text-rose-700 hover:underline"
                onClick={() => setQuestions((qs) => qs.filter((_, i) => i !== qi))}
              >
                Remove question
              </button>
            </div>
            <textarea
              id={`p-${q.key}`}
              rows={2}
              value={q.prompt}
              onChange={(e) => update(qi, (x) => ({ ...x, prompt: e.target.value }))}
              className="input"
              placeholder="What should the learner answer?"
            />
            <fieldset className="mt-3 space-y-2">
              <legend className="label">Options (select the correct one)</legend>
              {q.options.map((o, oi) => (
                <div key={o.key} className="flex items-center gap-2">
                  <input
                    type="radio"
                    name={`correct-${q.key}`}
                    checked={o.correct}
                    aria-label={`Option ${oi + 1} is correct`}
                    onChange={() =>
                      update(qi, (x) => ({ ...x, options: x.options.map((y, j) => ({ ...y, correct: j === oi })) }))
                    }
                    className="h-4 w-4 accent-brand-600"
                  />
                  <input
                    value={o.text}
                    aria-label={`Option ${oi + 1}`}
                    onChange={(e) =>
                      update(qi, (x) => ({
                        ...x,
                        options: x.options.map((y, j) => (j === oi ? { ...y, text: e.target.value } : y)),
                      }))
                    }
                    className="input"
                  />
                  <button
                    type="button"
                    className="btn-ghost px-2"
                    disabled={q.options.length <= 2}
                    aria-label={`Remove option ${oi + 1}`}
                    onClick={() =>
                      update(qi, (x) => {
                        const opts = x.options.filter((_, j) => j !== oi);
                        if (!opts.some((y) => y.correct)) opts[0] = { ...opts[0], correct: true };
                        return { ...x, options: opts };
                      })
                    }
                  >
                    ✕
                  </button>
                </div>
              ))}
              {q.options.length < 6 && (
                <button
                  type="button"
                  className="text-sm font-medium text-brand-700 hover:underline"
                  onClick={() => update(qi, (x) => ({ ...x, options: [...x.options, { key: key(), text: "", correct: false }] }))}
                >
                  + Add option
                </button>
              )}
            </fieldset>
            <label className="label mt-3" htmlFor={`e-${q.key}`}>
              Explanation <span className="font-normal text-ink-muted">(shown after the learner passes)</span>
            </label>
            <input
              id={`e-${q.key}`}
              value={q.explanation}
              onChange={(e) => update(qi, (x) => ({ ...x, explanation: e.target.value }))}
              className="input"
            />
          </li>
        ))}
      </ol>

      <div className="flex flex-wrap items-center gap-3">
        <button type="button" className="btn-secondary" onClick={() => setQuestions((qs) => [...qs, blankQuestion()])}>
          + Add question
        </button>
        <button type="button" className="btn-primary" onClick={save} disabled={busy}>
          {busy ? "Saving…" : "Save quiz"}
        </button>
        <a href={`/studio/courses/${courseId}`} className="btn-ghost">
          Done
        </a>
        {saved && (
          <span role="status" className="text-sm text-emerald-800">
            Saved ✓
          </span>
        )}
      </div>
      {error && (
        <p role="alert" className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">
          {error}
        </p>
      )}
    </div>
  );
}
