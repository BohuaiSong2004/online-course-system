"use client";
import { useHydrated } from "@ocs/shared/hooks";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { clientApi, errorMessage } from "@ocs/shared/client";
import type { QuizAttempt, QuizQuestion, QuizResult } from "@ocs/shared/types";
import { CourseCompleteBanner } from "./CompleteButton";

interface Props {
  chapterId: number;
  slug: string;
  questions: QuizQuestion[];
  passPercent: number;
  enrolled: boolean;
  bestAttempt: QuizAttempt | null;
}

/** Chapter quiz. Grading happens on the server; correct answers are revealed only after a pass. */
export function Quiz({ chapterId, slug, questions, passPercent, enrolled, bestAttempt }: Props) {
  const hydrated = useHydrated();
  const router = useRouter();
  const [answers, setAnswers] = useState<Record<number, number>>({});
  const [result, setResult] = useState<QuizResult | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const passedBefore = bestAttempt?.passed;
  const byQuestion = new Map(result?.results.map((r) => [r.questionId, r]));

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const res = await clientApi<QuizResult>(`/chapters/${chapterId}/quiz/attempts`, { method: "POST", json: { answers } });
      setResult(res);
      if (res.passed) router.refresh();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form method="post" onSubmit={submit} aria-describedby="quiz-help">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 id="quiz-heading" className="text-xl font-semibold">
          Check your understanding
        </h2>
        <p id="quiz-help" className="text-sm text-ink-muted">
          {questions.length} question{questions.length === 1 ? "" : "s"} · pass mark {passPercent}%
          {bestAttempt && ` · best so far ${bestAttempt.score}/${bestAttempt.total}${passedBefore ? " ✓" : ""}`}
        </p>
      </div>

      <ol className="mt-6 space-y-6">
        {questions.map((q, i) => {
          const r = byQuestion.get(q.id);
          return (
            <li key={q.id}>
              <fieldset>
                <legend className="font-medium">
                  {i + 1}. {q.prompt}
                </legend>
                <div className="mt-3 space-y-2">
                  {q.options.map((o) => {
                    const chosen = answers[q.id] === o.id;
                    const isAnswer = r?.correctOptionId === o.id;
                    return (
                      <label
                        key={o.id}
                        className={`flex cursor-pointer items-center gap-3 rounded-lg border px-4 py-3 text-sm transition ${
                          isAnswer
                            ? "border-emerald-400 bg-emerald-50"
                            : chosen && r && !r.correct
                              ? "border-rose-300 bg-rose-50"
                              : chosen
                                ? "border-brand-400 bg-brand-50"
                                : "border-slate-200 hover:bg-slate-50"
                        }`}
                      >
                        <input
                          type="radio"
                          name={`q-${q.id}`}
                          value={o.id}
                          checked={chosen}
                          disabled={!hydrated || !enrolled || busy}
                          onChange={() => {
                            setAnswers((a) => ({ ...a, [q.id]: o.id }));
                            if (result && !result.passed) setResult(null);
                          }}
                          className="h-4 w-4 accent-brand-600"
                        />
                        {o.text}
                      </label>
                    );
                  })}
                </div>
                {r && (
                  <p className={`mt-2 text-sm ${r.correct ? "text-emerald-800" : "text-rose-800"}`}>
                    {r.correct ? "✓ Correct" : "✗ Not quite"}
                    {r.explanation && <span className="text-ink-soft"> — {r.explanation}</span>}
                  </p>
                )}
              </fieldset>
            </li>
          );
        })}
      </ol>

      <div className="mt-6" aria-live="polite">
        {result && (
          <div className={`mb-4 rounded-lg px-4 py-3 text-sm ${result.passed ? "bg-emerald-50 text-emerald-900" : "bg-amber-50 text-amber-900"}`}>
            <strong>
              You scored {result.score}/{result.total} ({Math.round((100 * result.score) / result.total)}%).
            </strong>{" "}
            {result.passed ? "Passed — this chapter is complete." : `You need ${passPercent}% to pass. Review the chapter and try again.`}
          </div>
        )}
        {result?.progress?.courseCompleted && <CourseCompleteBanner certificate={result.progress.certificateCode} />}
        {result?.passed && result.progress?.nextChapterId && !result.progress.courseCompleted && (
          <a href={`/learn/${slug}/${result.progress.nextChapterId}`} className="btn-primary">
            Next chapter →
          </a>
        )}
        {enrolled ? (
          !result?.passed && (
            <button className="btn-primary" disabled={!hydrated || busy || Object.keys(answers).length === 0}>
              {busy ? "Checking…" : result ? "Try again" : "Submit answers"}
            </button>
          )
        ) : (
          <p className="text-sm text-ink-muted">Enroll in the course to take the quiz.</p>
        )}
        {error && (
          <p role="alert" className="mt-2 text-sm text-rose-700">
            {error}
          </p>
        )}
      </div>
    </form>
  );
}
