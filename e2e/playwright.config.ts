import { defineConfig, devices } from "@playwright/test";

/**
 * Runs against a live stack (API + all zones) at E2E_BASE_URL, seeded with the demo data.
 * Start it with `docker compose up` or the dev commands in the README. Tests create their own
 * users and courses, so they can be re-run against the same database.
 */
const executablePath = process.env.CHROME_PATH || undefined;

export default defineConfig({
  testDir: "./tests",
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [["github"], ["html", { open: "never" }]] : [["list"]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:3000",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    launchOptions: { executablePath, args: executablePath ? ["--no-sandbox"] : [] },
  },
  projects: [
    { name: "desktop", use: { ...devices["Desktop Chrome"] }, testIgnore: /mobile\.spec/ },
    { name: "mobile", use: { ...devices["Pixel 7"] }, testMatch: /mobile\.spec/ },
  ],
});
