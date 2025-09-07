import { PageShell } from "@ocs/shared/components/PageShell";
import { SignInPrompt } from "@ocs/shared/components/SignInPrompt";
import { getCurrentUser } from "@ocs/shared/server";
import type { UserView } from "@ocs/shared/types";

/** Resolves the author or returns the page to render instead (sign-in / no access). */
export async function requireAuthor(next: string): Promise<{ user: UserView } | { page: React.ReactElement }> {
  const user = await getCurrentUser();
  if (!user) {
    return {
      page: (
        <PageShell zone="studio" user={null}>
          <SignInPrompt next={next} title="Sign in to Studio" />
        </PageShell>
      ),
    };
  }
  if (user.role !== "INSTRUCTOR" && user.role !== "ADMIN") {
    return {
      page: (
        <PageShell zone="studio" user={user}>
          <div className="card mx-auto max-w-md p-8 text-center">
            <h1 className="text-xl font-semibold">Instructors only</h1>
            <p className="mt-2 text-sm text-ink-soft">Ask an admin to grant you the instructor role to create courses.</p>
          </div>
        </PageShell>
      ),
    };
  }
  return { user };
}
