export function formatMinutes(total: number): string {
  if (total < 60) return `${total} min`;
  const h = Math.floor(total / 60);
  const m = total % 60;
  return m === 0 ? `${h} h` : `${h} h ${m} min`;
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return "";
  return new Date(iso).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
}

export function levelLabel(level: string): string {
  return level.charAt(0) + level.slice(1).toLowerCase();
}

export function percent(done: number, total: number): number {
  return total === 0 ? 0 : Math.round((done / total) * 100);
}

/** "YYYY-MM-DD" (a calendar date, no time zone) -> "Dec 1, 2026". */
export function formatDay(day: string | null | undefined): string {
  if (!day) return "";
  const [y, m, d] = day.split("-").map(Number);
  return new Date(Date.UTC(y, m - 1, d)).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric", timeZone: "UTC" });
}

/** Whole days from today (UTC) until a "YYYY-MM-DD" date; negative when past. */
export function daysUntil(day: string): number {
  const [y, m, d] = day.split("-").map(Number);
  const now = new Date();
  const today = Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate());
  return Math.round((Date.UTC(y, m - 1, d) - today) / 86_400_000);
}

export function roleLabel(role: string): string {
  return role.charAt(0) + role.slice(1).toLowerCase();
}
