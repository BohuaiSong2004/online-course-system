import { expect, test } from "@playwright/test";
import { login, uid } from "./helpers";

test("instructor builds a course with video and quiz, then publishes it", async ({ page }) => {
  const title = `E2E Git Basics ${uid()}`;
  await login(page, "instructor@demo.dev", "/studio");
  await page.getByRole("link", { name: "+ New course" }).click();
  await page.getByLabel("Title").fill(title);
  await page.getByLabel(/^Summary/).fill("Branches, commits and pull requests.");
  await page.getByLabel("Category").fill("Engineering");
  await page.getByRole("button", { name: "Create course" }).click();
  await page.waitForURL(/\/studio\/courses\/\d+$/);

  await page.getByRole("button", { name: "Publish course" }).click();
  await expect(page.getByText("Add at least one chapter before publishing")).toBeVisible();

  await page.getByRole("link", { name: "+ Add chapter" }).click();
  await page.waitForURL(/chapters\/new$/);
  await page.getByLabel("Chapter title").fill("Your first commit");
  await page.getByLabel(/^Video URL/).fill("https://www.youtube.com/watch?v=aqz-KE-bpKQ");
  await page.getByLabel("Chapter content").fill("## Commit\n\n```bash\ngit commit -m \"first\"\n```");
  await page.getByRole("button", { name: "Preview" }).click();
  await expect(page.locator(".prose pre")).toContainText("git commit");
  await page.getByRole("button", { name: "Add chapter" }).click();
  await page.waitForURL(/\/studio\/courses\/\d+$/);

  await page.getByRole("link", { name: /Your first commit/ }).click();
  await page.getByRole("link", { name: "Add a quiz" }).click();
  await page.getByRole("button", { name: "+ Add question" }).click();
  await page.getByLabel("Question 1").fill("Which command records a snapshot?");
  await page.getByLabel("Option 1", { exact: true }).fill("git commit");
  await page.getByLabel("Option 2", { exact: true }).fill("git push");
  await page.getByRole("button", { name: "Save quiz" }).click();
  await expect(page.getByText("Saved ✓")).toBeVisible();
  await page.getByRole("link", { name: "Done" }).click();
  await expect(page.getByText("quiz · 1")).toBeVisible();

  await page.getByRole("button", { name: "Publish course" }).click();
  await expect(page.getByRole("button", { name: "Unpublish" })).toBeVisible();

  await page.goto(`/courses?q=${encodeURIComponent(title.split(" ").pop()!)}`);
  await expect(page.getByRole("heading", { name: title })).toBeVisible();
  await page.getByRole("heading", { name: title }).click();
  await expect(page.getByText("video · quiz (1 questions)")).toBeVisible();
});
