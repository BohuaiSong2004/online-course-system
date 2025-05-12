import { PageShell } from "@ocs/shared/components/PageShell";

export default function NotFound() {
  return (
    <PageShell zone="shell">
      <div className="py-20 text-center">
        <p className="text-sm font-semibold text-brand-600">404</p>
        <h1 className="mt-2 text-3xl font-bold">Page not found</h1>
        <a href="/" className="btn-primary mt-6">
          Back home
        </a>
      </div>
    </PageShell>
  );
}
