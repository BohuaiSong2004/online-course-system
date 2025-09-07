import type { Metadata } from "next";
import { PageShell } from "@ocs/shared/components/PageShell";
import { SignInPrompt } from "@ocs/shared/components/SignInPrompt";
import { roleLabel } from "@ocs/shared/format";
import { routes } from "@ocs/shared/routes";
import { getCurrentUser } from "@ocs/shared/server";
import { PasswordForm, ProfileForm } from "./AccountForms";

export const metadata: Metadata = { title: "Account", robots: { index: false } };

export default async function AccountPage() {
  const user = await getCurrentUser();
  if (!user) {
    return (
      <PageShell zone="learn" user={null}>
        <SignInPrompt next={routes.account} title="Sign in to manage your account" />
      </PageShell>
    );
  }
  return (
    <PageShell zone="learn" user={user}>
      <h1 className="text-3xl font-bold">Account</h1>
      <p className="mt-1 text-ink-soft">
        {user.email} · {roleLabel(user.role)}
      </p>
      <div className="mt-8 grid gap-6 lg:grid-cols-2">
        <section className="card p-6" aria-labelledby="profile-h">
          <h2 id="profile-h" className="font-semibold">
            Profile
          </h2>
          <ProfileForm displayName={user.displayName} />
        </section>
        <section className="card p-6" aria-labelledby="password-h">
          <h2 id="password-h" className="font-semibold">
            Change password
          </h2>
          <p className="mt-1 text-sm text-ink-soft">This signs you out on every other device.</p>
          <PasswordForm />
        </section>
      </div>
    </PageShell>
  );
}
