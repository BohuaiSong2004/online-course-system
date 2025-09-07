import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { PageShell } from "@ocs/shared/components/PageShell";
import { formatDate, formatMinutes } from "@ocs/shared/format";
import { routes } from "@ocs/shared/routes";
import { publicApiOrNull } from "@ocs/shared/server";
import type { Certificate } from "@ocs/shared/types";
import { PrintButton } from "./PrintButton";

type Params = Promise<{ code: string }>;

// Certificates never change once issued: cache verification pages for an hour.
export const revalidate = 3600;

// Generated on first request, then served from cache (on-demand ISR).
export async function generateStaticParams() {
  return [];
}

async function load(code: string) {
  if (!/^[A-Za-z0-9]{12}$/.test(code)) return null;
  return publicApiOrNull<Certificate>(`/certificates/${code}`, 3600);
}

export async function generateMetadata({ params }: { params: Params }): Promise<Metadata> {
  const cert = await load((await params).code);
  return cert
    ? { title: `Certificate: ${cert.courseTitle}`, description: `${cert.learnerName} completed ${cert.courseTitle}.`, robots: { index: false } }
    : { title: "Certificate not found", robots: { index: false } };
}

export default async function CertificatePage({ params }: { params: Params }) {
  const cert = await load((await params).code);
  if (!cert) notFound();
  return (
    <PageShell zone="shell">
      <div className="mx-auto max-w-3xl">
        <div className="mb-4 flex items-center justify-between print:hidden">
          <p className="text-sm text-emerald-700">✓ Verified certificate</p>
          <PrintButton />
        </div>
        <article className="relative overflow-hidden rounded-2xl border-8 border-double border-brand-200 bg-white px-10 py-14 text-center shadow-card print:shadow-none">
          <p className="text-sm font-semibold uppercase tracking-[0.3em] text-brand-700">Certificate of completion</p>
          <p className="mt-8 text-ink-soft">This certifies that</p>
          <h1 className="mt-2 font-serif text-4xl font-bold text-ink">{cert.learnerName}</h1>
          <p className="mt-6 text-ink-soft">has successfully completed</p>
          <h2 className="mt-2 text-2xl font-semibold text-ink">{cert.courseTitle}</h2>
          <p className="mt-2 text-sm text-ink-muted">
            {formatMinutes(cert.totalMinutes)} · taught by {cert.instructorName}
          </p>
          <div className="mt-12 flex flex-col items-center justify-between gap-4 text-sm text-ink-soft sm:flex-row">
            <div>
              <div className="font-medium text-ink">{formatDate(cert.issuedAt)}</div>
              <div className="text-xs">Date issued</div>
            </div>
            <div className="grid h-14 w-14 place-items-center rounded-full bg-brand-600 text-lg font-bold text-white" aria-hidden>
              OC
            </div>
            <div>
              <div className="font-mono font-medium text-ink">{cert.code}</div>
              <div className="text-xs">Verification code</div>
            </div>
          </div>
        </article>
        <p className="mt-4 text-center text-xs text-ink-muted print:hidden">
          Anyone can verify this certificate at this address.{" "}
          <a href={routes.course(cert.courseSlug)} className="text-brand-700 hover:underline">
            View the course
          </a>
        </p>
      </div>
    </PageShell>
  );
}
