import { expect, test } from "@playwright/test";

test("dispatcher reaches the recovery workspace", async ({ page }) => {
  await page.goto("/login");
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page.getByText("Priority trip")).toBeVisible();
  await page.getByRole("link", { name: "Open recovery workspace" }).click();
  await expect(page.getByText("Recovery alternatives")).toBeVisible();
  await expect(page.getByText("SAFE PARKING MOBILE MECHANIC")).toBeVisible();
});

test("driver follows recovery instructions and queues offline telemetry", async ({ page }) => {
  await page.goto("/login");
  await page.getByLabel("Demo role").click();
  await page.getByRole("option", { name: "Driver" }).click();
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.getByRole("link", { name: "Open driver trip" }).click();

  await expect(page.getByText("Next safe action:")).toBeVisible();
  await page.getByLabel("Reported network state").click();
  await page.getByRole("option", { name: "OFFLINE" }).click();
  await page.getByRole("button", { name: "Save observation offline" }).click();

  await expect(page.getByText("Observation saved securely on this device and queued for synchronization.")).toBeVisible();
  await expect(page.getByText("1 queued")).toBeVisible();
  await page.getByRole("button", { name: "Synchronize now" }).click();
  await expect(page.getByText("1 queued")).not.toBeVisible();
});

test("driver sees emergency and safely parked adaptive telemetry intervals", async ({ page }) => {
  await page.goto("/login");
  await page.getByLabel("Demo role").click();
  await page.getByRole("option", { name: "Driver" }).click();
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.getByRole("link", { name: "Open driver trip" }).click();

  await page.getByRole("button", { name: "Transmit observation" }).click();
  await expect(page.getByText(/BREAKDOWN OR EMERGENCY · next upload in 3 seconds/)).toBeVisible();
  await page.getByRole("checkbox", { name: "Vehicle is safely parked" }).check();
  await page.getByRole("button", { name: "Transmit observation" }).click();
  await expect(page.getByText(/SAFELY PARKED · next upload in 120 seconds/)).toBeVisible();
});
