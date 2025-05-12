import { PageShell } from "@ocs/shared/components/PageShell";
import { routes } from "@ocs/shared/routes";

export default function NotFound() {
  return (
    <PageShell zone="catalog">
      <div className="py-20 text-center">
        <h1 className="text-3xl font-bold">Course not found</h1>
        <p className="mt-2 text-ink-soft">It may have been unpublished or the link is wrong.</p>
        <a href={routes.catalog} className="btn-primary mt-6">
          Browse courses
        </a>
      </div>
    </PageShell>
  );
}
