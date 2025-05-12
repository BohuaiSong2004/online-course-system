import type { Course } from "../types";
import { formatMinutes } from "../format";
import { routes } from "../routes";
import { LevelBadge } from "./Badge";

// Deterministic accent per category so cards are visually distinct without images.
const ACCENTS = ["from-brand-500 to-brand-700", "from-emerald-500 to-teal-600", "from-amber-500 to-orange-600",
  "from-fuchsia-500 to-purple-600", "from-sky-500 to-cyan-600", "from-rose-500 to-pink-600"];

export function accentFor(key: string) {
  let h = 2166136261; // FNV-1a
  for (const ch of key) {
    h ^= ch.charCodeAt(0);
    h = Math.imul(h, 16777619) >>> 0;
  }
  return ACCENTS[h % ACCENTS.length];
}

export function CourseCard({ course }: { course: Course }) {
  return (
    <a
      href={routes.course(course.slug)}
      className="group flex flex-col overflow-hidden rounded-xl border border-slate-200 bg-white shadow-card transition hover:-translate-y-0.5 hover:border-brand-300"
    >
      <div className={`flex h-28 items-end bg-gradient-to-br p-4 ${accentFor(course.category)}`}>
        <span className="rounded bg-white/20 px-2 py-0.5 text-xs font-medium text-white backdrop-blur">
          {course.category}
        </span>
      </div>
      <div className="flex flex-1 flex-col gap-2 p-4">
        <h3 className="font-semibold text-ink group-hover:text-brand-700">{course.title}</h3>
        <p className="line-clamp-2 text-sm text-ink-soft">{course.summary}</p>
        <div className="mt-auto flex items-center justify-between pt-3 text-xs text-ink-muted">
          <span>{course.instructorName}</span>
          <span className="flex items-center gap-2">
            <LevelBadge level={course.level} />
            {course.chapterCount} ch · {formatMinutes(course.totalMinutes)}
          </span>
        </div>
      </div>
    </a>
  );
}
