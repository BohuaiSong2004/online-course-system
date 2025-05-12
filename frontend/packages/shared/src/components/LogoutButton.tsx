"use client";
import { useState } from "react";
import { clientApi } from "../client";

export function LogoutButton() {
  const [busy, setBusy] = useState(false);
  return (
    <button
      className="btn-ghost"
      disabled={busy}
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
