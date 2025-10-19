// Regenerates docs/screenshots from a running stack seeded with the demo data.
// Usage: (cd e2e && npm ci) && node scripts/screenshots.mjs [base-url]
import { createRequire } from "node:module";
const require = createRequire(new URL("../e2e/package.json", import.meta.url));
const { chromium, devices } = require("@playwright/test");

const BASE = process.argv[2] ?? "http://localhost:3000";
const OUT = new URL("../docs/screenshots/", import.meta.url).pathname;
const browser = await chromium.launch({ executablePath: process.env.CHROME_PATH || undefined, args: ["--no-sandbox"] });

async function session(email, opts = {}) {
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 860 }, deviceScaleFactor: 1, ...opts });
  const page = await ctx.newPage();
  if (email) {
    await page.goto(`${BASE}/login?next=/learn`);
    await page.getByLabel("Work email").fill(email);
    await page.getByLabel("Password").fill("password123");
    await page.getByRole("button", { name: "Sign in" }).click();
    await page.waitForURL(`${BASE}/learn`);
  }
  return page;
}
const shot = async (page, name, full = true) => {
  await page.waitForLoadState("networkidle");
  await page.screenshot({ path: `${OUT}${name}.png`, fullPage: full });
  console.log("saved", name);
};

let p = await session(null);
await p.goto(BASE + "/");
await shot(p, "home");
await p.goto(BASE + "/courses/sql-for-analysts");
await shot(p, "course");

p = await session("learner@demo.dev");
await shot(p, "dashboard");
await p.goto(BASE + "/courses/workplace-security-essentials");
await p.getByRole("link", { name: /Continue learning|Review course/ }).click();
await p.goto(BASE + p.url().replace(BASE, "").replace(/\/\d+$/, "") + "/" + (await p.evaluate(async () => {
  const d = await (await fetch("/backend/courses/workplace-security-essentials")).json();
  return d.chapters[0].id;
})));
await p.getByRole("group").first().getByRole("radio").nth(2).check();
await shot(p, "quiz");
if (process.env.CERT) {
  await p.goto(`${BASE}/certificates/${process.env.CERT}`);
  await shot(p, "certificate", false);
}

p = await session("admin@demo.dev");
await p.goto(BASE + "/admin");
await shot(p, "admin-overview");
await p.goto(BASE + "/admin/assignments?status=OVERDUE");
await shot(p, "admin-assignments");
await p.goto(BASE + "/studio");
await p.getByRole("link", { name: "SQL for Analysts" }).click();
await p.getByRole("link", { name: /Indexes and query plans/ }).click();
await p.getByRole("link", { name: /Edit quiz/ }).click();
await shot(p, "studio-quiz");

const phone = await browser.newContext({ ...devices["Pixel 7"] });
p = await phone.newPage();
await p.goto(BASE + "/courses/sql-for-analysts");
await p.getByRole("button", { name: "Menu" }).click();
await shot(p, "mobile", false);

await browser.close();
