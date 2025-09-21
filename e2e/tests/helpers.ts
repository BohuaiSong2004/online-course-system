import { expect, type Page } from "@playwright/test";

export const PASSWORD = "password123";
export const uid = () => Math.random().toString(36).slice(2, 8);

export async function login(page: Page, email: string, next = "/learn") {
  await page.goto(`/login?next=${encodeURIComponent(next)}`);
  await page.getByLabel("Work email").fill(email);
  await page.getByLabel("Password").fill(PASSWORD);
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.waitForURL((url) => url.pathname === next);
}

/** Registers a fresh learner and leaves the page signed in on /learn. */
export async function registerLearner(page: Page, name = `E2E ${uid()}`) {
  const email = `e2e-${uid()}-${Date.now()}@example.com`;
  await page.goto("/register?next=/learn");
  await page.getByLabel("Full name").fill(name);
  await page.getByLabel("Work email").fill(email);
  await page.getByLabel("Password").fill(PASSWORD);
  await page.getByRole("button", { name: "Create account" }).click();
  await page.waitForURL((url) => url.pathname === "/learn");
  await expect(page.getByRole("heading", { level: 1 })).toContainText("Hi,");
  return { email, name };
}
