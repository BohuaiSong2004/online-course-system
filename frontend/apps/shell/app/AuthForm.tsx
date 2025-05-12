"use client";
import { useState } from "react";
import { clientApi, errorMessage } from "@ocs/shared/client";
import { routes } from "@ocs/shared/routes";

export function AuthForm({ mode, next }: { mode: "login" | "register"; next: string }) {
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    const form = new FormData(e.currentTarget);
    try {
      await clientApi(`/auth/${mode}`, { method: "POST", json: Object.fromEntries(form) });
      window.location.href = next; // full load: the target is usually another zone
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="space-y-4">
      {mode === "register" && (
        <div>
          <label htmlFor="displayName" className="label">
            Full name
          </label>
          <input id="displayName" name="displayName" required maxLength={100} className="input" autoComplete="name" />
        </div>
      )}
      <div>
        <label htmlFor="email" className="label">
          Work email
        </label>
        <input id="email" name="email" type="email" required className="input" autoComplete="email" />
      </div>
      <div>
        <label htmlFor="password" className="label">
          Password
        </label>
        <input
          id="password"
          name="password"
          type="password"
          required
          minLength={mode === "register" ? 8 : undefined}
          className="input"
          autoComplete={mode === "login" ? "current-password" : "new-password"}
        />
        {mode === "register" && <p className="mt-1 text-xs text-ink-muted">At least 8 characters.</p>}
      </div>
      {error && (
        <p role="alert" className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">
          {error}
        </p>
      )}
      <button type="submit" className="btn-primary w-full" disabled={busy}>
        {busy ? "Please wait…" : mode === "login" ? "Sign in" : "Create account"}
      </button>
      <p className="text-center text-sm text-ink-muted">
        {mode === "login" ? (
          <>
            New here?{" "}
            <a className="font-medium text-brand-700 hover:underline" href={routes.register(next)}>
              Create an account
            </a>
          </>
        ) : (
          <>
            Already have an account?{" "}
            <a className="font-medium text-brand-700 hover:underline" href={routes.login(next)}>
              Sign in
            </a>
          </>
        )}
      </p>
    </form>
  );
}
