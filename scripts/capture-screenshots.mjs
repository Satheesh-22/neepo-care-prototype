import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import { createRequire } from "module";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const require = createRequire(path.resolve(__dirname, "../frontend/package.json"));
const { chromium } = require("@playwright/test");

const targetDir = process.env.SCREENSHOT_DIR || path.resolve(__dirname, "../qa/screenshots");
if (!fs.existsSync(targetDir)) {
  fs.mkdirSync(targetDir, { recursive: true });
}

async function capture() {
  const browser = await chromium.launch({ headless: true });

  // 1. Desktop Context
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    deviceScaleFactor: 2 // High-DPI capture
  });
  const page = await context.newPage();

  console.log("Capturing 1. Login Page...");
  await page.goto("http://localhost:3000/login", { waitUntil: "networkidle" });
  await page.waitForTimeout(600);
  await page.screenshot({ path: path.join(targetDir, "01_login_portal.png"), fullPage: true });

  console.log("Capturing 2. Operations Dashboard...");
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.waitForURL("http://localhost:3000/");
  await page.waitForTimeout(1000);
  await page.screenshot({ path: path.join(targetDir, "02_operations_dashboard.png"), fullPage: true });

  console.log("Capturing 3. Trip Decision Workspace...");
  await page.goto("http://localhost:3000/trips/00000000-0000-4000-8600-000000000001", { waitUntil: "networkidle" });
  await page.waitForTimeout(1200);
  await page.screenshot({ path: path.join(targetDir, "03_care_decision_workspace.png"), fullPage: true });

  console.log("Capturing 4. Driver Command Console...");
  await page.goto("http://localhost:3000/driver/trips/00000000-0000-4000-8600-000000000001", { waitUntil: "networkidle" });
  await page.waitForTimeout(1000);
  await page.screenshot({ path: path.join(targetDir, "04_driver_command_console.png"), fullPage: true });

  console.log("Capturing 5. Telemetry Simulator...");
  await page.goto("http://localhost:3000/simulator/trips/00000000-0000-4000-8600-000000000001", { waitUntil: "networkidle" });
  await page.waitForTimeout(1000);
  await page.screenshot({ path: path.join(targetDir, "05_telemetry_simulator.png"), fullPage: true });

  await context.close();

  // 2. Mobile Context (iPhone 14 / 390x844)
  console.log("Capturing 6. Mobile Responsive View...");
  const mobileContext = await browser.newContext({
    viewport: { width: 390, height: 844 },
    deviceScaleFactor: 2,
    isMobile: true
  });
  const mobilePage = await mobileContext.newPage();
  await mobilePage.goto("http://localhost:3000/login", { waitUntil: "networkidle" });
  await mobilePage.getByRole("button", { name: "Sign in" }).click();
  await mobilePage.waitForURL("http://localhost:3000/");
  await mobilePage.waitForTimeout(1000);
  await mobilePage.screenshot({ path: path.join(targetDir, "06_mobile_dashboard.png"), fullPage: true });

  console.log("Capturing 7. Mobile Driver Console...");
  await mobilePage.goto("http://localhost:3000/driver/trips/00000000-0000-4000-8600-000000000001", { waitUntil: "networkidle" });
  await mobilePage.waitForTimeout(1000);
  await mobilePage.screenshot({ path: path.join(targetDir, "07_mobile_driver_console.png"), fullPage: true });

  await mobileContext.close();
  await browser.close();

  console.log("All screenshots captured successfully to:", targetDir);
}

capture().catch((err) => {
  console.error("Capture failed:", err);
  process.exit(1);
});
