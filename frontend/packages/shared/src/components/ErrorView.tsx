"use client";

/** Shared error boundary UI: shown when the API is unreachable or a page throws. */
export function ErrorView({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="card max-w-md p-8 text-center">
        <p className="text-sm font-semibold text-rose-600">Something went wrong</p>
        <h1 className="mt-2 text-2xl font-bold">We couldn&apos;t load this page</h1>
        <p className="mt-2 text-sm text-ink-soft">The service may be temporarily unavailable. Please try again.</p>
        <div className="mt-6 flex justify-center gap-3">
          <button onClick={reset} className="btn-primary">
            Try again
          </button>
          <a href="/" className="btn-secondary">
            Home
          </a>
        </div>
      </div>
    </div>
  );
}
