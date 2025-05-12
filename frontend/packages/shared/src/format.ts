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
