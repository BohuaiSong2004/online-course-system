"use client";
import { useHydrated } from "@ocs/shared/hooks";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { clientApi, errorMessage } from "@ocs/shared/client";
import type { AdminUser, Team } from "@ocs/shared/types";

function ManagerSelect({ id, value, onChange, managers, label }: {
  id?: string; value: string; onChange: (v: string) => void; managers: AdminUser[]; label: string;
}) {
  const hydrated = useHydrated();
  return (
    <select disabled={!hydrated} id={id} value={value} onChange={(e) => onChange(e.target.value)} className="input w-auto" aria-label={label}>
      <option value="">No manager</option>
      {managers.map((m) => (
        <option key={m.id} value={m.id}>
          {m.displayName}
        </option>
      ))}
    </select>
  );
}

export function NewTeamForm({ managers }: { managers: AdminUser[] }) {
  const hydrated = useHydrated();
  const router = useRouter();
  const [name, setName] = useState("");
  const [managerId, setManagerId] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  return (
    <form method="post"
      className="mt-4 flex flex-wrap items-end gap-3"
      onSubmit={async (e) => {
        e.preventDefault();
        setBusy(true);
        setError(null);
        try {
          await clientApi("/admin/teams", { method: "POST", json: { name, managerId: managerId ? Number(managerId) : null } });
          setName("");
          setManagerId("");
          router.refresh();
        } catch (err) {
          setError(errorMessage(err));
        } finally {
          setBusy(false);
        }
      }}
    >
      <div>
        <label htmlFor="team-name" className="label">
          Name
        </label>
        <input disabled={!hydrated} id="team-name" value={name} onChange={(e) => setName(e.target.value)} required maxLength={100} className="input w-64" />
      </div>
      <div>
        <label htmlFor="team-manager" className="label">
          Manager
        </label>
        <ManagerSelect id="team-manager" value={managerId} onChange={setManagerId} managers={managers} label="Manager" />
      </div>
      <button className="btn-primary" disabled={!hydrated || busy}>
        {busy ? "Creating…" : "Create team"}
      </button>
      {error && (
        <p role="alert" className="w-full text-sm text-rose-700">
          {error}
        </p>
      )}
    </form>
  );
}

export function TeamRow({ team, managers }: { team: Team; managers: AdminUser[] }) {
  const hydrated = useHydrated();
  const router = useRouter();
  const [name, setName] = useState(team.name);
  const [managerId, setManagerId] = useState(team.managerId ? String(team.managerId) : "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const dirty = name !== team.name || managerId !== (team.managerId ? String(team.managerId) : "");

  async function run(fn: () => Promise<unknown>) {
    setBusy(true);
    setError(null);
    try {
      await fn();
      router.refresh();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <tr>
      <td className="px-4 py-3">
        <input disabled={!hydrated} value={name} onChange={(e) => setName(e.target.value)} className="input w-56" aria-label="Team name" />
      </td>
      <td className="px-4 py-3">
        <ManagerSelect value={managerId} onChange={setManagerId} managers={managers} label={`Manager of ${team.name}`} />
      </td>
      <td className="px-4 py-3 tabular-nums">{team.memberCount}</td>
      <td className="whitespace-nowrap px-4 py-3 text-right">
        <button
          type="button"
          className="btn-secondary py-1"
          disabled={!hydrated || !dirty || busy}
          onClick={() =>
            run(() => clientApi(`/admin/teams/${team.id}`, { method: "PUT", json: { name, managerId: managerId ? Number(managerId) : null } }))
          }
        >
          Save
        </button>{" "}
        <button
          type="button"
          className="btn-danger py-1"
          disabled={!hydrated || busy}
          onClick={() => {
            if (window.confirm(`Delete team "${team.name}"? Members stay, without a team.`)) {
              run(() => clientApi(`/admin/teams/${team.id}`, { method: "DELETE" }));
            }
          }}
        >
          Delete
        </button>
        {error && (
          <div role="alert" className="mt-1 text-xs text-rose-700">
            {error}
          </div>
        )}
      </td>
    </tr>
  );
}
