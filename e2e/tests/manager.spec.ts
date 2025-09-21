import { expect, test } from "@playwright/test";
import { login, registerLearner, uid } from "./helpers";

test("manager sees team reports, assigns a course with a due date and exports CSV", async ({ page }) => {
  await login(page, "manager@demo.dev", "/admin");
  await expect(page.getByText("Teams you manage")).toBeVisible();
  await expect(page.getByText("Completion rate")).toBeVisible();
  await expect(page.getByRole("heading", { name: "By team" })).toBeVisible();
  await expect(page.getByRole("cell", { name: /Engineering/ })).toBeVisible();
  await expect(page.getByRole("cell", { name: /People Ops/ })).toHaveCount(0); // not Mia's team
  await expect(page.getByRole("link", { name: "Users" })).toHaveCount(0); // admin-only tab

  const csv = await page.request.get("/backend/admin/reports/assignments.csv");
  expect(csv.headers()["content-type"]).toContain("text/csv");
  expect(await csv.text()).toMatch(/^learner,email,team,course,status/);

  await page.getByRole("link", { name: "Assign a course" }).click();
  await page.waitForURL(/\/admin\/assignments$/);
  const form = page.locator("form").filter({ has: page.getByRole("button", { name: "Assign", exact: true }) });
  await form.getByLabel("Course", { exact: true }).selectOption({ label: "SQL for Analysts" });
  const sales = await form.getByLabel("Team", { exact: true }).locator("option", { hasText: "Sales" }).getAttribute("value");
  await form.getByLabel("Team", { exact: true }).selectOption(sales!);
  const due = new Date(Date.now() + 21 * 86_400_000).toISOString().slice(0, 10);
  await form.getByLabel(/^Due date/).fill(due);
  await form.getByRole("button", { name: "Assign", exact: true }).click();
  await expect(page.getByRole("status")).toContainText(/assigned/);

  await page.getByRole("combobox", { name: "Course" }).last().selectOption({ label: "SQL for Analysts" });
  await page.getByRole("combobox", { name: "Team" }).last().selectOption({ label: "Sales" });
  await page.getByRole("button", { name: "Filter" }).click();
  await expect(page).toHaveURL(/courseId=\d+.*teamId=\d+/);
  const emma = page.getByRole("row").filter({ hasText: "Emma Kim" });
  await expect(emma).toHaveCount(1);
  await expect(emma).toContainText("SQL for Analysts");
});

test("learners can't open the admin area", async ({ page }) => {
  await registerLearner(page, `Nosy ${uid()}`);
  await page.goto("/admin");
  await expect(page.getByText("Managers and admins only")).toBeVisible();
});
