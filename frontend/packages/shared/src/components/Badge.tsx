import { daysUntil, formatDay, levelLabel } from "../format";
import type { AssignmentStatus } from "../types";

const LEVEL_STYLES: Record<string, string> = {
  BEGINNER: "bg-emerald-50 text-emerald-800 ring-emerald-600/20",
  INTERMEDIATE: "bg-amber-50 text-amber-800 ring-amber-600/20",
  ADVANCED: "bg-rose-50 text-rose-800 ring-rose-600/20",
};

export function Badge({ children, className = "" }: { children: React.ReactNode; className?: string }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset ${className}`}>
      {children}
    </span>
  );
}

export function LevelBadge({ level }: { level: string }) {
  return <Badge className={LEVEL_STYLES[level] ?? "bg-slate-50 text-slate-700 ring-slate-600/20"}>{levelLabel(level)}</Badge>;
}

export function StatusBadge({ status }: { status: string }) {
  return status === "PUBLISHED" ? (
    <Badge className="bg-brand-50 text-brand-800 ring-brand-600/20">Published</Badge>
  ) : (
    <Badge className="bg-slate-100 text-slate-700 ring-slate-500/20">Draft</Badge>
  );
}

const ASSIGNMENT_STYLES: Record<AssignmentStatus, [string, string]> = {
  COMPLETED: ["Completed", "bg-emerald-50 text-emerald-800 ring-emerald-600/20"],
  OVERDUE: ["Overdue", "bg-rose-50 text-rose-800 ring-rose-600/20"],
  IN_PROGRESS: ["In progress", "bg-brand-50 text-brand-800 ring-brand-600/20"],
  NOT_STARTED: ["Not started", "bg-slate-100 text-slate-700 ring-slate-500/20"],
};

export function AssignmentBadge({ status }: { status: AssignmentStatus }) {
  const [label, cls] = ASSIGNMENT_STYLES[status];
  return <Badge className={cls}>{label}</Badge>;
}

/** "Due in 3 days" / "Due today" / "Overdue by 2 days" for an open assignment. */
export function DueBadge({ dueDate, completed }: { dueDate: string; completed?: boolean }) {
  const d = daysUntil(dueDate);
  if (completed) return <Badge className="bg-slate-100 text-slate-700 ring-slate-500/20">Due {formatDay(dueDate)}</Badge>;
  if (d < 0)
    return (
      <Badge className="bg-rose-50 text-rose-800 ring-rose-600/20">
        Overdue by {-d} day{d === -1 ? "" : "s"}
      </Badge>
    );
  if (d === 0) return <Badge className="bg-amber-50 text-amber-800 ring-amber-600/20">Due today</Badge>;
  return (
    <Badge className={d <= 7 ? "bg-amber-50 text-amber-800 ring-amber-600/20" : "bg-slate-100 text-slate-700 ring-slate-500/20"}>
      Due in {d} day{d === 1 ? "" : "s"}
    </Badge>
  );
}
