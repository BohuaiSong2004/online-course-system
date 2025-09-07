import { PageShell } from "@ocs/shared/components/PageShell";

export default function NotFound() {
  return (
    <PageShell zone="admin">
      <div className="py-20 text-center">
        <h1 className="text-3xl font-bold">Not found</h1>
        <a href="/admin" className="btn-primary mt-6">
          Admin home
        </a>
      </div>
    </PageShell>
  );
}
