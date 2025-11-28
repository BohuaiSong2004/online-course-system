"use client";
import { useHydrated } from "@ocs/shared/hooks";
import { useState } from "react";
import { clientApi, errorMessage } from "@ocs/shared/client";
import { roleLabel } from "@ocs/shared/format";
import { ROLES, type AdminUser, type Team } from "@ocs/shared/types";

export function UserRow({ user, teams, isSelf }: { user: AdminUser; teams: Team[]; isSelf: boolean }) {
  const hydrated = useHydrated();
  const [role, setRole] = useState(user.role);
  const [teamId, setTeamId] = useState(user.teamId ? String(user.teamId) : "");
  const [active, setActive] = useState(user.active);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<{ ok: boolean; text: string } | null>(null);
  const dirty = role !== user.role || teamId !== (user.teamId ? String(user.teamId) : "") || active !== user.active;

  async function save() {
    setBusy(true);
    setMsg(null);
    try {
      const saved = await clientApi<AdminUser>(`/admin/users/${user.id}`, {
        method: "PUT",
        json: { role, teamId: teamId ? Number(teamId) : null, active },
      });
      Object.assign(user, saved);
      setMsg({ ok: true, text: "Saved" });
    } catch (e) {
      setMsg({ ok: false, text: errorMessage(e) });
    } finally {
      setBusy(false);
    }
  }

  return (
    <tr className={active ? "" : "bg-slate-50 text-ink-muted"}>
      <td className="px-4 py-3">
        <div className="font-medium">
          {user.displayName} {isSelf && <span className="text-xs text-ink-muted">(you)</span>}
        </div>
        <div className="text-xs text-ink-muted">{user.email}</div>
      </td>
      <td className="px-4 py-3">
        <select value={role} onChange={(e) => setRole(e.target.value as AdminUser["role"])} className="input w-auto" aria-label={`Role for ${user.displayName}`}>
          {ROLES.map((r) => (
            <option key={r} value={r}>
              {roleLabel(r)}
            </option>
          ))}
        </select>
      </td>
      <td className="px-4 py-3">
        <select value={teamId} onChange={(e) => setTeamId(e.target.value)} className="input w-auto" aria-label={`Team for ${user.displayName}`}>
          <option value="">No team</option>
          {teams.map((t) => (
            <option key={t.id} value={t.id}>
              {t.name}
            </option>
          ))}
        </select>
      </td>
      <td className="px-4 py-3">
        <input
          type="checkbox"
          checked={active}
          onChange={(e) => setActive(e.target.checked)}
          className="h-4 w-4 accent-brand-600"
          aria-label={`${user.displayName} is active`}
        />
      </td>
      <td className="whitespace-nowrap px-4 py-3 text-right">
        <button type="button" className="btn-secondary py-1" disabled={!hydrated || !dirty || busy} onClick={save}>
          {busy ? "Saving…" : "Save"}
        </button>
        {msg && (
          <div role={msg.ok ? "status" : "alert"} className={`mt-1 text-xs ${msg.ok ? "text-emerald-800" : "text-rose-700"}`}>
            {msg.text}
          </div>
        )}
      </td>
    </tr>
  );
}
