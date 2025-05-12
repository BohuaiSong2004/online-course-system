import { percent } from "../format";

export function ProgressBar({ done, total }: { done: number; total: number }) {
  const pct = percent(done, total);
  return (
    <div>
      <div className="h-2 w-full overflow-hidden rounded-full bg-slate-100" role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
        <div className="h-full rounded-full bg-brand-600 transition-all" style={{ width: `${pct}%` }} />
      </div>
      <div className="mt-1 text-xs text-ink-muted">
        {done}/{total} chapters · {pct}%
      </div>
    </div>
  );
}
