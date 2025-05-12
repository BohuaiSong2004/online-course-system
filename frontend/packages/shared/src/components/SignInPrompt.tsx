import { routes } from "../routes";

export function SignInPrompt({ next, title = "Sign in to continue" }: { next: string; title?: string }) {
  return (
    <div className="mx-auto max-w-md rounded-xl border border-slate-200 bg-white p-8 text-center shadow-card">
      <h1 className="text-xl font-semibold text-ink">{title}</h1>
      <p className="mt-2 text-sm text-ink-soft">You need an account to access this page.</p>
      <div className="mt-6 flex justify-center gap-3">
        <a href={routes.login(next)} className="btn-primary">
          Sign in
        </a>
        <a href={routes.register(next)} className="btn-secondary">
          Create account
        </a>
      </div>
    </div>
  );
}

export function EmptyState({ title, children }: { title: string; children?: React.ReactNode }) {
  return (
    <div className="rounded-xl border border-dashed border-slate-300 bg-white p-10 text-center">
      <h3 className="font-semibold text-ink">{title}</h3>
      {children && <div className="mt-2 text-sm text-ink-soft">{children}</div>}
    </div>
  );
}
