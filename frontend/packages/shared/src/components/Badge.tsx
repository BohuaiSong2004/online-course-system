import { levelLabel } from "../format";

const LEVEL_STYLES: Record<string, string> = {
  BEGINNER: "bg-emerald-50 text-emerald-700 ring-emerald-600/20",
  INTERMEDIATE: "bg-amber-50 text-amber-700 ring-amber-600/20",
  ADVANCED: "bg-rose-50 text-rose-700 ring-rose-600/20",
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
    <Badge className="bg-brand-50 text-brand-700 ring-brand-600/20">Published</Badge>
  ) : (
    <Badge className="bg-slate-100 text-slate-600 ring-slate-500/20">Draft</Badge>
  );
}
