import { expect, test } from "@playwright/test";
import { login, registerLearner, uid } from "./helpers";

test("admin creates a team and promotes a new hire into it", async ({ browser }) => {
  const hireCtx = await browser.newContext();
  const hire = await hireCtx.newPage();
  const { name } = await registerLearner(hire, `New Hire ${uid()}`);

  const page = await (await browser.newContext()).newPage();
  await login(page, "admin@demo.dev", "/admin");
  await page.getByRole("link", { name: "Teams" }).click();
  const team = `Platform ${uid()}`;
  await page.getByLabel("Name", { exact: true }).fill(team);
  await page.getByLabel("Manager", { exact: true }).selectOption({ label: "Mia Manager" });
  await page.getByRole("button", { name: "Create team" }).click();
  await expect
    .poll(() => page.getByRole("textbox", { name: "Team name" }).evaluateAll((els) => els.map((e) => (e as HTMLInputElement).value)))
    .toContain(team);

  await page.getByRole("link", { name: "Users" }).click();
  await page.getByLabel("Name or email").fill(name);
  await page.getByRole("button", { name: "Search" }).click();
  await page.getByLabel(`Role for ${name}`).selectOption("INSTRUCTOR");
  await page.getByLabel(`Team for ${name}`).selectOption({ label: team });
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(page.getByRole("status")).toHaveText("Saved");

  // The role change revoked the hire's refresh token: once the access cookie is gone, they're signed out.
  await hireCtx.clearCookies({ name: "ocs_token" });
  await hire.goto("/learn");
  await expect(hire.getByText("Sign in to see your courses")).toBeVisible();
});
