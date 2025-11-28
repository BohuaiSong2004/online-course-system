"use client";
import { useHydrated } from "../hooks";
import { useState } from "react";
import { clientApi } from "../client";

export function LogoutButton() {
  const hydrated = useHydrated();
  const [busy, setBusy] = useState(false);
  return (
    <button
      className="btn-ghost"
      disabled={!hydrated || busy}
      onClick={async () => {
        setBusy(true);
        try {
          await clientApi("/auth/logout", { method: "POST" });
        } finally {
          window.location.href = "/";
        }
      }}
    >
      Sign out
    </button>
  );
}
