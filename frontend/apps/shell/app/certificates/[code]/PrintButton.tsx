"use client";
import { useHydrated } from "@ocs/shared/hooks";

export function PrintButton() {
  const hydrated = useHydrated();
  return (
    <button disabled={!hydrated} type="button" className="btn-secondary" onClick={() => window.print()}>
      Print / save as PDF
    </button>
  );
}
