import { routes } from "@ocs/shared/routes";

/** Only allow same-site relative redirects (prevents open-redirects via ?next=). */
export function safeNext(raw: string | string[] | undefined): string {
  const v = Array.isArray(raw) ? raw[0] : raw;
  // Must be a same-site path: "//host" and "/\\host" are treated by browsers as other origins.
  // Control characters (tabs/newlines) are stripped by URL parsers and can smuggle "//" too.
  const ok = !!v && v.startsWith("/") && !v.startsWith("//") && !v.includes("\\") && !/[\u0000-\u001f\u007f]/.test(v);
  return ok ? v! : routes.dashboard;
}
