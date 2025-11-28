"use client";
import { useHydrated } from "@ocs/shared/hooks";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { clientApi, errorMessage } from "@ocs/shared/client";

function Status({ ok, error }: { ok: string | null; error: string | null }) {
  if (error)
    return (
      <p role="alert" className="text-sm text-rose-700">
        {error}
      </p>
    );
  if (ok)
    return (
      <p role="status" className="text-sm text-emerald-800">
        {ok}
      </p>
    );
  return null;
}

export function ProfileForm({ displayName }: { displayName: string }) {
  const hydrated = useHydrated();
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [ok, setOk] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  return (
    <form method="post"
      className="mt-4 space-y-4"
      onSubmit={async (e) => {
        e.preventDefault();
        setBusy(true);
        setOk(null);
        setError(null);
        try {
          await clientApi("/me/profile", { method: "PUT", json: Object.fromEntries(new FormData(e.currentTarget)) });
          setOk("Saved.");
          router.refresh();
        } catch (err) {
          setError(errorMessage(err));
        } finally {
          setBusy(false);
        }
      }}
    >
      <div>
        <label htmlFor="displayName" className="label">
          Display name
        </label>
        <input id="displayName" name="displayName" defaultValue={displayName} required maxLength={100} className="input" />
      </div>
      <Status ok={ok} error={error} />
      <button className="btn-primary" disabled={!hydrated || busy}>
        {busy ? "Saving…" : "Save"}
      </button>
    </form>
  );
}

export function PasswordForm() {
  const hydrated = useHydrated();
  const [busy, setBusy] = useState(false);
  const [ok, setOk] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  return (
    <form method="post"
      className="mt-4 space-y-4"
      onSubmit={async (e) => {
        e.preventDefault();
        const form = e.currentTarget;
        const data = Object.fromEntries(new FormData(form)) as Record<string, string>;
        setOk(null);
        setError(null);
        if (data.newPassword !== data.confirm) {
          setError("The new passwords don't match.");
          return;
        }
        setBusy(true);
        try {
          await clientApi("/me/password", {
            method: "PUT",
            json: { currentPassword: data.currentPassword, newPassword: data.newPassword },
          });
          form.reset();
          setOk("Password changed. Other devices have been signed out.");
        } catch (err) {
          setError(errorMessage(err));
        } finally {
          setBusy(false);
        }
      }}
    >
      <div>
        <label htmlFor="currentPassword" className="label">
          Current password
        </label>
        <input id="currentPassword" name="currentPassword" type="password" required autoComplete="current-password" className="input" />
      </div>
      <div>
        <label htmlFor="newPassword" className="label">
          New password
        </label>
        <input id="newPassword" name="newPassword" type="password" required minLength={8} maxLength={72} autoComplete="new-password" className="input" />
      </div>
      <div>
        <label htmlFor="confirm" className="label">
          Confirm new password
        </label>
        <input id="confirm" name="confirm" type="password" required minLength={8} maxLength={72} autoComplete="new-password" className="input" />
      </div>
      <Status ok={ok} error={error} />
      <button className="btn-primary" disabled={!hydrated || busy}>
        {busy ? "Saving…" : "Change password"}
      </button>
    </form>
  );
}
