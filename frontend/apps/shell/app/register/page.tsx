import type { Metadata } from "next";
import { PageShell } from "@ocs/shared/components/PageShell";
import { AuthForm } from "../AuthForm";
import { safeNext } from "../safeNext";

export const metadata: Metadata = { title: "Create account", robots: { index: false } };

export default async function RegisterPage({ searchParams }: { searchParams: Promise<Record<string, string | string[]>> }) {
  const next = safeNext((await searchParams).next);
  return (
    <PageShell zone="shell">
      <div className="mx-auto max-w-sm">
        <h1 className="text-2xl font-semibold">Create your account</h1>
        <p className="mt-1 text-sm text-ink-soft">Join your team&apos;s learning portal.</p>
        <div className="card mt-6 p-6">
          <AuthForm mode="register" next={next} />
        </div>
      </div>
    </PageShell>
  );
}
