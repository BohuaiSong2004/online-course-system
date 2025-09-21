import { expect, test } from "@playwright/test";
import { registerLearner } from "./helpers";

test("learner enrolls, fails then passes a quiz, completes the course and gets a certificate", async ({ page }) => {
  const { name } = await registerLearner(page);

  await page.goto("/courses/workplace-security-essentials");
  await page.getByRole("button", { name: "Enroll for free" }).click();
  await page.waitForURL(/\/learn\/workplace-security-essentials\/\d+$/);
  await expect(page.getByRole("heading", { name: "Spotting phishing" })).toBeVisible();
  await expect(page.getByText("Pass the quiz to complete this chapter")).toBeVisible();

  // Wrong answers: fail, no answer key revealed
  const q = (n: number) => page.getByRole("group").nth(n);
  await q(0).getByLabel("Reply with your password").check();
  await q(1).getByLabel("it@yourcompany.com").check();
  await q(2).getByLabel("Restart and hope").check();
  await page.getByRole("button", { name: "Submit answers" }).click();
  await expect(page.getByText(/You scored 0\/3/)).toBeVisible();
  await expect(page.getByText(/You need 70% to pass/)).toBeVisible();

  // Correct answers: pass, explanations shown, chapter completes
  await q(0).getByLabel("Report it with the Report Phishing button").check();
  await q(1).getByLabel("it@yourcompany-support.net").check();
  await q(2).getByLabel("Disconnect from the network and contact security").check();
  await page.getByRole("button", { name: "Submit answers" }).click(); // changing an answer resets the result
  await expect(page.getByText(/You scored 3\/3/)).toBeVisible();
  await expect(page.getByText(/IT never asks for passwords/)).toBeVisible();

  await page.getByRole("link", { name: "Next chapter →" }).click();
  for (let i = 0; i < 2; i++) {
    await page.getByRole("button", { name: "Mark complete & continue" }).click();
    if (i === 0) await expect(page.getByRole("heading", { name: "Reporting an incident" })).toBeVisible();
  }
  await expect(page.getByText("Course complete!")).toBeVisible();

  await page.getByRole("link", { name: "View your certificate" }).click();
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(name);
  await expect(page.getByText("Workplace Security Essentials")).toBeVisible();
  await expect(page.getByText("✓ Verified certificate")).toBeVisible();

  await page.goto("/learn");
  await expect(page.getByRole("link", { name: "🎓 Certificate" })).toBeVisible();
  await expect(page.getByText("Passed the quiz in")).toBeVisible();
});

test("locked chapters require enrollment; previews are public", async ({ page }) => {
  await page.goto("/courses/sql-for-analysts");
  await page.getByRole("link", { name: "SELECT, WHERE, ORDER BY" }).click();
  await expect(page.getByText("Free preview")).toBeVisible();
  await expect(page.getByRole("link", { name: "Enroll to track progress" })).toBeVisible();
});

test("account: change password signs the old password out", async ({ page }) => {
  const { email } = await registerLearner(page);
  await page.goto("/learn/account");
  await page.getByLabel("Current password").fill("password123");
  await page.getByLabel("New password", { exact: true }).fill("better-password-1");
  await page.getByLabel("Confirm new password").fill("better-password-1");
  await page.getByRole("button", { name: "Change password" }).click();
  await expect(page.getByText("Password changed")).toBeVisible();

  const res = await page.request.post("/backend/auth/login", { data: { email, password: "password123" } });
  expect(res.status()).toBe(401);
});
