import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";
import { login } from "./helpers";

async function audit(page: import("@playwright/test").Page) {
  const results = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21aa"]).analyze();
  const serious = results.violations.filter((v) => v.impact === "serious" || v.impact === "critical");
  expect(serious.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(" ")).join(", ")}`)).toEqual([]);
}

for (const path of ["/", "/courses", "/courses/sql-for-analysts", "/login", "/register"]) {
  test(`no serious accessibility violations on ${path}`, async ({ page }) => {
    await page.goto(path);
    await page.waitForLoadState("networkidle");
    await audit(page);
  });
}

test("no serious accessibility violations in signed-in areas", async ({ page }) => {
  await login(page, "admin@demo.dev", "/learn");
  for (const path of ["/learn", "/learn/account", "/studio", "/admin", "/admin/assignments", "/admin/users"]) {
    await page.goto(path);
    await page.waitForLoadState("networkidle");
    await audit(page);
  }
});
