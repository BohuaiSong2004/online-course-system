import { SiteFooter } from "./SiteFooter";
import { SiteHeader, type Zone } from "./SiteHeader";

export function PageShell({ zone, children }: { zone: Zone; children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col">
      <SiteHeader zone={zone} />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">{children}</main>
      <SiteFooter />
    </div>
  );
}
