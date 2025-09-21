import { expect, test } from "@playwright/test";

async function noHorizontalScroll(page: import("@playwright/test").Page) {
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
  expect(overflow).toBeLessThanOrEqual(1);
}

test("pages fit a phone screen and the menu opens", async ({ page }) => {
  for (const path of ["/", "/courses", "/courses/sql-for-analysts", "/login"]) {
    await page.goto(path);
    await noHorizontalScroll(page);
  }
  await page.goto("/");
  await page.getByRole("button", { name: "Menu" }).click();
  await expect(page.locator("#mobile-nav").getByRole("link", { name: "Courses" })).toBeVisible();
});
