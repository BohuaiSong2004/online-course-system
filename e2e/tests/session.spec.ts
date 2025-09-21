import { expect, test } from "@playwright/test";
import { login } from "./helpers";

test("expired access token is refreshed transparently (SSR and browser calls)", async ({ page, context }) => {
  await login(page, "learner@demo.dev", "/learn");
  const before = (await context.cookies()).find((c) => c.name === "ocs_refresh")?.value;

  await context.clearCookies({ name: "ocs_token" });
  await page.goto("/learn");
  await expect(page.getByRole("heading", { level: 1 })).toContainText("Hi, Leo");
  const cookies = await context.cookies();
  expect(cookies.find((c) => c.name === "ocs_token")).toBeTruthy();
  expect(cookies.find((c) => c.name === "ocs_refresh")?.value).not.toBe(before); // rotated

  await context.clearCookies({ name: "ocs_token" });
  const status = await page.evaluate(async () => (await fetch("/backend/me/enrollments")).status);
  expect(status).toBe(401); // raw call fails…
  await page.goto("/courses/sql-for-analysts");
  await expect(page.getByRole("link", { name: /Continue learning|Review course/ })).toBeVisible(); // …the app recovers
});

test("sign out clears the session", async ({ page, context }) => {
  await login(page, "learner@demo.dev", "/learn");
  await page.getByRole("button", { name: "Sign out" }).click();
  await page.waitForURL("/");
  expect((await context.cookies()).filter((c) => c.name.startsWith("ocs_"))).toHaveLength(0);
});
