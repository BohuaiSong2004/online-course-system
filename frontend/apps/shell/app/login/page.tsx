import type { Metadata } from "next";
import { PageShell } from "@ocs/shared/components/PageShell";
import { AuthForm } from "../AuthForm";
import { safeNext } from "../safeNext";

export const metadata: Metadata = { title: "Sign in", robots: { index: false } };

export default async function LoginPage({ searchParams }: { searchParams: Promise<Record<string, string | string[]>> }) {
  const next = safeNext((await searchParams).next);
  return (
    <PageShell zone="shell">
      <div className="mx-auto max-w-sm">
        <h1 className="text-2xl font-semibold">Welcome back</h1>
        <p className="mt-1 text-sm text-ink-soft">Sign in to continue your courses.</p>
        <div className="card mt-6 p-6">
          <AuthForm mode="login" next={next} />
        </div>
        <div className="mt-6 rounded-lg border border-brand-100 bg-brand-50 p-4 text-xs text-brand-900">
          <p className="font-semibold">Demo accounts (password: password123)</p>
          <ul className="mt-1 space-y-0.5">
            <li>learner@demo.dev — learner</li>
            <li>instructor@demo.dev — instructor</li>
            <li>admin@demo.dev — admin</li>
          </ul>
        </div>
      </div>
    </PageShell>
  );
}
