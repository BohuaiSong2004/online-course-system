import { expect, test } from "@playwright/test";

test("home page lists courses and links into the catalog", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { level: 1 })).toContainText("Onboard faster");
  await expect(page.getByRole("heading", { name: "Newest courses" })).toBeVisible();
  // Courses render even if this ISR page was generated while the API was down (browser fallback).
  await expect(page.getByRole("heading", { level: 3 }).first()).toBeVisible();
  await page.getByRole("link", { name: /^Browse( \d+)? courses$/ }).click();
  await expect(page).toHaveURL(/\/courses$/);
});

test("catalog search uses full-text search and filters", async ({ page }) => {
  await page.goto("/courses");
  await page.getByLabel("Search courses").fill("phishing");
  await page.getByRole("button", { name: "Search" }).click();
  await expect(page.getByRole("heading", { level: 3 })).toHaveText(["Workplace Security Essentials"]);

  await page.goto("/courses?category=Data");
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Data courses");
});

test("course page is SEO-ready: title, description, canonical, JSON-LD", async ({ page }) => {
  await page.goto("/courses/sql-for-analysts");
  await expect(page).toHaveTitle("SQL for Analysts · OpenCourse");
  await expect(page.locator('meta[name="description"]')).toHaveAttribute("content", /window functions/i);
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute("href", /\/courses\/sql-for-analysts$/);
  const ld = JSON.parse((await page.locator('script[type="application/ld+json"]').textContent()) ?? "{}");
  expect(ld["@type"]).toBe("Course");
  expect(ld.name).toBe("SQL for Analysts");
  await expect(page.getByRole("link", { name: "Sign in to enroll" })).toBeVisible();
  await expect(page.getByText("Free preview")).toBeVisible();
});

test("sitemap and robots", async ({ request }) => {
  const sitemap = await (await request.get("/sitemap.xml")).text();
  expect(sitemap).toContain("/courses/sql-for-analysts");
  const robots = await (await request.get("/robots.txt")).text();
  expect(robots).toContain("Disallow: /admin");
});

test("unknown course and certificate return 404 pages", async ({ page }) => {
  expect((await page.goto("/courses/does-not-exist"))?.status()).toBe(404);
  expect((await page.goto("/certificates/AAAAAAAAAAAA"))?.status()).toBe(404);
});
