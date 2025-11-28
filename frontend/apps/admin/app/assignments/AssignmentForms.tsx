"use client";
import { useHydrated } from "@ocs/shared/hooks";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { clientApi, errorMessage } from "@ocs/shared/client";
import type { AdminUser, AssignResult, Course, Team } from "@ocs/shared/types";

export function AssignForm({ courses, teams }: { courses: Course[]; teams: Team[] }) {
  const hydrated = useHydrated();
  const router = useRouter();
  const [teamId, setTeamId] = useState<string>(teams[0] ? String(teams[0].id) : "");
  const [members, setMembers] = useState<AdminUser[]>([]);
  const [picked, setPicked] = useState<Set<number>>(new Set());
  const [wholeTeam, setWholeTeam] = useState(true);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<{ ok: boolean; text: string } | null>(null);
  const today = new Date().toISOString().slice(0, 10);

  useEffect(() => {
    setPicked(new Set());
    if (!teamId) {
      setMembers([]);
      return;
    }
    clientApi<AdminUser[]>(`/admin/teams/${teamId}/members`)
      .then(setMembers)
      .catch(() => setMembers([]));
  }, [teamId]);

  async function submit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    setBusy(true);
    setMsg(null);
    try {
      const res = await clientApi<AssignResult>("/admin/assignments", {
        method: "POST",
        json: {
          courseId: Number(f.get("courseId")),
          teamId: wholeTeam && teamId ? Number(teamId) : null,
          userIds: wholeTeam ? [] : [...picked],
          dueDate: f.get("dueDate") || null,
        },
      });
      const parts = [`${res.created} assigned`];
      if (res.updated) parts.push(`${res.updated} due date${res.updated === 1 ? "" : "s"} updated`);
      if (res.unchanged) parts.push(`${res.unchanged} already assigned`);
      if (res.skipped) parts.push(`${res.skipped} skipped (outside your teams)`);
      setMsg({ ok: true, text: parts.join(" · ") });
      router.refresh();
    } catch (err) {
      setMsg({ ok: false, text: errorMessage(err) });
    } finally {
      setBusy(false);
    }
  }

  if (teams.length === 0) {
    return <p className="mt-4 text-sm text-ink-muted">You don&apos;t manage any teams yet.</p>;
  }

  return (
    <form method="post" onSubmit={submit} className="mt-4 grid gap-4 lg:grid-cols-4">
      <div className="lg:col-span-2">
        <label htmlFor="courseId" className="label">
          Course
        </label>
        <select id="courseId" name="courseId" required className="input">
          {courses.map((c) => (
            <option key={c.id} value={c.id}>
              {c.title}
            </option>
          ))}
        </select>
      </div>
      <div>
        <label htmlFor="teamId" className="label">
          Team
        </label>
        <select id="teamId" value={teamId} onChange={(e) => setTeamId(e.target.value)} className="input">
          {teams.map((t) => (
            <option key={t.id} value={t.id}>
              {t.name} ({t.memberCount})
            </option>
          ))}
        </select>
      </div>
      <div>
        <label htmlFor="dueDate" className="label">
          Due date <span className="font-normal text-ink-muted">(optional)</span>
        </label>
        <input id="dueDate" name="dueDate" type="date" min={today} className="input" />
      </div>

      <fieldset className="lg:col-span-4">
        <legend className="label">Who</legend>
        <div className="flex flex-wrap gap-4 text-sm">
          <label className="flex items-center gap-2">
            <input type="radio" checked={wholeTeam} onChange={() => setWholeTeam(true)} className="accent-brand-600" />
            Everyone in the team
          </label>
          <label className="flex items-center gap-2">
            <input type="radio" checked={!wholeTeam} onChange={() => setWholeTeam(false)} className="accent-brand-600" />
            Choose people
          </label>
        </div>
        {!wholeTeam && (
          <div className="mt-3 flex flex-wrap gap-2">
            {members.length === 0 && <span className="text-sm text-ink-muted">No members in this team.</span>}
            {members.map((m) => (
              <label
                key={m.id}
                className={`flex cursor-pointer items-center gap-2 rounded-full border px-3 py-1 text-sm ${
                  picked.has(m.id) ? "border-brand-400 bg-brand-50" : "border-slate-300"
                }`}
              >
                <input
                  type="checkbox"
                  checked={picked.has(m.id)}
                  onChange={(e) =>
                    setPicked((p) => {
                      const n = new Set(p);
                      if (e.target.checked) n.add(m.id);
                      else n.delete(m.id);
                      return n;
                    })
                  }
                  className="accent-brand-600"
                />
                {m.displayName}
              </label>
            ))}
          </div>
        )}
      </fieldset>

      <div className="flex flex-wrap items-center gap-3 lg:col-span-4">
        <button className="btn-primary" disabled={!hydrated || busy || (!wholeTeam && picked.size === 0)}>
          {busy ? "Assigning…" : "Assign"}
        </button>
        {msg && (
          <p role={msg.ok ? "status" : "alert"} className={`text-sm ${msg.ok ? "text-emerald-800" : "text-rose-700"}`}>
            {msg.text}
          </p>
        )}
      </div>
    </form>
  );
}

export function RemoveAssignment({ id, label }: { id: number; label: string }) {
  const hydrated = useHydrated();
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  return (
    <button
      type="button"
      className="text-sm text-rose-700 hover:underline disabled:opacity-50"
      disabled={!hydrated || busy}
      aria-label={`Remove assignment: ${label}`}
      onClick={async () => {
        if (!window.confirm(`Remove the assignment "${label}"? Their enrollment and progress are kept.`)) return;
        setBusy(true);
        try {
          await clientApi(`/admin/assignments/${id}`, { method: "DELETE" });
          router.refresh();
        } finally {
          setBusy(false);
        }
      }}
    >
      Remove
    </button>
  );
}
