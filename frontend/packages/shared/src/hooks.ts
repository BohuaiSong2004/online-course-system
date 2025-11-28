"use client";
import { useSyncExternalStore } from "react";

const subscribe = () => () => {};

/**
 * False during SSR and until React has hydrated this component, true afterwards. Interactive controls
 * use it to stay disabled until their handlers exist, so an early click is never silently lost (and a
 * form can't fall back to a native submit).
 */
export function useHydrated(): boolean {
  return useSyncExternalStore(subscribe, () => true, () => false);
}
